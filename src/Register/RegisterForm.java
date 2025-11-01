package Register;

import Login.Login;
import Utils.EmailSender;
import Utils.ThemeManager;

import javax.swing.*;
import java.awt.*;

/**
 * Professional, neatly sized registration form styled with ThemeManager.
 */
public class RegisterForm extends JFrame {

    public RegisterForm() {
        ThemeManager.initialize(); // load proper FlatLaf theme
        initComponents();
    }

    private void initComponents() {

        setTitle("Undergraduate Registration");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        // overall preferred size
        setPreferredSize(new Dimension(600, 550));

        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));

        // ---------- HEADER ----------
        JLabel lblHeader = new JLabel("Undergraduate Registration", SwingConstants.CENTER);
        lblHeader.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        lblHeader.setForeground(ThemeManager.LIGHT_BLUE);
        lblHeader.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
        mainPanel.add(lblHeader, BorderLayout.NORTH);

        // ---------- FORM ----------
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        Font labelFont = new Font("Segoe UI Semibold", Font.PLAIN, 15);
        Font fieldFont = new Font("Segoe UI", Font.PLAIN, 15);

        int textWidth = 250;

        gbc.gridx = 0; gbc.gridy = 0;
        formPanel.add(createLabel("First Name:", labelFont), gbc);
        gbc.gridx = 1;
        txtFirstName = createTextField(fieldFont, textWidth);
        formPanel.add(txtFirstName, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createLabel("Last Name:", labelFont), gbc);
        gbc.gridx = 1;
        txtLastName = createTextField(fieldFont, textWidth);
        formPanel.add(txtLastName, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createLabel("Email:", labelFont), gbc);
        gbc.gridx = 1;
        txtEmail = createTextField(fieldFont, textWidth);
        formPanel.add(txtEmail, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createLabel("Phone:", labelFont), gbc);
        gbc.gridx = 1;
        txtPhone = createTextField(fieldFont, textWidth);
        formPanel.add(txtPhone, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createLabel("Password:", labelFont), gbc);
        gbc.gridx = 1;
        txtPassword = createPasswordField(fieldFont, textWidth);
        formPanel.add(txtPassword, gbc);

        gbc.gridx = 0; gbc.gridy++;
        formPanel.add(createLabel("Confirm Password:", labelFont), gbc);
        gbc.gridx = 1;
        txtConfirmPassword = createPasswordField(fieldFont, textWidth);
        formPanel.add(txtConfirmPassword, gbc);

        mainPanel.add(formPanel, BorderLayout.CENTER);

        // ---------- BUTTONS ----------
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 30, 15));
        buttonPanel.setBackground(Color.WHITE);

        btnRegister = new JButton("Register");
        ThemeManager.stylePrimaryButton(btnRegister);
        btnRegister.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        btnRegister.setPreferredSize(new Dimension(130, 38));
        btnRegister.addActionListener(evt -> {
            try { btnRegisterActionPerformed(); } catch (Exception e) { e.printStackTrace(); }
        });

        btnBack = new JButton("Back to Login");
        ThemeManager.stylePrimaryButton(btnBack);
        btnBack.setBackground(new Color(210, 220, 235));
        btnBack.setForeground(new Color(40, 40, 40));
        btnBack.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        btnBack.setPreferredSize(new Dimension(150, 38));
        btnBack.addActionListener(evt -> {
            new Login().setVisible(true);
            this.dispose();
        });

        buttonPanel.add(btnRegister);
        buttonPanel.add(btnBack);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        getContentPane().add(mainPanel);
        pack();
        setLocationRelativeTo(null);
    }

    private JLabel createLabel(String text, Font font) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(new Color(60, 60, 60));
        return lbl;
    }

    private JTextField createTextField(Font font, int width) {
        JTextField tf = new JTextField();
        tf.setFont(font);
        tf.setPreferredSize(new Dimension(width, 30));
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        return tf;
    }

    private JPasswordField createPasswordField(Font font, int width) {
        JPasswordField pf = new JPasswordField();
        pf.setFont(font);
        pf.setPreferredSize(new Dimension(width, 30));
        pf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        return pf;
    }

    // -----------------------------------------------------
    //  REGISTER LOGIC (kept identical)
    // -----------------------------------------------------
    private void btnRegisterActionPerformed() throws Exception {
        String firstName = txtFirstName.getText().trim();
        String lastName = txtLastName.getText().trim();
        String email = txtEmail.getText().trim();
        String phone = txtPhone.getText().trim();
        String password = new String(txtPassword.getPassword());
        String confirmPassword = new String(txtConfirmPassword.getPassword());

        if (firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() ||
                phone.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Please fill in all fields."); return;
        }
        if (!email.matches("^[\\w._%+-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$")) {
            showError("Invalid email address."); return;
        }
        if (!phone.matches("^0\\d{9}$")) {
            showError("Phone number must start with 0 and have 10 digits."); return;
        }
        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match."); return;
        }

        Register registerLogic = new Register();
        String existingUserId = registerLogic.findUserByEmail(email);
        if (existingUserId != null) {
            JOptionPane.showMessageDialog(this,
                    "You are already registered! Please log in using your existing account.",
                    "Already Registered",
                    JOptionPane.INFORMATION_MESSAGE);
            Login loginForm = new Login();
            loginForm.prefillUserId(existingUserId);
            loginForm.setVisible(true);
            this.dispose();
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to register with these details?",
                "Confirm Registration",
                JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        String userId = registerLogic.registerUndergraduate(firstName, lastName, password, email, phone);
        if (userId != null) {
            JOptionPane.showMessageDialog(this, "Verification email sent to your email address.");
            boolean emailSent = EmailSender.sendRegistrationEmail(email, userId, password);
            if (emailSent) {
                JOptionPane.showMessageDialog(this,
                        "Registration successful! Please check your email for your credentials.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this,
                        "Account created successfully, but email could not be sent.",
                        "Warning", JOptionPane.WARNING_MESSAGE);
            }
            new Login().setVisible(true);
            this.dispose();
        } else {
            showError("Registration failed. Please try again.");
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Validation Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new RegisterForm().setVisible(true));
    }

    private JTextField txtFirstName;
    private JTextField txtLastName;
    private JTextField txtEmail;
    private JTextField txtPhone;
    private JPasswordField txtPassword;
    private JPasswordField txtConfirmPassword;
    private JButton btnRegister;
    private JButton btnBack;
}