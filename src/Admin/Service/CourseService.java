package Admin.Service;

import Database.ActivityLogger;
import Database.dbconnection;
import javax.swing.*;
import java.sql.*;

/**
 * Handles add / update / delete for the Course table.
 * Every successful operation writes a record to activity_log.
 */
public final class CourseService {

    private CourseService() {}

    // --------------------------- VALIDATION ---------------------------
    public static boolean validate(String id, String name, String lec,
                                   String creditTxt, String type, String feeTxt) {
        if (id == null || id.isBlank() ||
                name == null || name.isBlank() ||
                lec == null || lec.isBlank() ||
                creditTxt == null || creditTxt.isBlank() ||
                type == null || type.isBlank() ||
                feeTxt == null || feeTxt.isBlank()) {
            JOptionPane.showMessageDialog(null, "All fields are required!");
            return false;
        }
        try { Integer.parseInt(creditTxt); }
        catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Credit must be numeric!"); return false;
        }
        try { Double.parseDouble(feeTxt); }
        catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(null, "Course fee must be numeric!"); return false;
        }
        return true;
    }

    // ------------------------------ ADD ------------------------------
    public static boolean add(String id, String name, String lec,
                              int credit, String type, double fee) {
        String sql = """
            INSERT INTO course (course_id, course_name, lec_id, credit, course_type, course_fee)
            VALUES (?,?,?,?,?,?)
            """;
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, id);
            pst.setString(2, name);
            pst.setString(3, lec);
            pst.setInt(4, credit);
            pst.setString(5, type);
            pst.setDouble(6, fee);

            int rows = pst.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(null,
                        "✅ Course added successfully.",
                        "Added", JOptionPane.INFORMATION_MESSAGE);
                return true;
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Failed to add course:\n" + e.getMessage());
        }
        return false;
    }

    // ----------------------------- UPDATE -----------------------------
    public static boolean update(String id, String name, String lec,
                                 int credit, String type, double fee) {
        String sql = """
            UPDATE course
               SET course_name=?, lec_id=?, credit=?, course_type=?, course_fee=?
             WHERE course_id=?
            """;
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {

            pst.setString(1, name);
            pst.setString(2, lec);
            pst.setInt(3, credit);
            pst.setString(4, type);
            pst.setDouble(5, fee);
            pst.setString(6, id);

            int rows = pst.executeUpdate();
            if (rows > 0) {
                JOptionPane.showMessageDialog(null,
                        "✅ Course updated successfully.",
                        "Updated", JOptionPane.INFORMATION_MESSAGE);
                return true;
            } else {
                JOptionPane.showMessageDialog(null,
                        "⚠️ No course found with ID: " + id,
                        "Not Found", JOptionPane.WARNING_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error updating course:\n" + e.getMessage());
        }
        return false;
    }

    // ------------------------------ CHECKS ----------------------------
    public static boolean validateId(String id) {
        if (id == null || id.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Course ID is required!");
            return false;
        }
        return true;
    }

    public static boolean exists(String id) {
        String sql = "SELECT 1 FROM course WHERE course_id=?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error checking course existence:\n" + e.getMessage());
            return false;
        }
    }

    // ------------------------------ DELETE ----------------------------
    public static boolean delete(String courseId) {
        String checkSql  = "SELECT 1 FROM enrollment WHERE course_id=? LIMIT 1";
        String deleteSql = "DELETE FROM course WHERE course_id=?";
        try (Connection con = dbconnection.getConnection()) {

            // --- block if students enrolled ---
            try (PreparedStatement check = con.prepareStatement(checkSql)) {
                check.setString(1, courseId);
                ResultSet rs = check.executeQuery();
                if (rs.next()) {
                    JOptionPane.showMessageDialog(null,
                            "❌ Cannot delete this course:\n" +
                                    "Students are currently enrolled.\n" +
                                    "Please unenroll them first.",
                            "Delete Blocked", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
            }

            // --- perform deletion ---
            try (PreparedStatement pst = con.prepareStatement(deleteSql)) {
                pst.setString(1, courseId);
                int rows = pst.executeUpdate();
                if (rows > 0) {
                    JOptionPane.showMessageDialog(null,
                            "✅ Course deleted successfully.",
                            "Deleted", JOptionPane.INFORMATION_MESSAGE);

                    return true;
                } else {
                    JOptionPane.showMessageDialog(null,
                            "⚠️ No course found with ID: " + courseId,
                            "Not Found", JOptionPane.WARNING_MESSAGE);
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error deleting course:\n" + e.getMessage(),
                    "SQL Error", JOptionPane.ERROR_MESSAGE);
        }
        return false;
    }
}