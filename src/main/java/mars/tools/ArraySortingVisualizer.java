package mars.tools;

import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.RoundRectangle2D;
import java.util.Observable;

/**
 * Array Sorting Visualizer for MARS.
 *
 * Displays values stored in MIPS memory as an animated bar chart.
 * When your MIPS program writes to an array in the data segment,
 * this tool visually shows the values as bars and highlights
 * any memory writes in real-time — perfect for watching a
 * sorting algorithm in action!
 *
 * To use: Store an array starting at 0x10010000 (default .data address).
 * Open this tool, click "Connect to MIPS", then run your program.
 */
public class ArraySortingVisualizer extends AbstractMarsToolAndApplication {

    private static final String NAME    = "Array Sorting Visualizer";
    private static final String VERSION = "Version 1.0";
    private static final String HEADING = "Animated Array / Memory Visualizer";

    // How many words to display (configurable via spinner)
    private static final int DEFAULT_ARRAY_SIZE = 16;
    private static final int MAX_ARRAY_SIZE      = 64;

    // Base address watched (start of .data segment by default)
    private static final int BASE_ADDRESS = Memory.dataSegmentBaseAddress;

    // ---- State ----
    private int   arraySize   = DEFAULT_ARRAY_SIZE;
    private int[] values      = new int[MAX_ARRAY_SIZE];
    private int   lastWriteIdx = -1;   // index most recently written (for highlight)
    private int   compareIdx   = -1;   // secondary highlight

    // ---- Animation timer ----
    private javax.swing.Timer fadeTimer;
    private float highlightAlpha = 0f;

    // ---- GUI components ----
    private BarChartPanel chartPanel;
    private JLabel        statusLabel;
    private JSpinner      sizeSpinner;
    private JLabel        minLabel, maxLabel, writesLabel;
    private int           totalWrites = 0;

    // ---- Gradient colours (dark-mode friendly) ----
    private static final Color BAR_BASE_TOP    = new Color(0x5B86E5);
    private static final Color BAR_BASE_BOT    = new Color(0x36D1DC);
    private static final Color BAR_WRITE_TOP   = new Color(0xFF6B6B);
    private static final Color BAR_WRITE_BOT   = new Color(0xFFE66D);
    private static final Color BAR_COMPARE_TOP = new Color(0xA8EDEA);
    private static final Color BAR_COMPARE_BOT = new Color(0xFED6E3);
    private static final Color BG_COLOR        = new Color(0x1E1E2E);
    private static final Color GRID_COLOR      = new Color(0x44475A);
    private static final Color TEXT_COLOR       = new Color(0xCDD6F4);

    // ========================================================
    //  Constructors
    // ========================================================

    public ArraySortingVisualizer(String title, String heading) {
        super(title, heading);
    }

    public ArraySortingVisualizer() {
        super(NAME + ", " + VERSION, HEADING);
    }

    @Override
    public String getName() { return NAME; }

    // ========================================================
    //  Build GUI
    // ========================================================

    @Override
    protected JComponent buildMainDisplayArea() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(BG_COLOR);
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ---- chart ----
        chartPanel = new BarChartPanel();
        chartPanel.setPreferredSize(new Dimension(640, 320));
        root.add(chartPanel, BorderLayout.CENTER);

        // ---- controls row (north) ----
        root.add(buildControlBar(), BorderLayout.NORTH);

        // ---- status row (south) ----
        root.add(buildStatusBar(), BorderLayout.SOUTH);

        // ---- fade-out animation for the write highlight ----
        fadeTimer = new javax.swing.Timer(30, e -> {
            highlightAlpha -= 0.05f;
            if (highlightAlpha <= 0f) {
                highlightAlpha = 0f;
                lastWriteIdx = -1;
                ((javax.swing.Timer) e.getSource()).stop();
            }
            chartPanel.repaint();
        });

        return root;
    }

    private JPanel buildControlBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        bar.setBackground(BG_COLOR);

        JLabel sizeLabel = new JLabel("Array Size:");
        sizeLabel.setForeground(TEXT_COLOR);

        SpinnerNumberModel model = new SpinnerNumberModel(DEFAULT_ARRAY_SIZE, 2, MAX_ARRAY_SIZE, 1);
        sizeSpinner = new JSpinner(model);
        sizeSpinner.setPreferredSize(new Dimension(60, 24));
        sizeSpinner.addChangeListener(e -> {
            arraySize = (Integer) sizeSpinner.getValue();
            chartPanel.repaint();
        });

        JLabel addrLabel = new JLabel("  Base address: 0x" + Integer.toHexString(BASE_ADDRESS));
        addrLabel.setForeground(new Color(0x6C7086));

        bar.add(sizeLabel);
        bar.add(sizeSpinner);
        bar.add(addrLabel);
        return bar;
    }

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 4));
        bar.setBackground(new Color(0x181825));
        bar.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        statusLabel = makeStatusLabel("Waiting for connection…");
        minLabel    = makeStatusLabel("Min: —");
        maxLabel    = makeStatusLabel("Max: —");
        writesLabel = makeStatusLabel("Writes: 0");

        bar.add(statusLabel);
        bar.add(new JSeparator(JSeparator.VERTICAL));
        bar.add(minLabel);
        bar.add(maxLabel);
        bar.add(writesLabel);
        return bar;
    }

    private JLabel makeStatusLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_COLOR);
        l.setFont(new Font("Monospaced", Font.PLAIN, 12));
        return l;
    }

    // ========================================================
    //  MIPS Observer
    // ========================================================

    @Override
    protected void addAsObserver() {
        // Watch the data segment starting from BASE_ADDRESS
        addAsObserver(BASE_ADDRESS, BASE_ADDRESS + MAX_ARRAY_SIZE * 4);
        
        // Read initial values from memory so the chart isn't empty before execution
        try {
            for (int i = 0; i < MAX_ARRAY_SIZE; i++) {
                values[i] = mars.Globals.memory.getRawWord(BASE_ADDRESS + i * 4);
            }
        } catch (Exception ignored) {
            // Memory might not be allocated yet; ignore safely
        }
    }

    @Override
    protected void processMIPSUpdate(Observable resource, AccessNotice notice) {
        if (!notice.accessIsFromMIPS()) return;
        MemoryAccessNotice mem = (MemoryAccessNotice) notice;

        // Only care about WRITE operations into our observed range
        if (mem.getAccessType() != AccessNotice.WRITE) return;

        int address = mem.getAddress();
        int offset  = (address - BASE_ADDRESS) / 4;
        if (offset < 0 || offset >= MAX_ARRAY_SIZE) return;

        int wordValue = mem.getValue();

        // Store & update stats
        values[offset] = wordValue;
        totalWrites++;
        lastWriteIdx   = offset;
        highlightAlpha = 1.0f;

        // Calculate min/max over arraySize
        int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
        for (int i = 0; i < arraySize; i++) {
            if (values[i] < lo) lo = values[i];
            if (values[i] > hi) hi = values[i];
        }
        final int flo = lo, fhi = hi;

        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Last write → [" + offset + "] = " + wordValue);
            minLabel.setText("Min: " + flo);
            maxLabel.setText("Max: " + fhi);
            writesLabel.setText("Writes: " + totalWrites);
            if (!fadeTimer.isRunning()) fadeTimer.start();
            chartPanel.repaint();
        });
    }

    @Override
    protected void initializePreGUI() {
        values      = new int[MAX_ARRAY_SIZE];
        lastWriteIdx = -1;
        totalWrites  = 0;
        highlightAlpha = 0f;
    }

    @Override
    protected void reset() {
        values      = new int[MAX_ARRAY_SIZE];
        lastWriteIdx = -1;
        compareIdx   = -1;
        totalWrites  = 0;
        highlightAlpha = 0f;
        if (fadeTimer != null) fadeTimer.stop();
        SwingUtilities.invokeLater(() -> {
            statusLabel.setText("Reset — waiting…");
            minLabel.setText("Min: —");
            maxLabel.setText("Max: —");
            writesLabel.setText("Writes: 0");
            chartPanel.repaint();
        });
    }

    // ========================================================
    //  Inner class: the bar-chart canvas
    // ========================================================

    private class BarChartPanel extends JPanel {

        private static final int PADDING = 30;

        public BarChartPanel() {
            setBackground(BG_COLOR);
            setDoubleBuffered(true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            int w = getWidth();
            int h = getHeight();

            // Background
            g2.setColor(BG_COLOR);
            g2.fillRect(0, 0, w, h);

            int n = arraySize;
            if (n == 0) { g2.dispose(); return; }

            // Find max value for scaling (use at least 1 to avoid / 0)
            int maxVal = 1;
            for (int i = 0; i < n; i++) if (Math.abs(values[i]) > maxVal) maxVal = Math.abs(values[i]);

            int chartH = h - PADDING * 2;
            int chartW = w - PADDING * 2;
            int barW   = Math.max(2, chartW / n - 2);
            int gap    = Math.max(1, (chartW - barW * n) / (n + 1));

            // Draw horizontal grid lines
            g2.setColor(GRID_COLOR);
            g2.setStroke(new BasicStroke(0.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    0, new float[]{4, 4}, 0));
            int gridLines = 4;
            for (int i = 1; i <= gridLines; i++) {
                int y = PADDING + chartH * i / gridLines;
                g2.drawLine(PADDING, y, w - PADDING, y);
            }
            g2.setStroke(new BasicStroke(1f));

            // Draw bars
            for (int i = 0; i < n; i++) {
                int barH = (int) ((Math.abs(values[i]) / (double) maxVal) * chartH);
                barH = Math.max(barH, 2);   // always at least 2px tall
                int x = PADDING + gap + i * (barW + gap);
                int y = PADDING + chartH - barH;

                // Determine colours
                Color top, bot;
                if (i == lastWriteIdx && highlightAlpha > 0f) {
                    // Blend between base and write colour
                    top = blend(BAR_BASE_TOP, BAR_WRITE_TOP, highlightAlpha);
                    bot = blend(BAR_BASE_BOT, BAR_WRITE_BOT, highlightAlpha);
                } else if (i == compareIdx) {
                    top = BAR_COMPARE_TOP;
                    bot = BAR_COMPARE_BOT;
                } else {
                    top = BAR_BASE_TOP;
                    bot = BAR_BASE_BOT;
                }

                // Gradient fill
                GradientPaint gp = new GradientPaint(x, y, top, x, y + barH, bot);
                g2.setPaint(gp);
                RoundRectangle2D rr = new RoundRectangle2D.Float(x, y, barW, barH, 4, 4);
                g2.fill(rr);

                // Glow effect on write
                if (i == lastWriteIdx && highlightAlpha > 0.2f) {
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, highlightAlpha * 0.3f));
                    g2.setColor(BAR_WRITE_TOP);
                    g2.setStroke(new BasicStroke(3f));
                    g2.draw(rr);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
                    g2.setStroke(new BasicStroke(1f));
                }

                // Index label (only if bars are wide enough)
                if (barW >= 14) {
                    g2.setColor(TEXT_COLOR);
                    g2.setFont(new Font("Monospaced", Font.PLAIN, 9));
                    String idx = String.valueOf(i);
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = x + (barW - fm.stringWidth(idx)) / 2;
                    g2.drawString(idx, tx, h - 6);
                }

                // Value label on top of bar (only if bar tall enough and bars wide enough)
                if (barH > 18 && barW >= 20) {
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Monospaced", Font.BOLD, 9));
                    String val = String.valueOf(values[i]);
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = x + (barW - fm.stringWidth(val)) / 2;
                    g2.drawString(val, tx, y + 12);
                }
            }

            // Baseline
            g2.setColor(GRID_COLOR);
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(PADDING, h - PADDING, w - PADDING, h - PADDING);

            g2.dispose();
        }

        private Color blend(Color a, Color b, float t) {
            float s = 1 - t;
            return new Color(
                    (int)(a.getRed()   * s + b.getRed()   * t),
                    (int)(a.getGreen() * s + b.getGreen() * t),
                    (int)(a.getBlue()  * s + b.getBlue()  * t)
            );
        }
    }
}
