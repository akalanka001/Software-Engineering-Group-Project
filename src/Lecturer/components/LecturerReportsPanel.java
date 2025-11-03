package Lecturer.components;

import Reports.UserReportFrame;
import Reports.LecturerStudentProgressReportFrame;
import Utils.ThemeManager;

import javax.swing.*;
import java.awt.*;

/**
 * Lecturer Reports Panel
 *  - View Student User Report
 *  - View Student Progress Report (lecturer's courses only)
 *  - View My Students' Progress (summary frame)
 */
public class LecturerReportsPanel extends JPanel {

    public LecturerReportsPanel(String lecturerId) {
        ThemeManager.initialize();
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        JLabel title = new JLabel("Lecturer Reports", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        title.setForeground(ThemeManager.LIGHT_BLUE);
        title.setBorder(BorderFactory.createEmptyBorder(25, 0, 25, 0));
        add(title, BorderLayout.NORTH);

        // ---- Center content panel ----
        JPanel center = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 40));
        center.setBackground(Color.WHITE);

        // --- User Report button ---
        JButton btnUserReport = new JButton("View Student User Report");
        ThemeManager.stylePrimaryButton(btnUserReport);
        btnUserReport.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 16));
        btnUserReport.setPreferredSize(new Dimension(300, 80));
        btnUserReport.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnUserReport.addActionListener(e -> {
            UserReportFrame frame = new UserReportFrame("lecturer");
            frame.setVisible(true);
            JOptionPane.showMessageDialog(this,
                    "This report will display only students enrolled\n" +
                            "in your courses (filtered automatically).",
                    "Report Scope", JOptionPane.INFORMATION_MESSAGE);
        });

        // --- Student Progress Report button ---
        JButton btnProgressReport = new JButton("My Students' Progress");
        ThemeManager.stylePrimaryButton(btnProgressReport);
        btnProgressReport.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 16));
        btnProgressReport.setPreferredSize(new Dimension(300, 80));
        btnProgressReport.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnProgressReport.addActionListener(e ->
                new LecturerStudentProgressReportFrame(lecturerId).setVisible(true)
        );



        // add buttons to panel
        center.add(btnUserReport);
        center.add(btnProgressReport);


        add(center, BorderLayout.CENTER);
    }
}