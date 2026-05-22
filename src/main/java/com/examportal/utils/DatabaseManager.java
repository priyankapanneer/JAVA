package com.examportal.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:sqlite:examportal.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            connection.setAutoCommit(true);
            try (Statement s = connection.createStatement()) {
                s.execute("PRAGMA journal_mode=WAL");
                s.execute("PRAGMA foreign_keys=ON");
            }
            initializeDatabase();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get connection", e);
        }
        return connection;
    }

    private void initializeDatabase() throws SQLException {
        Statement stmt = connection.createStatement();

        // Users table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                email TEXT UNIQUE NOT NULL,
                full_name TEXT NOT NULL,
                role TEXT NOT NULL DEFAULT 'student',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """);

        // Exams table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS exams (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                description TEXT,
                duration INTEGER NOT NULL,
                total_marks INTEGER NOT NULL DEFAULT 0,
                passing_marks INTEGER NOT NULL DEFAULT 0,
                created_by INTEGER NOT NULL,
                status TEXT NOT NULL DEFAULT 'draft',
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (created_by) REFERENCES users(id)
            )
        """);

        // Questions table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS questions (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                exam_id INTEGER NOT NULL,
                question_text TEXT NOT NULL,
                option_a TEXT NOT NULL,
                option_b TEXT NOT NULL,
                option_c TEXT NOT NULL,
                option_d TEXT NOT NULL,
                correct_answer TEXT NOT NULL,
                marks INTEGER NOT NULL DEFAULT 1,
                order_num INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (exam_id) REFERENCES exams(id) ON DELETE CASCADE
            )
        """);

        // Results table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS results (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id INTEGER NOT NULL,
                exam_id INTEGER NOT NULL,
                score INTEGER NOT NULL DEFAULT 0,
                total_marks INTEGER NOT NULL,
                passing_marks INTEGER NOT NULL,
                time_taken INTEGER NOT NULL DEFAULT 0,
                passed INTEGER NOT NULL DEFAULT 0,
                correct_answers INTEGER NOT NULL DEFAULT 0,
                total_questions INTEGER NOT NULL DEFAULT 0,
                cheating_flag INTEGER NOT NULL DEFAULT 0,
                cheat_count INTEGER NOT NULL DEFAULT 0,
                question_times TEXT DEFAULT '{}',
                submitted_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id),
                FOREIGN KEY (exam_id) REFERENCES exams(id)
            )
        """);

        // Migrate existing results table if columns missing
        try {
            stmt.execute("ALTER TABLE results ADD COLUMN cheating_flag INTEGER NOT NULL DEFAULT 0");
        } catch (Exception ignored) {}
        try {
            stmt.execute("ALTER TABLE results ADD COLUMN cheat_count INTEGER NOT NULL DEFAULT 0");
        } catch (Exception ignored) {}
        try {
            stmt.execute("ALTER TABLE results ADD COLUMN question_times TEXT DEFAULT '{}'");
        } catch (Exception ignored) {}

        // Answers table
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS answers (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                result_id INTEGER NOT NULL,
                question_id INTEGER NOT NULL,
                selected_answer TEXT,
                is_correct INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY (result_id) REFERENCES results(id) ON DELETE CASCADE,
                FOREIGN KEY (question_id) REFERENCES questions(id)
            )
        """);

        // Insert default admin
        String checkAdmin = "SELECT COUNT(*) FROM users WHERE role='admin'";
        var rs = stmt.executeQuery(checkAdmin);
        if (rs.next() && rs.getInt(1) == 0) {
            stmt.execute("""
                INSERT INTO users (username, password, email, full_name, role)
                VALUES ('admin', 'admin123', 'admin@examportal.com', 'System Administrator', 'admin')
            """);
            System.out.println("[DB] Default admin created: admin / admin123");
        }

        stmt.close();
        System.out.println("[DB] Database initialized successfully.");
    }
}
