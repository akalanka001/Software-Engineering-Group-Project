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

public class FinancialFeeReportFrame extends JFrame {

    private JTable table;
    private DefaultTableModel model;
    private JLabel lblRevenue = new JLabel("Total Revenue: Rs. 0.00");

    // Filters
    private JComboBox<String> cmbYear, cmbStartMonth, cmbEndMonth, cmbBatch, cmbCourse;

    public FinancialFeeReportFrame() {
        ThemeManager.initialize();
        setTitle("Financial / Fee Report");
        setSize(1050, 700);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10,10));
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(15,20,15,20));

        // ---------- FILTER AREA (two rows) ----------
        JPanel filterContainer = new JPanel();
        filterContainer.setLayout(new BoxLayout(filterContainer, BoxLayout.Y_AXIS));
        filterContainer.setBackground(Color.WHITE);

        /* ---------------- Row 1 ---------------- */
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        row1.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("Financial / Fee Report");
        lblTitle.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        lblTitle.setForeground(ThemeManager.LIGHT_BLUE);
        row1.add(lblTitle);
        row1.add(Box.createHorizontalStrut(15));

        row1.add(new JLabel("Year:"));
        cmbYear = new JComboBox<>();
        cmbYear.addItem("All");
        int currentYear = LocalDate.now().getYear();
        for (int y = currentYear; y >= currentYear - 5; y--) cmbYear.addItem(String.valueOf(y));
        cmbYear.setPreferredSize(new Dimension(80, 28));
        row1.add(cmbYear);

        row1.add(new JLabel("Start Month:"));
        cmbStartMonth = createMonthCombo();
        row1.add(cmbStartMonth);

        row1.add(new JLabel("End Month:"));
        cmbEndMonth = createMonthCombo();
        row1.add(cmbEndMonth);

        filterContainer.add(row1);

        /* ---------------- Row 2 ---------------- */
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        row2.setBackground(Color.WHITE);

        row2.add(new JLabel("Batch:"));
        cmbBatch = new JComboBox<>(new String[]{"All","01","02","03","04","05","06","07","08"});
        cmbBatch.setPreferredSize(new Dimension(70,28));
        row2.add(cmbBatch);

        row2.add(new JLabel("Course:"));
        cmbCourse = new JComboBox<>();
        cmbCourse.setPreferredSize(new Dimension(200,28));
        loadCourses(); // keep your existing course-loading method
        row2.add(cmbCourse);

// Buttons side by side
        JButton btnView = new JButton("View Report");
        ThemeManager.stylePrimaryButton(btnView);
        btnView.addActionListener(e -> loadData());
        row2.add(btnView);

        JButton btnExport = new JButton("Export PDF");
        ThemeManager.stylePrimaryButton(btnExport);
        btnExport.addActionListener(this::exportPDF);
        row2.add(btnExport);

        filterContainer.add(row2);
        main.add(filterContainer, BorderLayout.NORTH);

        // ---------- TABLE ----------
        model = new DefaultTableModel(
                new Object[]{"Course ID","Course Name","Course Fee (Rs.)",
                        "Students Enrolled","Subtotal Revenue (Rs.)"},0
        ){ @Override public boolean isCellEditable(int r,int c){return false;} };

        table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ThemeManager.styleTableHeader(table);
        main.add(new JScrollPane(table), BorderLayout.CENTER);

        // ---------- FOOTER ----------
        lblRevenue.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        lblRevenue.setForeground(Color.DARK_GRAY);
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT,20,10));
        footer.setBackground(Color.WHITE);
        footer.add(lblRevenue);
        main.add(footer, BorderLayout.SOUTH);

        add(main);
    }

    private JComboBox<String> createMonthCombo() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.addItem("All");
        for (int m=1; m<=12; m++) combo.addItem(String.format("%02d", m));
        combo.setPreferredSize(new Dimension(75,28));
        return combo;
    }

    private void loadCourses() {
        cmbCourse.removeAllItems();
        cmbCourse.addItem("All");
        try(Connection conn=dbconnection.getConnection();
            PreparedStatement ps=conn.prepareStatement("SELECT course_id, course_name FROM course");
            ResultSet rs=ps.executeQuery()){
            while(rs.next()){
                cmbCourse.addItem(rs.getString("course_id")+" - "+rs.getString("course_name"));
            }
        }catch(Exception e){ e.printStackTrace(); }
    }

    // ----------------------------------------------------------------
    private void loadData() {
        model.setRowCount(0);
        double totalRevenue = 0.0;

        String year = cmbYear.getSelectedItem().toString();
        String startM = cmbStartMonth.getSelectedItem().toString();
        String endM = cmbEndMonth.getSelectedItem().toString();
        String batch = cmbBatch.getSelectedItem().toString();
        String course = cmbCourse.getSelectedItem().toString();

        StringBuilder sql = new StringBuilder();
        sql.append("""
                SELECT c.course_id, c.course_name, c.course_fee,
                       COUNT(e.ug_id) AS students,
                       COUNT(e.ug_id)*c.course_fee AS subtotal
                FROM course c
                LEFT JOIN enrollment e ON c.course_id=e.course_id
                LEFT JOIN undergraduate u ON e.ug_id=u.ug_id
                """);

        boolean hasWhere = false;
        if(!year.equals("All")){
            sql.append("WHERE YEAR(e.enroll_date)=? ");
            hasWhere = true;
        }
        if(!startM.equals("All") && !endM.equals("All")){
            sql.append(hasWhere ? "AND " : "WHERE ");
            sql.append("MONTH(e.enroll_date) BETWEEN ? AND ? ");
            hasWhere = true;
        }
        if(!batch.equals("All")){
            sql.append(hasWhere ? "AND " : "WHERE ");
            sql.append("u.ug_batch=? ");
            hasWhere = true;
        }
        if(!course.equals("All")){
            sql.append(hasWhere ? "AND " : "WHERE ");
            sql.append("c.course_id=? ");
            hasWhere = true;
        }

        sql.append("""
                GROUP BY c.course_id, c.course_name, c.course_fee
                ORDER BY c.course_id
                """);

        try(Connection conn=dbconnection.getConnection();
            PreparedStatement ps=conn.prepareStatement(sql.toString())) {

            int i=1;
            if(!year.equals("All")) ps.setInt(i++, Integer.parseInt(year));
            if(!startM.equals("All") && !endM.equals("All")) {
                ps.setInt(i++, Integer.parseInt(startM));
                ps.setInt(i++, Integer.parseInt(endM));
            }
            if(!batch.equals("All")) ps.setInt(i++, Integer.parseInt(batch));
            if(!course.equals("All")) ps.setString(i++, course.split(" ")[0]);

            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                int count = rs.getInt("students");
                double fee = rs.getDouble("course_fee");
                double subtotal = rs.getDouble("subtotal");
                model.addRow(new Object[]{
                        rs.getString("course_id"),
                        rs.getString("course_name"),
                        String.format("%.2f", fee),
                        count,
                        String.format("%.2f", subtotal)
                });
                totalRevenue += subtotal;
            }

            lblRevenue.setText(String.format("Total Revenue: Rs. %.2f", totalRevenue));

            if(model.getRowCount()==0)
                JOptionPane.showMessageDialog(this,"No financial data found for the period.");

        }catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,"Database error while loading fee report.",
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ----------------------------------------------------------------
    private void exportPDF(ActionEvent e) {
        if(model.getRowCount()==0){
            JOptionPane.showMessageDialog(this,"Nothing to export!"); return;
        }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("FinancialFeeReport.pdf"));
        if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;

        try{
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc,new FileOutputStream(chooser.getSelectedFile()));
            doc.open();

            com.itextpdf.text.Font titleFont =
                    new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,
                            18, com.itextpdf.text.Font.BOLD, BaseColor.BLUE);
            doc.add(new Paragraph("Financial / Fee Report", titleFont));
            doc.add(new Paragraph(" "));

            PdfPTable pdfTable = new PdfPTable(model.getColumnCount());
            pdfTable.setWidthPercentage(100);
            for(int c=0;c<model.getColumnCount();c++)
                pdfTable.addCell(new Phrase(model.getColumnName(c)));
            for(int r=0;r<model.getRowCount();r++)
                for(int c=0;c<model.getColumnCount();c++)
                    pdfTable.addCell(String.valueOf(model.getValueAt(r,c)));

            doc.add(pdfTable);
            doc.add(new Paragraph(lblRevenue.getText()));
            doc.close();
            JOptionPane.showMessageDialog(this,"PDF export successful.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

        }catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,"Error exporting report.",
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args){
        SwingUtilities.invokeLater(() -> new FinancialFeeReportFrame().setVisible(true));
    }
}