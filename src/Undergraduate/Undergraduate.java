package Undergraduate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import Database.dbconnection;
import Undergraduate.components.*;
import Login.Login;

public class Undergraduate extends JFrame implements ActionListener {

    private JPanel leftPanel, rightPanel;
    private CardLayout cardLayout;

    private JButton notifBtn, courseBtn, noticeBtn,
            profileBtn, enrollmentsBtn, transactionsBtn, logOutBtn;

    private final String userId;
    private final dbconnection dbConnector;

    public Undergraduate(String userId) {
        this.userId = userId;
        this.dbConnector = new dbconnection();

        setTitle("Undergraduate Portal");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 610);
        setLocationRelativeTo(null);
        setLayout(null);
        setResizable(false);

        initComponents();
        setVisible(true);
    }

    private void initComponents() {
        Font buttonFont = new Font("Roboto", Font.BOLD, 13);

        // ---------- Left navigation panel ----------
        leftPanel = new JPanel(null);
        leftPanel.setBounds(0, 0, 200, 600);
        leftPanel.setBackground(new Color(153, 187, 187));

        // 🔔 Notification icon at top (rounded like Lecturer)
        ImageIcon rawIcon = new ImageIcon(getClass().getResource("/icons/notification.png"));
        Image scaled = rawIcon.getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaled);

        notifBtn = new JButton(scaledIcon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Rounded (circle) background
                g2.setColor(getBackground());
                int arc = getHeight();
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

                super.paintComponent(g2);
                g2.dispose();
            }
        };
        notifBtn.setBounds(70, 20, 48, 48);
        notifBtn.setBackground(new Color(153, 187, 187));
        notifBtn.setBorderPainted(false);
        notifBtn.setFocusPainted(false);
        notifBtn.setContentAreaFilled(false);
        notifBtn.setToolTipText("Notifications");
        notifBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        notifBtn.addActionListener(this);

// Hover tint
        notifBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                notifBtn.setBackground(new Color(128, 170, 170));
                notifBtn.repaint();
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                notifBtn.setBackground(new Color(153, 187, 187));
                notifBtn.repaint();
            }
        });

        leftPanel.add(notifBtn);

        // Sidebar buttons — neatly spaced
        courseBtn        = createNavButton("COURSES", 120, buttonFont);
        noticeBtn        = createNavButton("NOTICES", 180, buttonFont);
        profileBtn       = createNavButton("MY PROFILE", 240, buttonFont);
        enrollmentsBtn   = createNavButton("ENROLLMENTS", 300, buttonFont);
        transactionsBtn  = createNavButton("TRANSACTIONS", 360, buttonFont);
        logOutBtn        = createNavButton("LOG OUT", 500, buttonFont);
        logOutBtn.setForeground(Color.RED);

        leftPanel.add(courseBtn);
        leftPanel.add(noticeBtn);
        leftPanel.add(profileBtn);
        leftPanel.add(enrollmentsBtn);
        leftPanel.add(transactionsBtn);
        leftPanel.add(logOutBtn);

        // ---------- Right content with cards ----------
        cardLayout = new CardLayout();
        rightPanel = new JPanel(cardLayout);
        rightPanel.setBounds(200, 0, 800, 600);

        rightPanel.add(new CoursePanel(userId),        "COURSES");
        rightPanel.add(new NoticePanel(userId),        "NOTICES");
        rightPanel.add(new ProfilePanel(userId),       "MY PROFILE");
        rightPanel.add(new EnrollmentPanel(userId),    "ENROLLMENTS");
        rightPanel.add(new UGNotificationPanel(userId),"UG_NOTIFICATION");
        rightPanel.add(new UGTransactionPanel(userId), "TRANSACTIONS"); // ← new panel

        add(leftPanel);
        add(rightPanel);

        // Default
        cardLayout.show(rightPanel, "COURSES");
        highlight(courseBtn);
    }

    private JButton createNavButton(String text, int y, Font font) {
        JButton btn = new JButton(text);
        btn.setBounds(0, y, 200, 50);
        btn.setForeground(Color.BLACK);
        btn.setBackground(new Color(153, 187, 187));
        btn.setFont(font);
        btn.setFocusable(false);
        btn.setBorderPainted(false);
        btn.addActionListener(this);
        return btn;
    }

    private void resetButtons() {
        JButton[] buttons = { notifBtn, courseBtn, noticeBtn, profileBtn, enrollmentsBtn, transactionsBtn };
        for (JButton btn : buttons) {
            btn.setEnabled(true);
            btn.setForeground(Color.BLACK);
            btn.setBackground(new Color(153, 187, 187));
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        resetButtons();
        Object src = e.getSource();

        if (src == notifBtn) {
            cardLayout.show(rightPanel, "UG_NOTIFICATION"); highlight(notifBtn);
        } else if (src == courseBtn) {
            cardLayout.show(rightPanel, "COURSES"); highlight(courseBtn);
        } else if (src == noticeBtn) {
            cardLayout.show(rightPanel, "NOTICES"); highlight(noticeBtn);
        } else if (src == profileBtn) {
            cardLayout.show(rightPanel, "MY PROFILE"); highlight(profileBtn);
        } else if (src == enrollmentsBtn) {
            cardLayout.show(rightPanel, "ENROLLMENTS"); highlight(enrollmentsBtn);
        } else if (src == transactionsBtn) {
            cardLayout.show(rightPanel, "TRANSACTIONS"); highlight(transactionsBtn);
        } else if (src == logOutBtn) {
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
    }

    private void highlight(JButton btn) {
        btn.setEnabled(false);
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(128, 170, 170));
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Undergraduate("TG1344").setVisible(true));
    }
}