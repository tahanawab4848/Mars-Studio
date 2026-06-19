package mars.venus;

import mars.*;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Action class for the Settings menu item to control the Look and Feel (Theme) of the UI.
 */
public class SettingsThemeAction extends GuiAction {

    private String themeName;

    public SettingsThemeAction(String name, Icon icon, String descrip,
                               Integer mnemonic, KeyStroke accel, VenusUI gui, String themeName) {
        super(name, icon, descrip, mnemonic, accel, gui);
        this.themeName = themeName;
    }

    public void actionPerformed(ActionEvent e) {
        try {
            if ("Dark".equals(themeName)) {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatDarkLaf());
            } else if ("Light".equals(themeName)) {
                UIManager.setLookAndFeel(new com.formdev.flatlaf.FlatLightLaf());
            }
            // Persist the selection first so ThemeManager.isDark() returns the
            // correct value when patchComponentTree() inspects it.
            Globals.getSettings().setTheme(themeName);
            // Update all open windows AND patch hard-coded colours.
            ThemeManager.applyThemeToAllWindows();
        } catch (Exception ex) {
            System.err.println("Failed to apply theme: " + ex.getMessage());
        }
    }
}
