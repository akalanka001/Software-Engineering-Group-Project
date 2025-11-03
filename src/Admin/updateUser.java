package Admin;

import Database.dbconnection;
import Utils.EmailSender;
import javax.swing.*;
import java.sql.*;

/**
 * Handles Admin / Lecturer / Undergraduate updates.
 *  - Validation and numeric checks
 *  - Updates user + related undergraduate/lecturer tables
 *  - Sends notifications and password‑change emails
 */
public class updateUser {

    // -------------------- Validation helpers --------------------

    private static boolean isValidUserId(String id) { return id.matches("^[A-Za-z]{2}\\d{4}$"); }

    private static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private static boolean isValidPhone(String phone) { return phone.matches("^0\\d{9}$"); }

    public static boolean validation(String id, String role,
                                     String name, String email, String phone) {
        if (id.isBlank() || role.isBlank() || name.isBlank()
                || email.isBlank() || phone.isBlank()) {
            JOptionPane.showMessageDialog(null, "All fields are required!");
            return false;
        }
        if (!isValidUserId(id)) {
            JOptionPane.showMessageDialog(null,
                    "User ID must be two letters followed by four digits (e.g., TG1234).");
            return false;
        }
        if (!isValidEmail(email)) {
            JOptionPane.showMessageDialog(null, "Invalid email format!");
            return false;
        }
        if (!isValidPhone(phone)) {
            JOptionPane.showMessageDialog(null,
                    "Phone number must start with 0 and be exactly 10 digits.");
            return false;
        }
        return true;
    }

    // -------------------- Notification helper --------------------

    private static void insertNotification(String role, String id, String message) {
        String table, column;
        switch (role.toLowerCase()) {
            case "lecturer":      table = "lec_notification"; column = "lec_id"; break;
            case "undergraduate": table = "ug_notification";  column = "ug_id";  break;
            default:              table = "admin_notification"; column = null;   break;
        }

        String sql = (column != null)
                ? "INSERT INTO " + table + " (" + column + ", message) VALUES (?,?)"
                : "INSERT INTO " + table + " (message) VALUES (?)";

        try (Connection c = dbconnection.getConnection();
             PreparedStatement pst = c.prepareStatement(sql)) {
            if (column != null) {
                pst.setString(1, id);
                pst.setString(2, message);
            } else pst.setString(1, message);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Notification insert failed: " + e.getMessage());
        }
    }

    // -------------------- Update without password --------------------

    public static boolean updateUserWithoutPassword(String id, String role,
                                                    String name, String email, String phone, String batch) {

        if (!validation(id, role, name, email, phone)) return false;

        // undergraduate batch validation
        if (role.equalsIgnoreCase("undergraduate")) {
            if (batch == null || batch.isBlank()) {
                JOptionPane.showMessageDialog(null, "Batch number is required for undergraduate users!");
                return false;
            }
            try {
                int intBatch = Integer.parseInt(batch);
                if (intBatch < 1 || intBatch > 8) {
                    JOptionPane.showMessageDialog(null, "Batch must be between 1 and 8!");
                    return false;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Batch must be numeric!");
                return false;
            }
        }

        String userSql = "UPDATE user SET user_name=?, user_email=?, user_phone=?, user_batch=? WHERE user_id=?";

        try (Connection con = dbconnection.getConnection()) {
            con.setAutoCommit(false);

            // --- update main user table ---
            try (PreparedStatement pst = con.prepareStatement(userSql)) {
                pst.setString(1, name);
                pst.setString(2, email);
                pst.setString(3, phone);
                pst.setString(4, (batch != null && !batch.isBlank()) ? batch : null);
                pst.setString(5, id);
                pst.executeUpdate();
            }

            // --- update linked table ---
            if (role.equalsIgnoreCase("undergraduate")) {
                try (PreparedStatement ug = con.prepareStatement(
                        "UPDATE undergraduate SET ug_batch=? WHERE ug_id=?")) {
                    ug.setString(1, batch);
                    ug.setString(2, id);
                    ug.executeUpdate();
                }
            } else if (role.equalsIgnoreCase("lecturer")) {
                try (PreparedStatement lecpst = con.prepareStatement(
                        "UPDATE lecturer SET lec_id=? WHERE lec_id=?")) {
                    // refresh same ID to keep lecturer synced if table exists
                    lecpst.setString(1, id);
                    lecpst.setString(2, id);
                    lecpst.executeUpdate();
                }
            }

            con.commit();

            // --- Notification about update ---
            String msg = "Your account details have been updated by the Administrator.";
            insertNotification(role, id, msg);

            return true;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error updating user: " + e.getMessage());
            return false;
        }
    }

    // -------------------- Update with password --------------------

    public static boolean updateUserWithPassword(String id, char[] pass, String role,
                                                 String name, String email, String phone, String batch) {

        if (!validation(id, role, name, email, phone)) return false;

        if (role.equalsIgnoreCase("undergraduate")) {
            if (batch == null || batch.isBlank()) {
                JOptionPane.showMessageDialog(null, "Batch number is required for undergraduate users!");
                return false;
            }
            try {
                int intBatch = Integer.parseInt(batch);
                if (intBatch < 1 || intBatch > 8) {
                    JOptionPane.showMessageDialog(null, "Batch must be between 1 and 8!");
                    return false;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Batch must be numeric!");
                return false;
            }
        }

        String userSql = "UPDATE user SET user_name=?, user_password=?, user_email=?, " +
                "user_phone=?, user_batch=? WHERE user_id=?";

        try (Connection con = dbconnection.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement pst = con.prepareStatement(userSql)) {

                // 🔐 Hash the password before saving
                String hashedPassword = Admin.encryption.hashpassword(new String(pass));

                pst.setString(1, name);
                pst.setString(2, hashedPassword);
                pst.setString(3, email);
                pst.setString(4, phone);
                pst.setString(5, (batch != null && !batch.isBlank()) ? batch : null);
                pst.setString(6, id);
                pst.executeUpdate();
            }

            // --- update linked undergraduate / lecturer tables ---
            if (role.equalsIgnoreCase("undergraduate")) {
                try (PreparedStatement ug = con.prepareStatement(
                        "UPDATE undergraduate SET ug_batch=? WHERE ug_id=?")) {
                    ug.setString(1, batch);
                    ug.setString(2, id);
                    ug.executeUpdate();
                }
            } else if (role.equalsIgnoreCase("lecturer")) {
                try (PreparedStatement lecpst = con.prepareStatement(
                        "UPDATE lecturer SET lec_id=? WHERE lec_id=?")) {
                    lecpst.setString(1, id);
                    lecpst.setString(2, id);
                    lecpst.executeUpdate();
                }
            }

            con.commit();

            // --- Notification & Email ---
            String msg = "Your account details have been updated by the Administrator.";
            insertNotification(role, id, msg);

            // Send email displaying TG number and plaintext password (not hashed)
            if (pass != null && pass.length > 0) {
                Utils.EmailSender.sendRegistrationEmail(email, id, new String(pass));
            }

            return true;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error updating user: " + e.getMessage());
            return false;
        }
    }
}