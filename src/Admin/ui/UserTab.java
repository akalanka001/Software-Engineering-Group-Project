package Admin.ui;

import Admin.Alerts;
import Utils.ThemeManager;
import Database.dbconnection;
import Admin.Service.AdminService;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;
import java.util.Vector;

/**
 * Themed User management tab for Admin dashboard.
 *  • Blue FlatLaf header
 *  • Rounded blue Add / Update / Delete buttons
 *  • Hidden vertical scrollbar for a clean professional look
 */
public class UserTab extends JPanel {

    private final JTable table = new JTable();
    private final String currentUser;

    public UserTab(String username) {
        this.currentUser = username;
        setLayout(new BorderLayout(10, 10));
        setBackground(UIManager.getColor("Panel.background"));

        // ---------------- TABLE ----------------
        table.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"User ID", "Name", "Email", "Phone", "Batch", "Role"}));
        table.setRowHeight(28);
        table.setFillsViewportHeight(true);
        table.setDefaultEditor(Object.class, null);
        ThemeManager.styleTableHeader(table);

        // ------------ SCROLLPANE (scrolling enabled, scrollbar hidden) ----------
        JScrollPane scroll = new JScrollPane(table) {
            @Override
            public void setViewportView(Component view) {
                super.setViewportView(view);
                // hide scroll shadow lines for a cleaner edge
                getViewport().setBorder(null);
            }
        };
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        scroll.getHorizontalScrollBar().setUI(new javax.swing.plaf.basic.BasicScrollBarUI() {
            @Override protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {}
            @Override protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {}
        });
        // hide the vertical bar but still allow scrolling with mouse wheel
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scroll, BorderLayout.CENTER);

        // ---------------- BUTTONS ----------------
        JButton addBtn = new JButton("Add");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");

        ThemeManager.stylePrimaryButton(addBtn);
        ThemeManager.stylePrimaryButton(updateBtn);
        ThemeManager.stylePrimaryButton(deleteBtn);

        addBtn.addActionListener(e -> addUserActionPerformed(currentUser));
        updateBtn.addActionListener(e -> updateUserActionPerformed());
        deleteBtn.addActionListener(e -> deleteUserActionPerformed());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        btnPanel.add(addBtn);
        btnPanel.add(updateBtn);
        btnPanel.add(deleteBtn);

        add(btnPanel, BorderLayout.SOUTH);

        loadUsers();
    }

    // ---------------------------------------------------------------------
    private void loadUsers() {
        DefaultTableModel dt = (DefaultTableModel) table.getModel();
        dt.setRowCount(0);
        String sql = "SELECT * FROM user";
        try (Connection c = dbconnection.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getObject("user_id"));
                row.add(rs.getObject("user_name"));
                row.add(rs.getObject("user_email"));
                row.add(rs.getObject("user_phone"));
                row.add(rs.getObject("user_batch"));
                row.add(rs.getObject("user_role"));
                dt.addRow(row);
            }
        } catch (Exception e) {
            Alerts.fail("Error loading users: " + e.getMessage());
        }
    }

    private void addUserActionPerformed(String adminId) {
        AdminService.addUserDialog(this, table, adminId);
        loadUsers();
    }


    private void updateUserActionPerformed() {
        AdminService.updateUserDialog(this, table);
        loadUsers();
    }

    private void deleteUserActionPerformed() {
        AdminService.deleteUserDialog(this, table);
        loadUsers();
    }
}