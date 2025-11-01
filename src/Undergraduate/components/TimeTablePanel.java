package Undergraduate.components;

import Database.dbconnection;
import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import Utils.ThemeManager;

public class TimeTablePanel extends JPanel {

    private final JTable table;
    private final DefaultTableModel model;

    public TimeTablePanel(dbconnection dbConnector) {
        setLayout(new BorderLayout(0, 15));
        setBackground(UIManager.getColor("Panel.background"));

        // ---------- Title ----------
        JLabel title = new JLabel("Weekly Time Table");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setBorder(BorderFactory.createEmptyBorder(15, 20, 0, 20));
        add(title, BorderLayout.NORTH);

        // ---------- Table (no header) ----------
        model = new DefaultTableModel(new Object[]{
                "Day", "Start", "End", "Course ID", "Type", "Lecturer", "Department"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // Disable editing
            }
        };
        table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setRowHeight(28);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 5));

        // Hide header for minimalist look
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 0));
        header.setVisible(false);
        table.setTableHeader(null);

        // Alternate row colors
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private final Color ALT_ROW_COLOR = new Color(247, 248, 252);
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : ALT_ROW_COLOR);
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(ThemeManager.LIGHT_BLUE);
                    c.setForeground(Color.WHITE);
                }
                setBorder(noFocusBorder);
                return c;
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        sp.getViewport().setBackground(Color.WHITE);

        add(sp, BorderLayout.CENTER);
        loadData();
    }

    // ---------------------------------------------------------------------
    // Database loading logic
    // ---------------------------------------------------------------------
    private void loadData() {
        String sql = """
            SELECT day, start_time, end_time, course_id, session_type, lec_id, department 
            FROM time_table 
            ORDER BY FIELD(day,'Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'), start_time
            """;
        try (Connection con = dbconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("day"),
                        rs.getString("start_time"),
                        rs.getString("end_time"),
                        rs.getString("course_id"),
                        rs.getString("session_type"),
                        rs.getString("lec_id"),
                        rs.getString("department")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "DB Error: " + e.getMessage());
        }
    }
}