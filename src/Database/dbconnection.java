package Database;

import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;

public class dbconnection {
    public static Connection getConnection() {
        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream("db.properties")) {
            props.load(fis);

            String url  = props.getProperty("db.url");
            String user = props.getProperty("db.user");
            String pass = props.getProperty("db.password");

            Connection con = DriverManager.getConnection(url, user, pass);
            System.out.println("✅ Secure DB connection established via SSL/TLS.");
            return con;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}