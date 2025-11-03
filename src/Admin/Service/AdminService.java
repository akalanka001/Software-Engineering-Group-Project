package Admin.Service;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

import Admin.Alerts;
import Database.ActivityLogger;
import Database.dbconnection;

/**
 * Central place for GUI-driven CRUD operations.
 * UserTab, CourseTab, etc., call these static methods.
 */
public class AdminService {

    private String  userId;

    // ------------ USER CRUD DIALOGS ----------------

    /** Add a new user */
    public static void addUserDialog(Component parent, JTable table, String adminId)
    {
        JTextField id = new JTextField();
        JTextField name = new JTextField();
        JPasswordField pass = new JPasswordField();
        JComboBox<String> role = new JComboBox<>(new String[]{
                "Admin","Lecturer"});
        JTextField email = new JTextField();
        JTextField phone = new JTextField();

        // no batch field anymore, so no enabling logic required

        JPanel panel = new JPanel(new GridLayout(6, 2, 6, 6));
        panel.add(new JLabel("User ID:")); panel.add(id);
        panel.add(new JLabel("Name:")); panel.add(name);
        panel.add(new JLabel("Password:")); panel.add(pass);
        panel.add(new JLabel("Role:")); panel.add(role);
        panel.add(new JLabel("Email:")); panel.add(email);
        panel.add(new JLabel("Phone:")); panel.add(phone);

        boolean valid = false;
        while (!valid) {
            int response = JOptionPane.showConfirmDialog(
                    parent, panel, "Add User",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (response != JOptionPane.OK_OPTION) return;

            try {
                String uid = id.getText().trim();
                String uname = name.getText().trim();
                char[] pwd = pass.getPassword();
                String urole = role.getSelectedItem().toString().toLowerCase();
                String uemail = email.getText().trim();
                String uphone = phone.getText().trim();

                // validate
                if (!Admin.addUser.validation(uid, pwd, urole, uname, uemail, uphone)) {
                    id.requestFocus();
                    continue;
                }

                if (Admin.addUser.usernameExists(uid)) {
                    JOptionPane.showMessageDialog(parent, "User ID already exists!");
                    id.setText("");
                    id.requestFocus();
                    continue;
                }

                boolean added = Admin.addUser.addUser(uid, uname, pwd, urole, uemail, uphone, "");
                if (added) {
                    Alerts.success("User added successfully!");
                    ActivityLogger.log("AD0001", uid, "Added new user");// 👈 Add this line
                    ((DefaultTableModel) table.getModel()).setRowCount(0);
                    valid = true;
                }
                else {
                    JOptionPane.showMessageDialog(parent, "Failed to add user!");
                }

            } catch (Exception e) {
                JOptionPane.showMessageDialog(parent, e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static void updateUserDialog(Component parent, JTable table) {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) {
            Alerts.fail("Please select a user from the table first!");
            return;
        }

        String selectedUserId = table.getValueAt(selectedRow, 0).toString();

        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement("SELECT * FROM user WHERE user_id=?")) {

            pst.setString(1, selectedUserId);
            ResultSet rs = pst.executeQuery();

            if (!rs.next()) {
                Alerts.fail("User not found!");
                return;
            }

            String currentEmail = rs.getString("user_email");
            String currentRole  = rs.getString("user_role");
            String currentBatch = rs.getString("user_batch");

            JTextField id    = new JTextField(rs.getString("user_id"));
            JTextField name  = new JTextField(rs.getString("user_name"));
            JPasswordField pass = new JPasswordField();
            JTextField email = new JTextField(currentEmail);
            JTextField phone = new JTextField(rs.getString("user_phone"));
            JTextField batch = new JTextField(currentBatch != null ? currentBatch : "");

            id.setEditable(false);

            // dynamic panel layout size
            JPanel panel;
            if (currentRole.equalsIgnoreCase("undergraduate")) {
                panel = new JPanel(new GridLayout(6, 2, 6, 6));
                panel.add(new JLabel("User ID:"));  panel.add(id);
                panel.add(new JLabel("Name:"));     panel.add(name);
                panel.add(new JLabel("Password (optional):")); panel.add(pass);
                panel.add(new JLabel("Email:"));    panel.add(email);
                panel.add(new JLabel("Phone:"));    panel.add(phone);
                panel.add(new JLabel("Batch (1‑9):"));  panel.add(batch);
            } else {
                panel = new JPanel(new GridLayout(5, 2, 6, 6));
                panel.add(new JLabel("User ID:"));  panel.add(id);
                panel.add(new JLabel("Name:"));     panel.add(name);
                panel.add(new JLabel("Password (optional):")); panel.add(pass);
                panel.add(new JLabel("Email:"));    panel.add(email);
                panel.add(new JLabel("Phone:"));    panel.add(phone);
            }

            boolean done = false;
            while (!done) {
                int result = JOptionPane.showConfirmDialog(
                        parent, panel, "Update User",
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

                if (result != JOptionPane.OK_OPTION) return;

                String uid      = id.getText().trim();
                String uname    = name.getText().trim();
                char[] pwd      = pass.getPassword();
                String uemail   = email.getText().trim();
                String uphone   = phone.getText().trim();
                String ubatch   = batch.getText().trim();

                // basic validation
                if (!Admin.updateUser.validation(uid, currentRole, uname, uemail, uphone))
                    continue;

                // --- batch validation only for undergraduates ---
                if (currentRole.equalsIgnoreCase("undergraduate")) {
                    if (ubatch.isBlank()) {
                        JOptionPane.showMessageDialog(parent, "Batch is required for undergraduate users!");
                        continue;
                    }
                    try {
                        int intBatch = Integer.parseInt(ubatch);
                        if (intBatch < 1 || intBatch > 9) {
                            JOptionPane.showMessageDialog(parent, "Batch must be between 1 and 8!");
                            continue;
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(parent, "Batch must be a numeric value!");
                        continue;
                    }
                }

                // warn if email changed
                if (!uemail.equalsIgnoreCase(currentEmail)) {
                    int warn = JOptionPane.showConfirmDialog(
                            parent,
                            "⚠️ You are changing the user's email.\n" +
                                    "We use this address for account communication.\n\n" +
                                    "Are you sure the new email is correct?",
                            "Confirm Email Change",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.WARNING_MESSAGE);
                    if (warn != JOptionPane.YES_OPTION) continue;
                }

                boolean ok;
                if (pwd.length > 0) {
                    ok = Admin.updateUser.updateUserWithPassword(uid, pwd, currentRole, uname, uemail, uphone,
                            currentRole.equalsIgnoreCase("undergraduate") ? ubatch : "");
                } else {
                    ok = Admin.updateUser.updateUserWithoutPassword(uid, currentRole, uname, uemail, uphone,
                            currentRole.equalsIgnoreCase("undergraduate") ? ubatch : "");
                }

                if (ok) {
                    Alerts.success("User updated successfully!");
                    Database.ActivityLogger.log("AD0001", uid, "Updated user");
                    ((DefaultTableModel) table.getModel()).setRowCount(0);
                    done = true;
                }
                else {
                    JOptionPane.showMessageDialog(parent, "Update failed!");
                }
            }

        } catch (SQLException e) {
            Alerts.fail("Error loading user data: " + e.getMessage());
        }
    }

    /** Delete user (unchanged) */
    /** Delete user (now confirms cascade warning) */
    public static void deleteUserDialog(Component parent, JTable table) {
        JTextField id = new JTextField();
        JPanel panel = new JPanel(new GridLayout(1, 2, 5, 5));
        panel.add(new JLabel("User ID:"));
        panel.add(id);

        if (JOptionPane.showConfirmDialog(parent, panel, "Delete User",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {

            try {
                String uid = id.getText().trim();
                if (!Admin.deleteUser.validation(uid)) return;
                if (!Admin.deleteUser.usernameExists(uid)) {
                    Alerts.fail("User not found.");
                    return;
                }

                // --- check lecturer reference ---
                // (Only run this if the user is a lecturer)
                if (Admin.deleteUser.isLecturer(uid)) {
                    if (Admin.deleteUser.isReferenced(uid)) {
                        Alerts.fail("Cannot delete this lecturer because they are referenced by courses or notifications.");
                        return; // stop deletion
                    }
                }

                // --- new confirmation message ---
                int confirm = JOptionPane.showConfirmDialog(
                        parent,
                        "⚠️ Warning!\n\nAll data related to this user will also be permanently deleted.\n" +
                                "This includes courses, enrollments, notifications, and any linked records.\n\n" +
                                "Are you absolutely sure you want to continue?",
                        "Confirm Deletion",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (confirm != JOptionPane.YES_OPTION) {
                    Alerts.fail("Deletion canceled.");
                    return;
                }

                // Proceed only if confirmed
                if (Admin.deleteUser.deleteUser(uid)) {
                    Alerts.success("User and all related data removed successfully.");
                    Database.ActivityLogger.log("AD0001", uid, "Deleted user");
                    ((DefaultTableModel) table.getModel()).setRowCount(0);
                } else {
                    Alerts.fail("Delete failed.");
                }

            } catch (Exception e) {
                Alerts.fail("Error deleting user: " + e.getMessage());
            }
        }
    }


    private static String capitalize(String txt) {
        if (txt == null || txt.isBlank()) return "";
        return txt.substring(0, 1).toUpperCase() + txt.substring(1).toLowerCase();
    }

    // ------------ COURSE CRUD DIALOGS ----------------

    /** Dialog for adding a course (now includes fee) */
    // ---------- COURSE CRUD DIALOGS ----------

    /** Dialog for adding a course (includes fee and creates activity log) */
    public static void addCourseDialog(Component parent) {
        JTextField idField    = new JTextField();
        JTextField nameField  = new JTextField();
        JTextField lecField   = new JTextField();
        JTextField creditField= new JTextField();
        JComboBox<String> typeBox =
                new JComboBox<>(new String[]{"Theory","Practical","Both"});
        JTextField feeField   = new JTextField();

        JPanel form = new JPanel(new GridLayout(6, 2, 6, 6));
        form.add(new JLabel("Course ID:"));     form.add(idField);
        form.add(new JLabel("Course Name:"));   form.add(nameField);
        form.add(new JLabel("Lecturer ID:"));   form.add(lecField);
        form.add(new JLabel("Credit:"));        form.add(creditField);
        form.add(new JLabel("Type:"));          form.add(typeBox);
        form.add(new JLabel("Course Fee:"));    form.add(feeField);

        int result = JOptionPane.showConfirmDialog(
                parent, form, "Add Course", JOptionPane.OK_CANCEL_OPTION);

        if (result == JOptionPane.OK_OPTION) {
            String id       = idField.getText().trim();
            String name     = nameField.getText().trim();
            String lec      = lecField.getText().trim();
            String creditTxt= creditField.getText().trim();
            String type     = typeBox.getSelectedItem().toString().toLowerCase();
            String feeTxt   = feeField.getText().trim();

            try {
                if (!CourseService.validate(id, name, lec, creditTxt, type, feeTxt)) return;
                int credit = Integer.parseInt(creditTxt);
                double fee = Double.parseDouble(feeTxt);

                if (CourseService.add(id, name, lec, credit, type, fee)) {
                    Alerts.success("Course added successfully!");
                } else {
                    Alerts.fail("Add course failed!");
                }
            } catch (Exception e) {
                Alerts.fail("Error: " + e.getMessage());
            }
        }
    }
}