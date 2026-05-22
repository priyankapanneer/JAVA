package com.examportal.handlers;

import com.examportal.dao.ResultDao;
import com.examportal.dao.UserDao;
import com.examportal.models.Result;
import com.examportal.models.User;
import com.examportal.utils.DatabaseManager;
import com.examportal.utils.HttpUtils;
import com.examportal.utils.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.util.List;

public class ExportHandler implements HttpHandler {
    private final ResultDao resultDao = new ResultDao();
    private final UserDao userDao = new UserDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            HttpUtils.sendResponse(exchange, 204, "");
            return;
        }

        String token = HttpUtils.getAuthToken(exchange);
        Integer userId = SessionManager.getUserIdFromToken(token);
        if (userId == null) { HttpUtils.sendError(exchange, 401, "Unauthorized"); return; }

        try {
            User user = userDao.findById(userId);
            if (user == null || !"admin".equals(user.getRole())) {
                HttpUtils.sendError(exchange, 403, "Admin access required");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/results")) {
                exportResults(exchange);
            } else if (path.endsWith("/students")) {
                exportStudents(exchange);
            } else {
                HttpUtils.sendError(exchange, 404, "Not found");
            }
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Server error: " + e.getMessage());
        }
    }

    private void exportResults(HttpExchange exchange) throws Exception {
        List<Result> results = resultDao.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Student,Username,Exam,Score,Total Marks,Percentage,Status,Correct,Total Questions,Time Taken (s),Cheat Count,Submitted At\n");
        for (Result r : results) {
            double pct = r.getTotalMarks() > 0 ? (double) r.getScore() / r.getTotalMarks() * 100 : 0;
            csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",%d,%d,%.1f%%,%s,%d,%d,%d,%d,\"%s\"\n",
                r.getId(), esc(r.getFullName()), esc(r.getUsername()), esc(r.getExamTitle()),
                r.getScore(), r.getTotalMarks(), pct,
                r.isPassed() ? "PASS" : "FAIL",
                r.getCorrectAnswers(), r.getTotalQuestions(), r.getTimeTaken(),
                r.getCheatCount(), r.getSubmittedAt()
            ));
        }
        sendCsv(exchange, "results.csv", csv.toString());
    }

    private void exportStudents(HttpExchange exchange) throws Exception {
        StringBuilder csv = new StringBuilder();
        csv.append("ID,Username,Full Name,Email,Role,Registered At\n");
        try (var conn = DatabaseManager.getInstance().getConnection()) {
            ResultSet rs = conn.createStatement().executeQuery(
                "SELECT id, username, full_name, email, role, created_at FROM users WHERE role='student' ORDER BY created_at DESC"
            );
            while (rs.next()) {
                csv.append(String.format("%d,\"%s\",\"%s\",\"%s\",%s,\"%s\"\n",
                    rs.getInt("id"), esc(rs.getString("username")),
                    esc(rs.getString("full_name")), esc(rs.getString("email")),
                    rs.getString("role"), rs.getString("created_at")
                ));
            }
        }
        sendCsv(exchange, "students.csv", csv.toString());
    }

    private void sendCsv(HttpExchange exchange, String filename, String csv) throws IOException {
        byte[] bytes = csv.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/csv; charset=UTF-8");
        exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" + filename + "\"");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }
}
