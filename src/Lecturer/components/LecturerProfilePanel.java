package Lecturer.components;

import Database.dbconnection;
import Utils.ThemeManager;
import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.sql.*;
import Utils.CircularImagePanel;
/**
 * Lecturer profile panel styled like Undergraduate profile panel.
 *  - Uses FlatLaf via ThemeManager
 *  - Edit phone number, change password and profile picture
 */
public class LecturerProfilePanel extends JPanel {

    private final String userId;
    private String name, email, phone, hashedPassword, profilePicPath;

    private JLabel lblPhone;
    private JButton btnChangePic, btnChangePhone, btnChangePass;

    public LecturerProfilePanel(String userId) {
        this.userId = userId;
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));
        loadUserDetails();
        initUI();
    }

    // ------------------------------------------------------------------------
    //  Load user data
    // ------------------------------------------------------------------------
    private void loadUserDetails() {
        try (Connection con = dbconnection.getConnection()) {
            String sql = "SELECT user_name,user_email,user_phone,user_password,user_pro_pic FROM user WHERE user_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                name = rs.getString("user_name");
                email = rs.getString("user_email");
                phone = rs.getString("user_phone");
                hashedPassword = rs.getString("user_password");
                profilePicPath = rs.getString("user_pro_pic");
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading user: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------------
    //  UI setup
    // ------------------------------------------------------------------------
    private void initUI() {
        // Header
        JLabel header = new JLabel("My Profile", SwingConstants.CENTER);
        header.setFont(new Font("Segoe UI Black", Font.BOLD, 26));
        header.setForeground(Color.WHITE);
        header.setOpaque(true);
        header.setBackground(new Color(153, 187, 187));
        header.setBorder(BorderFactory.createEmptyBorder(15, 0, 15, 0));
        add(header, BorderLayout.NORTH);

        // Main content
        JPanel content = new JPanel(null);
        content.setOpaque(false);
        add(content, BorderLayout.CENTER);

        if (profilePicPath == null || profilePicPath.isBlank()) {
            profilePicPath = "/Images/lecturer.png";
        }

        // Profile photo area
        CircularImagePanel photo = new CircularImagePanel(profilePicPath, 160);
        photo.setBounds(550, 70, 160, 160);
        content.add(photo);

        btnChangePic = new JButton("Change Picture");
        ThemeManager.stylePrimaryButton(btnChangePic);
        btnChangePic.setBounds(550, 250, 160, 35);
        content.add(btnChangePic);

        // Info card
        JPanel card = new JPanel(null);
        card.setBackground(new Color(245, 247, 255));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 220), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));
        card.setBounds(80, 70, 380, 360);
        content.add(card);

        Font labelFont = new Font("Segoe UI Semibold", Font.BOLD, 15);
        Font valueFont = new Font("Segoe UI", Font.PLAIN, 15);
        int y = 20, gap = 60;

        card.add(labelPair("Name:", name, y, labelFont, valueFont));
        card.add(labelPair("Lecturer ID:", userId, y += gap, labelFont, valueFont));
        card.add(labelPair("Email:", email, y += gap, labelFont, valueFont));

        // Phone
        JLabel lblPhoneTitle = new JLabel("Phone:");
        lblPhoneTitle.setFont(labelFont);
        lblPhoneTitle.setBounds(550, 360, 70, 25);
        content.add(lblPhoneTitle);

        lblPhone = new JLabel(phone);
        lblPhone.setFont(valueFont);
        lblPhone.setBounds(600, 360, 120, 25);
        lblPhone.setForeground(Color.DARK_GRAY);
        content.add(lblPhone);

        btnChangePhone = new JButton("Edit Phone Number");
        ThemeManager.stylePrimaryButton(btnChangePhone);
        btnChangePhone.setBounds(550, 400, 160, 35);
        content.add(btnChangePhone);

        // Change password button
        btnChangePass = new JButton("Change Password");
        ThemeManager.stylePrimaryButton(btnChangePass);
        btnChangePass.setBounds(550, 450, 160, 35);
        content.add(btnChangePass);

        // Actions
        btnChangePic.addActionListener(e -> handleChangePicture());
        btnChangePhone.addActionListener(e -> handlePhoneEdit());
        btnChangePass.addActionListener(e -> handlePasswordChange());
    }

    private JPanel labelPair(String label, String value, int y, Font labelFont, Font valueFont) {
        JPanel p = new JPanel(null);
        p.setBounds(10, y, 350, 40);
        p.setOpaque(false);

        JLabel l = new JLabel(label);
        l.setFont(labelFont);
        l.setBounds(10, 0, 120, 25);
        p.add(l);

        JLabel v = new JLabel(value);
        v.setFont(valueFont);
        v.setForeground(Color.DARK_GRAY);
        v.setBounds(130, 0, 210, 25);
        p.add(v);
        return p;
    }

    // ------------------------------------------------------------------------
    //  Picture, phone, password logic
    // ------------------------------------------------------------------------
    private void handleChangePicture() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Profile Picture");
        chooser.setFileFilter(
                new javax.swing.filechooser.FileNameExtensionFilter("Image Files", "jpg", "jpeg", "png"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File selected = chooser.getSelectedFile();
            updateProfilePic(selected.getAbsolutePath());
            JOptionPane.showMessageDialog(this,
                    "Profile picture updated! Reopen this page to refresh.");
        }
    }

    private void updateProfilePic(String path) {
        try (Connection con = dbconnection.getConnection()) {
            String sql = "UPDATE user SET user_pro_pic=? WHERE user_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, path);
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating picture: " + e.getMessage());
        }
    }

    private void handlePhoneEdit() {
        String newPhone = JOptionPane.showInputDialog(this,
                "Enter new phone number:", phone);
        if (newPhone == null) return;
        if (!newPhone.matches("\\d{10}")) {
            JOptionPane.showMessageDialog(this,
                    "Invalid phone. Enter a 10-digit number.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try (Connection con = dbconnection.getConnection()) {
            String sql = "UPDATE user SET user_phone=? WHERE user_id=?";
            PreparedStatement ps = con.prepareStatement(sql);
            ps.setString(1, newPhone);
            ps.setString(2, userId);
            ps.executeUpdate();
            lblPhone.setText(newPhone);
            JOptionPane.showMessageDialog(this, "Phone updated!");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating phone: " + e.getMessage());
        }
    }

    private void handlePasswordChange() {
        JPanel panel = new JPanel(new GridLayout(3, 2, 5, 5));
        JPasswordField current = new JPasswordField();
        JPasswordField newPass = new JPasswordField();

        panel.add(new JLabel("Current Password:"));
        panel.add(current);
        panel.add(new JLabel("New Password:"));
        panel.add(newPass);

        int res = JOptionPane.showConfirmDialog(this, panel, "Change Password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res != JOptionPane.OK_OPTION) return;

        String currentPwd = new String(current.getPassword());
        String newPwd = new String(newPass.getPassword());

        // validation
        if (currentPwd.isBlank() || newPwd.isBlank()) {
            JOptionPane.showMessageDialog(this, "Fields cannot be empty."); return;
        }
        if (newPwd.length() > 8 || !newPwd.matches("[A-Za-z0-9]+")) {
            JOptionPane.showMessageDialog(this,
                    "New password must be letters/numbers only and up to 8 characters.");
            return;
        }

        try (Connection con = dbconnection.getConnection()) {
            // verify current password
            String hashCur = Admin.encryption.hashpassword(currentPwd);
            PreparedStatement chk = con.prepareStatement("SELECT user_password FROM user WHERE user_id=?");
            chk.setString(1, userId);
            ResultSet rs = chk.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("user_password");
                if (!storedHash.equals(hashCur)) {
                    JOptionPane.showMessageDialog(this, "Current password is incorrect."); return;
                }
            }

            // update new password
            String hashNew = Admin.encryption.hashpassword(newPwd);
            PreparedStatement up = con.prepareStatement(
                    "UPDATE user SET user_password=? WHERE user_id=?");
            up.setString(1, hashNew);
            up.setString(2, userId);
            up.executeUpdate();

            JOptionPane.showMessageDialog(this, "Password changed successfully!");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error changing password: " + e.getMessage());
        }
    }
}