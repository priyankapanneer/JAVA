package com.examportal.handlers;

import com.examportal.dao.ResultDao;
import com.examportal.utils.HttpUtils;
import com.examportal.utils.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

public class LeaderboardHandler implements HttpHandler {
    private final ResultDao resultDao = new ResultDao();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equals(exchange.getRequestMethod())) {
            HttpUtils.sendResponse(exchange, 204, "");
            return;
        }

        String token = HttpUtils.getAuthToken(exchange);
        if (SessionManager.getUserIdFromToken(token) == null) {
            HttpUtils.sendError(exchange, 401, "Unauthorized");
            return;
        }

        try {
            List<String> rows = resultDao.getLeaderboard();
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < rows.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(rows.get(i));
            }
            sb.append("]");
            HttpUtils.sendSuccess(exchange, sb.toString());
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Server error: " + e.getMessage());
        }
    }
}
