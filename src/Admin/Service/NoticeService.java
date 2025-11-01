package Admin.Service;

import Database.dbconnection;

import javax.swing.*;
import java.sql.*;

/**
 * Handles all Notice CRUD logic with simple validation helpers.
 */
public class NoticeService {

    // ---------- Validation ----------
    public static boolean validateFields(String title, String content) {
        if (title == null || title.trim().isEmpty()
                || content == null || content.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null,
                    "Title and content are required!",
                    "Validation Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    public static boolean validateId(String idText) {
        if (idText == null || idText.trim().isEmpty()) {
            JOptionPane.showMessageDialog(null, "Notice ID is required!");
            return false;
        }
        try {
            Integer.parseInt(idText);
            return true;
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(null, "Invalid Notice ID format.");
            return false;
        }
    }

    // ---------- CREATE ----------
    public static boolean add(String title, String content, java.sql.Date date) {
        String sql = "INSERT INTO notice (notice_title, notice_content, notice_date) VALUES (?,?,?)";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, title);
            pst.setString(2, content);
            pst.setDate(3, date);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error adding notice:\n" + e.getMessage());
            return false;
        }
    }

    // ---------- READ ----------
    public static ResultSet getAll() throws SQLException {
        Connection con = dbconnection.getConnection();
        Statement st = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        return st.executeQuery("SELECT * FROM notice ORDER BY notice_date DESC");
    }

    // ---------- UPDATE ----------
    public static boolean update(int id, String title, String content, java.sql.Date date) {
        String sql = "UPDATE notice SET notice_title=?, notice_content=?, notice_date=? WHERE notice_id=?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, title);
            pst.setString(2, content);
            pst.setDate(3, date);
            pst.setInt(4, id);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error updating notice:\n" + e.getMessage());
            return false;
        }
    }

    // ---------- DELETE ----------
    public static boolean delete(int id) {
        String sql = "DELETE FROM notice WHERE notice_id=?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, id);
            return pst.executeUpdate() > 0;
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, "Error deleting notice:\n" + e.getMessage());
            return false;
        }
    }

    // ---------- EXISTENCE CHECK ----------
    public static boolean exists(int id) {
        String sql = "SELECT 1 FROM notice WHERE notice_id=?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null,
                    "Error checking notice existence:\n" + e.getMessage());
            return false;
        }
    }
}