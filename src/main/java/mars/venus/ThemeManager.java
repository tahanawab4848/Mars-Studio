package mars.venus;

import mars.Globals;

import javax.swing.*;
import java.awt.*;

/**
 * ThemeManager — centralises all theme-aware colour lookups.
 *
 * Every component that previously used a hard-coded {@code Color.WHITE} or
 * {@code Color.GRAY} should call the appropriate method here so that the
 * value tracks the active FlatLaf look-and-feel automatically.
 *
 * <p>Design notes:
 * <ul>
 *   <li>All colours are obtained from the active UIManager at call-time so
 *       they remain correct after a runtime theme switch.</li>
 *   <li>Fallback values are provided for environments where the UI key is
 *       absent (e.g. headless tests).</li>
 *   <li>The static {@link #applyThemeToAllWindows()} method can be called
 *       after {@link UIManager#setLookAndFeel} to repaint and fix any
 *       component whose colours were baked in at construction time.</li>
 * </ul>
 */
public final class ThemeManager {

    private ThemeManager() { /* utility class */ }

    // ── current theme name ─────────────────────────────────────────────

    /** Returns the currently stored theme name ("Dark" or "Light"). */
    public static String currentTheme() {
        try {
            return Globals.getSettings().getTheme();
        } catch (Exception e) {
            return "Dark";
        }
    }

    public static boolean isDark() {
        String t = currentTheme();
        return t == null || !"Light".equals(t);
    }

    // ── standard colour accessors ──────────────────────────────────────

    /**
     * Background colour for an editable text area.
     * In Dark theme this is the panel background; in Light theme it is white.
     */
    public static Color editableBackground() {
        Color c = UIManager.getColor("TextArea.background");
        return (c != null) ? c : (isDark() ? new Color(0x1E1E2E) : Color.WHITE);
    }

    /**
     * Background colour for a <em>read-only</em> text area.
     */
    public static Color readOnlyBackground() {
        Color c = UIManager.getColor("TextArea.disabledBackground");
        if (c == null) c = UIManager.getColor("Panel.background");
        return (c != null) ? c : (isDark() ? new Color(0x181825) : new Color(0xE0E0E0));
    }

    /** Default foreground colour (text). */
    public static Color defaultForeground() {
        Color c = UIManager.getColor("TextArea.foreground");
        return (c != null) ? c : (isDark() ? new Color(0xCDD6F4) : Color.BLACK);
    }

    /** Panel / window background colour. */
    public static Color panelBackground() {
        Color c = UIManager.getColor("Panel.background");
        return (c != null) ? c : (isDark() ? new Color(0x1E1E2E) : new Color(0xF5F5F5));
    }

    /** Selection / highlight background that contrasts well in both themes. */
    public static Color selectionBackground() {
        Color c = UIManager.getColor("Table.selectionBackground");
        return (c != null) ? c : (isDark() ? new Color(0x313244) : new Color(0xBBDEFB));
    }

    /** A colour suitable for checkbox backgrounds (should match panel). */
    public static Color checkBoxBackground() {
        // FlatLaf usually makes checkboxes transparent; we just want a neutral colour.
        Color c = UIManager.getColor("CheckBox.background");
        return (c != null) ? c : panelBackground();
    }

    /** Accent / highlight colour for borders, labels, etc. */
    public static Color accent() {
        return isDark() ? new Color(0x89B4FA) : new Color(0x1565C0);
    }

    /** A muted border colour. */
    public static Color border() {
        return isDark() ? new Color(0x45475A) : new Color(0xBDBDBD);
    }

    // ── theme-switch utility ───────────────────────────────────────────

    /**
     * Forces every open window to repaint itself with the current
     * look-and-feel, and additionally re-applies colours to any components
     * that baked in hard-coded values at construction time.
     *
     * Call this immediately after {@link UIManager#setLookAndFeel(LookAndFeel)}.
     */
    public static void applyThemeToAllWindows() {
        for (Window w : Window.getWindows()) {
            SwingUtilities.updateComponentTreeUI(w);
            // Walk every component and patch up known hard-coded colours.
            patchComponentTree(w);
            w.repaint();
        }
    }

    /**
     * Recursively walk the component tree rooted at {@code comp} and
     * neutralise any hard-coded colours so they follow the current LAF.
     */
    public static void patchComponentTree(Component comp) {
        if (comp == null) return;

        // ── JTextArea / JTextPane (read-only mode bakes in Color.GRAY) ──
        if (comp instanceof JTextArea) {
            JTextArea ta = (JTextArea) comp;
            if (!ta.isEditable()) {
                ta.setBackground(readOnlyBackground());
                ta.setForeground(defaultForeground());
            } else {
                ta.setBackground(editableBackground());
                ta.setForeground(defaultForeground());
            }
        }

        // ── JTextField ──────────────────────────────────────────────────
        if (comp instanceof JTextField) {
            JTextField tf = (JTextField) comp;
            tf.setBackground(editableBackground());
            tf.setForeground(defaultForeground());
            tf.setCaretColor(defaultForeground());
        }

        // ── JCheckBox – condition flags in Coprocessor1Window ───────────
        if (comp instanceof JCheckBox) {
            ((JCheckBox) comp).setBackground(checkBoxBackground());
        }

        // ── JTable – row-selection highlight ────────────────────────────
        if (comp instanceof JTable) {
            ((JTable) comp).setSelectionBackground(selectionBackground());
        }

        // Recurse into children
        if (comp instanceof Container) {
            for (Component child : ((Container) comp).getComponents()) {
                patchComponentTree(child);
            }
        }
    }
}
