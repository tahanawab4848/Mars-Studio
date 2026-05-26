package mars.tools;

import mars.mips.hardware.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Observable;

/**
 * Stack Frame Visualizer for MARS.
 *
 * Observes the $sp register (Register 29) and memory writes to the stack
 * segment. Draws an animated stack growing downward, highlighting the
 * current stack pointer and labelling each word that has been pushed.
 *
 * HOW TO USE:
 *  1. Open the tool from the Tools menu.
 *  2. Click "Connect to MIPS".
 *  3. Assemble and run (or Step through) your MIPS program.
 *  Any change to $sp or stack memory is instantly reflected.
 */
public class StackVisualizer extends AbstractMarsToolAndApplication {

    private static final String NAME    = "Stack Frame Visualizer";
    private static final String VERSION = "Version 1.0";
    private static final String HEADING = "Animated MIPS Stack Visualizer";

    // MIPS convention: stack starts at 0x7FFFEFFC and grows downward
    private static final int STACK_BASE = Memory.stackPointer; // high address (bottom of visual)
    private static final int SP_REG     = 29;
    private static final int VISIBLE_WORDS = 20;   // cells shown at once

    // ---- state ----
    private int   currentSP   = STACK_BASE;
    private int   previousSP  = STACK_BASE;
    private int[] stackMem    = new int[512];   // shadow of top 512 words of stack
    private int   lastChanged = -1;             // word-index recently written
    private float pulseAlpha  = 0f;

    // ---- animation ----
    private javax.swing.Timer pulseTimer;

    // ---- gui ----
    private StackPanel stackPanel;
    private JLabel     spLabel, sizeLabel, statusLabel;

    // ---- colours ----
    private static final Color BG       = new Color(0x1E1E2E);
    private static final Color CELL_BG  = new Color(0x313244);
    private static final Color SP_LINE  = new Color(0xF38BA8);   // red — current SP
    private static final Color PUSHED   = new Color(0xA6E3A1);   // green — recently pushed
    private static final Color NORMAL   = new Color(0x89B4FA);   // blue — older data
    private static final Color ADDR_FG  = new Color(0x6C7086);
    private static final Color VAL_FG   = new Color(0xCDD6F4);
    private static final Color LABEL_FG = new Color(0xF9E2AF);

    // ============================================================
    //  Constructors
    // ============================================================

    public StackVisualizer(String title, String heading) { super(title, heading); }

    public StackVisualizer() { super(NAME + ", " + VERSION, HEADING); }

    @Override public String getName() { return NAME; }

    // ============================================================
    //  Build GUI
    // ============================================================

    @Override
    protected JComponent buildMainDisplayArea() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Header info bar
        root.add(buildInfoBar(), BorderLayout.NORTH);

        // Scrollable stack panel
        stackPanel = new StackPanel();
        stackPanel.setPreferredSize(new Dimension(500, 480));
        JScrollPane scroll = new JScrollPane(stackPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(BG);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0x45475A), 1));
        root.add(scroll, BorderLayout.CENTER);

        // Status bar
        statusLabel = new JLabel("Connect to MIPS to start observing the stack.");
        statusLabel.setForeground(ADDR_FG);
        statusLabel.setFont(new Font("Monospaced", Font.PLAIN, 11));
        statusLabel.setBorder(new EmptyBorder(4, 6, 4, 6));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        south.setBackground(new Color(0x181825));
        south.add(statusLabel);
        root.add(south, BorderLayout.SOUTH);

        // Pulse animation
        pulseTimer = new javax.swing.Timer(30, e -> {
            pulseAlpha -= 0.04f;
            if (pulseAlpha <= 0f) {
                pulseAlpha = 0f;
                ((javax.swing.Timer) e.getSource()).stop();
            }
            stackPanel.repaint();
        });

        return root;
    }

    private JPanel buildInfoBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 18, 4));
        bar.setBackground(new Color(0x181825));
        bar.setBorder(new EmptyBorder(4, 4, 4, 4));

        spLabel   = makeInfo("$sp = 0x" + Integer.toHexString(STACK_BASE).toUpperCase());
        sizeLabel = makeInfo("Frame size: 0 bytes");

        bar.add(colorDot(SP_LINE));
        bar.add(makeInfo("$sp (current)"));
        bar.add(colorDot(PUSHED));
        bar.add(makeInfo("Recently pushed"));
        bar.add(colorDot(NORMAL));
        bar.add(makeInfo("Stack data"));
        bar.add(Box.createHorizontalStrut(20));
        bar.add(spLabel);
        bar.add(Box.createHorizontalStrut(12));
        bar.add(sizeLabel);
        return bar;
    }

    private JLabel makeInfo(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(VAL_FG);
        l.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return l;
    }

    private JLabel colorDot(Color c) {
        JLabel dot = new JLabel("●");
        dot.setForeground(c);
        dot.setFont(new Font("Dialog", Font.PLAIN, 12));
        return dot;
    }

    // ============================================================
    //  MIPS Observer
    // ============================================================

    @Override
    protected void addAsObserver() {
        // Watch $sp register
        addAsObserver(RegisterFile.getRegisters()[SP_REG]);
        // Watch stack memory (top 512 words = 2048 bytes)
        int lo = STACK_BASE - 512 * 4;
        addAsObserver(lo, STACK_BASE);
    }

    @Override
    protected void deleteAsObserver() {
        deleteAsObserver(RegisterFile.getRegisters()[SP_REG]);
        super.deleteAsObserver();
    }

    @Override
    protected void processMIPSUpdate(Observable resource, AccessNotice notice) {
        if (!notice.accessIsFromMIPS()) return;

        if (resource instanceof Register) {
            // $sp changed — read its current value directly from the register file
            RegisterAccessNotice rn = (RegisterAccessNotice) notice;
            if (!rn.getRegisterName().equals("$sp")) return;
            previousSP = currentSP;
            try {
                currentSP = RegisterFile.getRegisters()[SP_REG].getValueNoNotify();
            } catch (Exception ex) {
                return;   // register file not ready yet — skip safely
            }
            // Sanity-check: ignore clearly invalid SP values
            if (currentSP == 0 || (currentSP > 0 && currentSP < Memory.dataBaseAddress)) return;
            int frameSize = previousSP - currentSP;   // positive when growing down
            final int spSnap   = currentSP;
            final int fsSnap   = frameSize;

            SwingUtilities.invokeLater(() -> {
                spLabel.setText("$sp = 0x" + Integer.toHexString(spSnap).toUpperCase());
                sizeLabel.setText("\u0394 = " + fsSnap + " bytes");
                String dir = fsSnap > 0 ? "\u25bc Push (" + fsSnap + " B)" :
                             fsSnap < 0 ? "\u25b2 Pop ("  + (-fsSnap) + " B)" : "No change";
                statusLabel.setText(dir);
                stackPanel.repaint();
            });

        } else {
            // Stack memory written
            if (notice.getAccessType() != AccessNotice.WRITE) return;
            MemoryAccessNotice mn = (MemoryAccessNotice) notice;
            int addr = mn.getAddress();
            int idx  = (STACK_BASE - addr) / 4;
            if (idx < 0 || idx >= stackMem.length) return;
            try {
                stackMem[idx] = mn.getValue();
            } catch (Exception ex) {
                return;
            }
            lastChanged = idx;
            pulseAlpha  = 1.0f;

            SwingUtilities.invokeLater(() -> {
                if (!pulseTimer.isRunning()) pulseTimer.start();
                stackPanel.repaint();
            });
        }
    }

    @Override
    protected void initializePreGUI() {
        currentSP  = STACK_BASE;
        previousSP = STACK_BASE;
        stackMem   = new int[512];
        lastChanged = -1;
        pulseAlpha  = 0f;
    }

    @Override
    protected void reset() {
        initializePreGUI();
        if (pulseTimer != null) pulseTimer.stop();
        SwingUtilities.invokeLater(() -> {
            spLabel.setText("$sp = 0x" + Integer.toHexString(STACK_BASE).toUpperCase());
            sizeLabel.setText("Frame size: 0 bytes");
            statusLabel.setText("Reset — waiting…");
            stackPanel.repaint();
        });
    }

    // ============================================================
    //  Inner painting panel
    // ============================================================

    private class StackPanel extends JPanel {

        private static final int CELL_H  = 36;
        private static final int CELL_W  = 420;
        private static final int PAD_X   = 10;
        private static final int PAD_Y   = 10;
        private static final int ARC     = 6;

        public StackPanel() {
            setBackground(BG);
            setDoubleBuffered(true);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(CELL_W + PAD_X * 2, VISIBLE_WORDS * CELL_H + PAD_Y * 2 + 30);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g2.setColor(BG);
            g2.fillRect(0, 0, getWidth(), getHeight());

            // Title
            g2.setFont(new Font("Monospaced", Font.BOLD, 13));
            g2.setColor(LABEL_FG);
            g2.drawString("  HIGH ADDRESS (Stack Base = 0x" +
                    Integer.toHexString(STACK_BASE).toUpperCase() + ")", PAD_X, PAD_Y + 14);

            // How many words of stack are "in use" (from STACK_BASE down to currentSP)
            int wordsInUse = (STACK_BASE - currentSP) / 4;
            int show       = Math.min(Math.max(wordsInUse + 4, VISIBLE_WORDS), stackMem.length);

            int spWordIdx  = wordsInUse;   // index into stackMem where $sp sits

            for (int i = 0; i < show; i++) {
                int addr = STACK_BASE - i * 4;
                int y    = PAD_Y + 22 + i * CELL_H;
                int x    = PAD_X;

                boolean isSP      = (i == spWordIdx);
                boolean isChanged = (i == lastChanged);
                boolean isUsed    = (i < wordsInUse);

                // Cell background
                Color cellColor = CELL_BG;
                if (isSP) {
                    cellColor = SP_LINE.darker().darker();
                } else if (isChanged && pulseAlpha > 0) {
                    cellColor = blend(CELL_BG, PUSHED.darker(), pulseAlpha * 0.6f);
                } else if (isUsed) {
                    cellColor = new Color(0x2A2A3E);
                }

                g2.setColor(cellColor);
                g2.fillRoundRect(x, y, CELL_W, CELL_H - 2, ARC, ARC);

                // Left-side accent bar
                Color accent = isUsed ? NORMAL : CELL_BG;
                if (isSP)          accent = SP_LINE;
                else if (isChanged && pulseAlpha > 0) accent = blend(NORMAL, PUSHED, pulseAlpha);
                g2.setColor(accent);
                g2.fillRoundRect(x, y, 4, CELL_H - 2, ARC, ARC);

                // Address text
                g2.setColor(ADDR_FG);
                g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
                g2.drawString("0x" + String.format("%08X", addr), x + 10, y + 14);

                // Value text
                g2.setColor(isUsed ? VAL_FG : ADDR_FG.darker());
                g2.setFont(new Font("Monospaced", Font.BOLD, 12));
                String val = isUsed ? String.format("0x%08X  (%d)", stackMem[i], stackMem[i]) : "—";
                g2.drawString(val, x + 150, y + 14);

                // $sp arrow
                if (isSP) {
                    g2.setColor(SP_LINE);
                    g2.setFont(new Font("Dialog", Font.BOLD, 14));
                    g2.drawString("◄ $sp", x + CELL_W - 70, y + 17);
                }

                // divider
                g2.setColor(new Color(0x45475A));
                g2.drawLine(x, y + CELL_H - 2, x + CELL_W, y + CELL_H - 2);
            }

            // Footer
            g2.setFont(new Font("Monospaced", Font.BOLD, 13));
            g2.setColor(LABEL_FG);
            int footerY = PAD_Y + 22 + show * CELL_H + 14;
            g2.drawString("  LOW ADDRESS (Stack grows ↓)", PAD_X, footerY);

            g2.dispose();
        }

        private Color blend(Color a, Color b, float t) {
            float s = 1 - t;
            return new Color(
                    Math.min(255, (int)(a.getRed()   * s + b.getRed()   * t)),
                    Math.min(255, (int)(a.getGreen() * s + b.getGreen() * t)),
                    Math.min(255, (int)(a.getBlue()  * s + b.getBlue()  * t))
            );
        }
    }
}
