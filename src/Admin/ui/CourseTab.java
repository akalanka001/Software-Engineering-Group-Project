package Admin.ui;

import Admin.Service.AdminService;
import Admin.Alerts;
import Admin.Service.CourseService;
import Utils.ThemeManager;
import Database.dbconnection;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

public class CourseTab extends JPanel {


    private final JTable table = new JTable();
    private final String currentUser;

    public CourseTab(String username) {
        this.currentUser = username;
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        // ----- Table -----
        table.setModel(new DefaultTableModel(new Object[][]{},
                new String[]{"Course ID", "Course Name", "Lecturer ID", "Modules", "Type", "Fee"}));
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setDefaultEditor(Object.class, null);

        ThemeManager.styleTableHeader(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        add(scroll, BorderLayout.CENTER);

        // ----- Buttons (CRUD) -----
        JButton addBtn    = new JButton("Add");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");

        ThemeManager.stylePrimaryButton(addBtn);
        ThemeManager.stylePrimaryButton(updateBtn);
        ThemeManager.stylePrimaryButton(deleteBtn);

        addBtn.addActionListener(e -> add_courseActionPerformed());
        updateBtn.addActionListener(e -> update_courseActionPerformed());
        deleteBtn.addActionListener(e -> delete_courseActionPerformed());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        btnPanel.setOpaque(false);
        btnPanel.add(addBtn);
        btnPanel.add(updateBtn);
        btnPanel.add(deleteBtn);
        add(btnPanel, BorderLayout.SOUTH);

        loadCourseTable();
    }

    // ------------ load table ------------
    public void loadCourseTable() {
        DefaultTableModel dt = (DefaultTableModel) table.getModel();
        dt.setRowCount(0);
        String sql = "SELECT * FROM course";
        try (Connection con = dbconnection.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getString("course_id"));
                row.add(rs.getString("course_name"));
                row.add(rs.getString("lec_id"));
                row.add(rs.getInt("modules"));
                row.add(rs.getString("course_type"));
                row.add(rs.getDouble("course_fee"));
                dt.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading courses: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------ Add ------------
    private void add_courseActionPerformed() {
        AdminService.addCourseDialog(this);
        loadCourseTable();
    }

    // ------------ Update ------------
    private void update_courseActionPerformed() {
        JTextField idF = new JTextField();
        JTextField nameF = new JTextField();
        JTextField lecF = new JTextField();
        JTextField creditF = new JTextField();
        JComboBox<String> typeBox = new JComboBox<>(new String[]{"Theory", "Practical", "Both"});
        JTextField feeF = new JTextField();

        JPanel panel = new JPanel(new GridLayout(6, 2, 6, 6));
        panel.add(new JLabel("Course ID:"));   panel.add(idF);
        panel.add(new JLabel("Course Name:")); panel.add(nameF);
        panel.add(new JLabel("Lecturer ID:")); panel.add(lecF);
        panel.add(new JLabel("Credit:"));      panel.add(creditF);
        panel.add(new JLabel("Type:"));        panel.add(typeBox);
        panel.add(new JLabel("Course Fee:"));  panel.add(feeF);

        int opt = JOptionPane.showConfirmDialog(this, panel,
                "Update Course", JOptionPane.OK_CANCEL_OPTION);

        if (opt == JOptionPane.OK_OPTION) {
            String id = idF.getText().trim();
            String name = nameF.getText().trim();
            String lec = lecF.getText().trim();
            String creditText = creditF.getText().trim();
            String type = typeBox.getSelectedItem().toString().toLowerCase();
            String feeText = feeF.getText().trim();

            try {
                if (!CourseService.validate(id, name, lec, creditText, type, feeText)) return;

                int credit = Integer.parseInt(creditText);
                double fee = Double.parseDouble(feeText);

                if (CourseService.update(id, name, lec, credit, type, fee)) {
                    Alerts.success("Course updated successfully!");
                    loadCourseTable();
                } else {
                    Alerts.fail("Update failed!");
                }
            } catch (Exception e) {
                Alerts.fail(e.getMessage());
            }
        }
    }

    // ------------ Delete ------------
    private void delete_courseActionPerformed() {
        JTextField idF = new JTextField();
        JPanel panel = new JPanel(new GridLayout(1, 2, 6, 6));
        panel.add(new JLabel("Course ID:")); panel.add(idF);

        int opt = JOptionPane.showConfirmDialog(this, panel,
                "Delete Course", JOptionPane.OK_CANCEL_OPTION);

        if (opt == JOptionPane.OK_OPTION) {
            String id = idF.getText().trim();
            try {
                if (!CourseService.validateId(id)) return;
                if (!CourseService.exists(id)) {
                    Alerts.fail("Course not found!");
                    return;
                }
                if (CourseService.delete(id)) {
                    Alerts.success("Course deleted successfully!");
                    loadCourseTable();
                } else {
                    Alerts.fail("Delete failed!");
                }
            } catch (Exception e) {
                Alerts.fail(e.getMessage());
            }
        }
    }
}