package Lecturer.components;

import Database.dbconnection;
import Utils.ThemeManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.*;

/**
 * Lecturer panel for uploading and managing course video modules.
 *  - Upload / delete authenticated by lecturer password
 *  - Removes dependent progress safely
 *  - Sends notifications to enrolled undergraduates on add/update/delete
 */
public class LecturerVideoPanel extends JPanel {

    private final String lecturerId;
    private JComboBox<String> courseComboBox;
    private JTable moduleTable;
    private DefaultTableModel model;
    private JButton addBtn, deleteBtn, refreshBtn;

    public LecturerVideoPanel(String lecturerId) {
        this.lecturerId = lecturerId;
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));
        initUI();
        loadCourses();
    }

    // ---------------------------------------------------------------------
    private void initUI() {
        JLabel title = new JLabel("Upload Course Videos (Modules)");
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        add(title, BorderLayout.NORTH);

        // Top bar
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        courseComboBox = new JComboBox<>();
        top.add(new JLabel("Course:"));
        top.add(courseComboBox);

        addBtn = new JButton("Add Video");
        deleteBtn = new JButton("Delete");
        refreshBtn = new JButton("Reload");
        ThemeManager.stylePrimaryButton(addBtn);
        ThemeManager.stylePrimaryButton(deleteBtn);
        ThemeManager.stylePrimaryButton(refreshBtn);

        top.add(addBtn);
        top.add(deleteBtn);
        top.add(refreshBtn);
        add(top, BorderLayout.BEFORE_FIRST_LINE);

        // Table
        model = new DefaultTableModel(new String[]{"ID", "Module Title", "Video Path"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        moduleTable = new JTable(model);
        moduleTable.setRowHeight(26);
        ThemeManager.styleTableHeader(moduleTable);
        JScrollPane scroll = new JScrollPane(moduleTable);
        scroll.setPreferredSize(new Dimension(750, 280));
        add(scroll, BorderLayout.CENTER);

        // Listeners
        refreshBtn.addActionListener(e -> reloadModules());
        courseComboBox.addActionListener(e -> reloadModules());
        addBtn.addActionListener(e -> addModule());
        deleteBtn.addActionListener(e -> deleteModule());
    }

    // ---------------------------------------------------------------------
    private void loadCourses() {
        courseComboBox.removeAllItems();
        String sql = """
            SELECT course_id, course_name
            FROM course
            WHERE lec_id=?
            ORDER BY course_name
            """;
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, lecturerId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                String c = rs.getString("course_name") + " (" + rs.getString("course_id") + ")";
                courseComboBox.addItem(c);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Load Courses", JOptionPane.ERROR_MESSAGE);
        }
        reloadModules();
    }

    private String extractCourseId() {
        Object sel = courseComboBox.getSelectedItem();
        if (sel == null) return null;
        String str = sel.toString();
        int a = str.indexOf('(');
        int b = str.indexOf(')');
        return (a != -1 && b != -1) ? str.substring(a + 1, b) : str;
    }

    // ---------------------------------------------------------------------
    private void reloadModules() {
        model.setRowCount(0);
        if (courseComboBox.getItemCount() == 0) return;
        String courseId = extractCourseId();
        if (courseId == null) return;

        String sql = "SELECT module_id, title, video_path FROM module WHERE course_id=?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, courseId);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getInt("module_id"),
                        rs.getString("title"),
                        rs.getString("video_path")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Load Modules", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------------------------------------------------------------
    private void addModule() {
        if (courseComboBox.getItemCount() == 0) {
            JOptionPane.showMessageDialog(this, "No course selected!");
            return;
        }
        String courseId = extractCourseId();

        JTextField titleField = new JTextField(20);
        JTextField pathField = new JTextField(20);
        JButton browse = new JButton("Browse...");
        ThemeManager.stylePrimaryButton(browse);

        browse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int res = fc.showOpenDialog(this);
            if (res == JFileChooser.APPROVE_OPTION) {
                try {
                    File selectedFile = fc.getSelectedFile();
                    File destDir = new File(System.getProperty("user.dir"), "videos");
                    destDir.mkdirs();
                    File dest = new File(destDir, selectedFile.getName());
                    Files.copy(selectedFile.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    pathField.setText("videos/" + selectedFile.getName());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(this, "Copy error: " + ex.getMessage());
                }
            }
        });

        JPanel panel = new JPanel(new GridLayout(2, 2, 6, 6));
        panel.add(new JLabel("Module Title:"));
        panel.add(titleField);
        panel.add(browse);
        panel.add(pathField);

        int res = JOptionPane.showConfirmDialog(this, panel, "Add Video Module",
                JOptionPane.OK_CANCEL_OPTION);
        if (res != JOptionPane.OK_OPTION) return;

        String title = titleField.getText().trim();
        String path = pathField.getText().trim();
        if (title.isEmpty() || path.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Title and video are required.");
            return;
        }

        String sql = "INSERT INTO module(course_id, title, video_path) VALUES(?,?,?)";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, courseId);
            pst.setString(2, title);
            pst.setString(3, path);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Video module added!");
            notifyStudents(courseId,
                    "A new video module \"" + title + "\" has been uploaded in your course (" + courseId + ").");
            reloadModules();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Add Module", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------------------------------------------------------------
    private void deleteModule() {
        int row = moduleTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a module!");
            return;
        }
        int moduleId = (int) model.getValueAt(row, 0);
        String courseId = extractCourseId();

        // Password verification
        JPasswordField passField = new JPasswordField();
        JPanel p = new JPanel(new GridLayout(2, 1));
        p.add(new JLabel("Enter your password to confirm deletion:"));
        p.add(passField);
        int chk = JOptionPane.showConfirmDialog(this, p, "Confirm Delete",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
        if (chk != JOptionPane.OK_OPTION) return;

        String entered = new String(passField.getPassword());
        if (!verifyLecturerPassword(entered)) {
            JOptionPane.showMessageDialog(this, "Incorrect password!",
                    "Authentication Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

        int sure = JOptionPane.showConfirmDialog(this,
                "Deleting this video will also reset all student progress.\nProceed?",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (sure != JOptionPane.YES_OPTION) return;

        try (Connection con = dbconnection.getConnection()) {
            // delete dependent student progress first
            try (PreparedStatement p1 = con.prepareStatement("DELETE FROM module_progress WHERE module_id=?")) {
                p1.setInt(1, moduleId);
                p1.executeUpdate();
            }
            // delete actual module
            try (PreparedStatement p2 = con.prepareStatement("DELETE FROM module WHERE module_id=?")) {
                p2.setInt(1, moduleId);
                p2.executeUpdate();
            }
            JOptionPane.showMessageDialog(this, "Module deleted successfully!");
            notifyStudents(courseId,
                    "A video module has been removed from your course (" + courseId + ").");
            reloadModules();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Delete error: " + e.getMessage(),
                    "Delete Module", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---------------------------------------------------------------------
    // Utility helpers
    // ---------------------------------------------------------------------
    private boolean verifyLecturerPassword(String pass) {
        String sql = "SELECT user_password FROM user WHERE user_id=?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, lecturerId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String stored = rs.getString("user_password");
                String enteredHash = Admin.encryption.hashpassword(pass);
                return stored.equals(enteredHash);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Password verification error: " + e.getMessage());
        }
        return false;
    }

    /** Notify all enrolled undergraduates about a change in course materials. */
    private void notifyStudents(String courseId, String message) {
        String select = "SELECT DISTINCT ug_id FROM enrollment "
                + "WHERE course_id=? AND approval_status='approved'";
        String insert = "INSERT INTO ug_notification (ug_id, message) VALUES (?, ?)";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement psSel = con.prepareStatement(select);
             PreparedStatement psIns = con.prepareStatement(insert)) {

            psSel.setString(1, courseId);
            ResultSet rs = psSel.executeQuery();
            while (rs.next()) {
                psIns.setString(1, rs.getString("ug_id"));
                psIns.setString(2, message);
                psIns.addBatch();
            }
            psIns.executeBatch();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Notification send failure: " + e.getMessage(),
                    "Notify Students", JOptionPane.WARNING_MESSAGE);
        }
    }
}