package Admin.ui;

import Login.Login;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Admin dashboard frame with navigation sidebar and Reports section.
 */
public class AdminFrame extends JFrame {

    private final String username;
    private final JPanel leftPanel;
    private final JPanel rightPanel;
    private final List<JButton> navButtons = new ArrayList<>();

    public AdminFrame(String u_name) {
        this.username = u_name;

        setTitle("Admin Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        setResizable(false);

        // ---------- LEFT PANEL ----------
        leftPanel = new JPanel(null);
        leftPanel.setPreferredSize(new Dimension(200, 700));
        leftPanel.setBackground(new Color(128, 170, 170));  // calm teal tone

        Font buttonFont = new Font("Segoe UI Semibold", Font.PLAIN, 14);

        // --- Notification icon (rounded like Lecturer) ---
        ImageIcon bellRaw = new ImageIcon(getClass().getResource("/icons/notification.png"));
        Image bellScaled = bellRaw.getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH);
        ImageIcon bellIcon = new ImageIcon(bellScaled);

        JButton btnNotification = new JButton(bellIcon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // circular background
                g2.setColor(getBackground());
                int arc = getHeight();
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

                super.paintComponent(g2);
                g2.dispose();
            }
        };
        btnNotification.setBounds(70, 20, 48, 48);
        btnNotification.setBackground(new Color(128, 170, 170));
        btnNotification.setBorderPainted(false);
        btnNotification.setFocusPainted(false);
        btnNotification.setContentAreaFilled(false);
        btnNotification.setToolTipText("View Notifications");
        btnNotification.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

// Hover tint
        btnNotification.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btnNotification.setBackground(new Color(100, 150, 150));
                btnNotification.repaint();
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btnNotification.setBackground(new Color(128, 170, 170));
                btnNotification.repaint();
            }
        });

// Keep existing action connection (notifications view)
        leftPanel.add(btnNotification);

        // --- Main navigation buttons ---
        JButton btnUsers       = createNavButton("USERS",       100, buttonFont);
        JButton btnCourses     = createNavButton("COURSES",     150, buttonFont);
        JButton btnNotices     = createNavButton("NOTICES",     200, buttonFont);
        JButton btnProfile     = createNavButton("PROFILE",     250, buttonFont);
        JButton btnEnrollments = createNavButton("ENROLLMENTS", 300, buttonFont);
        JButton btnReports     = createNavButton("REPORTS",     350, buttonFont);
        JButton btnLogout      = createLogoutButton("LOG OUT",  420, buttonFont);

        leftPanel.add(btnUsers);
        leftPanel.add(btnCourses);
        leftPanel.add(btnNotices);
        leftPanel.add(btnProfile);
        leftPanel.add(btnEnrollments);
        leftPanel.add(btnReports);
        leftPanel.add(btnLogout);
        add(leftPanel, BorderLayout.WEST);

        // ---------- RIGHT PANEL ----------
        rightPanel = new JPanel(new CardLayout());
        rightPanel.add(new UserTab(username),            "user");
        rightPanel.add(new CourseTab(username),          "course");
        rightPanel.add(new NoticeTab(username),          "notice");
        rightPanel.add(new ProfileTab(username),         "profile");
        rightPanel.add(new AdminEnrollmentPanel(username),"enrollment");
        rightPanel.add(new NotificationPanel(username),  "notification");
        rightPanel.add(new ReportsPanel(username),       "reports"); // <-- New reports hub
        add(rightPanel, BorderLayout.CENTER);

        // ---------- Navigation actions ----------
        CardLayout cl = (CardLayout) rightPanel.getLayout();

        btnNotification.addActionListener(e -> { cl.show(rightPanel, "notification"); highlightButton(null); });
        btnUsers.addActionListener(e -> { cl.show(rightPanel, "user");        highlightButton(btnUsers); });
        btnCourses.addActionListener(e -> { cl.show(rightPanel, "course");    highlightButton(btnCourses); });
        btnNotices.addActionListener(e -> { cl.show(rightPanel, "notice");    highlightButton(btnNotices); });
        btnProfile.addActionListener(e -> { cl.show(rightPanel, "profile");   highlightButton(btnProfile); });
        btnEnrollments.addActionListener(e -> { cl.show(rightPanel, "enrollment"); highlightButton(btnEnrollments); });
        btnReports.addActionListener(e -> { cl.show(rightPanel, "reports");   highlightButton(btnReports); });
        btnLogout.addActionListener(e -> logout());

        // Default selection
        cl.show(rightPanel, "user");
        highlightButton(btnUsers);
    }

    // ---------- Sidebar button builders ----------

    private JButton createIconButton(int y, String symbol) {
        JButton btn = new JButton(symbol);
        btn.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24)); // allows emoji bell
        btn.setBounds(0, y, 200, 50);
        btn.setBackground(new Color(128, 170, 170));
        btn.setBorderPainted(false);
        btn.setFocusable(false);
        btn.setForeground(Color.DARK_GRAY);
        btn.setToolTipText("View Notifications");

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(150, 190, 190));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(new Color(128, 170, 170));
            }
        });
        return btn;
    }

    private JButton createNavButton(String text, int y, Font font) {
        JButton btn = new JButton(text);
        btn.setBounds(0, y, 200, 50);
        btn.setFont(font);
        btn.setForeground(Color.BLACK);
        btn.setBackground(new Color(128, 170, 170));
        btn.setFocusable(false);
        btn.setBorderPainted(false);
        navButtons.add(btn);

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(new Color(150, 190, 190));
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                if (btn.isEnabled()) btn.setBackground(new Color(128, 170, 170));
            }
        });
        return btn;
    }

    private JButton createLogoutButton(String text, int y, Font font) {
        JButton btn = new JButton(text);
        btn.setBounds(0, y, 200, 50);
        btn.setFont(font);
        btn.setForeground(new Color(220, 60, 60));
        btn.setBackground(new Color(128, 170, 170));
        btn.setFocusable(false);
        btn.setBorderPainted(false);
        return btn;
    }

    // ---------- Highlight helpers ----------

    private void resetHighlights() {
        for (JButton b : navButtons) {
            b.setEnabled(true);
            b.setForeground(Color.BLACK);
            b.setBackground(new Color(128, 170, 170));
        }
    }

    private void highlightButton(JButton active) {
        resetHighlights();
        if (active != null) {
            active.setEnabled(false);
            active.setBackground(new Color(100, 150, 150));
            active.setForeground(Color.WHITE);
        }
    }

    // ---------- Logout ----------
    private void logout() {
        int res = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to log out?",
                "Confirm Logout",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE
        );
        if (res == JOptionPane.YES_OPTION) {
            dispose();
            new Login().setVisible(true);
        }
    }

    // ---------- Main ----------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AdminFrame("AD0001").setVisible(true));
    }
}