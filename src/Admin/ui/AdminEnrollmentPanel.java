package Admin.ui;

import Database.dbconnection;
import Utils.ThemeManager;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.*;
import java.time.format.DateTimeFormatter;

public class AdminEnrollmentPanel extends JPanel {

    private final String currentUser;      // admin ID
    private DefaultTableModel model;
    private JTable table;
    private JTextArea reasonArea;
    private JButton approveBtn, rejectBtn;

    public AdminEnrollmentPanel(String currentUser) {
        this.currentUser = currentUser;

        setLayout(new BorderLayout(10,10));
        setBackground(UIManager.getColor("Panel.background"));

        JLabel title = new JLabel("Pending Enrollments");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setBorder(BorderFactory.createEmptyBorder(10, 20, 0, 0));
        add(title, BorderLayout.NORTH);

        // ---------- TABLE ----------
        model = new DefaultTableModel(new Object[]{
                "User ID","Course ID","Course Name","Course Fee","Date","Payment Slip"
        },0);
        table = new JTable(model){
            @Override public boolean isCellEditable(int r,int c){ return false; }
        };
        table.setRowHeight(28);
        ThemeManager.styleTableHeader(table);

        // renderer for “Payment Slip” column as button-look
        table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable tbl, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                JButton btn = new JButton("View");
                ThemeManager.stylePrimaryButton(btn);
                btn.setFocusable(false);
                return btn;
            }
        });

        // ✅ robust click listener: opens file on any click
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (row >= 0 && col == 5) {
                    String filePath = (String) model.getValueAt(row, 5);
                    if (filePath == null || filePath.isBlank()) {
                        JOptionPane.showMessageDialog(AdminEnrollmentPanel.this,
                                "No payment slip path found for this record.");
                        return;
                    }
                    File file = new File(filePath);
                    if (!file.exists()) {
                        JOptionPane.showMessageDialog(AdminEnrollmentPanel.this,
                                "File not found: " + filePath);
                        return;
                    }
                    try {
                        Desktop.getDesktop().open(file);
                    } catch (Exception ex) {
                        JOptionPane.showMessageDialog(AdminEnrollmentPanel.this,
                                "Cannot open file:\n" + ex.getMessage());
                    }
                }
            }
        });

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        add(scroll, BorderLayout.CENTER);

        // ---------- BOTTOM: reason + buttons ----------
        JPanel bottom = new JPanel(new BorderLayout(10,10));
        bottom.setBorder(BorderFactory.createEmptyBorder(0,20,20,20));

        reasonArea = new JTextArea(3,40);
        reasonArea.setBorder(BorderFactory.createTitledBorder("Reject Reason (required if rejecting)"));
        JScrollPane reasonScroll = new JScrollPane(reasonArea);
        bottom.add(reasonScroll, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,15,10));
        approveBtn = new JButton("Approve");
        rejectBtn  = new JButton("Reject");
        ThemeManager.stylePrimaryButton(approveBtn);
        ThemeManager.stylePrimaryButton(rejectBtn);
        btnPanel.add(approveBtn);
        btnPanel.add(rejectBtn);
        bottom.add(btnPanel, BorderLayout.SOUTH);
        add(bottom, BorderLayout.SOUTH);

        approveBtn.addActionListener(e -> handleApproval(true));
        rejectBtn.addActionListener(e -> handleApproval(false));

        loadPending();
    }

    // ----------------------------------------------------------------------
    private void loadPending() {
        model.setRowCount(0);
        String sql = """
                SELECT e.ug_id, e.course_id, c.course_name, c.course_fee,
                       e.enroll_date, e.payment_slip
                FROM enrollment e
                JOIN course c ON e.course_id = c.course_id
                WHERE e.approval_status='pending'
                ORDER BY e.enroll_date DESC
                """;
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql);
             ResultSet rs = pst.executeQuery()) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("ug_id"),
                        rs.getString("course_id"),
                        rs.getString("course_name"),
                        rs.getDouble("course_fee"),
                        rs.getTimestamp("enroll_date") != null ?
                                rs.getTimestamp("enroll_date").toLocalDateTime().format(fmt) : "",
                        rs.getString("payment_slip")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,"Error loading enrollments: "+e.getMessage());
        }
    }

    // ----------------------------------------------------------------------
    private void handleApproval(boolean approve) {
        int row = table.getSelectedRow();
        if(row == -1){
            JOptionPane.showMessageDialog(this,"Please select a row first.");
            return;
        }

        String ugId = (String) model.getValueAt(row,0);
        String courseId = (String) model.getValueAt(row,1);

        if (approve) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Approve this enrollment for student "+ugId+" ?",
                    "Confirm Approve", JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) return;
            updateStatus(ugId, courseId, "approved", null);
        } else {
            String reason = reasonArea.getText().trim();
            if (reason.isBlank()) {
                JOptionPane.showMessageDialog(this,"Reject reason is required!");
                return;
            }
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Reject this enrollment for "+ugId+" ?",
                    "Confirm Rejection", JOptionPane.YES_NO_OPTION,JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;
            updateStatus(ugId, courseId, "rejected", reason);
        }
        reasonArea.setText("");
        loadPending();
    }

    // ----------------------------------------------------------------------
    private void updateStatus(String ugId,String courseId,String status,String reason){
        try (Connection con = dbconnection.getConnection()) {

            // update enrollment status
            try (PreparedStatement pst = con.prepareStatement(
                    "UPDATE enrollment SET approval_status=?, approved_by=? WHERE ug_id=? AND course_id=?")) {
                pst.setString(1,status);
                pst.setString(2,currentUser);
                pst.setString(3,ugId);
                pst.setString(4,courseId);
                pst.executeUpdate();
            }

            // message for notification
            String message;
            if ("rejected".equalsIgnoreCase(status)) {
                message = "Your enrollment for course "+courseId+" has been rejected. Reason: "+reason;
            } else {
                message = "Your enrollment for course "+courseId+" has been approved.";
            }

            try (PreparedStatement pst2 = con.prepareStatement(
                    "INSERT INTO ug_notification (ug_id,message) VALUES (?,?)")) {
                pst2.setString(1, ugId);
                pst2.setString(2, message);
                pst2.executeUpdate();
            }

            JOptionPane.showMessageDialog(this,"Status updated to "+status.toUpperCase()+".");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,"Database Error: "+e.getMessage());
        }
    }
}