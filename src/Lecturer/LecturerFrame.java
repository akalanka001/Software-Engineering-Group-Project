package Lecturer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import Database.dbconnection;
import Login.Login;
import Lecturer.components.*;
import Utils.ThemeManager;

/**
 * Lecturer portal frame with navigation sidebar and notification icon.
 * Includes Profile, Course, Student, Notices, User Reports, and Notification panels.
 */
public class LecturerFrame extends JFrame implements ActionListener {

    private JPanel leftPanel, rightPanel;
    private CardLayout cardLayout;

    private JButton profileBtn, courseBtn, studentBtn, noticeBtn, reportsBtn, logOutBtn, notifIcon;
    private final String userId;      // e.g., LC0003
    private final dbconnection dbConnector;

    public LecturerFrame(String userId) {
        this.userId = userId;
        this.dbConnector = new dbconnection();

        setTitle("Lecturer Portal");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(null);
        setResizable(false);

        initComponents();
        setVisible(true);
    }

    private void initComponents() {



        Font font = new Font("Roboto", Font.BOLD, 13);
        Color navColor = new Color(153, 187, 187);

        // ---------- Left navigation panel ----------
        leftPanel = new JPanel(null);
        leftPanel.setBounds(0, 0, 200, 700);
        leftPanel.setBackground(navColor);

        // --- Notification icon with rounded background ---
        ImageIcon rawIcon = new ImageIcon(getClass().getResource("/icons/notification.png"));
        Image scaled = rawIcon.getImage().getScaledInstance(28, 28, Image.SCALE_SMOOTH);
        ImageIcon scaledIcon = new ImageIcon(scaled);

        notifIcon = new JButton(scaledIcon) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // Rounded background (circle look)
                g2.setColor(getBackground());
                int arc = getHeight(); // full round
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

                super.paintComponent(g2);
                g2.dispose();
            }
        };

        notifIcon.setBounds(70, 20, 48, 48);
        notifIcon.setBackground(new Color(153, 187, 187));
        notifIcon.setBorderPainted(false);
        notifIcon.setFocusPainted(false);
        notifIcon.setContentAreaFilled(false);
        notifIcon.setToolTipText("View Notifications");
        notifIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        notifIcon.addActionListener(this);

// Hover effect (slightly darker when mouse over)
        notifIcon.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                notifIcon.setBackground(new Color(128, 170, 170)); // lighter/darker tint
                notifIcon.repaint();
            }

            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                notifIcon.setBackground(new Color(153, 187, 187));
                notifIcon.repaint();
            }
        });

        leftPanel.add(notifIcon);

        // ---------- Navigation buttons ----------
        profileBtn = createNavButton("PROFILE", 100, font);
        courseBtn  = createNavButton("COURSE", 160, font);
        studentBtn = createNavButton("STUDENT", 220, font);
        noticeBtn  = createNavButton("NOTICES", 280, font);
        reportsBtn = createNavButton("USER REPORT", 340, font);
        logOutBtn  = createNavButton("LOGOUT", 500, font);
        logOutBtn.setForeground(Color.RED);
        logOutBtn.addActionListener(this);

        leftPanel.add(profileBtn);
        leftPanel.add(courseBtn);
        leftPanel.add(studentBtn);
        leftPanel.add(noticeBtn);
        leftPanel.add(reportsBtn);
        leftPanel.add(logOutBtn);

        // ---------- Right side (CardLayout) ----------
        cardLayout = new CardLayout();
        rightPanel = new JPanel(cardLayout);
        rightPanel.setBounds(200, 0, 800, 700);

        rightPanel.add(new LecturerProfilePanel(userId), "PROFILE");
        rightPanel.add(new LecturerVideoPanel(userId),  "COURSE");
        rightPanel.add(new LecturerStudentPanel(userId), "STUDENT");
        rightPanel.add(new LecturerNoticePanel(userId),  "NOTICES");
        rightPanel.add(new LecturerReportsPanel(userId), "REPORTS");
        rightPanel.add(new LecturerNotificationPanel(userId), "NOTIFICATION");

        add(leftPanel);
        add(rightPanel);

        // Default display
        cardLayout.show(rightPanel, "PROFILE");
        highlight(profileBtn);
    }

    private JButton createNavButton(String text, int y, Font font) {
        JButton btn = new JButton(text);
        btn.setBounds(0, y, 200, 50);
        btn.setFont(font);
        btn.setForeground(Color.BLACK);
        btn.setBackground(new Color(153, 187, 187));
        btn.setBorderPainted(false);
        btn.setFocusable(false);
        btn.addActionListener(this);
        return btn;
    }

    private void resetButtons() {
        JButton[] buttons = { notifIcon, profileBtn, courseBtn, studentBtn, noticeBtn, reportsBtn };
        for (JButton btn : buttons) {
            btn.setEnabled(true);
            btn.setForeground(Color.BLACK);
            btn.setBackground(new Color(153, 187, 187));
        }
        logOutBtn.setForeground(Color.RED);
    }

    private void highlight(JButton btn) {
        btn.setEnabled(false);
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(128, 170, 170));
        btn.repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        resetButtons();
        Object src = e.getSource();

        if (src == notifIcon) {
            cardLayout.show(rightPanel, "NOTIFICATION");
            highlight(notifIcon);
        }
        else if (src == profileBtn) {
            cardLayout.show(rightPanel, "PROFILE"); highlight(profileBtn);
        }
        else if (src == courseBtn) {
            cardLayout.show(rightPanel, "COURSE"); highlight(courseBtn);
        }
        else if (src == studentBtn) {
            cardLayout.show(rightPanel, "STUDENT"); highlight(studentBtn);
        }
        else if (src == noticeBtn) {
            cardLayout.show(rightPanel, "NOTICES"); highlight(noticeBtn);
        }
        else if (src == reportsBtn) {
            cardLayout.show(rightPanel, "REPORTS"); highlight(reportsBtn);
        }
        else if (src == logOutBtn) {
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

    // --- For testing standalone ---
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new LecturerFrame("LC0002").setVisible(true));
    }
}