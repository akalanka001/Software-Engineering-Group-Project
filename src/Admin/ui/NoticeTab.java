package Admin.ui;

import Admin.Service.NoticeService;
import Utils.ThemeManager;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;

public class NoticeTab extends JPanel {

    private final JTable noticeTable = new JTable();
    private final JTextField nIdField = new JTextField(20);
    private final JTextField nTitleField = new JTextField(20);
    private final JTextArea nContentArea = new JTextArea(3, 20);
    private final String currentUser;

    public NoticeTab(String username) {
        this.currentUser = username;
        setLayout(new BorderLayout(10,10));
        setBackground(UIManager.getColor("Panel.background"));

        // ---------------- TABLE ----------------
        noticeTable.setModel(new DefaultTableModel(
                new Object[][]{},
                new String[]{"Notice ID", "Notice Title", "Notice Content", "Notice Date"}));
        noticeTable.setRowHeight(28);
        noticeTable.setFillsViewportHeight(true);
        noticeTable.setDefaultEditor(Object.class, null);
        ThemeManager.styleTableHeader(noticeTable);

        JScrollPane tableScroll = new JScrollPane(noticeTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        add(tableScroll, BorderLayout.CENTER);

        // ✅ ADD THIS: Click on row fills the fields
        noticeTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int selectedRow = noticeTable.getSelectedRow();
                if (selectedRow != -1) {
                    // Fill the fields with selected row data
                    nIdField.setText(String.valueOf(noticeTable.getValueAt(selectedRow, 0)));
                    nTitleField.setText(String.valueOf(noticeTable.getValueAt(selectedRow, 1)));
                    nContentArea.setText(String.valueOf(noticeTable.getValueAt(selectedRow, 2)));
                }
            }
        });

        // ---------------- COMPACT FORM ----------------
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setBorder(BorderFactory.createTitledBorder("Notice Details"));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 8, 4, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // row 0: ID
        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(new JLabel("Notice ID:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(nIdField, gbc);

        // row 1: Title
        gbc.gridx = 0; gbc.gridy = 1; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Title:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.HORIZONTAL;
        formPanel.add(nTitleField, gbc);

        // row 2: Content
        gbc.gridx = 0; gbc.gridy = 2; gbc.fill = GridBagConstraints.NONE;
        formPanel.add(new JLabel("Content:"), gbc);
        gbc.gridx = 1; gbc.fill = GridBagConstraints.BOTH;
        JScrollPane contentScroll = new JScrollPane(nContentArea);
        formPanel.add(contentScroll, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);
        formPanel.add(Box.createVerticalStrut(5), gbc);

        add(formPanel, BorderLayout.NORTH);

        // ---------------- BUTTONS ----------------
        JButton addBtn = new JButton("Add");
        JButton updateBtn = new JButton("Update");
        JButton deleteBtn = new JButton("Delete");
        ThemeManager.stylePrimaryButton(addBtn);
        ThemeManager.stylePrimaryButton(updateBtn);
        ThemeManager.stylePrimaryButton(deleteBtn);

        addBtn.addActionListener(e -> addNoticeActionPerformed());
        updateBtn.addActionListener(e -> updateNoticeActionPerformed());
        deleteBtn.addActionListener(e -> deleteNoticeActionPerformed());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        buttonPanel.setOpaque(false);
        buttonPanel.add(addBtn);
        buttonPanel.add(updateBtn);
        buttonPanel.add(deleteBtn);

        add(buttonPanel, BorderLayout.SOUTH);

        loadNotices();
    }

    private void addNoticeActionPerformed() {
        java.sql.Date sqlDate = java.sql.Date.valueOf(java.time.LocalDate.now());
        String title = nTitleField.getText().trim();
        String content = nContentArea.getText().trim();

        if (!NoticeService.validateFields(title, content)) return;

        if (NoticeService.add(title, content, sqlDate)) {
            JOptionPane.showMessageDialog(this, "Notice added successfully!");
            nIdField.setText(""); nTitleField.setText(""); nContentArea.setText("");
            loadNotices();
        } else {
            JOptionPane.showMessageDialog(this, "Failed to add notice!");
        }
    }

    private void updateNoticeActionPerformed() {
        String idText = nIdField.getText().trim();
        String title = nTitleField.getText().trim();
        String content = nContentArea.getText().trim();

        if (!NoticeService.validateId(idText) || !NoticeService.validateFields(title, content)) return;

        int id = Integer.parseInt(idText);
        java.sql.Date sqlDate = java.sql.Date.valueOf(java.time.LocalDate.now());
        if (NoticeService.update(id, title, content, sqlDate)) {
            JOptionPane.showMessageDialog(this, "Notice updated successfully!");
            loadNotices();
        } else {
            JOptionPane.showMessageDialog(this, "Update failed!");
        }
    }

    private void deleteNoticeActionPerformed() {
        String idText = nIdField.getText().trim();
        if (!NoticeService.validateId(idText)) return;
        int id = Integer.parseInt(idText);

        if (!NoticeService.exists(id)) {
            JOptionPane.showMessageDialog(this, "Notice not found.");
            return;
        }
        if (NoticeService.delete(id)) {
            JOptionPane.showMessageDialog(this, "Deleted successfully!");
            loadNotices();
        } else {
            JOptionPane.showMessageDialog(this, "Delete failed!");
        }
    }

    private void loadNotices() {
        DefaultTableModel dt = (DefaultTableModel) noticeTable.getModel();
        dt.setRowCount(0);
        try (ResultSet rs = NoticeService.getAll()) {
            while (rs.next()) {
                Object[] row = {
                        rs.getInt("notice_id"),
                        rs.getString("notice_title"),
                        rs.getString("notice_content"),
                        rs.getDate("notice_date")
                };
                dt.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,
                    "Error loading notices: " + e.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}