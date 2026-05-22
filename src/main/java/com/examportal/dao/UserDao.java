package com.examportal.dao;

import com.examportal.models.User;
import com.examportal.utils.DatabaseManager;
import com.examportal.utils.SessionManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {
    private Connection conn() { return DatabaseManager.getInstance().getConnection(); }

    public User findByUsername(String username) throws SQLException {
        PreparedStatement ps = conn().prepareStatement(
            "SELECT * FROM users WHERE username = ?"
        );
        ps.setString(1, username);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapRow(rs);
        return null;
    }

    public User findById(int id) throws SQLException {
        PreparedStatement ps = conn().prepareStatement("SELECT * FROM users WHERE id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapRow(rs);
        return null;
    }

    public User authenticate(String username, String password) throws SQLException {
        String hashed = SessionManager.hashPassword(password);
        // Support plain password for admin (legacy) + hashed
        PreparedStatement ps = conn().prepareStatement(
            "SELECT * FROM users WHERE username = ? AND (password = ? OR password = ?)"
        );
        ps.setString(1, username);
        ps.setString(2, password); // plain (for default admin)
        ps.setString(3, hashed);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapRow(rs);
        return null;
    }

    public boolean existsByUsername(String username) throws SQLException {
        PreparedStatement ps = conn().prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?");
        ps.setString(1, username);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    public boolean existsByEmail(String email) throws SQLException {
        PreparedStatement ps = conn().prepareStatement("SELECT COUNT(*) FROM users WHERE email = ?");
        ps.setString(1, email);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    public int create(User user) throws SQLException {
        PreparedStatement ps = conn().prepareStatement(
            "INSERT INTO users (username, password, email, full_name, role) VALUES (?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        );
        ps.setString(1, user.getUsername());
        ps.setString(2, SessionManager.hashPassword(user.getPassword()));
        ps.setString(3, user.getEmail());
        ps.setString(4, user.getFullName());
        ps.setString(5, user.getRole() != null ? user.getRole() : "student");
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        return keys.next() ? keys.getInt(1) : -1;
    }

    public List<User> findAll() throws SQLException {
        List<User> users = new ArrayList<>();
        ResultSet rs = conn().createStatement().executeQuery("SELECT * FROM users ORDER BY created_at DESC");
        while (rs.next()) users.add(mapRow(rs));
        return users;
    }

    public List<User> findStudents() throws SQLException {
        List<User> users = new ArrayList<>();
        PreparedStatement ps = conn().prepareStatement("SELECT * FROM users WHERE role='student' ORDER BY full_name");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) users.add(mapRow(rs));
        return users;
    }

    public void updatePassword(int userId, String newPassword) throws SQLException {
        PreparedStatement ps = conn().prepareStatement("UPDATE users SET password=? WHERE id=?");
        ps.setString(1, SessionManager.hashPassword(newPassword));
        ps.setInt(2, userId);
        ps.executeUpdate();
    }

    public boolean delete(int userId) throws SQLException {
        PreparedStatement ps = conn().prepareStatement("DELETE FROM users WHERE id=? AND role!='admin'");
        ps.setInt(1, userId);
        return ps.executeUpdate() > 0;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        return new User(
            rs.getInt("id"),
            rs.getString("username"),
            rs.getString("password"),
            rs.getString("email"),
            rs.getString("full_name"),
            rs.getString("role"),
            rs.getString("created_at")
        );
    }
}
