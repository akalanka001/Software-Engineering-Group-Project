package Admin.ui;

import Database.dbconnection;
import Utils.ThemeManager;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * Admin notification view panel.
 * Shows date/time and message (newest first),
 * with FlatLaf blue header style and tooltips on hover.
 */
public class NotificationPanel extends JPanel {

    private JTable table;
    private DefaultTableModel tableModel;

    public NotificationPanel(String user) {
        setLayout(new BorderLayout(10, 10));
        setBackground(Color.WHITE);

        // --- Table model: only date/time and message ---
        tableModel = new DefaultTableModel(new Object[]{"Date / Time", "Message"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        table = new JTable(tableModel) {
            // --- Tooltip for message text ---
            @Override
            public String getToolTipText(java.awt.event.MouseEvent e) {
                java.awt.Point p = e.getPoint();
                int rowIndex = rowAtPoint(p);
                int colIndex = columnAtPoint(p);
                if (rowIndex >= 0 && colIndex >= 0) {
                    Object value = getValueAt(rowIndex, colIndex);
                    if (value != null) return value.toString();
                }
                return null;
            }
        };

        // --- Table styling ---
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(30);
        table.setFillsViewportHeight(true);
        table.setGridColor(new Color(230, 230, 230));
        table.setSelectionBackground(ThemeManager.LIGHT_BLUE.brighter());
        table.setSelectionForeground(Color.BLACK);

        // --- Align column content ---
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.getColumnModel().getColumn(0).setCellRenderer(centerRenderer); // date/time centered

        // --- Header styling ---
        ThemeManager.styleTableHeader(table);

        // --- Scroll panel ---
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(scrollPane, BorderLayout.CENTER);

        loadNotifications();
    }

    // --------------------------------------------------------------------
    //  Fetch notifications from database, newest first
    // --------------------------------------------------------------------
    private void loadNotifications() {
        tableModel.setRowCount(0);
        String sql = "SELECT notice_date, message FROM admin_notification ORDER BY notice_date DESC";
        try (Connection conn = dbconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Timestamp date = rs.getTimestamp("notice_date");
                String msg = rs.getString("message");
                tableModel.addRow(new Object[]{date, msg});
            }

        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading notifications from database.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}