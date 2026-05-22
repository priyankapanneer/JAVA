package com.examportal.handlers;

import com.examportal.dao.UserDao;
import com.examportal.models.User;
import com.examportal.utils.HttpUtils;
import com.examportal.utils.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

public class UserHandler implements HttpHandler {
    private final UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        if ("OPTIONS".equals(method)) {
            HttpUtils.sendResponse(exchange, 204, "");
            return;
        }

        String token = HttpUtils.getAuthToken(exchange);
        Integer userId = SessionManager.getUserIdFromToken(token);
        if (userId == null) { HttpUtils.sendError(exchange, 401, "Unauthorized"); return; }

        try {
            User currentUser = userDao.findById(userId);
            if (currentUser == null) { HttpUtils.sendError(exchange, 401, "Unauthorized"); return; }

            String[] parts = path.split("/");
            // /api/users  GET (admin only)
            // /api/users/{id}  GET, DELETE (admin only)
            // /api/users/me/password  PUT

            if (parts.length == 3) {
                if ("GET".equals(method) && "admin".equals(currentUser.getRole())) {
                    List<User> users = userDao.findAll();
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < users.size(); i++) {
                        if (i > 0) sb.append(",");
                        sb.append(users.get(i).toJson());
                    }
                    sb.append("]");
                    HttpUtils.sendSuccess(exchange, sb.toString());
                } else {
                    HttpUtils.sendError(exchange, 403, "Forbidden");
                }
            } else if (parts.length == 4) {
                if ("me".equals(parts[3])) {
                    HttpUtils.sendSuccess(exchange, currentUser.toJson());
                } else if ("admin".equals(currentUser.getRole())) {
                    int targetId = Integer.parseInt(parts[3]);
                    if ("GET".equals(method)) {
                        User u = userDao.findById(targetId);
                        if (u == null) { HttpUtils.sendError(exchange, 404, "User not found"); return; }
                        HttpUtils.sendSuccess(exchange, u.toJson());
                    } else if ("DELETE".equals(method)) {
                        boolean deleted = userDao.delete(targetId);
                        if (!deleted) { HttpUtils.sendError(exchange, 404, "User not found or cannot delete admin"); return; }
                        HttpUtils.sendSuccessMessage(exchange, "User deleted");
                    }
                } else {
                    HttpUtils.sendError(exchange, 403, "Forbidden");
                }
            } else if (parts.length == 5 && "me".equals(parts[3]) && "password".equals(parts[4])) {
                String body = HttpUtils.readRequestBody(exchange);
                String newPassword = HttpUtils.parseJsonValue(body, "password");
                if (newPassword == null || newPassword.length() < 6) {
                    HttpUtils.sendError(exchange, 400, "Password must be at least 6 characters");
                    return;
                }
                userDao.updatePassword(userId, newPassword);
                HttpUtils.sendSuccessMessage(exchange, "Password updated");
            } else {
                HttpUtils.sendError(exchange, 404, "Not found");
            }
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Error: " + e.getMessage());
        }
    }
}
