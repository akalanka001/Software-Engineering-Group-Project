package Admin;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import Database.dbconnection;

/**
 * Modal dialog that lets the admin edit their profile
 * (name, email, phone). Works exactly as before.
 */
public class editprofiledailog extends JDialog {

    private final String username;
    private final JTextField nameField = new JTextField(15);
    private final JTextField emailField = new JTextField(15);
    private final JTextField phoneField = new JTextField(10);
    private final JButton saveBtn = new JButton("Save");
    private final JButton cancelBtn = new JButton("Cancel");

    public editprofiledailog(Frame owner, boolean modal, String username) {
        super(owner, "Edit Profile", modal);
        this.username = username;
        buildUI();
        loadCurrentValues();
    }

    private void buildUI() {
        JPanel form = new JPanel(new GridLayout(4, 2, 8, 8));
        form.add(new JLabel("Name:"));  form.add(nameField);
        form.add(new JLabel("Email:")); form.add(emailField);
        form.add(new JLabel("Phone:")); form.add(phoneField);

        saveBtn.addActionListener(e -> saveChanges());
        cancelBtn.addActionListener(e -> dispose());

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(saveBtn);
        buttonPanel.add(cancelBtn);

        getContentPane().setLayout(new BorderLayout(10, 10));
        add(form, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(getOwner());
    }

    private void loadCurrentValues() {
        String sql = "SELECT user_name, user_email, user_phone FROM user WHERE user_id=?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, username);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    nameField.setText(rs.getString("user_name"));
                    emailField.setText(rs.getString("user_email"));
                    phoneField.setText(rs.getString("user_phone"));
                }
            }
        } catch (Exception e) {
            Alerts.fail(e.getMessage());
        }
    }

    private void saveChanges() {
        String name = nameField.getText().trim();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        if (name.isEmpty() || email.isEmpty() || phone.isEmpty()) {
            Alerts.fail("All fields are required!");
            return;
        }

        String sql = "UPDATE user SET user_name=?, user_email=?, user_phone=? WHERE user_id=?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, name);
            pst.setString(2, email);
            pst.setString(3, phone);
            pst.setString(4, username);
            pst.executeUpdate();
            Alerts.success("Profile updated successfully!");
            dispose();
        } catch (Exception e) {
            Alerts.fail(e.getMessage());
        }
    }
}