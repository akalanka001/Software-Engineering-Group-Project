package Lecturer;


import Database.dbconnection;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.JComboBox;
import javax.swing.JTable;


    

public class Course {

    private Connection con;
    private ResultSet rs;
    private PreparedStatement ps;
    
    
    public void loadCoursesToComboBox(JComboBox<String> courseComboBox, String userId) {
        con = dbconnection.getConnection();
        String sql = "SELECT course_id, course_name FROM course where lec_id = ?";
        try {
            ps = con.prepareStatement(sql);
            ps.setString(1, userId);
            rs = ps.executeQuery();

            while (rs.next()) {
                String courseId = rs.getString("course_id");
                String courseName = rs.getString("course_name");
                String comboItem = courseId + " - " + courseName;
                courseComboBox.addItem(comboItem);
            }
            rs.close();
            ps.close();
            con.close();
        } catch (SQLException ex) {
            System.out.println(ex.getMessage());

        }
    }
    
    public void loadCourseMaterials(JTable courseMaterialTable,String courseId) {
    con = dbconnection.getConnection();
    String sql = "SELECT material_name, material_path, material_type FROM lecture_materials WHERE course_id = ?";
    
    try {
        ps = con.prepareStatement(sql);
        ps.setString(1, courseId);
        rs = ps.executeQuery();
        
        
        DefaultTableModel model = (DefaultTableModel) courseMaterialTable.getModel();
        model.setRowCount(0); // Clear existing rows first
        
        while (rs.next()) {
            String materialName = rs.getString("material_name");
            String materialPath = rs.getString("material_path");
            String materialType = rs.getString("material_type");  
            
            model.addRow(new Object[] { materialName, materialPath });
        }
        
        rs.close();
        ps.close();
        con.close();
    } catch (SQLException ex) {
        System.out.println(ex.getMessage());
    }
}

}
