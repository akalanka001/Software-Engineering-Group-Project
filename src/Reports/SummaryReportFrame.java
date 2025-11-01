package Reports;

import Database.dbconnection;
import Utils.ThemeManager;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.FileOutputStream;
import java.sql.*;

/**
 * Summary / Dashboard Report for Admin
 * Displays key totals with a small recent-enrollments table.
 */
public class SummaryReportFrame extends JFrame {

    private final JLabel lblAdmins = new JLabel("0");
    private final JLabel lblLecturers = new JLabel("0");
    private final JLabel lblStudents = new JLabel("0");
    private final JLabel lblCourses = new JLabel("0");
    private final JLabel lblEnrollments = new JLabel("0");
    private final JLabel lblRevenue = new JLabel("0.00");

    private JTable table;
    private DefaultTableModel model;

    public SummaryReportFrame() {
        ThemeManager.initialize();
        setTitle("Summary / Dashboard Report");
        setSize(950, 600);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10,10));
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(15,20,15,20));

        // ----- HEADER -----
        JLabel lblTitle = new JLabel("Summary / Dashboard Report", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI Black", Font.BOLD, 24));
        lblTitle.setForeground(ThemeManager.LIGHT_BLUE);
        main.add(lblTitle, BorderLayout.NORTH);

        // ----- STATS GRID -----
        JPanel statsPanel = new JPanel(new GridLayout(2,3,15,15));
        statsPanel.setBackground(Color.WHITE);
        statsPanel.add(createCard("Admins", lblAdmins));
        statsPanel.add(createCard("Lecturers", lblLecturers));
        statsPanel.add(createCard("Students", lblStudents));
        statsPanel.add(createCard("Courses", lblCourses));
        statsPanel.add(createCard("Enrollments", lblEnrollments));
        statsPanel.add(createCard("Revenue (Rs.)", lblRevenue));
        main.add(statsPanel, BorderLayout.CENTER);

        // ----- RECENT ENROLLMENTS TABLE -----
        model = new DefaultTableModel(
                new Object[]{"Student ID", "Course ID", "Enroll Date"}, 0
        ){ @Override public boolean isCellEditable(int r, int c) {return false;} };

        table = new JTable(model);
        table.setRowHeight(24);
        ThemeManager.styleTableHeader(table);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Recent Enrollments"));
        main.add(scroll, BorderLayout.SOUTH);

        // ----- BUTTON BAR -----
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER,20,10));
        buttons.setBackground(Color.WHITE);

        JButton btnRefresh = new JButton("Refresh");
        ThemeManager.stylePrimaryButton(btnRefresh);
        btnRefresh.addActionListener(e -> loadData());
        buttons.add(btnRefresh);

        JButton btnExport = new JButton("Export PDF");
        ThemeManager.stylePrimaryButton(btnExport);
        btnExport.addActionListener(this::exportPDF);
        buttons.add(btnExport);

        main.add(buttons, BorderLayout.PAGE_END);

        add(main);
        loadData(); // initial load
    }

    private JPanel createCard(String title, JLabel numberLabel) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(new Color(240,245,255));
        card.setBorder(BorderFactory.createLineBorder(ThemeManager.LIGHT_BLUE,2));
        JLabel lblTitle = new JLabel(title, SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 16));
        lblTitle.setForeground(Color.DARK_GRAY);
        numberLabel.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        numberLabel.setForeground(ThemeManager.LIGHT_BLUE);
        numberLabel.setHorizontalAlignment(SwingConstants.CENTER);
        card.add(lblTitle, BorderLayout.NORTH);
        card.add(numberLabel, BorderLayout.CENTER);
        return card;
    }

    // ------------------------------------------------------------------
    // Load all stats + recent enrollments
    // ------------------------------------------------------------------
    private void loadData() {
        try (Connection conn = dbconnection.getConnection()) {
            lblAdmins.setText(getCount(conn, "SELECT COUNT(*) FROM user WHERE user_role='admin'"));
            lblLecturers.setText(getCount(conn, "SELECT COUNT(*) FROM user WHERE user_role='lecturer'"));
            lblStudents.setText(getCount(conn, "SELECT COUNT(*) FROM user WHERE user_role='undergraduate'"));
            lblCourses.setText(getCount(conn, "SELECT COUNT(*) FROM course"));
            lblEnrollments.setText(getCount(conn, "SELECT COUNT(*) FROM enrollment"));
            lblRevenue.setText(getRevenue(conn));

            // --- Recent Enrollments ---
            model.setRowCount(0);
            String sql = "SELECT ug_id, course_id, enroll_date FROM enrollment ORDER BY enroll_date DESC LIMIT 8";
            try(PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()){
                while(rs.next()){
                    model.addRow(new Object[]{
                            rs.getString("ug_id"),
                            rs.getString("course_id"),
                            rs.getTimestamp("enroll_date")
                    });
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading summary data.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String getCount(Connection conn, String query) {
        try (PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return String.valueOf(rs.getInt(1));
        } catch (Exception e) { e.printStackTrace(); }
        return "0";
    }

    private String getRevenue(Connection conn) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT SUM(c.course_fee) FROM course c JOIN enrollment e ON e.course_id = c.course_id");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return String.format("%.2f", rs.getDouble(1));
        } catch (Exception e) { e.printStackTrace(); }
        return "0.00";
    }

    // ------------------------------------------------------------------
    // Export view to PDF (summary + table)
    // ------------------------------------------------------------------
    private void exportPDF(ActionEvent e){
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("SummaryReport.pdf"));
        if(fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            Document doc = new Document(PageSize.A4);
            PdfWriter.getInstance(doc, new FileOutputStream(fc.getSelectedFile()));
            doc.open();

            com.itextpdf.text.Font titleFont =
                    new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,
                            18, com.itextpdf.text.Font.BOLD, BaseColor.BLUE);
            doc.add(new Paragraph("Summary / Dashboard Report", titleFont));
            doc.add(new Paragraph(" "));

            doc.add(new Paragraph(
                    String.format("Admins: %s   Lecturers: %s   Students: %s   Courses: %s   Enrollments: %s   Revenue: Rs.%s",
                            lblAdmins.getText(), lblLecturers.getText(), lblStudents.getText(),
                            lblCourses.getText(), lblEnrollments.getText(), lblRevenue.getText())
            ));
            doc.add(new Paragraph(" "));

            PdfPTable pdfTable = new PdfPTable(model.getColumnCount());
            pdfTable.setWidthPercentage(100);
            for(int i=0;i<model.getColumnCount();i++)
                pdfTable.addCell(new Phrase(model.getColumnName(i)));
            for(int r=0;r<model.getRowCount();r++)
                for(int c=0;c<model.getColumnCount();c++)
                    pdfTable.addCell(String.valueOf(model.getValueAt(r,c)));

            doc.add(pdfTable);
            doc.close();
            JOptionPane.showMessageDialog(this,"Summary report exported successfully!",
                    "Success",JOptionPane.INFORMATION_MESSAGE);

        }catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,"Error exporting PDF.","Export Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> new SummaryReportFrame().setVisible(true));
    }
}