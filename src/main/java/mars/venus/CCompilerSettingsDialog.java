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

    // Colours are resolved at method-call time via ThemeManager so the dialog
    // automatically adapts to the active FlatLaf Look & Feel.
    private static Color bg()      { return mars.venus.ThemeManager.panelBackground(); }
    private static Color text()    { return mars.venus.ThemeManager.defaultForeground(); }
    private static Color accent()  { return mars.venus.ThemeManager.accent(); }
    private static Color border()  { return mars.venus.ThemeManager.border(); }
    private static Color success() { return mars.venus.ThemeManager.isDark() ? new Color(0xA6E3A1) : new Color(0x2E7D32); }
    private static Color error()   { return mars.venus.ThemeManager.isDark() ? new Color(0xF38BA8) : new Color(0xC62828); }
    private static Color fieldBg() { return mars.venus.ThemeManager.editableBackground(); }

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
        setBackground(bg());
        getContentPane().setBackground(bg());
        buildUI();
        populateFromConfig();
        pack();
        setMinimumSize(new Dimension(580, 480));
        setLocationRelativeTo(parent);
    }

    // ── UI ─────────────────────────────────────────────────────────────
    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(8, 8));
        root.setBackground(bg());
        root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        root.add(buildFormPanel(), BorderLayout.CENTER);
        root.add(buildButtonPanel(), BorderLayout.SOUTH);
        setContentPane(root);
    }

    private JPanel buildFormPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(bg());
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
        pathRow.setBackground(bg());
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
        tempRow.setBackground(bg());
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
        testBtn.setForeground(accent());
        testBtn.addActionListener(e -> testCompiler());
        panel.add(testBtn, gc);
        row++;

        gc.gridx = 0; gc.gridy = row; gc.gridwidth = 2;
        gc.weightx = 1; gc.weighty = 1; gc.fill = GridBagConstraints.BOTH;
        testResultArea = new JTextArea(5, 0);
        testResultArea.setBackground(mars.venus.ThemeManager.readOnlyBackground());
        testResultArea.setForeground(text());
        testResultArea.setFont(new Font("Monospaced", Font.PLAIN, 11));
        testResultArea.setEditable(false);
        testResultArea.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        JScrollPane testScroll = new JScrollPane(testResultArea);
        TitledBorder tb = BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(border()), "Test Result");
        tb.setTitleColor(accent());
        testScroll.setBorder(tb);
        panel.add(testScroll, gc);

        return panel;
    }

    private JPanel buildButtonPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(bg());
        p.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, border()));

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("SansSerif", Font.ITALIC, 12));
        statusLabel.setForeground(text());
        statusLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 6));
        btnRow.setBackground(bg());
        JButton saveBtn   = styledBtn("Save");
        JButton cancelBtn = styledBtn("Cancel");
        JButton autoBtn   = styledBtn("Auto-detect Now");

        saveBtn.setForeground(success());
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
        setStatus("Settings saved.", success());
        dispose();
    }

    private void autoDetect() {
        String found = null;
        for (String c : new String[]{"mips-linux-gnu-gcc","mips-elf-gcc","mips64-linux-gnu-gcc","clang"}) {
            if (CCompiler.isOnPath(c)) { found = c; break; }
        }
        if (found != null) {
            pathField.setText(found);
            setStatus("Found: " + found, success());
        } else {
            setStatus("No MIPS compiler found on PATH.", error());
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
                        testResultArea.setForeground(success());
                    } else {
                        testResultArea.setText("[ERR] FAILED (exit " + out.getExitCode() + ")\n");
                        testResultArea.append(out.getFormattedErrorReport());
                        testResultArea.setForeground(error());
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
        lbl.setForeground(accent());
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        p.add(lbl, gc);
    }

    private void addField(JPanel p, GridBagConstraints gc, int row, JComponent comp) {
        gc.gridx = 1; gc.gridy = row; gc.gridwidth = 1; gc.weightx = 0.75;
        p.add(comp, gc);
    }

    private JTextField styledField(String placeholder) {
        JTextField f = new JTextField();
        f.setBackground(fieldBg());
        f.setForeground(text());
        f.setCaretColor(text());
        f.setFont(new Font("Monospaced", Font.PLAIN, 12));
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border(), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        return f;
    }

    private JButton styledBtn(String text) {
        JButton b = new JButton(text);
        b.setBackground(fieldBg());
        b.setForeground(this.text());
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.PLAIN, 12));
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(border(), 1),
            BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        return b;
    }

    private void styleCombo(JComboBox<String> combo) {
        combo.setBackground(fieldBg());
        combo.setForeground(text());
        combo.setFont(new Font("SansSerif", Font.PLAIN, 12));
    }

    private void styleSpinner(JSpinner spinner) {
        spinner.setBackground(fieldBg());
        spinner.setForeground(text());
        spinner.setFont(new Font("Monospaced", Font.PLAIN, 12));
    }
}
