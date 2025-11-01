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
import java.time.LocalDate;

public class CourseSummaryReportFrame extends JFrame {

    private JTable table;
    private DefaultTableModel model;
    private JLabel lblTotalCourses = new JLabel();
    private JLabel lblTotalRevenue = new JLabel();

    // New filter components
    private JComboBox<String> cmbYear, cmbStartMonth, cmbEndMonth;

    public CourseSummaryReportFrame() {
        ThemeManager.initialize();
        setTitle("Course Summary Report");
        setSize(1100, 700);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10,10));
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(15,20,15,20));

        // ---------- HEADER & FILTERS ----------
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT,15,10));
        filterPanel.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("Course Summary Report");
        lblTitle.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        lblTitle.setForeground(ThemeManager.LIGHT_BLUE);
        filterPanel.add(lblTitle);

        filterPanel.add(Box.createHorizontalStrut(25));
        filterPanel.add(new JLabel("Year:"));
        cmbYear = new JComboBox<>();
        cmbYear.addItem("All");
        int currentYear = LocalDate.now().getYear();
        for(int y=currentYear; y>=currentYear-5; y--) cmbYear.addItem(String.valueOf(y));
        cmbYear.setPreferredSize(new Dimension(80,28));
        filterPanel.add(cmbYear);

        filterPanel.add(new JLabel("Start Month:"));
        cmbStartMonth = createMonthCombo();
        filterPanel.add(cmbStartMonth);

        filterPanel.add(new JLabel("End Month:"));
        cmbEndMonth = createMonthCombo();
        filterPanel.add(cmbEndMonth);

        JButton btnView = new JButton("View Report");
        ThemeManager.stylePrimaryButton(btnView);
        btnView.addActionListener(e -> loadData());
        filterPanel.add(btnView);

        JButton btnExport = new JButton("Export PDF");
        ThemeManager.stylePrimaryButton(btnExport);
        btnExport.addActionListener(this::exportPDF);
        filterPanel.add(btnExport);

        main.add(filterPanel, BorderLayout.NORTH);

        // ---------- TABLE ----------
        model = new DefaultTableModel(
                new Object[]{"Course ID","Course Name","Lecturer ID","Credits",
                        "Enrolled Students","Course Fee (Rs.)","Total Revenue (Rs.)"},0
        ){ @Override public boolean isCellEditable(int r,int c){return false;} };
        table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ThemeManager.styleTableHeader(table);
        main.add(new JScrollPane(table), BorderLayout.CENTER);

        // ---------- FOOTER ----------
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT,20,10));
        footer.setBackground(Color.WHITE);
        lblTotalCourses.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        lblTotalRevenue.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        footer.add(lblTotalCourses);
        footer.add(Box.createHorizontalStrut(50));
        footer.add(lblTotalRevenue);
        main.add(footer, BorderLayout.SOUTH);
        add(main);

        loadData();
    }

    private JComboBox<String> createMonthCombo() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.addItem("All");
        for(int m=1;m<=12;m++)
            combo.addItem(String.format("%02d", m));
        combo.setSelectedIndex(LocalDate.now().getMonthValue());
        combo.setPreferredSize(new Dimension(75,28));
        return combo;
    }

    // ----------------------------------------------------------------
    private void loadData() {
        model.setRowCount(0);
        double total = 0.0;

        String year = cmbYear.getSelectedItem().toString();
        String startM = cmbStartMonth.getSelectedItem().toString();
        String endM = cmbEndMonth.getSelectedItem().toString();

        // --- Build SQL dynamically with proper spaces ---
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT c.course_id, c.course_name, c.lec_id, c.credit, c.course_fee, ")
                .append("COUNT(e.ug_id) AS student_count, ")
                .append("COUNT(e.ug_id) * c.course_fee AS total_revenue ")
                .append("FROM course c ")
                .append("LEFT JOIN enrollment e ON c.course_id = e.course_id ");

        boolean hasCondition = false;

        // Add WHERE clauses conditionally
        if (!year.equals("All")) {
            sql.append("WHERE YEAR(e.enroll_date) = ? ");
            hasCondition = true;
        }

        if (!startM.equals("All") && !endM.equals("All")) {
            sql.append(hasCondition ? "AND " : "WHERE ");
            sql.append("MONTH(e.enroll_date) BETWEEN ? AND ? ");
            hasCondition = true;
        }

        // Grouping and ordering – always separated by spaces
        sql.append("GROUP BY c.course_id, c.course_name, c.lec_id, c.credit, c.course_fee ")
                .append("ORDER BY c.course_id");

        try (Connection conn = dbconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {

            int idx = 1;

            if (!year.equals("All")) {
                ps.setInt(idx++, Integer.parseInt(year));
            }

            if (!startM.equals("All") && !endM.equals("All")) {
                ps.setInt(idx++, Integer.parseInt(startM));
                ps.setInt(idx++, Integer.parseInt(endM));
            }

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int count = rs.getInt("student_count");
                double fee = rs.getDouble("course_fee");
                double revenue = rs.getDouble("total_revenue");

                model.addRow(new Object[]{
                        rs.getString("course_id"),
                        rs.getString("course_name"),
                        rs.getString("lec_id"),
                        rs.getInt("credit"),
                        count,
                        String.format("%.2f", fee),
                        String.format("%.2f", revenue)
                });
                total += revenue;
            }

            lblTotalCourses.setText("Courses: " + model.getRowCount());
            lblTotalRevenue.setText(String.format("Total Revenue: Rs. %.2f", total));

            if (model.getRowCount() == 0) {
                JOptionPane.showMessageDialog(this, "No data found for chosen period.",
                        "No Results", JOptionPane.INFORMATION_MESSAGE);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error loading data from the database.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    // ----------------------------------------------------------------
    private void exportPDF(ActionEvent e) {
        if (model.getRowCount()==0) { JOptionPane.showMessageDialog(this,"Nothing to export!"); return; }

        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("CourseSummaryReport.pdf"));
        if (chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;

        try {
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc, new FileOutputStream(chooser.getSelectedFile()));
            doc.open();
            com.itextpdf.text.Font fTitle =
                    new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,
                            18, com.itextpdf.text.Font.BOLD, BaseColor.BLUE);
            doc.add(new Paragraph("Course Summary Report", fTitle));
            doc.add(new Paragraph(" "));

            PdfPTable pdfTable = new PdfPTable(model.getColumnCount());
            pdfTable.setWidthPercentage(100);
            for(int c=0;c<model.getColumnCount();c++)
                pdfTable.addCell(new Phrase(model.getColumnName(c)));
            for(int r=0;r<model.getRowCount();r++)
                for(int c=0;c<model.getColumnCount();c++)
                    pdfTable.addCell(String.valueOf(model.getValueAt(r,c)));

            doc.add(pdfTable);
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph(lblTotalRevenue.getText()));
            doc.close();
            JOptionPane.showMessageDialog(this,"Report exported successfully.",
                    "Success",JOptionPane.INFORMATION_MESSAGE);
        } catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,"Error exporting PDF.","Export Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> new CourseSummaryReportFrame().setVisible(true));
    }
}