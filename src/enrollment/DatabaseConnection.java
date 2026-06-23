package enrollment;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseConnection {

    private static final String DB_URL = "jdbc:sqlite:enrollment.db";
    private static Connection connection = null;

    

    // Returns a single shared connection (Singleton pattern)
    public static Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            System.err.println("Connection error: " + e.getMessage());
        }
        return connection;
    }

    // Creates all tables if they don't exist yet
    public static void initializeDatabase() {
        String createUsers = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password TEXT NOT NULL
                );
                """;

        String createStudents = """
                CREATE TABLE IF NOT EXISTS students (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id TEXT NOT NULL UNIQUE,
                    name TEXT NOT NULL,
                    course TEXT NOT NULL,
                    year_level INTEGER NOT NULL,
                    contact TEXT
                );
                """;

        String createSubjects = """
                CREATE TABLE IF NOT EXISTS subjects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    subject_code TEXT NOT NULL UNIQUE,
                    subject_name TEXT NOT NULL,
                    units INTEGER NOT NULL,
                    schedule TEXT
                );
                """;

        String createEnrollments = """
                CREATE TABLE IF NOT EXISTS enrollments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    student_id TEXT NOT NULL,
                    subject_code TEXT NOT NULL,
                    date_enrolled TEXT NOT NULL,
                    FOREIGN KEY (student_id) REFERENCES students(student_id),
                    FOREIGN KEY (subject_code) REFERENCES subjects(subject_code)
                );
                """;

        String insertDefaultAdmin = """
                INSERT OR IGNORE INTO users (username, password)
                VALUES ('admin', 'admin123');
                """;

        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(createUsers);
            stmt.execute(createStudents);
            stmt.execute(createSubjects);
            stmt.execute(createEnrollments);
            stmt.execute(insertDefaultAdmin);
            System.out.println("Database initialized successfully.");
        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
        }
    }

    // Closes the connection cleanly
    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}