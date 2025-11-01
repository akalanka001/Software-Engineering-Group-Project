package Welcome;

import Login.Login;
import Utils.ThemeManager;

import javax.swing.*;
import java.awt.*;

/**
 * Splash / loading screen for the LMS.
 * FlatLaf blue theme, minimal, faster loading.
 */
public class Welcome extends JFrame {

    private final JProgressBar progressBar;
    private final JLabel lblStatus;

    public Welcome() {
        ThemeManager.applyLightTheme();
        UIManager.put("Component.accentColor", "#3777FF");

        setTitle("Learning Management System");
        setResizable(false);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);

        // gradient background panel
        JPanel background = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color c1 = new Color(240, 246, 255);
                Color c2 = new Color(200, 220, 255);
                g2.setPaint(new GradientPaint(0, 0, c1, 0, getHeight(), c2));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        background.setLayout(new BoxLayout(background, BoxLayout.Y_AXIS));
        background.setBorder(BorderFactory.createEmptyBorder(80, 60, 60, 60));

        JLabel lblTitle = new JLabel("Learning Management System", SwingConstants.CENTER);
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTitle.setFont(new Font("Segoe UI Black", Font.BOLD, 32));
        lblTitle.setForeground(new Color(55, 119, 255));

        lblStatus = new JLabel("Loading...", SwingConstants.CENTER);
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblStatus.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        lblStatus.setForeground(Color.DARK_GRAY);
        lblStatus.setBorder(BorderFactory.createEmptyBorder(20, 0, 12, 0));

        progressBar = new JProgressBar();
        progressBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        progressBar.setPreferredSize(new Dimension(400, 22));
        progressBar.setStringPainted(true);
        progressBar.putClientProperty("JComponent.roundRect", true);

        background.add(lblTitle);
        background.add(lblStatus);
        background.add(progressBar);
        add(background);

        setSize(720, 360);
        setLocationRelativeTo(null);
    }

    // ---------------------------------------------------------------------
    public static void main(String[] args) {
        UIManager.put("Component.accentColor", "#3777FF");
        ThemeManager.applyLightTheme();

        SwingUtilities.invokeLater(() -> {
            Welcome splash = new Welcome();
            splash.setVisible(true);

            new Thread(() -> {
                try {
                    // quicker: jump by 4 each tick, 10 ms delay ≈ 1.3 s total
                    for (int i = 0; i <= 100; i += 2) {
                        int val = Math.min(i, 100);
                        SwingUtilities.invokeLater(() ->
                                splash.progressBar.setValue(val));
                        Thread.sleep(20);
                    }
                    SwingUtilities.invokeLater(() -> {
                        splash.dispose();
                        new Login().setVisible(true);
                    });
                } catch (InterruptedException ignored) { }
            }).start();
        });
    }
}