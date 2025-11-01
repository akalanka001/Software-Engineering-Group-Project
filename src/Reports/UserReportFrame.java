package Reports;

import Database.dbconnection;
import Utils.ThemeManager;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * User Report window for Admin / Lecturer.
 * Displays users by role, totals them, allows PDF export.
 */
public class UserReportFrame extends JFrame {

    private JComboBox<String> cmbRole;
    private JTable table;
    private DefaultTableModel model;
    private final JLabel lblCounts = new JLabel(" ");
    private final String currentRole;

    public UserReportFrame(String currentRole) {
        this.currentRole = currentRole.toLowerCase();

        ThemeManager.initialize();
        setTitle("User Report");
        setSize(850, 580);
        setResizable(false);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel main = new JPanel(new BorderLayout(10, 10));
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // ---------- top bar ----------
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        top.setBackground(Color.WHITE);

        JLabel lblTitle = new JLabel("User Report");
        lblTitle.setFont(new Font("Segoe UI Black", Font.BOLD, 22));
        lblTitle.setForeground(ThemeManager.LIGHT_BLUE);
        top.add(lblTitle);
        top.add(Box.createHorizontalStrut(30));

        cmbRole = new JComboBox<>(new String[]{"Student", "Lecturer", "Admin", "All"});
        cmbRole.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        cmbRole.setPreferredSize(new Dimension(150, 30));
        applyRoleRestrictions();
        top.add(new JLabel("Select Role:"));
        top.add(cmbRole);

        JButton btnView = new JButton("View Report");
        ThemeManager.stylePrimaryButton(btnView);
        btnView.addActionListener(this::loadData);
        top.add(btnView);

        JButton btnExport = new JButton("Export PDF");
        ThemeManager.stylePrimaryButton(btnExport);
        btnExport.addActionListener(this::exportPDF);
        top.add(btnExport);

        main.add(top, BorderLayout.NORTH);

        // ---------- data table ----------
        model = new DefaultTableModel(new Object[]{"User ID", "Name", "Email", "Role"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        table.setRowHeight(26);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        ThemeManager.styleTableHeader(table);
        main.add(new JScrollPane(table), BorderLayout.CENTER);

        // ---------- footer counts ----------
        lblCounts.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 14));
        lblCounts.setForeground(new Color(60, 60, 60));
        lblCounts.setBorder(BorderFactory.createEmptyBorder(5, 10, 10, 10));
        main.add(lblCounts, BorderLayout.SOUTH);

        add(main);
    }

    private void applyRoleRestrictions() {
        if (currentRole.equals("lecturer")) {
            cmbRole.removeAllItems();
            cmbRole.addItem("Student");
        }
    }

    // -------------------------------------------------------------------
    //  Load data and count totals
    // -------------------------------------------------------------------
    private void loadData(ActionEvent e) {
        model.setRowCount(0);
        lblCounts.setText("Loading...");

        String selection = cmbRole.getSelectedItem().toString().toLowerCase();
        String roleForQuery = switch (selection) {
            case "student" -> "undergraduate";
            case "lecturer" -> "lecturer";
            case "admin" -> "admin";
            default -> "all";
        };

        String sql = roleForQuery.equals("all")
                ? "SELECT user_id, user_name, user_email, user_role FROM user"
                : "SELECT user_id, user_name, user_email, user_role FROM user WHERE user_role = ?";

        try (Connection conn = dbconnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            if (!roleForQuery.equals("all"))
                ps.setString(1, roleForQuery);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("user_id"),
                        rs.getString("user_name"),
                        rs.getString("user_email"),
                        rs.getString("user_role")
                });
            }
            rs.close();

            if (model.getRowCount() == 0) {
                lblCounts.setText("No users found for selected category.");
                return;
            }

            // --- Count summary ---
            if (roleForQuery.equals("all")) {
                int admins = getCount(conn, "admin");
                int lecturers = getCount(conn, "lecturer");
                int students = getCount(conn, "undergraduate");
                int total = admins + lecturers + students;
                lblCounts.setText(String.format(
                        "<html><b>Total Users:</b> %d &nbsp;&nbsp;&nbsp;"
                                + "<font color='#2A7CEB'>Admins:</font> %d &nbsp;&nbsp;"
                                + "<font color='#2A7CEB'>Lecturers:</font> %d &nbsp;&nbsp;"
                                + "<font color='#2A7CEB'>Students:</font> %d</html>",
                        total, admins, lecturers, students));
            } else {
                lblCounts.setText(String.format("<html><b>Total %s:</b> %d</html>",
                        capitalize(selection), model.getRowCount()));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            lblCounts.setText("Database error.");
            JOptionPane.showMessageDialog(this,
                    "Error loading data from database.",
                    "Database Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String capitalize(String txt) {
        return txt.substring(0, 1).toUpperCase() + txt.substring(1);
    }

    // query helper
    private int getCount(Connection conn, String role) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM user WHERE user_role = ?")) {
            ps.setString(1, role);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception ignored) {}
        return 0;
    }

    // -------------------------------------------------------------------
    //  Export PDF (unchanged core, explicit Font reference)
    // -------------------------------------------------------------------
    private void exportPDF(ActionEvent e) {
        if (model.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Nothing to export!");
            return;
        }

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Report");
        chooser.setSelectedFile(new java.io.File("UserReport.pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        try {
            Document doc = new Document(PageSize.A4.rotate());
            PdfWriter.getInstance(doc, new FileOutputStream(chooser.getSelectedFile()));
            doc.open();

            com.itextpdf.text.Font titleFont =
                    new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA,
                            18, com.itextpdf.text.Font.BOLD, BaseColor.BLUE);

            doc.add(new Paragraph("User Report", titleFont));
            doc.add(new Paragraph(" "));

            PdfPTable pdfTable = new PdfPTable(model.getColumnCount());
            pdfTable.setWidthPercentage(100);

            for (int i = 0; i < model.getColumnCount(); i++)
                pdfTable.addCell(new Phrase(model.getColumnName(i)));
            for (int i = 0; i < model.getRowCount(); i++)
                for (int j = 0; j < model.getColumnCount(); j++)
                    pdfTable.addCell(String.valueOf(model.getValueAt(i, j)));

            doc.add(pdfTable);
            doc.close();

            JOptionPane.showMessageDialog(this, "Report exported successfully!",
                    "Success", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Error exporting PDF file.",
                    "Export Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UserReportFrame("admin").setVisible(true));
    }
}