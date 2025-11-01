package Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

/** Writes audit-trail entries to activity_log. */
public class ActivityLogger {

    /**
     * Logs a CRUD action in activity_log.
     * @param performedBy The admin performing the action
     * @param targetUserId The user affected by the action
     * @param action Description of the action (e.g., "Added user TG1234")
     */
    public static void log(String performedBy, String targetUserId, String action) {
        if (performedBy == null || performedBy.isBlank()) return;

        String sql = "INSERT INTO activity_log (user_id, action) VALUES (?, ?)";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, performedBy);
            pst.setString(2, action + " [" + targetUserId + "]");
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("⚠ Could not write audit log: " + e.getMessage());
        }
    }
}
