package mars.venus;

import mars.ccompiler.CCompiler;
import mars.ccompiler.CCompilerConfig;
import mars.ccompiler.CCompilerConfig.CompilerType;
import mars.ccompiler.CompilerOutput;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;

/*
 * CCompilerSettingsDialog — settings dialog for GCC/Clang MIPS cross-compiler config.
 *
 * Opened via Settings → C Compiler → Configure Compiler…
 * Persists settings via CCompilerConfig (Java Preferences API).
 * Author: MARS C Extension
 */
public class CCompilerSettingsDialog extends JDialog {

    private static final Color BG       = new Color(0x1E1E2E);
    private static final Color PANEL_BG = new Color(0x252537);
    private static final Color TEXT     = new Color(0xCDD6F4);
    private static final Color ACCENT   = new Color(0x89B4FA);
    private static final Color BORDER   = new Color(0x45475A);
    private static final Color SUCCESS  = new Color(0xA6E3A1);
    private static final Color ERROR    = new Color(0xF38BA8);

    // ── GUI fields ─────────────────────────────────────────────────────
    private JComboBox<String> typeCombo;
    private JTextField        pathField;
    private JTextField        flagsField;
    private JTextField        tempDirField;
    private JSpinner          timeoutSpinner;
    private JTextArea         testResultArea;
    private JLabel            statusLabel;

    public CCompilerSettingsDialog(Frame parent) {
        super(parent, "C Compiler Settings", true);
        setBackground(BG);
        getContentPane().setBackground(BG);
        buildUI();
        populateFromConfig();
        pack();
        setMinimumSize(new Dimension(580, 480));
        setLocationRelativeTo(parent);
    }

    // ── UI ─────────────────────────────────────────────────────────────
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(BG);
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        root.add(buildFormPanel(), BorderLayout.CENTER);
        root.add(buildButtonPanel(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.fill   = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // ── Compiler Type ──────────────────────────────────────────────
        addLabel(panel, gc, row, "Compiler Type:");
        typeCombo = new JComboBox<>(new String[]{"Auto-detect", "GCC (mips-linux-gnu-gcc)", "Clang"});
        styleCombo(typeCombo);
        addField(panel, gc, row++, typeCombo);

        // ── Compiler Path ──────────────────────────────────────────────
        addLabel(panel, gc, row, "Compiler Path:");
        pathField = styledField("Leave empty for auto-detect from PATH");
        JPanel pathRow = new JPanel(new BorderLayout(4, 0));
        pathRow.setBackground(BG);
        pathRow.add(pathField, BorderLayout.CENTER);
        JButton browseBtn = styledBtn("Browse…");
        browseBtn.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Select Compiler Executable");
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                pathField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        pathRow.add(browseBtn, BorderLayout.EAST);
        addField(panel, gc, row++, pathRow);

        // ── Extra Flags ────────────────────────────────────────────────
        addLabel(panel, gc, row, "Extra Flags:");
        flagsField = styledField("-O0 -mips32r2 -mabi=32 -g");
        addField(panel, gc, row++, flagsField);

        // ── Temp Directory ─────────────────────────────────────────────
        addLabel(panel, gc, row, "Temp Directory:");
        tempDirField = styledField(System.getProperty("java.io.tmpdir"));
        JPanel tempRow = new JPanel(new BorderLayout(4, 0));
        tempRow.setBackground(BG);
        tempRow.add(tempDirField, BorderLayout.CENTER);
        JButton tempBrowse = styledBtn("Browse…");
        tempBrowse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                tempDirField.setText(fc.getSelectedFile().getAbsolutePath());
            }
        });
        tempRow.add(tempBrowse, BorderLayout.EAST);
        addField(panel, gc, row++, tempRow);

        // ── Timeout ────────────────────────────────────────────────────
        addLabel(panel, gc, row, "Timeout (seconds):");
        timeoutSpinner = new JSpinner(new SpinnerNumberModel(30, 5, 300, 5));
        styleSpinner(timeoutSpinner);
        addField(panel, gc, row++, timeoutSpinner);

        // ── Test area ──────────────────────────────────────────────────
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        JButton testBtn = styledBtn("[Test] Compiler");
        testBtn.setForeground(ACCENT);
        testBtn.addActionListener(e -> testCompiler());
        panel.add(testBtn, gc);
        row++;

        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        gc.weightx = 1; gc.weighty = 1; gc.fill = GridBagConstraints.BOTH;
        testResultArea = new JTextArea(5, 0);
        testResultArea.setBackground(new Color(0x11111B));
        testResultArea.setForeground(TEXT);
        testResultArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        testResultArea.setEditable(false);
        testResultArea.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        JScrollPane testScroll = new JScrollPane(testResultArea);
        testScroll.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        TitledBorder tb = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(BORDER), "Test Result");
        tb.setTitleColor(ACCENT);
        testScroll.setBorder(tb);
        panel.add(testScroll, gc);

        return panel;
    }

    private JPanel buildButtonPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(BG);
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        statusLabel.setForeground(TEXT);
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        btnRow.setBackground(BG);
        JButton saveBtn   = styledBtn("Save");
        JButton cancelBtn = styledBtn("Cancel");
        JButton autoBtn   = styledBtn("Auto-detect Now");

        saveBtn.setForeground(SUCCESS);
        saveBtn.addActionListener(e -> saveAndClose());
        cancelBtn.addActionListener(e -> dispose());
        autoBtn.addActionListener(e -> autoDetect());

        btnRow.add(autoBtn);
        btnRow.add(cancelBtn);
        btnRow.add(saveBtn);

        p.add(statusLabel, BorderLayout.WEST);
        p.add(btnRow,      BorderLayout.EAST);
        return p;
    }

    // ── Logic ──────────────────────────────────────────────────────────

    private void populateFromConfig() {
        CCompilerConfig cfg = CCompilerConfig.getInstance();
        switch (cfg.getCompilerType()) {
            case GCC:         typeCombo.setSelectedIndex(1); break;
            case CLANG:       typeCombo.setSelectedIndex(2); break;
            default:          typeCombo.setSelectedIndex(0); break;
        }
        pathField.setText(cfg.getCompilerPath());
        flagsField.setText(cfg.getExtraFlags());
        tempDirField.setText(cfg.getTempDir());
        timeoutSpinner.setValue(cfg.getTimeoutSeconds());
    }

    private void saveAndClose() {
        CCompilerConfig cfg = CCompilerConfig.getInstance();
        int typeIdx = typeCombo.getSelectedIndex();
        switch (typeIdx) {
            case 1:  cfg.setCompilerType(CompilerType.GCC);         break;
            case 2:  cfg.setCompilerType(CompilerType.CLANG);        break;
            default: cfg.setCompilerType(CompilerType.AUTO_DETECT);  break;
        }
        cfg.setCompilerPath(pathField.getText().trim());
        cfg.setExtraFlags(flagsField.getText().trim());
        cfg.setTempDir(tempDirField.getText().trim());
        cfg.setTimeoutSeconds((Integer) timeoutSpinner.getValue());
        cfg.save();
        setStatus("Settings saved.", SUCCESS);
        dispose();
    }

    private void autoDetect() {
        String found = null;
        for (String c : new String[]{"mips-linux-gnu-gcc","mips-elf-gcc","mips64-linux-gnu-gcc","clang"}) {
            if (CCompiler.isOnPath(c)) { found = c; break; }
        }
        if (found != null) {
            pathField.setText(found);
            setStatus("Found: " + found, SUCCESS);
        } else {
            setStatus("No MIPS compiler found on PATH.", ERROR);
        }
    }

    private void testCompiler() {
        testResultArea.setText("Testing compiler...\n");
        String path = pathField.getText().trim();
        CCompilerConfig testCfg = CCompilerConfig.getInstance();
        testCfg.setCompilerPath(path.isEmpty() ? "" : path);
        testCfg.setExtraFlags(flagsField.getText().trim());
        testCfg.setTempDir(tempDirField.getText().trim());

        String testSrc = "int main(){int x=42;return x;}\n";
        new SwingWorker<CompilerOutput, Void>() {
            protected CompilerOutput doInBackground() {
                return CCompiler.compile(testSrc, testCfg);
            }
            protected void done() {
                try {
                    CompilerOutput out = get();
                    if (out.isSuccess()) {
                        testResultArea.setText("[OK] SUCCESS (" + out.getCompilationTimeMs() + " ms)\n");
                        testResultArea.append("Output: " + out.getGeneratedAsmPath() + "\n");
                        testResultArea.setForeground(SUCCESS);
                    } else {
                        testResultArea.setText("[ERR] FAILED (exit " + out.getExitCode() + ")\n");
                        testResultArea.append(out.getFormattedErrorReport());
                        testResultArea.setForeground(ERROR);
                    }
                } catch (Exception e) {
                    testResultArea.setText("Error: " + e.getMessage());
                }
            }
        }.execute();
    }

    private void setStatus(String msg, Color color) {
        statusLabel.setText(" " + msg);
        statusLabel.setForeground(color);
    }

    // ── Styling helpers ────────────────────────────────────────────────

    private void addLabel(JPanel p, GridBagConstraints gc, int row, String text) {
        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 1; gc.weightx = 0.25;
        JLabel lbl = new JLabel(text);
        lbl.setForeground(ACCENT);
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        p.add(lbl, gc);
    }

    private void addField(JPanel p, GridBagConstraints gc, int row, JComponent comp) {
        gc.gridx = 1; gc.gridy = row; gc.gridwidth = 1; gc.weightx = 0.75;
        p.add(comp, gc);
    }

    private JTextField styledField(String placeholder) {
        JTextField f = new JTextField();
        f.setBackground(new Color(0x313147));
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setFont(new Font("Monospaced", Font.PLAIN, 12));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        return f;
    }

    private JButton styledBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(new Color(0x313147));
        b.setForeground(TEXT);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER, 1),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        return b;
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setBackground(new Color(0x313147));
        combo.setForeground(TEXT);
        combo.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBackground(new Color(0x313147));
        spinner.setForeground(TEXT);
        spinner.setFont(new Font("Monospaced", Font.PLAIN, 12));
    }
}
