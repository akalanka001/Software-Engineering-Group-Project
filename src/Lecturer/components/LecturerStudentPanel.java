package Lecturer.components;

import Database.dbconnection;
import Lecturer.CRUD_Operation_Interface;
import Utils.ThemeManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Converted from StudentDetails JFrame to a JPanel for Lecturer CardLayout.
 * Functionality unchanged; fits the unified portal layout.
 */
public class LecturerStudentPanel extends JPanel implements CRUD_Operation_Interface {

    private final String currentUserId;

    private JTable tblStudents;
    private JTextField txtSearch;
    private JButton btnSearch;
    private JButton btnRefresh;

    public LecturerStudentPanel(String userId) {
        this.currentUserId = userId;
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));
        initComponents();
        loadAllStudents();
    }

    // ---------------------------------------------------------------------
    //  UI Setup
    // ---------------------------------------------------------------------
    private void initComponents() {
        // ----- Header -----
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(153, 187, 187));
        JLabel title = new JLabel("STUDENT DETAILS", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // ----- Controls -----
        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12));
        controls.setOpaque(false);

        JLabel lblSearch = new JLabel("Search Student:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 15));
        txtSearch = new JTextField(20);

        btnSearch = new JButton("Search");
        ThemeManager.stylePrimaryButton(btnSearch);
        btnSearch.addActionListener(e -> onSearch());

        btnRefresh = new JButton("Refresh");
        ThemeManager.stylePrimaryButton(btnRefresh);
        btnRefresh.addActionListener(e -> onRefresh());

        controls.add(lblSearch);
        controls.add(txtSearch);
        controls.add(btnSearch);
        controls.add(btnRefresh);

        // ----- Table -----
        tblStudents = new JTable(new DefaultTableModel(
                new Object[][]{},
                new String[]{"Student ID", "Name", "Phone Number", "Email"}
        ));
        tblStudents.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblStudents.setRowHeight(25);
        ThemeManager.styleTableHeader(tblStudents); // apply blue header
        JScrollPane scrollPane = new JScrollPane(tblStudents);
        scrollPane.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // ----- Main content -----
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(controls, BorderLayout.NORTH);
        main.add(scrollPane, BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);
    }

    // ---------------------------------------------------------------------
    //  Logic
    // ---------------------------------------------------------------------
    private void loadAllStudents() {
        getDetails(tblStudents, "", currentUserId);
    }

    private void onSearch() {
        String query = txtSearch.getText().trim();
        if (!validateSearchLength(query, 50)) return;
        getDetails(tblStudents, query, currentUserId);
    }

    private void onRefresh() {
        txtSearch.setText("");
        getDetails(tblStudents, "", currentUserId);
    }
    /** Validate that a search string isn't empty or overly long. */
    private boolean validateSearchLength(String value, int maxLength) {
        if (value.isBlank()) {
            JOptionPane.showMessageDialog(this, "Please enter text to search.");
            return false;
        }
        if (value.length() > maxLength) {
            JOptionPane.showMessageDialog(this,
                    "Search text is too long (" + value.length() + " chars). "
                            + "Maximum allowed is " + maxLength + ".",
                    "Invalid Search", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    // ---------------------------------------------------------------------
    //  Database logic (unchanged)
    // ---------------------------------------------------------------------
    @Override
    public void getDetails(JTable table, String searchValue, String lecId) {
        String sql = """
                SELECT u.user_id, u.user_name, u.user_phone, u.user_email
                  FROM user u
                  JOIN undergraduate ug ON u.user_id = ug.ug_id
                 WHERE u.user_role = 'undergraduate'
                   AND (u.user_id LIKE ? OR u.user_name LIKE ? OR u.user_email LIKE ?)
                 ORDER BY u.user_id ASC
                """;

        try (Connection con = dbconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            String pattern = "%" + searchValue + "%";
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                model.setRowCount(0);
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getString("user_id"),
                            rs.getString("user_name"),
                            rs.getString("user_phone"),
                            rs.getString("user_email")
                    });
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(LecturerStudentPanel.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this,
                    "Error loading student data: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}