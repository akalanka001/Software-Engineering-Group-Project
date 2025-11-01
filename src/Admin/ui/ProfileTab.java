package Admin.ui;

import Admin.Alerts;
import Admin.editprofiledailog;
import Utils.ThemeManager;
import Database.dbconnection;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.sql.*;

/**
 * Professional, modernized Admin ProfileTab
 * • Balanced layout & alignment
 * • Refined typography and spacing
 * • Consistent with ThemeManager visuals
 */
public class ProfileTab extends JPanel {

    private final JLabel nameLbl = new JLabel();
    private final JLabel emailLbl = new JLabel();
    private final JLabel roleLbl = new JLabel();
    private final JLabel phoneLbl = new JLabel();
    private final JLabel picLbl = new JLabel();
    private final String currentUser;

    public ProfileTab(String username) {
        this.currentUser = username;
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        add(buildMainPanel(), BorderLayout.CENTER);

        // Load user info and picture
        display_profile(currentUser);
        SwingUtilities.invokeLater(() -> {
            setDefaultProfilePic();
            loadProfilePic();
        });
    }

    // --------------------------------------------------------------------
    // BUILD MAIN CONTENT
    // --------------------------------------------------------------------
    private JPanel buildMainPanel() {
        // Container provides padding around content
        JPanel content = new JPanel(new BorderLayout(40, 0));
        content.setOpaque(false);
        content.setBorder(BorderFactory.createEmptyBorder(40, 60, 40, 60));

        content.add(buildInfoPanel(), BorderLayout.CENTER);    // left
        content.add(buildPicturePanel(), BorderLayout.EAST);    // right

        return content;
    }

    // Left column: user information fields
    private JPanel buildInfoPanel() {
        JPanel infoPanel = new JPanel(new GridBagLayout());
        infoPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(12, 15, 12, 15);
        gbc.anchor = GridBagConstraints.WEST;

        Font labelFont = new Font("Segoe UI Semibold", Font.PLAIN, 16);
        Font valueFont = new Font("Segoe UI", Font.PLAIN, 15);
        Color labelColor = new Color(60, 60, 60);
        Color valueColor = new Color(25, 25, 25);

        int row = 0;
        gbc.gridx = 0; gbc.gridy = row;
        addLabel(infoPanel, "Name:", labelFont, labelColor, gbc);
        gbc.gridx = 1; addValue(infoPanel, nameLbl, valueFont, valueColor, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        addLabel(infoPanel, "Email:", labelFont, labelColor, gbc);
        gbc.gridx = 1; addValue(infoPanel, emailLbl, valueFont, valueColor, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        addLabel(infoPanel, "Role:", labelFont, labelColor, gbc);
        gbc.gridx = 1; addValue(infoPanel, roleLbl, valueFont, ThemeManager.LIGHT_BLUE, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        addLabel(infoPanel, "Phone:", labelFont, labelColor, gbc);
        gbc.gridx = 1; addValue(infoPanel, phoneLbl, valueFont, valueColor, gbc);

        return infoPanel;
    }

    // Right column: profile picture + buttons
    private JPanel buildPicturePanel() {
        JPanel picPanel = new JPanel(new BorderLayout(10, 20));
        picPanel.setOpaque(false);
        picPanel.setPreferredSize(new Dimension(260, 260));
        picPanel.setBorder(BorderFactory.createEmptyBorder(10, 15, 10, 15));

        // profile image
        picLbl.setPreferredSize(new Dimension(150, 150));
        picLbl.setHorizontalAlignment(SwingConstants.CENTER);
        picLbl.setVerticalAlignment(SwingConstants.CENTER);
        picLbl.setBorder(BorderFactory.createLineBorder(ThemeManager.LIGHT_BLUE, 2, true));
        JPanel picHolder = new JPanel(new FlowLayout(FlowLayout.CENTER));
        picHolder.setOpaque(false);
        picHolder.add(picLbl);
        picPanel.add(picHolder, BorderLayout.CENTER);

        // buttons
        JButton editBtn = new JButton("Edit Profile");
        JButton changeBtn = new JButton("Change Picture");
        ThemeManager.stylePrimaryButton(editBtn);
        ThemeManager.stylePrimaryButton(changeBtn);

        JPanel btnPanel = new JPanel(new GridLayout(2, 1, 8, 8));
        btnPanel.setOpaque(false);
        btnPanel.add(editBtn);
        btnPanel.add(changeBtn);
        picPanel.add(btnPanel, BorderLayout.SOUTH);

        editBtn.addActionListener(e -> editProfileActionPerformed());
        changeBtn.addActionListener(e -> changePicture());

        return picPanel;
    }

    private void addLabel(JPanel panel, String text, Font font, Color color, GridBagConstraints gbc) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(font);
        lbl.setForeground(color);
        panel.add(lbl, gbc);
    }

    private void addValue(JPanel panel, JLabel valueLabel, Font font, Color color, GridBagConstraints gbc) {
        valueLabel.setFont(font);
        valueLabel.setForeground(color);
        panel.add(valueLabel, gbc);
    }

    // --------------------------------------------------------------------
    // DATABASE ACTIONS
    // --------------------------------------------------------------------
    private void editProfileActionPerformed() {
        editprofiledailog dlg = new editprofiledailog(null, true, currentUser);
        dlg.setVisible(true);
        display_profile(currentUser);
        loadProfilePic();
    }

    private void display_profile(String username) {
        String sql = "SELECT * FROM user WHERE user_id=?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, username);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    nameLbl.setText(rs.getString("user_name"));
                    emailLbl.setText(rs.getString("user_email"));
                    roleLbl.setText(rs.getString("user_role").toUpperCase());
                    phoneLbl.setText(rs.getString("user_phone"));
                }
            }
        } catch (Exception e) {
            Alerts.fail(e.getMessage());
        }
    }

    private void loadProfilePic() {
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(
                     "SELECT user_pro_pic FROM user WHERE user_id=?")) {
            pst.setString(1, currentUser);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    String path = rs.getString("user_pro_pic");
                    if (path != null && !path.isBlank() && new File(path).exists()) {
                        picLbl.setIcon(resizeImage(path, picLbl));
                        return;
                    }
                }
            }
            setDefaultProfilePic();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setDefaultProfilePic() {
        String defaultPath = "/Images/UG_DP.png";
        File f = new File(defaultPath);
        if (f.exists()) picLbl.setIcon(resizeImage(defaultPath, picLbl));
    }

    private ImageIcon resizeImage(String path, JLabel label) {
        ImageIcon icon = new ImageIcon(path);
        int w = label.getWidth() > 0 ? label.getWidth() : 150;
        int h = label.getHeight() > 0 ? label.getHeight() : 150;
        Image newImg = icon.getImage().getScaledInstance(w, h, Image.SCALE_SMOOTH);
        return new ImageIcon(newImg);
    }

    private void changePicture() {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String path = chooser.getSelectedFile().getAbsolutePath();
            if (!isValidImageFile(path)) {
                Alerts.fail("Invalid format. Choose JPG, PNG, GIF or BMP.");
                return;
            }
            try (Connection con = dbconnection.getConnection();
                 PreparedStatement pst = con.prepareStatement(
                         "UPDATE user SET user_pro_pic=? WHERE user_id=?")) {
                pst.setString(1, path);
                pst.setString(2, currentUser);
                pst.executeUpdate();
                Alerts.success("Profile picture updated.");
                loadProfilePic();
            } catch (Exception e) {
                Alerts.fail(e.getMessage());
            }
        }
    }

    private boolean isValidImageFile(String path) {
        String lower = path.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg")
                || lower.endsWith(".png") || lower.endsWith(".gif")
                || lower.endsWith(".bmp");
    }
}