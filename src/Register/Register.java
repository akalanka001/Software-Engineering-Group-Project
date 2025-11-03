package Register;

import Admin.encryption;
import Database.dbconnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class Register {

    public String registerUndergraduate(String firstName, String lastName,
                                        String password, String email, String phone) throws Exception {

        String userId = generateNextUndergraduateID();
        String userName = firstName + " " + lastName;
        String role = "undergraduate";
        String proPic = null;
        int batch = 7;

        String hashedPassword = encryption.hashpassword(password);

        String sqlUser =
                "INSERT INTO user (user_id, user_password, user_name, user_email, user_phone, user_batch, user_pro_pic, user_role) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlUG =
                "INSERT INTO undergraduate (ug_id, ug_batch) VALUES (?, ?)";
        // 🌟 simplified notification insert (no admin_id)
        String sqlAdminNotif =
                "INSERT INTO admin_notification (message) VALUES (?)";

        try (Connection conn = dbconnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement psUser = conn.prepareStatement(sqlUser);
                 PreparedStatement psUG = conn.prepareStatement(sqlUG);
                 PreparedStatement psAdminNotif = conn.prepareStatement(sqlAdminNotif)) {

                // --- Insert into user table ---
                psUser.setString(1, userId);
                psUser.setString(2, hashedPassword);
                psUser.setString(3, userName);
                psUser.setString(4, email);
                psUser.setString(5, phone);
                psUser.setString(6, String.valueOf(batch));
                psUser.setNull(7, java.sql.Types.VARCHAR);
                psUser.setString(8, role);
                psUser.executeUpdate();

                // --- Insert into undergraduate table ---
                psUG.setString(1, userId);
                psUG.setInt(2, batch);
                psUG.executeUpdate();

                // --- Insert into admin_notification ---
                String msg = "A new undergraduate (" + userId + ") registered successfully: " + userName;
                psAdminNotif.setString(1, msg);
                psAdminNotif.executeUpdate();

                conn.commit();
                return userId;

            } catch (Exception e) {
                conn.rollback();
                e.printStackTrace();
                return null;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String generateNextUndergraduateID() {
        String prefix = "TG";
        String sql = "SELECT ug_id FROM undergraduate ORDER BY ug_id DESC LIMIT 1";
        try (Connection conn = dbconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                String lastId = rs.getString("ug_id");
                int number = Integer.parseInt(lastId.substring(2));
                number++;
                return prefix + number;
            } else {
                return prefix + "1000";
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    public String findUserByEmail(String email) {
        String sql = "SELECT user_id FROM user WHERE user_email = ?";
        try (Connection conn = dbconnection.getConnection();
             java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("user_id");
                }
            }
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
        }
        return null; // Email not found
    }
}