package Admin.ui;

import Reports.*;
import Utils.ThemeManager;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ReportsPanel extends JPanel {

    private final String user;

    public ReportsPanel(String user) {
        this.user = user;
        ThemeManager.initialize();

        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("Reports Hub", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI Black", Font.BOLD, 24));
        lblTitle.setForeground(ThemeManager.LIGHT_BLUE);
        lblTitle.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));
        add(lblTitle, BorderLayout.NORTH);

        // ---- Button grid ----
        JPanel btnGrid = new JPanel(new GridLayout(3, 3, 25, 25)); // extra row for new buttons
        btnGrid.setBackground(Color.WHITE);
        btnGrid.setBorder(BorderFactory.createEmptyBorder(20, 40, 40, 40));

        List<JButton> buttons = new ArrayList<>();
        buttons.add(createReportButton("User Report"));
        buttons.add(createReportButton("Enrollment Report"));
        buttons.add(createReportButton("Notification Report"));
        buttons.add(createReportButton("Course Summary Report"));
        buttons.add(createReportButton("Financial / Fee Report"));
        buttons.add(createReportButton("Summary Dashboard"));

        // ----- NEW BUTTONS -----
        buttons.add(createReportButton("Activity Log Report"));
        buttons.add(createReportButton("Student Progress Report"));

        for (JButton b : buttons) btnGrid.add(b);
        add(btnGrid, BorderLayout.CENTER);
    }

    // ---------------------------------------------------------
    private JButton createReportButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 15));
        ThemeManager.stylePrimaryButton(btn);
        btn.setPreferredSize(new Dimension(220, 100));
        btn.setFocusPainted(false);
        btn.addActionListener(e -> openReport(text));
        return btn;
    }

    // ---------------------------------------------------------
    private void openReport(String text) {
        switch (text) {
            case "User Report" ->
                    new UserReportFrame("admin").setVisible(true);

            case "Enrollment Report" ->
                    new EnrollmentReportFrame("admin").setVisible(true);

            case "Notification Report" ->
                    new NotificationReportFrame().setVisible(true);

            case "Course Summary Report" ->
                    new CourseSummaryReportFrame().setVisible(true);

            case "Financial / Fee Report" ->
                    new FinancialFeeReportFrame().setVisible(true);

            case "Summary Dashboard" ->
                    new SummaryReportFrame().setVisible(true);

            // -------- NEW HANDLERS --------
            case "Activity Log Report" ->
                    new ActivityLogReportFrame().setVisible(true);

            case "Student Progress Report" ->
                    new StudentProgressReportFrame("admin").setVisible(true);

            default ->
                    JOptionPane.showMessageDialog(
                            this,
                            "Report not configured.",
                            "Notice",
                            JOptionPane.INFORMATION_MESSAGE
                    );
        }
    }
}