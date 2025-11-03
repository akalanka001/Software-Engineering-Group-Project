import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;
import java.sql.*;

public class test extends JFrame {
    private JComboBox<String> dayComboBox;
    private JTextField subjectField;
    private JButton addButton, updateButton;
    private JTable table;
    private DefaultTableModel model;

    public test() {
        setTitle("Admin - Time Table Management");
        setSize(600, 400);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);

        JLabel dayLabel = new JLabel("Day:");
        dayLabel.setBounds(20, 20, 50, 25);
        add(dayLabel);

        dayComboBox = new JComboBox<>(new String[]{"Monday", "Tuesday", "Wednesday", "Thursday", "Friday"});
        dayComboBox.setBounds(80, 20, 120, 25);
        add(dayComboBox);

        JLabel subjectLabel = new JLabel("Subject & Time:");
        subjectLabel.setBounds(20, 60, 100, 25);
        add(subjectLabel);

        subjectField = new JTextField();
        subjectField.setBounds(130, 60, 150, 25);
        add(subjectField);

        addButton = new JButton("Add");
        addButton.setBounds(300, 60, 80, 25);
        add(addButton);
        
        updateButton = new JButton("Update");
        updateButton.setBounds(400, 60, 100, 25);
        add(updateButton);
        
        model = new DefaultTableModel(new String[]{"ID", "Day", "Subject", "Time"}, 0);
        table = new JTable(model);
        JScrollPane pane = new JScrollPane(table);
        pane.setBounds(20, 100, 540, 200);
        add(pane);
        
        loadTimeTable();

        addButton.addActionListener(e -> addSubject());
        updateButton.addActionListener(e -> updateSubject());
    }

    private void addSubject() {
        String day = (String) dayComboBox.getSelectedItem();
        String subjectTime = subjectField.getText();
        String query = "INSERT INTO timetable (day, subject, time) VALUES (?, ?, ?)";
        try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/yourDB", "user", "password");
             PreparedStatement pst = con.prepareStatement(query)) {
            String[] parts = subjectTime.split(" - ");
            if (parts.length < 2) return;
            pst.setString(1, day);
            pst.setString(2, parts[0]);
            pst.setString(3, parts[1]);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Subject Added Successfully!");
            loadTimeTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateSubject() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow == -1) return;
        int id = (int) model.getValueAt(selectedRow, 0);
        String subjectTime = subjectField.getText();
        String query = "UPDATE timetable SET subject = ?, time = ? WHERE id = ?";
        try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/yourDB", "user", "password");
             PreparedStatement pst = con.prepareStatement(query)) {
            String[] parts = subjectTime.split(" - ");
            if (parts.length < 2) return;
            pst.setString(1, parts[0]);
            pst.setString(2, parts[1]);
            pst.setInt(3, id);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Subject Updated Successfully!");
            loadTimeTable();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void loadTimeTable() {
        model.setRowCount(0);
        String query = "SELECT * FROM timetable";
        try (Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/yourDB", "user", "password");
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id"), rs.getString("day"), rs.getString("subject"), rs.getString("time")});
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new test().setVisible(true));
    }
}
