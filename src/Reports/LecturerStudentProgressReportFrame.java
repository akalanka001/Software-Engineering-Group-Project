package Reports;

import Database.dbconnection;
import Utils.ThemeManager;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDate;

/**
 * Lecturer view of their students' module progress.
 *  - Filters: Year, start month, end month, batch, completion checkbox
 *  - Lists each student's percentage progress for lecturer's own courses
 */
public class LecturerStudentProgressReportFrame extends JFrame {

    private final String lecturerId;
    private JComboBox<String> cmbYear, cmbStartM, cmbEndM, cmbBatch;
    private JCheckBox chkCompleted;
    private JTable table;
    private DefaultTableModel model;
    private final JLabel lblCount = new JLabel();

    public LecturerStudentProgressReportFrame(String lecturerId) {
        this.lecturerId = lecturerId;
        ThemeManager.initialize();
        setTitle("Student Progress Report – Lecturer View");
        setSize(1000, 620);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10,10));
        main.setBorder(new EmptyBorder(15,20,15,20));
        main.setBackground(Color.WHITE);

        // ------------------- Filter area -------------------
        JPanel filters = new JPanel();
        filters.setLayout(new BoxLayout(filters, BoxLayout.Y_AXIS));
        filters.setBackground(Color.WHITE);

        // Row 1
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT,15,8));
        row1.setBackground(Color.WHITE);

        JLabel title = new JLabel("My Students' Progress");
        title.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        title.setForeground(ThemeManager.LIGHT_BLUE);
        row1.add(title);

        row1.add(Box.createHorizontalStrut(25));
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
                "All","01","02","03","04","05","06","07","08","09"});
        cmbBatch.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        cmbBatch.setPreferredSize(new Dimension(80,28));
        row1.add(cmbBatch);

        chkCompleted = new JCheckBox("Show only completed", true);
        chkCompleted.setOpaque(false);
        row1.add(chkCompleted);

        filters.add(row1);

        // Row 2 buttons
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT,15,5));
        row2.setBackground(Color.WHITE);

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

        // ------------------- Table -------------------
        model = new DefaultTableModel(new Object[]{
                "UG ID", "Student Name",
                "Batch", "Course ID", "Course Name",
                "Completed Modules", "Total Modules", "Progress %"
        }, 0){ @Override public boolean isCellEditable(int r,int c){return false;}};

        table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ThemeManager.styleTableHeader(table);
        main.add(new JScrollPane(table), BorderLayout.CENTER);

        lblCount.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        lblCount.setForeground(Color.DARK_GRAY);
        lblCount.setBorder(BorderFactory.createEmptyBorder(5,10,10,10));
        main.add(lblCount, BorderLayout.SOUTH);

        add(main);
    }

    // --------------------------------------------------------------------------
    private JComboBox<String> createMonthCombo(){
        JComboBox<String> combo=new JComboBox<>();
        combo.setFont(new Font("Segoe UI",Font.PLAIN,13));
        for(int m=1;m<=12;m++)
            combo.addItem(String.format("%02d",m));
        combo.setSelectedIndex(LocalDate.now().getMonthValue()-1);
        combo.setPreferredSize(new Dimension(70,28));
        return combo;
    }

    private JComboBox<String> createYearCombo(){
        JComboBox<String> combo=new JComboBox<>();
        combo.setFont(new Font("Segoe UI", Font.PLAIN,13));
        combo.addItem("All");
        int cur=LocalDate.now().getYear();
        for(int y=cur;y>=cur-5;y--)
            combo.addItem(String.valueOf(y));
        combo.setPreferredSize(new Dimension(80,28));
        return combo;
    }

    // --------------------------------------------------------------------------
    private void loadData(ActionEvent e){
        model.setRowCount(0);
        lblCount.setText("Loading...");

        int start=Integer.parseInt(cmbStartM.getSelectedItem().toString());
        int end=Integer.parseInt(cmbEndM.getSelectedItem().toString());
        String yearText=cmbYear.getSelectedItem().toString();
        String batch=cmbBatch.getSelectedItem().toString();
        boolean onlyCompleted=chkCompleted.isSelected();

        StringBuilder sql=new StringBuilder("""
            SELECT u.ug_id, usr.user_name, u.ug_batch,
                   c.course_id, c.course_name,
                   COUNT(mp.module_id) AS completed_modules,
                   (SELECT COUNT(*) FROM module m WHERE m.course_id=c.course_id) AS total_modules,
                   ROUND(COUNT(mp.module_id)*100.0/
                         (SELECT COUNT(*) FROM module m WHERE m.course_id=c.course_id),2) AS progress_percent
            FROM undergraduate u
              JOIN user usr ON usr.user_id=u.ug_id
              JOIN enrollment e ON e.ug_id=u.ug_id
              JOIN course c ON e.course_id=c.course_id
              LEFT JOIN module_progress mp
                     ON mp.ug_id=u.ug_id
                    AND mp.course_id=c.course_id
                    AND mp.completed=1
            WHERE e.approval_status='approved' AND c.lec_id=? 
            """);

        if (!yearText.equals("All"))
            sql.append(" AND YEAR(e.enroll_date)=?");
        sql.append(" AND MONTH(e.enroll_date) BETWEEN ? AND ?");
        if (!batch.equals("All"))
            sql.append(" AND u.ug_batch=?");
        // completed filter: hide rows with 0% if checked=false
        sql.append(" GROUP BY u.ug_id, c.course_id ");
        if (onlyCompleted)
            sql.append(" HAVING progress_percent>=100");
        sql.append(" ORDER BY u.ug_id;");

        try (Connection con=dbconnection.getConnection();
             PreparedStatement ps=con.prepareStatement(sql.toString())){
            int i=1;
            ps.setString(i++,lecturerId);
            if (!yearText.equals("All"))
                ps.setInt(i++,Integer.parseInt(yearText));
            ps.setInt(i++,start);
            ps.setInt(i++,end);
            if (!batch.equals("All"))
                ps.setInt(i++,Integer.parseInt(batch));

            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                model.addRow(new Object[]{
                        rs.getString("ug_id"),
                        rs.getString("user_name"),
                        rs.getInt("ug_batch"),
                        rs.getString("course_id"),
                        rs.getString("course_name"),
                        rs.getInt("completed_modules"),
                        rs.getInt("total_modules"),
                        rs.getDouble("progress_percent")+"%"
                });
            }
            lblCount.setText(String.format("<html><b>Total Students:</b> %d</html>", model.getRowCount()));
            if(model.getRowCount()==0)
                JOptionPane.showMessageDialog(this,"No records found.");
        }catch(SQLException ex){
            ex.printStackTrace();
            lblCount.setText("Error fetching data.");
            JOptionPane.showMessageDialog(this,"Database error: "+ex.getMessage());
        }
    }

    // --------------------------------------------------------------------------
    private void exportPDF(ActionEvent e){
        if(model.getRowCount()==0){
            JOptionPane.showMessageDialog(this,"Nothing to export!"); return;
        }
        JFileChooser fc=new JFileChooser();
        fc.setSelectedFile(new java.io.File("LecturerProgressReport.pdf"));
        if(fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION)return;

        try{
            Document doc=new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc,new FileOutputStream(fc.getSelectedFile()));
            doc.open();
            com.itextpdf.text.Font pdfFont=new com.itextpdf.text.Font(
                    com.itextpdf.text.Font.FontFamily.HELVETICA,
                    18,
                    com.itextpdf.text.Font.BOLD,
                    BaseColor.BLUE);
            doc.add(new Paragraph("Students Progress Report (My Courses)",pdfFont));
            doc.add(new Paragraph(" "));

            PdfPTable t=new PdfPTable(model.getColumnCount());
            t.setWidthPercentage(100);
            for(int c=0;c<model.getColumnCount();c++)
                t.addCell(new Phrase(model.getColumnName(c)));
            for(int r=0;r<model.getRowCount();r++)
                for(int c=0;c<model.getColumnCount();c++)
                    t.addCell(String.valueOf(model.getValueAt(r,c)));
            doc.add(t);
            doc.close();
            JOptionPane.showMessageDialog(this,"Report exported successfully.","Success",JOptionPane.INFORMATION_MESSAGE);
        }catch(Exception ex){
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,"PDF export error: "+ex.getMessage());
        }
    }

    // --------------------------------------------------------------------------
    public static void main(String[] args){
        SwingUtilities.invokeLater(() ->
                new LecturerStudentProgressReportFrame("LC0002").setVisible(true));
    }
}