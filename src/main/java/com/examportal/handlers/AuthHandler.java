package com.examportal.handlers;

import com.examportal.dao.UserDao;
import com.examportal.models.User;
import com.examportal.utils.HttpUtils;
import com.examportal.utils.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

public class AuthHandler implements HttpHandler {
    private final UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // Handle CORS preflight
        if ("OPTIONS".equals(method)) {
            HttpUtils.sendResponse(exchange, 204, "");
            return;
        }

        if (path.endsWith("/login") && "POST".equals(method)) {
            handleLogin(exchange);
        } else if (path.endsWith("/register") && "POST".equals(method)) {
            handleRegister(exchange);
        } else if (path.endsWith("/logout") && "POST".equals(method)) {
            handleLogout(exchange);
        } else if (path.endsWith("/me") && "GET".equals(method)) {
            handleMe(exchange);
        } else {
            HttpUtils.sendError(exchange, 404, "Not found");
        }
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        try {
            String body = HttpUtils.readRequestBody(exchange);
            String username = HttpUtils.parseJsonValue(body, "username");
            String password = HttpUtils.parseJsonValue(body, "password");

            if (username == null || password == null || username.isBlank() || password.isBlank()) {
                HttpUtils.sendError(exchange, 400, "Username and password are required");
                return;
            }

            User user = userDao.authenticate(username.trim(), password);
            if (user == null) {
                HttpUtils.sendError(exchange, 401, "Invalid username or password");
                return;
            }

            String token = SessionManager.createSession(user.getId());
            String response = String.format(
                "{\"success\":true,\"token\":\"%s\",\"user\":%s}",
                token, user.toJson()
            );
            HttpUtils.sendResponse(exchange, 200, response);
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Login failed: " + e.getMessage());
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        try {
            String body = HttpUtils.readRequestBody(exchange);
            String username = HttpUtils.parseJsonValue(body, "username");
            String password = HttpUtils.parseJsonValue(body, "password");
            String email = HttpUtils.parseJsonValue(body, "email");
            String fullName = HttpUtils.parseJsonValue(body, "fullName");

            if (username == null || password == null || email == null || fullName == null) {
                HttpUtils.sendError(exchange, 400, "All fields are required");
                return;
            }
            if (username.trim().length() < 3) {
                HttpUtils.sendError(exchange, 400, "Username must be at least 3 characters");
                return;
            }
            if (password.length() < 6) {
                HttpUtils.sendError(exchange, 400, "Password must be at least 6 characters");
                return;
            }
            if (userDao.existsByUsername(username.trim())) {
                HttpUtils.sendError(exchange, 409, "Username already exists");
                return;
            }
            if (userDao.existsByEmail(email.trim())) {
                HttpUtils.sendError(exchange, 409, "Email already registered");
                return;
            }

            User user = new User();
            user.setUsername(username.trim());
            user.setPassword(password);
            user.setEmail(email.trim().toLowerCase());
            user.setFullName(fullName.trim());
            user.setRole("student");

            int id = userDao.create(user);
            if (id == -1) {
                HttpUtils.sendError(exchange, 500, "Failed to create user");
                return;
            }
            user.setId(id);

            String token = SessionManager.createSession(id);
            String response = String.format(
                "{\"success\":true,\"token\":\"%s\",\"user\":%s}",
                token, user.toJson()
            );
            HttpUtils.sendResponse(exchange, 201, response);
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Registration failed: " + e.getMessage());
        }
    }

    private void handleLogout(HttpExchange exchange) throws IOException {
        String token = HttpUtils.getAuthToken(exchange);
        if (token != null) SessionManager.invalidateSession(token);
        HttpUtils.sendSuccessMessage(exchange, "Logged out successfully");
    }

    private void handleMe(HttpExchange exchange) throws IOException {
        try {
            String token = HttpUtils.getAuthToken(exchange);
            Integer userId = SessionManager.getUserIdFromToken(token);
            if (userId == null) {
                HttpUtils.sendError(exchange, 401, "Unauthorized");
                return;
            }
            User user = userDao.findById(userId);
            if (user == null) {
                HttpUtils.sendError(exchange, 404, "User not found");
                return;
            }
            HttpUtils.sendSuccess(exchange, user.toJson());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Error: " + e.getMessage());
        }
    }
}
