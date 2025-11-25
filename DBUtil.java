// DBUtil.java
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBUtil {
    // Update these to match your local MySQL settings
    private static final String URL = "jdbc:mysql://localhost:3306/classmates_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "password";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found. Add connector jar to classpath.");
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASS);
    }
}
