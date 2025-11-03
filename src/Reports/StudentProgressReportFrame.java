package Reports;

// ---------- PDF (iText) ----------
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

// ---------- Swing / AWT ----------
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;               // uses java.awt.Font
import java.awt.event.ActionEvent;

// ---------- Java / JDBC ----------
import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDate;

import Database.dbconnection;
import Utils.ThemeManager;

/**
 * Student Progress Report
 *  - Filters: year, start month, end month, batch, course
 *  - Lists each student's module completions
 *  - Exports to PDF
 */
public class StudentProgressReportFrame extends JFrame {

    private JComboBox<String> cmbYear, cmbStartM, cmbEndM, cmbBatch, cmbCourse;
    private JTable table;
    private DefaultTableModel model;
    private final JLabel lblCount = new JLabel();

    public StudentProgressReportFrame(String role) {
        ThemeManager.initialize();
        setTitle("Student Progress Report");
        setSize(1000, 620);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBorder(new EmptyBorder(15, 20, 15, 20));
        main.setBackground(Color.WHITE);

        // ---------------------- Top Filter Panel ----------------------
        JPanel filters = new JPanel();
        filters.setLayout(new BoxLayout(filters, BoxLayout.Y_AXIS));
        filters.setBackground(Color.WHITE);

        // row 1
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 8));
        row1.setBackground(Color.WHITE);
        JLabel title = new JLabel("Student Progress Report");
        title.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        title.setForeground(ThemeManager.LIGHT_BLUE);
        row1.add(title);

        row1.add(Box.createHorizontalStrut(30));
        row1.add(new JLabel("Year:"));
        cmbYear = createYearCombo();
        row1.add(cmbYear);

        row1.add(new JLabel("Start:"));
        cmbStartM = createMonthCombo();
        row1.add(cmbStartM);

        row1.add(new JLabel("End:"));
        cmbEndM = createMonthCombo();
        row1.add(cmbEndM);

        row1.add(new JLabel("Batch:"));
        cmbBatch = new JComboBox<>(new String[]{
                "All", "01","02","03","04","05","06","07","08","09"});
        cmbBatch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbBatch.setPreferredSize(new Dimension(80, 28));
        row1.add(cmbBatch);

        filters.add(row1);

        // row 2
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        row2.setBackground(Color.WHITE);
        row2.add(new JLabel("Course:"));
        cmbCourse = new JComboBox<>();
        cmbCourse.setPreferredSize(new Dimension(180, 28));
        loadCourses();
        row2.add(cmbCourse);

        JButton btnView = new JButton("View Report");
        ThemeManager.stylePrimaryButton(btnView);
        btnView.addActionListener(this::loadData);
        row2.add(btnView);

        JButton btnPDF = new JButton("Export PDF");
        ThemeManager.stylePrimaryButton(btnPDF);
        btnPDF.addActionListener(this::exportPDF);
        row2.add(btnPDF);

        filters.add(row2);
        main.add(filters, BorderLayout.NORTH);

        // ---------------------- Table ----------------------
        model = new DefaultTableModel(new Object[]{
                "Completed Date", "UG ID", "Student Name",
                "Batch", "Course ID", "Course Name", "Module Title"
        }, 0) { @Override public boolean isCellEditable(int r,int c){return false;} };

        table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ThemeManager.styleTableHeader(table);
        main.add(new JScrollPane(table), BorderLayout.CENTER);

        // footer
        lblCount.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        lblCount.setForeground(Color.DARK_GRAY);
        lblCount.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        main.add(lblCount, BorderLayout.SOUTH);

        add(main);
    }

    // -----------------------------------------------------------------
    private JComboBox<String> createMonthCombo() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        for (int m=1;m<=12;m++)
            combo.addItem(String.format("%02d",m));
        combo.setSelectedIndex(LocalDate.now().getMonthValue()-1);
        combo.setPreferredSize(new Dimension(70,28));
        return combo;
    }

    private JComboBox<String> createYearCombo() {
        JComboBox<String> combo = new JComboBox<>();
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        combo.addItem("All");
        int cur = LocalDate.now().getYear();
        for(int y=cur;y>=cur-5;y--) combo.addItem(String.valueOf(y));
        combo.setPreferredSize(new Dimension(80,28));
        return combo;
    }

    private void loadCourses() {
        cmbCourse.removeAllItems();
        cmbCourse.addItem("All");
        String sql = "SELECT course_id, course_name FROM course";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                cmbCourse.addItem(rs.getString("course_id")+" - "+rs.getString("course_name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this,"Error loading courses: "+e.getMessage());
        }
    }

    // -----------------------------------------------------------------
    private void loadData(ActionEvent e) {
        model.setRowCount(0);
        lblCount.setText("Loading...");

        int start = Integer.parseInt(cmbStartM.getSelectedItem().toString());
        int end = Integer.parseInt(cmbEndM.getSelectedItem().toString());
        String yearText = cmbYear.getSelectedItem().toString();
        String batch = cmbBatch.getSelectedItem().toString();
        String courseSel = cmbCourse.getSelectedItem().toString();

        StringBuilder sql = new StringBuilder("""
                SELECT mp.completed_at, u.ug_id, usr.user_name, u.ug_batch,
                       c.course_id, c.course_name, m.title
                FROM module_progress mp
                  JOIN undergraduate u ON mp.ug_id=u.ug_id
                  JOIN user usr ON u.ug_id=usr.user_id
                  JOIN course c ON mp.course_id=c.course_id
                  JOIN module m ON mp.module_id=m.module_id
                WHERE mp.completed=1
                """);

        if (!yearText.equals("All")) sql.append(" AND YEAR(mp.completed_at)=?");
        sql.append(" AND MONTH(mp.completed_at) BETWEEN ? AND ?");
        if (!batch.equals("All")) sql.append(" AND u.ug_batch=?");
        if (!courseSel.equals("All")) sql.append(" AND c.course_id=?");
        sql.append(" ORDER BY mp.completed_at DESC");

        try (Connection con = dbconnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql.toString())) {

            int i=1;
            if (!yearText.equals("All"))
                ps.setInt(i++, Integer.parseInt(yearText));
            ps.setInt(i++, start);
            ps.setInt(i++, end);
            if (!batch.equals("All"))
                ps.setInt(i++, Integer.parseInt(batch));
            if (!courseSel.equals("All"))
                ps.setString(i++, courseSel.split(" ")[0]);

            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                model.addRow(new Object[]{
                        rs.getTimestamp("completed_at"),
                        rs.getString("ug_id"),
                        rs.getString("user_name"),
                        rs.getInt("ug_batch"),
                        rs.getString("course_id"),
                        rs.getString("course_name"),
                        rs.getString("title")
                });
            }
            lblCount.setText(String.format("<html><b>Total Completions:</b> %d</html>", model.getRowCount()));
            if(model.getRowCount()==0) JOptionPane.showMessageDialog(this,"No records found.");
        } catch (SQLException ex) {
            ex.printStackTrace();
            lblCount.setText("Error fetching data.");
            JOptionPane.showMessageDialog(this,"Database error.","Error",JOptionPane.ERROR_MESSAGE);
        }
    }

    // -----------------------------------------------------------------
    private void exportPDF(ActionEvent e) {
        if (model.getRowCount()==0) {
            JOptionPane.showMessageDialog(this,"Nothing to export!"); return;
        }
        JFileChooser fc = new JFileChooser();
        fc.setSelectedFile(new java.io.File("StudentProgressReport.pdf"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            Document doc=new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc,new FileOutputStream(fc.getSelectedFile()));
            doc.open();
            com.itextpdf.text.Font headerFont =
                    new com.itextpdf.text.Font(
                            com.itextpdf.text.Font.FontFamily.HELVETICA,
                            18,
                            com.itextpdf.text.Font.BOLD,
                            BaseColor.BLUE
                    );

            doc.add(new Paragraph("Student Progress Report", headerFont));
            doc.add(new Paragraph(" "));

            PdfPTable t = new PdfPTable(model.getColumnCount());
            t.setWidthPercentage(100);
            for(int c=0;c<model.getColumnCount();c++)
                t.addCell(new Phrase(model.getColumnName(c)));
            for(int r=0;r<model.getRowCount();r++)
                for(int c=0;c<model.getColumnCount();c++)
                    t.addCell(String.valueOf(model.getValueAt(r,c)));
            doc.add(t);
            doc.close();
            JOptionPane.showMessageDialog(this,"Report exported successfully.","Success",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,"PDF export error: "+ex.getMessage());
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StudentProgressReportFrame("admin").setVisible(true));
    }
}