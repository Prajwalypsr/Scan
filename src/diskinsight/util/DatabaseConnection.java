package diskinsight.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {

    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    static {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("db.properties")) {
            props.load(in);
            URL = props.getProperty("db.url",
                    "jdbc:mysql://localhost:3306/diskinsight?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC");
            USER = props.getProperty("db.user", "root");
            PASSWORD = props.getProperty("db.password", "");
        } catch (IOException e) {
            throw new RuntimeException("Could not load db.properties", e);
        }
    }

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}