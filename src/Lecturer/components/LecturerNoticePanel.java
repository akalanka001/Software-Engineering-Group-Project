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
 * Converted Notice JFrame into LecturerNoticePanel (for CardLayout).
 * Functionality unchanged — now embedded in the Lecturer portal.
 * Only theme integration added.
 */
public class LecturerNoticePanel extends JPanel implements CRUD_Operation_Interface {

    private final String currentUserId;

    private JTable tblNotice;
    private JTextField txtSearch;
    private JButton btnSearch, btnRefresh;

    public LecturerNoticePanel(String userId) {
        this.currentUserId = userId;
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));
        initComponents();
        viewNotices();
    }

    // ------------------------------------------------------------------
    //  UI Setup
    // ------------------------------------------------------------------
    private void initComponents() {
        // ----- Header -----
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(153, 187, 187));
        JLabel title = new JLabel("NOTICES", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        header.add(title, BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);

        // ----- Controls -----
        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 12));
        controlPanel.setOpaque(false);

        JLabel lblSearch = new JLabel("Search Notice:");
        lblSearch.setFont(new Font("Segoe UI", Font.BOLD, 15));

        txtSearch = new JTextField(20);

        btnSearch = new JButton("Search");
        ThemeManager.stylePrimaryButton(btnSearch);
        btnSearch.addActionListener(e -> onSearch());

        btnRefresh = new JButton("Refresh");
        ThemeManager.stylePrimaryButton(btnRefresh);
        btnRefresh.addActionListener(e -> onRefresh());

        controlPanel.add(lblSearch);
        controlPanel.add(txtSearch);
        controlPanel.add(btnSearch);
        controlPanel.add(btnRefresh);

        // ----- Table -----
        tblNotice = new JTable(new DefaultTableModel(
                new Object[][]{},
                new String[]{"Notice ID", "Title", "Content", "Date"}
        ));
        tblNotice.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        tblNotice.setRowHeight(25);
        ThemeManager.styleTableHeader(tblNotice); // apply blue header style
        JScrollPane scroll = new JScrollPane(tblNotice);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        // ----- Combine -----
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.add(controlPanel, BorderLayout.NORTH);
        main.add(scroll, BorderLayout.CENTER);
        add(main, BorderLayout.CENTER);
    }

    // ------------------------------------------------------------------
    //  Logic — same as original
    // ------------------------------------------------------------------
    private void viewNotices() {
        getDetails(tblNotice, "", currentUserId);
    }

    private void onSearch() {
        String term = txtSearch.getText().trim();
        if (!validateSearchLength(term, 50)) return;
        getDetails(tblNotice, term, currentUserId);
    }

    private void onRefresh() {
        txtSearch.setText("");
        getDetails(tblNotice, "", currentUserId);
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

    // ------------------------------------------------------------------
    //  Database query (unchanged)
    // ------------------------------------------------------------------
    @Override
    public void getDetails(JTable table, String searchValue, String lecId) {
        String sql = """
                SELECT notice_id, notice_title, notice_content, notice_date
                  FROM notice
                 WHERE CONCAT(notice_id, notice_title, notice_content, notice_date) LIKE ?
                 ORDER BY notice_id ASC
                """;

        try (Connection con = dbconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, "%" + searchValue + "%");

            try (ResultSet rs = ps.executeQuery()) {
                DefaultTableModel model = (DefaultTableModel) table.getModel();
                model.setRowCount(0);
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getString("notice_id"),
                            rs.getString("notice_title"),
                            rs.getString("notice_content"),
                            rs.getString("notice_date")
                    });
                }
            }

        } catch (SQLException ex) {
            Logger.getLogger(LecturerNoticePanel.class.getName()).log(Level.SEVERE, null, ex);
            JOptionPane.showMessageDialog(this,
                    "Error loading notices: " + ex.getMessage(),
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}