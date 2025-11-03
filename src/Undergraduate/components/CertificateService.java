package Undergraduate.components;

import Database.dbconnection;
import Utils.EmailSender;

import javax.swing.*;
import java.io.File;
import java.sql.*;

/**
 * Handles course-completion certificate issuing.
 *  - Prevents duplicates for the same student & course
 *  - Generates PDF via CertificateGenerator
 *  - Emails the certificate to the student's registered email
 */
public class CertificateService {

    /** Checks if the given student has finished all modules in a course. */
    public static boolean isCourseCompleted(String ugId, String courseId) {
        String sql = """
            SELECT COUNT(*) AS total,
                   SUM(CASE WHEN mp.completed = 1 THEN 1 ELSE 0 END) AS done
            FROM module m
            LEFT JOIN module_progress mp
              ON m.module_id = mp.module_id AND mp.ug_id = ?
            WHERE m.course_id = ?
            """;
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, ugId);
            pst.setString(2, courseId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                int total = rs.getInt("total");
                int done  = rs.getInt("done");
                return total > 0 && total == done;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Returns student's full name (from user table). */
    private static String getStudentName(String ugId) {
        String sql = "SELECT user_name FROM user WHERE user_id=? AND user_role='undergraduate'";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, ugId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getString("user_name");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return ugId; // fallback
    }

    /** Returns student's email (from user table). */
    private static String getStudentEmail(String ugId) {
        String sql = "SELECT user_email FROM user WHERE user_id=? AND user_role='undergraduate'";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, ugId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getString("user_email");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /** Returns course name. */
    private static String getCourseName(String courseId) {
        String sql = "SELECT course_name FROM course WHERE course_id=?";
        try (Connection con = dbconnection.getConnection();
             PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, courseId);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getString("course_name");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return courseId;
    }

    /**
     * Main method to issue a certificate.
     *  - Aborts if one already exists for the same student & course.
     *  - Generates PDF, inserts DB record, sends email.
     */
    public static void issueCertificate(String ugId, String courseId) {
        try (Connection con = dbconnection.getConnection()) {

            // ---------------------------------------------------------------
            // 1️⃣ Check if certificate already exists for this student/course
            String check = "SELECT file_path FROM certificate WHERE ug_id=? AND course_id=?";
            try (PreparedStatement pst = con.prepareStatement(check)) {
                pst.setString(1, ugId);
                pst.setString(2, courseId);
                ResultSet rs = pst.executeQuery();
                if (rs.next()) {
                    JOptionPane.showMessageDialog(null,
                            "You already completed this course.\nCheck your email for your certificate.",
                            "Certificate Already Issued", JOptionPane.INFORMATION_MESSAGE);
                    System.out.println("Duplicate certificate avoided for (" + ugId + ", " + courseId + ")");
                    return;
                }
            }

            // ---------------------------------------------------------------
            // 2️⃣ Confirm completion
            if (!isCourseCompleted(ugId, courseId)) {
                System.out.println("Certificate not issued — course not yet fully completed.");
                return;
            }

            // ---------------------------------------------------------------
            // 3️⃣ Fetch details
            String studentName = getStudentName(ugId);
            String studentEmail = getStudentEmail(ugId);
            String courseName  = getCourseName(courseId);

            // ---------------------------------------------------------------
            // 4️⃣ Generate certificate PDF
            File dir = new File("certificates");
            if (!dir.exists()) dir.mkdirs();
            String pdfPath = dir.getPath() + File.separator + ugId + "_" + courseId + "_certificate.pdf";

            CertificateGenerator.generate(studentName, courseName, pdfPath);

            // ---------------------------------------------------------------
            // 5️⃣ Insert DB record (prevent duplicates permanently)
            String insert = """
                INSERT INTO certificate (ug_id, course_id, file_path, issued_at)
                VALUES (?, ?, ?, NOW())
                """;
            try (PreparedStatement pst = con.prepareStatement(insert)) {
                pst.setString(1, ugId);
                pst.setString(2, courseId);
                pst.setString(3, pdfPath);
                pst.executeUpdate();
                System.out.println("🎓 Certificate record saved in database.");
            }

            // ---------------------------------------------------------------
            // 6️⃣ Email the certificate
            if (studentEmail != null && !studentEmail.isEmpty()) {
                boolean sent = EmailSender.sendCertificateEmail(studentEmail, studentName, courseName, pdfPath);
                if (sent) {
                    JOptionPane.showMessageDialog(null,
                            "🎉 Congratulations!\nCertificate generated and emailed successfully.",
                            "Certificate Issued", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(null,
                            "Certificate generated but could not send email.",
                            "Email Error", JOptionPane.WARNING_MESSAGE);
                }
            } else {
                JOptionPane.showMessageDialog(null,
                        "No registered email found for this student.\nCertificate saved locally.",
                        "Missing Email", JOptionPane.WARNING_MESSAGE);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}