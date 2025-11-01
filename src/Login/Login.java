package Login;

import Admin.ui.AdminFrame;
import Lecturer.LecturerFrame;
import Database.dbconnection;
import Undergraduate.Undergraduate;
import Utils.ThemeManager;
import Utils.EmailSender;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.util.Random;

/**
 * Login window styled according to ThemeManager.
 * Includes "Forgot Password?" flow with email reset.
 */
public class Login extends javax.swing.JFrame {

    public Login() {
        try {
            ThemeManager.initialize(); // ensure theme loads before building
            initComponents();
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    // ------------------------------------------------------------------
    // UI Initialisation
    // ------------------------------------------------------------------
    @SuppressWarnings("unchecked")
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        cancel = new javax.swing.JButton();
        login = new javax.swing.JButton();
        username = new javax.swing.JTextField();
        password = new javax.swing.JPasswordField();
        register = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        forgotPassword = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Login");
        setResizable(false);

        jPanel1.setBackground(new Color(250, 250, 250));

        jLabel1.setFont(new java.awt.Font("Segoe UI Black", Font.BOLD, 22));
        jLabel1.setForeground(ThemeManager.LIGHT_BLUE);
        jLabel1.setHorizontalAlignment(SwingConstants.CENTER);
        jLabel1.setText("LOGIN");

        jLabel2.setFont(new java.awt.Font("Segoe UI", Font.BOLD, 14));
        jLabel2.setText("USERNAME :");

        jLabel3.setFont(new java.awt.Font("Segoe UI", Font.BOLD, 14));
        jLabel3.setText("PASSWORD :");

        // --- Apply unified ThemeManager button styling ---
        ThemeManager.stylePrimaryButton(login);
        login.setText("LOGIN");
        login.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        login.addActionListener(evt -> loginActionPerformed(evt));

        ThemeManager.stylePrimaryButton(register);
        register.setText("REGISTER");
        register.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        register.addActionListener(evt -> registerActionPerformed(evt));

        // Cancel button: inverted style for contrast
        ThemeManager.stylePrimaryButton(cancel);
        cancel.setBackground(new Color(220, 220, 220));
        cancel.setForeground(Color.DARK_GRAY);
        cancel.setText("CANCEL");
        cancel.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        cancel.addActionListener(evt -> cancelActionPerformed(evt));

        username.setFont(new java.awt.Font("Segoe UI", Font.PLAIN, 14));
        password.setFont(new java.awt.Font("Segoe UI", Font.PLAIN, 14));

        // ---------- Forgot Password link ----------
        forgotPassword.setText("<HTML><U>Forgot Password?</U></HTML>");
        forgotPassword.setForeground(new java.awt.Color(100, 149, 237)); // cornflower blue
        forgotPassword.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        forgotPassword.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        forgotPassword.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                forgotPasswordActionPerformed(evt);
            }
            @Override
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                forgotPassword.setForeground(ThemeManager.LIGHT_BLUE.darker());
            }
            @Override
            public void mouseExited(java.awt.event.MouseEvent evt) {
                forgotPassword.setForeground(new java.awt.Color(100, 149, 237));
            }
        });

        // Layout for left panel (form)
        GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(60)
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel2, GroupLayout.PREFERRED_SIZE, 95, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jLabel3, GroupLayout.PREFERRED_SIZE, 95, GroupLayout.PREFERRED_SIZE))
                                .addGap(30)
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING, false)
                                        .addComponent(username)
                                        .addComponent(password, GroupLayout.PREFERRED_SIZE, 180, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(forgotPassword))
                                .addContainerGap(45, Short.MAX_VALUE))
                        .addGroup(GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel1, GroupLayout.PREFERRED_SIZE, 130, GroupLayout.PREFERRED_SIZE)
                                .addGap(130))
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(40)
                                .addComponent(login, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                                .addGap(20)
                                .addComponent(register, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE)
                                .addGap(20)
                                .addComponent(cancel, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(40, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(50)
                                .addComponent(jLabel1)
                                .addGap(40)
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel2)
                                        .addComponent(username, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE))
                                .addGap(20)
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(jLabel3)
                                        .addComponent(password, GroupLayout.PREFERRED_SIZE, 30, GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(forgotPassword)
                                .addGap(35)
                                .addGroup(jPanel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                        .addComponent(login, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(register, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE)
                                        .addComponent(cancel, GroupLayout.PREFERRED_SIZE, 35, GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(50, Short.MAX_VALUE))
        );

        jPanel2.setBackground(new Color(128, 170, 170)); // calm teal-green shade

        jLabel4.setHorizontalAlignment(SwingConstants.CENTER);
        ImageIcon originalIcon = new ImageIcon(getClass().getResource("/images/lms.png"));
        Image scaledImage = originalIcon.getImage().getScaledInstance(180, 180, Image.SCALE_SMOOTH);
        jLabel4.setIcon(new ImageIcon(scaledImage));

        GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel4, GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(70)
                                .addComponent(jLabel4)
                                .addGap(70))
        );

        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createSequentialGroup()
                        .addComponent(jPanel2, GroupLayout.PREFERRED_SIZE, 320, GroupLayout.PREFERRED_SIZE)
                        .addComponent(jPanel1, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
        layout.setVerticalGroup(
                layout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(jPanel2, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel1, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
        setLocationRelativeTo(null);
    }

    // ------------------------------------------------------------------
    // Button actions and Authentication logic
    // ------------------------------------------------------------------

    private void cancelActionPerformed(java.awt.event.ActionEvent evt) {
        dispose();
    }

    private void loginActionPerformed(java.awt.event.ActionEvent evt) {
        String username_new = username.getText();
        String password_new = new String(password.getPassword());
        String loginStatus = authenticate(username_new, password_new);

        switch (loginStatus) {
            case "USER_NOT_FOUND" -> JOptionPane.showMessageDialog(this, "Please register first.");
            case "INVALID_PASSWORD" -> JOptionPane.showMessageDialog(this, "Invalid username or password.");
            case "DB_ERROR" -> JOptionPane.showMessageDialog(this, "Database error. Please contact admin.");
            case "admin", "lecturer", "undergraduate", "tech_officer" -> {
                dispose();
                openRoleWindow(loginStatus);
            }
            default -> JOptionPane.showMessageDialog(this, "An unknown error occurred.");
        }
    }

    private String authenticate(String username, String password) {
        String query = "SELECT user_password, user_role FROM user WHERE user_id = ?";
        try (Connection conn = dbconnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, username);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                String storedPasswordHash = rs.getString("user_password");
                String userRole = rs.getString("user_role");
                String providedPasswordHash = Admin.encryption.hashpassword(password);
                if (providedPasswordHash.equals(storedPasswordHash))
                    return userRole;
                else return "INVALID_PASSWORD";
            } else return "USER_NOT_FOUND";
        } catch (Exception e) {
            e.printStackTrace();
            return "DB_ERROR";
        }
    }

    private void registerActionPerformed(java.awt.event.ActionEvent evt) {
        new Register.RegisterForm().setVisible(true);
        dispose();
    }

    private void openRoleWindow(String role) {
        if (role == null) {
            JOptionPane.showMessageDialog(this, "Role missing.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }
        String r = role.trim().toLowerCase();
        switch (r) {
            case "admin" -> new AdminFrame(username.getText().toLowerCase()).setVisible(true);
            case "lecturer" -> new LecturerFrame(username.getText().toLowerCase()).setVisible(true);
            case "undergraduate" -> new Undergraduate(username.getText().toLowerCase()).setVisible(true);
            default -> JOptionPane.showMessageDialog(this, "Unknown role: " + role);
        }
    }

    // ------------------------------------------------------------------
    // Forgot Password Feature
    // ------------------------------------------------------------------
    private void forgotPasswordActionPerformed(java.awt.event.MouseEvent evt) {
        String email = JOptionPane.showInputDialog(this,
                "Enter your registered email:",
                "Forgot Password",
                JOptionPane.QUESTION_MESSAGE);

        if (email == null || email.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Email cannot be empty.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String query = "SELECT user_id FROM user WHERE user_email = ?";
        try (Connection conn = dbconnection.getConnection();
             PreparedStatement pst = conn.prepareStatement(query)) {

            pst.setString(1, email.trim());
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                String userId = rs.getString("user_id");
                String newPassword = generateRandomPassword(8);
                String newHash = Admin.encryption.hashpassword(newPassword);

                // update DB with the new password
                String updateQuery = "UPDATE user SET user_password = ? WHERE user_email = ?";
                try (PreparedStatement updatePst = conn.prepareStatement(updateQuery)) {
                    updatePst.setString(1, newHash);
                    updatePst.setString(2, email);
                    updatePst.executeUpdate();
                }

                // send email with new credentials
                boolean emailSent = EmailSender.sendRegistrationEmail(email, userId, newPassword);

                if (emailSent) {
                    JOptionPane.showMessageDialog(this,
                            "A new password has been sent to your email.",
                            "Password Reset Successful",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Failed to send email. Please contact admin.",
                            "Email Error",
                            JOptionPane.ERROR_MESSAGE);
                }

            } else {
                JOptionPane.showMessageDialog(this,
                        "No account found with that email.",
                        "Not Found",
                        JOptionPane.WARNING_MESSAGE);
            }

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "An error occurred. Please try again later.",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Public helper
    // ------------------------------------------------------------------
    public void prefillUserId(String userId) {
        username.setText(userId);
    }

    // ------------------------------------------------------------------
    // Main
    // ------------------------------------------------------------------
    public static void main(String[] args) {
        java.awt.EventQueue.invokeLater(() -> new Login().setVisible(true));
    }

    // ------------------------------------------------------------------
    // Variables
    // ------------------------------------------------------------------
    private JButton cancel;
    private JLabel jLabel1;
    private JLabel jLabel2;
    private JLabel jLabel3;
    private JLabel jLabel4;
    private JPanel jPanel1;
    private JPanel jPanel2;
    private JButton login;
    private JPasswordField password;
    private JButton register;
    private JTextField username;
    private JLabel forgotPassword;
}