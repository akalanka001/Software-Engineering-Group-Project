package Undergraduate.components;

import Database.dbconnection;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * Transaction history panel: shows all enrollment-payment records for one UG.
 */
public class UGTransactionPanel extends JPanel {

    private final String ugId;
    private final JTable table;
    private final DefaultTableModel model;
    private final JLabel lblTotal = new JLabel("Total: Rs. 0.00");

    public UGTransactionPanel(String ugId) {
        this.ugId = ugId;

        setLayout(new BorderLayout(10,10));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(20,20,20,20));

        JLabel label = new JLabel("Transaction History", SwingConstants.CENTER);
        label.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        label.setForeground(new Color(0x4A90E2));
        add(label, BorderLayout.NORTH);

        model = new DefaultTableModel(
                new Object[]{"Course ID", "Course Name", "Fee (Rs.)", "Enrollment Date", "Status"}, 0
        ){ @Override public boolean isCellEditable(int r,int c){return false;}};

        table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setBackground(new Color(140,170,255));
        table.getTableHeader().setForeground(Color.WHITE);
        table.getTableHeader().setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));

        add(new JScrollPane(table), BorderLayout.CENTER);

        // footer total label
        lblTotal.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 15));
        lblTotal.setForeground(Color.DARK_GRAY);
        lblTotal.setBorder(BorderFactory.createEmptyBorder(5,10,10,10));
        add(lblTotal, BorderLayout.SOUTH);

        loadTransactions();
    }

    private void loadTransactions() {
        model.setRowCount(0);
        double total = 0.0;

        String sql = """
                SELECT e.course_id, c.course_name, c.course_fee, e.enroll_date, e.approval_status
                FROM enrollment e
                JOIN course c ON e.course_id = c.course_id
                WHERE e.ug_id = ?
                ORDER BY e.enroll_date DESC
                """;

        try (Connection conn = dbconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, ugId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String courseId = rs.getString("course_id");
                    String courseName = rs.getString("course_name");
                    double fee = rs.getDouble("course_fee");
                    String date = rs.getString("enroll_date");
                    String status = rs.getString("approval_status");

                    model.addRow(new Object[] {
                            courseId, courseName,
                            String.format("%.2f", fee),
                            date, status
                    });
                    total += fee;
                }
            }

            lblTotal.setText(String.format("Total Enrollment Fees: Rs. %.2f", total));

            if (model.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this,
                        "No enrollment transactions found.",
                        "Information",
                        JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading transaction data.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}