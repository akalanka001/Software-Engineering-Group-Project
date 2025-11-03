package Undergraduate.components;

import Database.dbconnection;
import Utils.ThemeManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import Undergraduate.components.CertificateService;

public class CoursePanel extends JPanel {

    private final String currentUser;
    private JTable moduleTable;
    private DefaultTableModel moduleModel;
    private JComboBox<String> courseComboBox;
    private JButton loadButton;
    private JProgressBar progressBar;
    private JLabel progressLabel;
    private JLabel modulesCountLabel; // NEW label: “n modules”

    public CoursePanel(String currentUser) {
        this.currentUser = currentUser;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 15, 10, 15));
        setBackground(UIManager.getColor("Panel.background"));

        // Title
        JLabel title = new JLabel("My Course Modules");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(UIManager.getColor("Label.foreground"));
        add(title, BorderLayout.NORTH);

        // Course selector
        JPanel selector = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        selector.setOpaque(false);

        courseComboBox = new JComboBox<>();
        courseComboBox.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        styleComboBox(courseComboBox);

        loadButton = new JButton("Load Modules");
        ThemeManager.stylePrimaryButton(loadButton);
        loadButton.setPreferredSize(new Dimension(140, 36));

        modulesCountLabel = new JLabel("0 modules");
        modulesCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        modulesCountLabel.setForeground(UIManager.getColor("Label.foreground"));

        selector.add(new JLabel("Course:"));
        selector.add(courseComboBox);
        selector.add(loadButton);
        selector.add(modulesCountLabel);
        add(selector, BorderLayout.BEFORE_FIRST_LINE);

        // Progress panel
        JPanel progressPanel = new JPanel(new BorderLayout(5, 5));
        progressPanel.setOpaque(false);
        progressPanel.setBorder(new EmptyBorder(10, 0, 10, 0));

        progressLabel = new JLabel("Progress: 0 / 0");
        progressLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        progressLabel.setForeground(UIManager.getColor("Label.foreground"));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        progressBar.setForeground(ThemeManager.LIGHT_BLUE);

        progressPanel.add(progressLabel, BorderLayout.WEST);
        progressPanel.add(progressBar, BorderLayout.CENTER);

        // Table setup
        String[] cols = {"#", "Module Title", "Status", "Action"};
        moduleModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int col) {
                return col == 3;
            }
        };
        moduleTable = new JTable(moduleModel);
        moduleTable.setRowHeight(32);
        moduleTable.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        moduleTable.setGridColor(new Color(230, 230, 230));
        ThemeManager.styleTableHeader(moduleTable);

        moduleTable.getColumn("Action").setCellRenderer(new ButtonRenderer());
        moduleTable.getColumn("Action").setCellEditor(new ButtonEditor(new JCheckBox()));

        JScrollPane scroll = new JScrollPane(moduleTable);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));

        JPanel centerPanel = new JPanel(new BorderLayout(3, 3));
        centerPanel.setOpaque(false);
        centerPanel.add(progressPanel, BorderLayout.NORTH);
        centerPanel.add(scroll, BorderLayout.CENTER);
        add(centerPanel, BorderLayout.CENTER);

        loadButton.addActionListener(e -> loadModules());
        loadCourseCombo();

        // Optional: live update label when changing course
        courseComboBox.addActionListener(e -> {
            String selectedCourse = (String) courseComboBox.getSelectedItem();
            if (selectedCourse != null) updateModulesCountLabel(selectedCourse);
        });
    }

    // ------------------------------------------------------------
    private void styleComboBox(JComboBox<?> combo) {
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                                                          int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                setBorder(new EmptyBorder(5, 10, 5, 10));
                return this;
            }
        });
    }

    private static class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setFont(new Font("Segoe UI", Font.PLAIN, 13));
            setBorder(new EmptyBorder(6, 12, 6, 12));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus, int row, int col) {
            setText(value == null ? "" : value.toString());
            ThemeManager.stylePrimaryButton(this);
            return this;
        }
    }

    private class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private int currentRow;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            button.setBorder(new EmptyBorder(6, 12, 6, 12));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            ThemeManager.stylePrimaryButton(button);
            button.addActionListener(e -> {
                if (moduleTable.isEditing()) fireEditingStopped();
                SwingUtilities.invokeLater(() -> openVideoFrame(currentRow));
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int col) {
            button.setText(value == null ? "" : value.toString());
            currentRow = row;
            return button;
        }

        @Override public Object getCellEditorValue() { return button.getText(); }
    }

    // ------------------------------------------------------------
    private void loadCourseCombo() {
        courseComboBox.removeAllItems();
        String sql = """
            SELECT c.course_name
            FROM enrollment e
            JOIN course c ON e.course_id = c.course_id
            WHERE e.ug_id = ? AND e.approval_status = 'approved'
            """;
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, currentUser);
            ResultSet rs = pst.executeQuery();
            while (rs.next())
                courseComboBox.addItem(rs.getString("course_name"));
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    // helper: update “n modules” label
    private void updateModulesCountLabel(String courseName) {
        String sql = "SELECT modules FROM course WHERE course_name = ?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, courseName);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                int count = rs.getInt("modules");
                modulesCountLabel.setText(count + (count == 1 ? " module" : " modules"));
            } else {
                modulesCountLabel.setText("0 modules");
            }
        } catch (SQLException ex) {
            modulesCountLabel.setText("?");
            System.err.println("updateModulesCountLabel: " + ex.getMessage());
        }
    }

    // ------------------------------------------------------------
    private void loadModules() {
        String selectedCourse = (String) courseComboBox.getSelectedItem();
        if (selectedCourse == null) return;
        moduleModel.setRowCount(0);

        // get total (from course table)
        int expectedModules = getExpectedModuleCount(selectedCourse);
        updateModulesCountLabel(selectedCourse);

        String sql = """
            SELECT m.module_id, m.title,
                   IFNULL(mp.completed, FALSE) AS completed
            FROM module m
            JOIN course c ON m.course_id = c.course_id
            LEFT JOIN module_progress mp
              ON mp.module_id = m.module_id AND mp.ug_id = ?
            WHERE c.course_name = ?
            ORDER BY m.module_id
            """;
        int done = 0;
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, currentUser);
            pst.setString(2, selectedCourse);
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                boolean completed = rs.getBoolean("completed");
                if (completed) done++;
                String label = completed ? "Completed" : "Pending";
                String action = completed ? "View Again" : "Start";
                moduleModel.addRow(new Object[]{
                        rs.getInt("module_id"),
                        rs.getString("title"),
                        label,
                        action
                });
            }
            updateProgress(done, expectedModules);
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private int getExpectedModuleCount(String courseName) {
        String sql = "SELECT modules FROM course WHERE course_name = ?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, courseName);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt("modules");
        } catch (SQLException ex) {
            System.err.println("getExpectedModuleCount: " + ex.getMessage());
        }
        return 0;
    }

    private void updateProgress(int done, int totalExpected) {
        progressLabel.setText("Progress: " + done + " / " + totalExpected);
        int percent = totalExpected > 0 ? (int) ((done * 100.0) / totalExpected) : 0;
        progressBar.setValue(percent);

        // 🎓 If course fully completed, generate certificate
        if (done == totalExpected && totalExpected > 0) {
            // Use any module to resolve course_id
            if (moduleModel.getRowCount() > 0) {
                int firstModuleId = (int) moduleModel.getValueAt(0, 0);
                String courseId = resolveCourseIdForModule(firstModuleId);
                CertificateService.issueCertificate(currentUser, courseId);
                JOptionPane.showMessageDialog(this,
                        "🎉 Congratulations! You’ve completed this course.\nYour certificate has been generated.");
            }
        }
    }

    // ------------------------------------------------------------
    private void openVideoFrame(int row) {
        int moduleId = (int) moduleModel.getValueAt(row, 0);
        String moduleTitle = (String) moduleModel.getValueAt(row, 1);

        String videoPath = null;
        String sql = "SELECT video_path FROM module WHERE module_id=?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, moduleId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                videoPath = rs.getString("video_path");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
            return;
        }

        if (videoPath == null || videoPath.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Warning: No video found for module: " + moduleTitle);
            return;
        }

        launchPlayer(moduleTitle, videoPath, moduleId);
    }

    private void launchPlayer(String title, String path, int id) {
        SwingUtilities.invokeLater(() -> {
            new RestrictedVideoPlayerFrame(title, path, () -> {
                markModuleCompleted(id);
                loadModules();
            });
        });
    }

    // ------------------------------------------------------------
    private void markModuleCompleted(int moduleId) {
        String courseId = resolveCourseIdForModule(moduleId);
        if (courseId == null) {
            JOptionPane.showMessageDialog(this, "Could not determine course for module.");
            return;
        }

        String sql = """
            INSERT INTO module_progress(ug_id, course_id, module_id, completed, completed_at)
            VALUES (?, ?, ?, TRUE, NOW())
            ON DUPLICATE KEY UPDATE
                completed=TRUE,
                completed_at=NOW(),
                course_id=VALUES(course_id)
            """;
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, currentUser);
            pst.setString(2, courseId);
            pst.setInt(3, moduleId);
            pst.executeUpdate();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        }
    }

    private String resolveCourseIdForModule(int moduleId) {
        String sql = "SELECT course_id FROM module WHERE module_id=?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, moduleId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getString("course_id");
        } catch (SQLException e) {
            System.err.println("resolveCourseIdForModule: " + e.getMessage());
        }
        return null;
    }
}