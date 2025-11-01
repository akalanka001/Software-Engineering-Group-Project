package Lecturer.components;

import Utils.ThemeManager;
import Database.dbconnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

public class LecturerNotificationPanel extends JPanel {

    private final String lecId;
    private final JTable table;
    private final DefaultTableModel model;

    public LecturerNotificationPanel(String lecId) {
        this.lecId = lecId;
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        model = new DefaultTableModel(new String[]{"Date", "Message"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        ThemeManager.styleTableHeader(table);

        JScrollPane scroll = new JScrollPane(table);
        add(scroll, BorderLayout.CENTER);

        JButton refreshBtn = new JButton("Refresh");
        ThemeManager.stylePrimaryButton(refreshBtn);
        refreshBtn.addActionListener(e -> loadNotifications());
        add(refreshBtn, BorderLayout.SOUTH);

        loadNotifications();
    }

    private void loadNotifications() {
        model.setRowCount(0);
        String sql = "SELECT notice_date, message "
                + "FROM lec_notification "
                + "WHERE lec_id=? ORDER BY notice_date DESC";

        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, lecId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getTimestamp("notice_date"),
                            rs.getString("message")
                    });
                }
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this,
                    "Error loading notifications:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}