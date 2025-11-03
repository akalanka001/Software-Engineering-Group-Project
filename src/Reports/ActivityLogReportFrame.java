package Reports;

import Database.dbconnection;
import Utils.ThemeManager;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.YearMonth;
import java.util.Vector;

/**
 * Activity Log Report Frame
 *  - Filter by start month/year and end month/year
 *  - View logs in JTable
 *  - Export as PDF via iText
 */
public class ActivityLogReportFrame extends JFrame {

    private JComboBox<String> cmbStartMonth, cmbEndMonth;
    private JSpinner spnStartYear, spnEndYear;
    private JTable table;
    private DefaultTableModel model;

    public ActivityLogReportFrame() {
        ThemeManager.initialize();
        setTitle("Activity Log Report");
        setSize(1000, 550);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new BorderLayout(15, 15));
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(content);

        // ----- Title -----
        JLabel lblTitle = new JLabel("Activity Log Report", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        lblTitle.setForeground(ThemeManager.LIGHT_BLUE);
        lblTitle.setBorder(new EmptyBorder(5, 0, 15, 0));
        content.add(lblTitle, BorderLayout.NORTH);

        // ----- Filter Section -----
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 10));
        filterPanel.setBackground(Color.WHITE);

        cmbStartMonth = new JComboBox<>(getMonths());
        cmbEndMonth = new JComboBox<>(getMonths());
        spnStartYear = new JSpinner(new SpinnerNumberModel(2024, 2000, 2100, 1));
        spnEndYear = new JSpinner(new SpinnerNumberModel(2024, 2000, 2100, 1));

        JLabel lblFrom = new JLabel("From:");
        JLabel lblTo = new JLabel("To:");
        lblFrom.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        lblTo.setFont(new Font("Segoe UI", Font.PLAIN, 15));

        JButton btnView = new JButton("View");
        JButton btnExport = new JButton("Export as PDF");
        ThemeManager.stylePrimaryButton(btnView);
        ThemeManager.stylePrimaryButton(btnExport);
        btnView.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 15));
        btnExport.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 15));
        btnView.setPreferredSize(new Dimension(140, 35));
        btnExport.setPreferredSize(new Dimension(160, 35));

        filterPanel.add(lblFrom);
        filterPanel.add(cmbStartMonth);
        filterPanel.add(spnStartYear);
        filterPanel.add(lblTo);
        filterPanel.add(cmbEndMonth);
        filterPanel.add(spnEndYear);
        filterPanel.add(btnView);
        filterPanel.add(btnExport);
        content.add(filterPanel, BorderLayout.PAGE_START);

        // ----- Table -----
        String[] columns = {"Log ID", "User ID", "Action", "Log Time"};
        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setFont(new Font("Segoe UI Semibold", Font.PLAIN, 15));
        table.getTableHeader().setBackground(ThemeManager.LIGHT_BLUE);
        table.getTableHeader().setForeground(Color.WHITE);
        table.setRowHeight(26);
        table.setGridColor(ThemeManager.LIGHT_BLUE);
        table.setSelectionBackground(new Color(225, 240, 255));

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(ThemeManager.LIGHT_BLUE, 2, true));
        content.add(scroll, BorderLayout.CENTER);

        // ----- Actions -----
        btnView.addActionListener(e -> loadLogs());
        btnExport.addActionListener(e -> exportToPDF());
    }

    // -----------------------------------------------------------
    private String[] getMonths() {
        return new String[]{
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };
    }

    private void loadLogs() {
        int startMonth = cmbStartMonth.getSelectedIndex() + 1;
        int endMonth = cmbEndMonth.getSelectedIndex() + 1;
        int startYear = (int) spnStartYear.getValue();
        int endYear = (int) spnEndYear.getValue();

        YearMonth startYM = YearMonth.of(startYear, startMonth);
        YearMonth endYM = YearMonth.of(endYear, endMonth);

        model.setRowCount(0);

        try (Connection conn = dbconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT log_id, user_id, action, log_time " +
                             "FROM activity_log WHERE log_time BETWEEN ? AND ? ORDER BY log_time ASC")) {

            ps.setTimestamp(1, Timestamp.valueOf(startYM.atDay(1).atStartOfDay()));
            ps.setTimestamp(2, Timestamp.valueOf(endYM.atEndOfMonth().atTime(23, 59, 59)));

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                row.add(rs.getInt("log_id"));
                row.add(rs.getString("user_id"));
                row.add(rs.getString("action"));
                row.add(rs.getTimestamp("log_time"));
                model.addRow(row);
            }
            if (model.getRowCount() == 0)
                JOptionPane.showMessageDialog(this, "No logs found for this range.",
                        "No Data", JOptionPane.INFORMATION_MESSAGE);

        } catch (SQLException ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error retrieving logs:\n" + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // -----------------------------------------------------------
    private void exportToPDF() {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "No data to export. Please view logs first.",
                    "No Data", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("ActivityLogReport.pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc, new FileOutputStream(chooser.getSelectedFile()));
            doc.open();

            com.itextpdf.text.Font titleFont = new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA,
                    18,
                    com.itextpdf.text.Font.BOLD,
                    BaseColor.BLUE);

            doc.add(new Paragraph("Activity Log Report", titleFont));
            doc.add(new Paragraph(" "));

            PdfPTable pdfTable = new PdfPTable(model.getColumnCount());
            pdfTable.setWidthPercentage(100);
            // Column headers
            for (int c = 0; c < model.getColumnCount(); c++)
                pdfTable.addCell(new Phrase(model.getColumnName(c)));

            // Table data
            for (int r = 0; r < model.getRowCount(); r++)
                for (int c = 0; c < model.getColumnCount(); c++)
                    pdfTable.addCell(String.valueOf(model.getValueAt(r, c)));

            doc.add(pdfTable);
            doc.close();

            JOptionPane.showMessageDialog(this, "Report exported successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "PDF export error: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}