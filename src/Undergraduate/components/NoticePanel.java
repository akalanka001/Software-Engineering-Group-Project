package Undergraduate.components;

import Database.dbconnection;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.sql.*;
import Utils.ThemeManager;

public class NoticePanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;
    private JTextField searchField;
    private TableRowSorter<DefaultTableModel> sorter;

    public NoticePanel(String userId) {
        setLayout(new BorderLayout(0, 15));
        setBackground(UIManager.getColor("Panel.background"));

        // ---------- Search Bar ----------
        JPanel topBar = new JPanel(new BorderLayout(8, 8));
        topBar.setOpaque(false);
        topBar.setBorder(BorderFactory.createEmptyBorder(15, 20, 5, 20));

        JLabel label = new JLabel("🔍 Search:");
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        searchField = new JTextField();
        searchField.setPreferredSize(new Dimension(200, 30));

        topBar.add(label, BorderLayout.WEST);
        topBar.add(searchField, BorderLayout.CENTER);
        add(topBar, BorderLayout.NORTH);

        // ---------- Table (no header) ----------
        model = new DefaultTableModel(new String[]{"Title", "Content", "Date"}, 0);
        table = new JTable(model);
        table.setRowHeight(28);
        table.setShowGrid(false);                              // remove grid lines
        table.setIntercellSpacing(new Dimension(0, 5));        // soft row spacing
        table.setDefaultEditor(Object.class, null);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        // Hide header for a clean look
        JTableHeader header = table.getTableHeader();
        header.setPreferredSize(new Dimension(0, 0));          // effectively hides header
        header.setVisible(false);
        table.setTableHeader(null);

        // Apply alternate row colors for readability
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            private final Color ALT_ROW_COLOR = new Color(245, 248, 255);
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int column) {
                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : ALT_ROW_COLOR);
                } else {
                    c.setBackground(ThemeManager.LIGHT_BLUE);
                    c.setForeground(Color.WHITE);
                }
                setBorder(noFocusBorder);
                return c;
            }
        });

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        // ---------- Scroll Pane ----------
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        sp.getViewport().setBackground(Color.WHITE);
        add(sp, BorderLayout.CENTER);

        // ---------- Live search filtering ----------
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            private void filter() {
                String txt = searchField.getText();
                sorter.setRowFilter(txt.trim().isEmpty()
                        ? null
                        : RowFilter.regexFilter("(?i)" + txt));
            }
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
        });

        loadNoticeData();

        // ---------- Double-click to read full notice ----------
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() >= 0) {
                    int r = table.convertRowIndexToModel(table.getSelectedRow());
                    String t = (String) model.getValueAt(r, 0);
                    String c = (String) model.getValueAt(r, 1);
                    String d = model.getValueAt(r, 2).toString();

                    JTextArea area = new JTextArea(c);
                    area.setWrapStyleWord(true);
                    area.setLineWrap(true);
                    area.setEditable(false);
                    area.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

                    JOptionPane.showMessageDialog(
                            NoticePanel.this,
                            new JScrollPane(area),
                            t + " (" + d + ")",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                }
            }
        });
    }

    // ---------------------------------------------------------------------
    // Load notices from DB
    // ---------------------------------------------------------------------
    private void loadNoticeData() {
        try (Connection con = dbconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(
                     "SELECT notice_title, notice_content, notice_date FROM notice ORDER BY notice_date DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("notice_title"),
                        rs.getString("notice_content"),
                        rs.getDate("notice_date")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
        }
    }
}