package Utils;

import com.formdev.flatlaf.intellijthemes.FlatArcIJTheme;
import com.formdev.flatlaf.intellijthemes.FlatArcDarkIJTheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.util.prefs.Preferences;

/**
 * Global theme manager for the LMS application.
 *  • Loads a blue FlatArc theme (IntelliJ‑style)
 *  • Can toggle between Light / Dark
 *  • Stores the last‑used theme preference
 *  • Refreshes all open Swing windows after switching
 */
public final class ThemeManager {

    private static final String PREF_KEY = "themeMode"; // "light" or "dark"
    public static final Color LIGHT_BLUE = new Color(140, 170, 255);
    private static final Preferences prefs =
            Preferences.userNodeForPackage(ThemeManager.class);

    private ThemeManager() {} // Prevent instantiation

    // ---------------------------------------------------------------------
    //  Public API
    // ---------------------------------------------------------------------

    /** Initializes the last‑used theme or default (light). */
    public static void initialize() {
        String mode = prefs.get(PREF_KEY, "light");
        if ("dark".equalsIgnoreCase(mode))
            applyDarkTheme();
        else
            applyLightTheme();
    }

    /** Gives JTable header a blue background and white text. */
    public static void styleTableHeader(JTable table) {
        JTableHeader header = table.getTableHeader();
        header.setBackground(LIGHT_BLUE);
        header.setForeground(Color.WHITE);
        header.setFont(header.getFont().deriveFont(Font.BOLD, header.getFont().getSize()));
        header.setOpaque(true);
        // Remove default border lines for a clean aesthetic
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.CENTER);
    }

    public static void stylePrimaryButton(JButton b) {
        b.putClientProperty("JButton.buttonType", "roundRect");
        b.setBackground(LIGHT_BLUE);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
    }


    /** Applies the blue IntelliJ‑style light theme (FlatArcIJTheme). */
    public static void applyLightTheme() {
        try {
            // Install the Arc Light / blue theme
            UIManager.setLookAndFeel(new FlatArcIJTheme());
            prefs.put(PREF_KEY, "light");
            refreshAllWindows();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Applies the matching dark Arc theme. */
    public static void applyDarkTheme() {
        try {
            UIManager.setLookAndFeel(new FlatArcDarkIJTheme());
            prefs.put(PREF_KEY, "dark");
            refreshAllWindows();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /** Toggles between light and dark at runtime. */
    public static void toggleTheme() {
        if (isDark()) applyLightTheme();
        else applyDarkTheme();
    }

    /** True if the current preference is the dark theme. */
    public static boolean isDark() {
        return "dark".equalsIgnoreCase(prefs.get(PREF_KEY, "light"));
    }

    /** Refreshes all open frames and dialogs after theme change. */
    public static void refreshAllWindows() {
        for (Frame frame : Frame.getFrames()) {
            SwingUtilities.updateComponentTreeUI(frame);
            for (Window w : frame.getOwnedWindows())
                SwingUtilities.updateComponentTreeUI(w);
        }
    }
}