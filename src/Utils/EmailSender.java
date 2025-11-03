package Utils;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;

import java.io.File;
import java.util.Properties;

public class EmailSender {

    public static boolean sendRegistrationEmail(String toEmail, String userId, String password) {
        final String fromEmail = "contact.cg.akalanka@gmail.com"; // replace with your email
        final String appPassword = "dluu gvgx eivy ulya";   // use Gmail App Password

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, appPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail, "ELMS Registration"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Your ELMS Account Details");
            message.setText("Dear User,\n\nYour account has been created successfully.\n\nUser ID: " + userId +
                    "\nPassword: " + password + "\n\nPlease log in and change your password.\n\nRegards,\nELMS Team");

            Transport.send(message);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static boolean sendPasswordChangeEmail(String toEmail, String userId) {
        final String fromEmail = "contact.cg.akalanka@gmail.com";
        final String appPassword = "dluu gvgx eivy ulya";

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, appPassword);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail, "ELMS Security"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("ELMS Password Changed");
            message.setText("Dear User,\n\nYour ELMS password was recently changed by the administrator."
                    + "\nIf you did not expect this, please contact support.\n\nRegards,\nELMS Team");
            Transport.send(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Sends a course‑completion certificate as a PDF attachment.
     * This does not modify any of your original methods.
     */
    public static boolean sendCertificateEmail(String toEmail,
                                               String studentName,
                                               String courseName,
                                               String pdfPath) {
        final String fromEmail = "contact.cg.akalanka@gmail.com"; // same sender
        final String appPassword = "dluu gvgx eivy ulya";

        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(fromEmail, appPassword);
            }
        });

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail, "ELMS Certificates"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Your Certificate – " + courseName);

            MimeBodyPart textPart = new MimeBodyPart();
            textPart.setText(
                    "Dear " + studentName + ",\n\n" +
                            "Congratulations on successfully completing the course \"" + courseName + "\".\n" +
                            "Please find your certificate attached.\n\n" +
                            "Regards,\nELMS Team"
            );

            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.attachFile(new File(pdfPath));

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(textPart);
            multipart.addBodyPart(attachmentPart);

            message.setContent(multipart);

            Transport.send(message);
            System.out.println("📧 Certificate email sent to " + toEmail);
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}