package com.examportal.models;

public class User {
    private int id;
    private String username;
    private String password;
    private String email;
    private String fullName;
    private String role; // "admin" or "student"
    private String createdAt;

    public User() {}

    public User(int id, String username, String password, String email, String fullName, String role, String createdAt) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String toJson() {
        return String.format(
            "{\"id\":%d,\"username\":\"%s\",\"email\":\"%s\",\"fullName\":\"%s\",\"role\":\"%s\",\"createdAt\":\"%s\"}",
            id, escapeJson(username), escapeJson(email), escapeJson(fullName), escapeJson(role), escapeJson(createdAt)
        );
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
