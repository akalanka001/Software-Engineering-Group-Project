package Admin;

import Database.dbconnection;
import javax.swing.JOptionPane;
import java.sql.*;

/**
 * Deletes users from the system.
 */
public class deleteUser {

    public static boolean validation(String id) {
        if (id == null || id.isBlank()) {
            JOptionPane.showMessageDialog(null, "User ID is required!");
            return false;
        }
        return true;
    }

    public static boolean usernameExists(String id) {
        String sql = "SELECT 1 FROM user WHERE user_id=?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error checking user existence: " + e.getMessage());
            return false;
        }
    }

    public static boolean deleteUser(String id) {
        // First, detect whether this user is referenced elsewhere
        if (isReferenced(id)) {
            JOptionPane.showMessageDialog(null,
                    "Cannot delete this user because they are linked to courses or enrollments.\n" +
                            "Please remove or reassign those records first.");
            return false;
        }

        String sql = "DELETE FROM user WHERE user_id=?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);
            return pst.executeUpdate() > 0;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error deleting user: " + e.getMessage());
            return false;
        }
    }
    public static boolean isLecturer(String userId) {
        String sql = "SELECT 1 FROM lecturer WHERE lec_id = ?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, userId);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error checking lecturer: " + e.getMessage());
        }
        return false;
    }


    public static boolean isReferenced(String lecId) {
        String checkRefQuery = """
        SELECT 1
        FROM course c
        JOIN enrollment e ON c.course_id = e.course_id
        WHERE c.lec_id = ?
        LIMIT 1
    """;

        String checkNotifQuery = "SELECT 1 FROM lec_notification WHERE lec_id = ? LIMIT 1";

        try (Connection con = dbconnection.getConnection()) {

            // 1️⃣ Check if lecturer has any courses with enrolled students
            try (PreparedStatement pst = con.prepareStatement(checkRefQuery)) {
                pst.setString(1, lecId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        // Lecturer has active courses with enrollments
                        return true;
                    }
                }
            }

            // 2️⃣ Check if lecturer has any notifications
            try (PreparedStatement pst = con.prepareStatement(checkNotifQuery)) {
                pst.setString(1, lecId);
                try (ResultSet rs = pst.executeQuery()) {
                    if (rs.next()) {
                        // Lecturer has notifications linked to them
                        return true;
                    }
                }
            }

        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error checking lecturer references: " + e.getMessage());
        }

        // ✅ No references found → safe to delete
        return false;
    }


}