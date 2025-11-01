package Undergraduate.components;

import Database.dbconnection;
import Utils.ThemeManager;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.time.format.DateTimeFormatter;

/**
 * Undergraduate notifications panel.
 * Shows messages from ug_notification table.
 */
public class UGNotificationPanel extends JPanel {

    private final String currentUser;
    private final DefaultTableModel model;
    private final JTable table;

    public UGNotificationPanel(String currentUser) {
        this.currentUser = currentUser;

        setLayout(new BorderLayout(10, 10));
        setBackground(UIManager.getColor("Panel.background"));

        JLabel title = new JLabel("Notifications");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setBorder(BorderFactory.createEmptyBorder(10, 20, 0, 0));
        add(title, BorderLayout.NORTH);

        // ---------- TABLE ----------
        model = new DefaultTableModel(new Object[]{"ID", "Date", "Message"}, 0);
        table = new JTable(model);
        table.setRowHeight(28);
        table.setDefaultEditor(Object.class, null);
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(550);
        ThemeManager.styleTableHeader(table);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        add(scroll, BorderLayout.CENTER);

        // ---------- MOUSE CLICK HANDLER ----------
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row >= 0 && e.getClickCount() == 2) {
                    handleRowClick(row);
                }
            }
        });

        loadNotifications();
    }

    private void loadNotifications() {
        model.setRowCount(0);
        String sql = """
                SELECT notice_id, notice_date, message
                FROM ug_notification
                WHERE ug_id = ?
                ORDER BY notice_date DESC
                """;

        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, currentUser);
            ResultSet rs = pst.executeQuery();

            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("notice_date");
                String formatted = ts != null ? ts.toLocalDateTime().format(fmt) : "";
                model.addRow(new Object[]{
                        rs.getInt("notice_id"),
                        formatted,
                        rs.getString("message")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading notifications: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void handleRowClick(int row) {
        int id = (int) model.getValueAt(row, 0);
        String message = (String) model.getValueAt(row, 2);

        int first = JOptionPane.showConfirmDialog(
                this,
                "<html><b>Message:</b><br>" + message + "<br><br>"
                        + "Do you want to mark this message as read?</html>",
                "Mark as Read?",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (first != JOptionPane.YES_OPTION) return;

        int second = JOptionPane.showConfirmDialog(
                this,
                "If you mark this as read, it will be removed from notifications.\nContinue?",
                "Confirm Remove",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (second != JOptionPane.YES_OPTION) return;

        deleteNotification(id);
    }

    private void deleteNotification(int id) {
        String sql = "DELETE FROM ug_notification WHERE notice_id = ?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, id);
            int r = pst.executeUpdate();
            if (r > 0) {
                JOptionPane.showMessageDialog(this,
                        "Notification marked as read and removed successfully.",
                        "Removed", JOptionPane.INFORMATION_MESSAGE);
                loadNotifications();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error removing notification: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}