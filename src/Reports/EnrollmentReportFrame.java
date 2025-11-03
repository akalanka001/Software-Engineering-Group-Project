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
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

public class EnrollmentReportFrame extends JFrame {

    private JComboBox<String> cmbBatch, cmbCourse, cmbYear, cmbStartMonth, cmbEndMonth;
    private JTextField txtUgID, txtCourseId;
    private JTable table;
    private DefaultTableModel model;
    private final JLabel lblCount = new JLabel();
    private final String currentRole;

    public EnrollmentReportFrame(String role) {
        this.currentRole = role.toLowerCase();
        ThemeManager.initialize();

        setTitle("Enrollment Report");
        setSize(1000, 620);
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
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        row1.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("Enrollment Report");
        lblTitle.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        lblTitle.setForeground(ThemeManager.LIGHT_BLUE);
        row1.add(lblTitle);

        row1.add(Box.createHorizontalStrut(20));
        row1.add(new JLabel("Year:"));
        cmbYear = createYearCombo();
        row1.add(cmbYear);

        row1.add(new JLabel("Start Month:"));
        cmbStartMonth = createMonthCombo();
        row1.add(cmbStartMonth);

        row1.add(new JLabel("End Month:"));
        cmbEndMonth = createMonthCombo();
        row1.add(cmbEndMonth);

        row1.add(new JLabel("Batch:"));
        cmbBatch = new JComboBox<>(new String[]{"All", "01", "02", "03", "04", "05", "06", "07", "08"});
        cmbBatch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbBatch.setPreferredSize(new Dimension(80, 28));
        row1.add(cmbBatch);

        filterContainer.add(row1);

// ---- Row 2 ----
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        row2.setBackground(Color.WHITE);

        row2.add(new JLabel("Course:"));
        cmbCourse = new JComboBox<>();
        cmbCourse.setPreferredSize(new Dimension(180, 28));
        loadCourses();
        row2.add(cmbCourse);

        row2.add(new JLabel("Course ID:"));
        txtCourseId = new JTextField(6);
        row2.add(txtCourseId);

        row2.add(new JLabel("UG ID:"));
        txtUgID = new JTextField(8);
        row2.add(txtUgID);

        JButton btnView = new JButton("View Report");
        ThemeManager.stylePrimaryButton(btnView);
        btnView.addActionListener(this::loadData);
        row2.add(btnView);

        JButton btnPDF = new JButton("Export PDF");
        ThemeManager.stylePrimaryButton(btnPDF);
        btnPDF.addActionListener(this::exportPDF);
        row2.add(btnPDF);

        filterContainer.add(row2);

// finally, add filterContainer to main panel
        main.add(filterContainer, BorderLayout.NORTH);

        // ---------- TABLE ----------
        model = new DefaultTableModel(new Object[]{
                "Enroll Date", "Course ID", "Course Name",
                "Student ID", "Student Name", "Batch"
        }, 0) { @Override public boolean isCellEditable(int r, int c) { return false; } };

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

    // ------------------------------------------------------------------
    private JComboBox<String> createMonthCombo() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        for (int m=1; m<=12; m++)
            combo.addItem(String.format("%02d", m));
        combo.setSelectedIndex(LocalDate.now().getMonthValue()-1);
        combo.setPreferredSize(new Dimension(70,28));
        return combo;
    }

    private JComboBox<String> createYearCombo() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.addItem("All");
        int currentY = LocalDate.now().getYear();
        for (int y=currentY; y>=currentY-5; y--) combo.addItem(String.valueOf(y));
        combo.setPreferredSize(new Dimension(80,28));
        return combo;
    }

    private void loadCourses() {
        cmbCourse.removeAllItems();
        cmbCourse.addItem("All");
        try (Connection conn=dbconnection.getConnection();
             PreparedStatement ps=conn.prepareStatement("SELECT course_id, course_name FROM course");
             ResultSet rs=ps.executeQuery()) {
            while (rs.next()) cmbCourse.addItem(rs.getString("course_id")+" - "+rs.getString("course_name"));
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ------------------------------------------------------------------
    //  Load report data
    // ------------------------------------------------------------------
    private void loadData(ActionEvent e) {
        model.setRowCount(0);
        lblCount.setText("Loading...");

        int startM = Integer.parseInt(cmbStartMonth.getSelectedItem().toString());
        int endM   = Integer.parseInt(cmbEndMonth.getSelectedItem().toString());
        String batch = cmbBatch.getSelectedItem().toString();
        String course = cmbCourse.getSelectedItem().toString();
        String typedCourse = txtCourseId.getText().trim();
        String ugId = txtUgID.getText().trim();
        String yearText = cmbYear.getSelectedItem().toString();

        StringBuilder sql = new StringBuilder(
                "SELECT e.enroll_date, c.course_id, c.course_name, u.ug_id, usr.user_name, u.ug_batch " +
                        "FROM enrollment e " +
                        "JOIN course c ON e.course_id=c.course_id " +
                        "JOIN undergraduate u ON e.ug_id=u.ug_id " +
                        "JOIN user usr ON usr.user_id=u.ug_id WHERE 1=1");

        if (!yearText.equals("All")) sql.append(" AND YEAR(e.enroll_date)=?");
        sql.append(" AND MONTH(e.enroll_date) BETWEEN ? AND ?");
        if (!batch.equals("All")) sql.append(" AND u.ug_batch=?");
        if (!course.equals("All")) sql.append(" AND e.course_id=?");
        if (!typedCourse.isEmpty()) sql.append(" AND e.course_id=?");
        if (!ugId.isEmpty()) sql.append(" AND u.ug_id=?");
        sql.append(" ORDER BY e.enroll_date DESC");

        try (Connection conn=dbconnection.getConnection();
             PreparedStatement ps=conn.prepareStatement(sql.toString())) {

            int i=1;
            if (!yearText.equals("All")) ps.setInt(i++, Integer.parseInt(yearText));
            ps.setInt(i++, startM);
            ps.setInt(i++, endM);
            if (!batch.equals("All")) ps.setInt(i++, Integer.parseInt(batch));
            if (!course.equals("All")) ps.setString(i++, course.split(" ")[0]);
            if (!typedCourse.isEmpty()) ps.setString(i++, typedCourse);
            if (!ugId.isEmpty()) ps.setString(i++, ugId);

            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                model.addRow(new Object[]{
                        rs.getTimestamp("enroll_date"),
                        rs.getString("course_id"),
                        rs.getString("course_name"),
                        rs.getString("ug_id"),
                        rs.getString("user_name"),
                        rs.getInt("ug_batch")
                });
            }
            lblCount.setText(String.format("<html><b>Total Enrollments:</b> %d</html>", model.getRowCount()));
            if(model.getRowCount()==0)
                JOptionPane.showMessageDialog(this,"No enrollments found.");

        } catch (Exception ex) {
            ex.printStackTrace();
            lblCount.setText("Error loading data.");
            JOptionPane.showMessageDialog(this,"Database error.","Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    // ------------------------------------------------------------------
    private void exportPDF(ActionEvent e) {
        if (model.getRowCount()==0) {
            JOptionPane.showMessageDialog(this,"Nothing to export!"); return;
        }
        JFileChooser fc=new JFileChooser();
        fc.setSelectedFile(new java.io.File("EnrollmentReport.pdf"));
        if(fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return;

        try {
            Document doc=new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc,new FileOutputStream(fc.getSelectedFile()));
            doc.open();
            com.itextpdf.text.Font titleFont =
                    new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,
                            18, com.itextpdf.text.Font.BOLD, BaseColor.BLUE);
            doc.add(new Paragraph("Enrollment Report",titleFont));
            doc.add(new Paragraph(" "));

            PdfPTable pdfTable=new PdfPTable(model.getColumnCount());
            pdfTable.setWidthPercentage(100);
            for(int i=0;i<model.getColumnCount();i++)
                pdfTable.addCell(new Phrase(model.getColumnName(i)));
            for(int r=0;r<model.getRowCount();r++)
                for(int c=0;c<model.getColumnCount();c++)
                    pdfTable.addCell(String.valueOf(model.getValueAt(r,c)));

            doc.add(pdfTable);
            doc.close();
            JOptionPane.showMessageDialog(this,"Report exported successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,"Error exporting PDF.","Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new EnrollmentReportFrame("admin").setVisible(true));
    }
}