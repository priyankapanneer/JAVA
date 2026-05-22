package com.examportal.handlers;

import com.examportal.dao.ResultDao;
import com.examportal.dao.UserDao;
import com.examportal.models.User;
import com.examportal.utils.HttpUtils;
import com.examportal.utils.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

public class AnalyticsHandler implements HttpHandler {
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

            List<String> examStats = resultDao.getAnalytics();
            int totalStudents = resultDao.countTotalStudents();
            int totalExams = resultDao.countTotalExams();
            int totalAttempts = resultDao.countTotalAttempts();

            StringBuilder sb = new StringBuilder();
            sb.append("{\"totalStudents\":").append(totalStudents)
              .append(",\"totalExams\":").append(totalExams)
              .append(",\"totalAttempts\":").append(totalAttempts)
              .append(",\"examStats\":[");
            for (int i = 0; i < examStats.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(examStats.get(i));
            }
            sb.append("]}");
            HttpUtils.sendSuccess(exchange, sb.toString());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Server error: " + e.getMessage());
        }
    }
}
