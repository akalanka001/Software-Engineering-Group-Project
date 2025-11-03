package Admin;

import Database.dbconnection;
import javax.swing.JOptionPane;
import java.sql.*;

public class addUser {

    // ---------------------------------------------------------------------
    //  Validation helpers
    // ---------------------------------------------------------------------
    private static boolean isValidUserId(String id) {
        // 2 letters + 4 digits, total length = 6
        return id.matches("^[A-Za-z]{2}\\d{4}$");
    }

    private static boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }

    private static boolean isValidPhone(String phone) {
        return phone.matches("^0\\d{9}$"); // starts with 0, 10 digits total
    }

    // ---------------------------------------------------------------------
    public static boolean validation(String id, char[] pass, String role,
                                     String name, String email, String phone) {

        if (id.isBlank() || pass.length == 0 || role.isBlank()
                || name.isBlank() || email.isBlank() || phone.isBlank()) {
            JOptionPane.showMessageDialog(null, "All fields are required!");
            return false;
        }

        if (!isValidUserId(id)) {
            JOptionPane.showMessageDialog(null,
                    "User ID must be exactly 6 characters: first two letters followed by four digits (e.g., TG1234).");
            return false;
        }

        if (!isValidEmail(email)) {
            JOptionPane.showMessageDialog(null, "Invalid email address format!");
            return false;
        }

        if (!isValidPhone(phone)) {
            JOptionPane.showMessageDialog(null,
                    "Phone number must start with 0 and contain exactly 10 digits.");
            return false;
        }

        return true;
    }

    // ---------------------------------------------------------------------
    public static boolean usernameExists(String id) {
        String sql = "SELECT 1 FROM user WHERE user_id=?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, id);
            ResultSet rs = pst.executeQuery();
            return rs.next();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error checking username: " + e.getMessage());
            return false;
        }
    }

    // ---------------------------------------------------------------------
    public static boolean addUser(String id, String name, char[] pass,
                                  String role, String email, String phone, String batch) {

        if (!validation(id, pass, role, name, email, phone)) return false;

        // Extra batch validation for undergraduates
        if (role.equalsIgnoreCase("undergraduate")) {
            if (batch == null || batch.isBlank()) {
                JOptionPane.showMessageDialog(null, "Batch number is required for undergraduate users!");
                return false;
            }
            try {
                int intBatch = Integer.parseInt(batch);
                if (intBatch < 1 || intBatch >= 9) {
                    JOptionPane.showMessageDialog(null,
                            "Batch value must be between 1 and 8 for undergraduates!");
                    return false;
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Batch value must be a valid number!");
                return false;
            }
        }

        String userSql = "INSERT INTO user (user_id, user_name, user_password, user_role, user_email, user_phone, user_batch) VALUES (?,?,?,?,?,?,?)";

        try (Connection con = dbconnection.getConnection()) {
            con.setAutoCommit(false);

            try (PreparedStatement pst = con.prepareStatement(userSql)) {
                pst.setString(1, id);
                pst.setString(2, name);
                pst.setString(3, new String(pass));
                pst.setString(4, role);
                pst.setString(5, email);
                pst.setString(6, phone);
                pst.setString(7, (batch != null && !batch.isBlank()) ? batch : null);
                pst.executeUpdate();
            }

            switch (role.toLowerCase()) {
                case "undergraduate" -> {
                    try (PreparedStatement ug =
                                 con.prepareStatement("INSERT INTO undergraduate (ug_id, ug_batch) VALUES (?, ?)")) {
                        ug.setString(1, id);
                        ug.setString(2, batch);
                        ug.executeUpdate();
                    }
                }
                case "lecturer" -> {
                    try (PreparedStatement lec =
                                 con.prepareStatement("INSERT INTO lecturer (lec_id) VALUES (?)")) {
                        lec.setString(1, id);
                        lec.executeUpdate();
                    }
                }
                case "tech_officer" -> {
                    try (PreparedStatement tech =
                                 con.prepareStatement("INSERT INTO tech_officer (to_id, to_department) VALUES (?, ?)")) {
                        tech.setString(1, id);
                        tech.setString(2, "ICT"); // default dept
                        tech.executeUpdate();
                    }
                }
                case "admin" -> {
                    try (PreparedStatement adm =
                                 con.prepareStatement("INSERT INTO admin (admin_id) VALUES (?)")) {
                        adm.setString(1, id);
                        adm.executeUpdate();
                    }
                }
            }

            con.commit();
            return true;

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error adding user: " + e.getMessage());
            return false;
        }
    }
}