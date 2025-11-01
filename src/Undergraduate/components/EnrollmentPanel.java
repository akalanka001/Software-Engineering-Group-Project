package Undergraduate.components;

import Database.dbconnection;
import Utils.ThemeManager;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.sql.*;
import java.time.format.DateTimeFormatter;

public class EnrollmentPanel extends JPanel {

    private final String currentUser;
    private JTable enrolledTable;
    private JTable availableTable;
    private DefaultTableModel enrolledModel;
    private DefaultTableModel availableModel;
    private JComboBox<String> courseCombo;
    private JTextField fileField;
    private JButton browseBtn;
    private JButton enrollBtn;
    private File selectedFile;

    public EnrollmentPanel(String currentUser) {
        this.currentUser = currentUser;
        setPreferredSize(new Dimension(800,600));
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        // ---------- Top: Enrolled Courses ----------
        enrolledModel = new DefaultTableModel(new Object[]{
                "Course ID","Course Name","Enroll Date","Status","Course Fee"
        }, 0);
        enrolledTable = new JTable(enrolledModel);
        enrolledTable.setRowHeight(28);
        enrolledTable.setDefaultEditor(Object.class,null);
        ThemeManager.styleTableHeader(enrolledTable);
        enrolledTable.getColumnModel().getColumn(3)
                .setCellRenderer(new StatusRenderer());

        JScrollPane enrolledScroll = new JScrollPane(enrolledTable);
        enrolledScroll.setBorder(BorderFactory.createTitledBorder("Enrolled Courses"));
        enrolledScroll.setPreferredSize(new Dimension(0,150));

        // ---------- Middle: New Courses (Not Enrolled) ----------
        availableModel = new DefaultTableModel(new Object[]{
                "Course ID","Course Name","Course Fee"
        },0);
        availableTable = new JTable(availableModel);
        availableTable.setRowHeight(28);
        availableTable.setDefaultEditor(Object.class,null);
        ThemeManager.styleTableHeader(availableTable);

        JScrollPane availableScroll = new JScrollPane(availableTable);
        availableScroll.setBorder(BorderFactory.createTitledBorder("New Courses"));
        availableScroll.setPreferredSize(new Dimension(0,150));

        // ---------- Bottom: Enrollment Form ----------
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder("Enroll In Course"));
        formPanel.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8,8,8,8);
        gbc.anchor = GridBagConstraints.WEST;

        JLabel cLabel = new JLabel("Course ID:");
        courseCombo = new JComboBox<>();

        JLabel fLabel = new JLabel("Payment Slip:");
        fileField = new JTextField(25);
        fileField.setEditable(false);
        browseBtn = new JButton("Browse...");
        ThemeManager.stylePrimaryButton(browseBtn);
        enrollBtn = new JButton("Enroll");
        ThemeManager.stylePrimaryButton(enrollBtn);

        browseBtn.addActionListener(e -> chooseFile());
        enrollBtn.addActionListener(e -> enrollCourse());

        // row 0
        gbc.gridx=0; gbc.gridy=0;
        formPanel.add(cLabel,gbc);
        gbc.gridx=1;
        formPanel.add(courseCombo,gbc);

        // row 1
        gbc.gridx=0; gbc.gridy=1;
        formPanel.add(fLabel,gbc);
        gbc.gridx=1;
        formPanel.add(fileField,gbc);
        gbc.gridx=2;
        formPanel.add(browseBtn,gbc);
        gbc.gridx=3;
        formPanel.add(enrollBtn,gbc);

        // ---------- Combine 3 sections vertically ----------
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.add(enrolledScroll);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(availableScroll);
        centerPanel.add(Box.createVerticalStrut(8));
        centerPanel.add(formPanel);

        JScrollPane mainScroll = new JScrollPane(centerPanel);
        mainScroll.getVerticalScrollBar().setUnitIncrement(16);
        mainScroll.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

        add(mainScroll, BorderLayout.CENTER);

        loadEnrolledCourses();
        loadAvailableCourses();
    }

    // ----------------------------------------------------------------------
    private void loadEnrolledCourses() {
        enrolledModel.setRowCount(0);
        String sql = """
            SELECT e.course_id, c.course_name, e.enroll_date,
                   e.approval_status, c.course_fee
            FROM enrollment e
            JOIN course c ON e.course_id = c.course_id
            WHERE e.ug_id = ?
            ORDER BY e.enroll_date DESC
            """;
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, currentUser);
            ResultSet rs = pst.executeQuery();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
            while (rs.next()) {
                enrolledModel.addRow(new Object[]{
                        rs.getString("course_id"),
                        rs.getString("course_name"),
                        rs.getTimestamp("enroll_date") != null ?
                                rs.getTimestamp("enroll_date").toLocalDateTime().format(fmt) : "",
                        rs.getString("approval_status"),
                        rs.getDouble("course_fee")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,"Error loading enrolled courses: "+e.getMessage());
        }
    }

    // ----------------------------------------------------------------------
    private void loadAvailableCourses() {
        availableModel.setRowCount(0);
        courseCombo.removeAllItems();

        String sql = """
        SELECT c.course_id, c.course_name, c.course_fee
        FROM course c
        LEFT JOIN enrollment e 
               ON c.course_id = e.course_id 
              AND e.ug_id = ?
        WHERE e.course_id IS NULL 
           OR e.approval_status = 'rejected'
        ORDER BY c.course_id
        """;

        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, currentUser);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                String cid = rs.getString("course_id");
                availableModel.addRow(new Object[]{
                        cid,
                        rs.getString("course_name"),
                        rs.getDouble("course_fee")
                });
                courseCombo.addItem(cid);
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading available courses: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ----------------------------------------------------------------------
    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Payment Slip (PDF/Image)");
        int r = chooser.showOpenDialog(this);
        if (r==JFileChooser.APPROVE_OPTION) {
            selectedFile = chooser.getSelectedFile();
            fileField.setText(selectedFile.getAbsolutePath());
        }
    }

    // ----------------------------------------------------------------------
    // ----------------------------------------------------------------------
    private void enrollCourse() {
        String selectedCourse = (String) courseCombo.getSelectedItem();
        if (selectedCourse == null || selectedCourse.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please select a course.");
            return;
        }
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this, "Please attach your payment slip before enrolling.");
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to enroll in " + selectedCourse + "?",
                "Confirm Enrollment", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        // ✅ include payment_slip column and value
        String sql = """
        INSERT INTO enrollment
        (ug_id, course_id, approved_by, approval_status, payment_slip)
        VALUES (?, ?, NULL, DEFAULT, ?)
        """;

        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, currentUser);
            pst.setString(2, selectedCourse);
            pst.setString(3, "src/paymentSlips/" + selectedFile.getName());
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this,
                    "Thank you!\nWaiting for admin approval please.");

            // refresh
            loadEnrolledCourses();
            loadAvailableCourses();

            // clear form
            fileField.setText("");
            selectedFile = null;

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error enrolling: " + e.getMessage());
        }
    }

    // ----------------------------------------------------------------------
    static class StatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                    table,value,isSelected,hasFocus,row,column);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            String status = value==null?"":value.toString().toLowerCase();
            Color approved = new Color(0x28A745);
            Color pending  = new Color(0xF39C12);
            Color rejected = new Color(0xE74C3C);
            if(!isSelected){
                switch(status){
                    case "approved" -> lbl.setForeground(approved);
                    case "pending"  -> lbl.setForeground(pending);
                    case "rejected" -> lbl.setForeground(rejected);
                    default -> lbl.setForeground(UIManager.getColor("Label.foreground"));
                }
            }
            return lbl;
        }
    }
}