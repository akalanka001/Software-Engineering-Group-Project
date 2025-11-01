package Reports;

import Database.dbconnection;
import Utils.ThemeManager;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDate;

public class NotificationReportFrame extends JFrame {

    private JComboBox<String> cmbRole;
    private JTextField txtUserId;
    private JComboBox<String> cmbStartMonth, cmbEndMonth, cmbYear;
    private JTable table;
    private DefaultTableModel model;
    private final JLabel lblCount = new JLabel();

    public NotificationReportFrame() {
        ThemeManager.initialize();
        setTitle("Activity / Notification Report");
        setSize(1100, 600);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10,10));
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(15,20,15,20));

        // ---------- FILTER AREA ----------
        JPanel filterContainer = new JPanel();
        filterContainer.setLayout(new BoxLayout(filterContainer, BoxLayout.Y_AXIS));
        filterContainer.setBackground(Color.WHITE);

        // ---- Row 1 ----
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT,15,8));
        row1.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("Activity / Notification Report");
        lblTitle.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        lblTitle.setForeground(ThemeManager.LIGHT_BLUE);
        row1.add(lblTitle);
        row1.add(Box.createHorizontalStrut(20));

        row1.add(new JLabel("Year:"));
        cmbYear = new JComboBox<>();
        cmbYear.addItem("All");
        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear; y >= currentYear - 5; y--) cmbYear.addItem(String.valueOf(y));
        row1.add(cmbYear);

        row1.add(new JLabel("Start Month:"));
        cmbStartMonth = createMonthCombo();
        row1.add(cmbStartMonth);

        row1.add(new JLabel("End Month:"));
        cmbEndMonth = createMonthCombo();
        row1.add(cmbEndMonth);

        row1.add(new JLabel("Role:"));
        cmbRole = new JComboBox<>(new String[]{"All", "Admin", "Undergraduate"});
        row1.add(cmbRole);

        filterContainer.add(row1);

        // ---- Row 2 ----
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT,15,8));
        row2.setBackground(Color.WHITE);

        row2.add(new JLabel("User ID:"));
        txtUserId = new JTextField(10);
        row2.add(txtUserId);

        JButton btnView = new JButton("View Report");
        ThemeManager.stylePrimaryButton(btnView);
        btnView.addActionListener(this::loadData);
        row2.add(btnView);

        JButton btnPDF = new JButton("Export PDF");
        ThemeManager.stylePrimaryButton(btnPDF);
        btnPDF.addActionListener(this::exportPDF);
        row2.add(btnPDF);

        filterContainer.add(row2);
        main.add(filterContainer, BorderLayout.NORTH);

        // ---------- TABLE ----------
        model = new DefaultTableModel(
                new Object[]{"Role", "User ID", "Date / Time", "Message"}, 0
        ) { @Override public boolean isCellEditable(int r, int c) { return false; } };

        table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ThemeManager.styleTableHeader(table);
        main.add(new JScrollPane(table), BorderLayout.CENTER);

        // ---------- FOOTER ----------
        lblCount.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        lblCount.setForeground(Color.DARK_GRAY);
        lblCount.setBorder(BorderFactory.createEmptyBorder(5,10,10,10));
        main.add(lblCount, BorderLayout.SOUTH);

        add(main);
    }

    private JComboBox<String> createMonthCombo() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        for (int m = 1; m <= 12; m++)
            combo.addItem(String.format("%02d", m));
        combo.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
        combo.setPreferredSize(new Dimension(70, 28));
        return combo;
    }

    // ------------------------------------------------------------------
    //  Load data from both notification tables using UNION
    // ------------------------------------------------------------------
    private void loadData(ActionEvent e) {
        model.setRowCount(0);
        lblCount.setText("Loading...");

        String role = cmbRole.getSelectedItem().toString();
        String userId = txtUserId.getText().trim();
        String yearSel = cmbYear.getSelectedItem().toString();
        int startM = Integer.parseInt(cmbStartMonth.getSelectedItem().toString());
        int endM = Integer.parseInt(cmbEndMonth.getSelectedItem().toString());

        StringBuilder sql = new StringBuilder(
                "SELECT 'Undergraduate' AS role, ug_id AS user_id, notice_date, message " +
                        "FROM ug_notification WHERE 1=1"
        );
        sql.append(" AND MONTH(notice_date) BETWEEN ? AND ?");
        if (!yearSel.equals("All")) sql.append(" AND YEAR(notice_date)=?");
        if (!userId.isEmpty()) sql.append(" AND ug_id=?");

        StringBuilder sql2 = new StringBuilder(
                "SELECT 'Admin' AS role, NULL AS user_id, notice_date, message " +
                        "FROM admin_notification WHERE 1=1"
        );
        sql2.append(" AND MONTH(notice_date) BETWEEN ? AND ?");
        if (!yearSel.equals("All")) sql2.append(" AND YEAR(notice_date)=?");

        String finalSQL;
        if (role.equals("All")) {
            finalSQL = sql + " UNION ALL " + sql2 + " ORDER BY notice_date DESC";
        } else if (role.equalsIgnoreCase("Undergraduate")) {
            finalSQL = sql + " ORDER BY notice_date DESC";
        } else {
            finalSQL = sql2 + " ORDER BY notice_date DESC";
        }

        try (Connection conn = dbconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(finalSQL)) {

            int i = 1;
            if (role.equals("All") || role.equalsIgnoreCase("Undergraduate")) {
                ps.setInt(i++, startM);
                ps.setInt(i++, endM);
                if (!yearSel.equals("All")) ps.setInt(i++, Integer.parseInt(yearSel));
                if (!userId.isEmpty()) ps.setString(i++, userId);
            }
            if (role.equals("All") || role.equalsIgnoreCase("Admin")) {
                ps.setInt(i++, startM);
                ps.setInt(i++, endM);
                if (!yearSel.equals("All")) ps.setInt(i++, Integer.parseInt(yearSel));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("role"),
                        rs.getString("user_id"),
                        rs.getTimestamp("notice_date"),
                        rs.getString("message")
                });
            }

            lblCount.setText(String.format("<html><b>Total Notifications:</b> %d</html>", model.getRowCount()));
            if (model.getRowCount() == 0)
                JOptionPane.showMessageDialog(this, "No notifications found.");

        } catch (Exception ex) {
            ex.printStackTrace();
            lblCount.setText("Database error.");
            JOptionPane.showMessageDialog(this,
                    "Error loading notifications.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------------
    //  Export to PDF
    // ------------------------------------------------------------------
    private void exportPDF(ActionEvent e) {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Nothing to export!");
            return;
        }
        JFileChooser jc = new JFileChooser();
        jc.setSelectedFile(new java.io.File("NotificationReport.pdf"));
        if (jc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc, new FileOutputStream(jc.getSelectedFile()));
            doc.open();

            com.itextpdf.text.Font titleFont =
                    new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,
                            18, com.itextpdf.text.Font.BOLD, BaseColor.BLUE);
            doc.add(new Paragraph("Activity / Notification Report", titleFont));
            doc.add(new Paragraph(" "));

            PdfPTable pdfTable = new PdfPTable(model.getColumnCount());
            pdfTable.setWidthPercentage(100);
            for (int i = 0; i < model.getColumnCount(); i++)
                pdfTable.addCell(new Phrase(model.getColumnName(i)));
            for (int r = 0; r < model.getRowCount(); r++)
                for (int c = 0; c < model.getColumnCount(); c++)
                    pdfTable.addCell(String.valueOf(model.getValueAt(r, c)));

            doc.add(pdfTable);
            doc.close();
            JOptionPane.showMessageDialog(this, "PDF exported successfully!",
                    "Export Done", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error exporting PDF.",
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new NotificationReportFrame().setVisible(true));
    }
}