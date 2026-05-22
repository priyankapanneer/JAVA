package com.examportal.handlers;

import com.examportal.dao.ExamDao;
import com.examportal.dao.QuestionDao;
import com.examportal.dao.ResultDao;
import com.examportal.dao.UserDao;
import com.examportal.models.*;
import com.examportal.utils.HttpUtils;
import com.examportal.utils.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

public class ResultHandler implements HttpHandler {
    private final ResultDao resultDao = new ResultDao();
    private final ExamDao examDao = new ExamDao();
    private final QuestionDao questionDao = new QuestionDao();
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
            User user = userDao.findById(userId);
            String[] parts = path.split("/");

            // /api/results  GET (admin: all, student: own)
            // /api/results/{id}  GET
            // /api/results/submit  POST
            // /api/results/exam/{examId}  GET (admin only)
            // /api/dashboard  GET

            if (parts.length == 3 && "results".equals(parts[2])) {
                if ("GET".equals(method)) {
                    handleListResults(exchange, user);
                } else if ("POST".equals(method)) {
                    // check if it's a submit action via query
                    handleSubmitExam(exchange, user);
                }
            } else if (parts.length == 4 && "results".equals(parts[2])) {
                if ("submit".equals(parts[3]) && "POST".equals(method)) {
                    handleSubmitExam(exchange, user);
                } else {
                    int resultId = Integer.parseInt(parts[3]);
                    handleGetResult(exchange, resultId, user);
                }
            } else if (parts.length == 5 && "results".equals(parts[2]) && "exam".equals(parts[3])) {
                int examId = Integer.parseInt(parts[4]);
                handleExamResults(exchange, examId, user);
            } else if (parts.length == 3 && "dashboard".equals(parts[2])) {
                handleDashboard(exchange, user);
            } else {
                HttpUtils.sendError(exchange, 404, "Not found");
            }
        } catch (NumberFormatException e) {
            HttpUtils.sendError(exchange, 400, "Invalid ID format");
        } catch (Exception e) {
            e.printStackTrace();
            HttpUtils.sendError(exchange, 500, "Server error: " + e.getMessage());
        }
    }

    private void handleSubmitExam(HttpExchange exchange, User user) throws Exception {
        String body = HttpUtils.readRequestBody(exchange);
        String examIdStr = HttpUtils.parseJsonValue(body, "examId");
        String timeTakenStr = HttpUtils.parseJsonValue(body, "timeTaken");
        String cheatCountStr = HttpUtils.parseJsonValue(body, "cheatCount");
        String questionTimesRaw = null;
        // Extract questionTimes JSON object
        int qtIdx = body.indexOf("\"questionTimes\"");
        if (qtIdx != -1) {
            int start = body.indexOf("{", qtIdx + 14);
            if (start != -1) {
                int depth = 0, end = start;
                while (end < body.length()) {
                    char c = body.charAt(end);
                    if (c == '{') depth++; else if (c == '}') { depth--; if (depth == 0) break; }
                    end++;
                }
                questionTimesRaw = body.substring(start, end + 1);
            }
        }

        if (examIdStr == null) { HttpUtils.sendError(exchange, 400, "examId required"); return; }

        int examId = Integer.parseInt(examIdStr);
        Exam exam = examDao.findById(examId);
        if (exam == null) { HttpUtils.sendError(exchange, 404, "Exam not found"); return; }

        if (resultDao.hasAttempted(user.getId(), examId)) {
            HttpUtils.sendError(exchange, 409, "You have already attempted this exam");
            return;
        }

        List<Question> questions = questionDao.findByExamId(examId);

        int score = 0;
        int correctCount = 0;
        int cheatCount = cheatCountStr != null ? Integer.parseInt(cheatCountStr) : 0;

        Result result = new Result();
        result.setUserId(user.getId());
        result.setExamId(examId);
        result.setTotalMarks(exam.getTotalMarks());
        result.setPassingMarks(exam.getPassingMarks());
        result.setTimeTaken(timeTakenStr != null ? Integer.parseInt(timeTakenStr) : 0);
        result.setTotalQuestions(questions.size());
        result.setCheatingFlag(cheatCount >= 3);
        result.setCheatCount(cheatCount);
        result.setQuestionTimes(questionTimesRaw != null ? questionTimesRaw : "{}");

        int resultId = resultDao.create(result);

        for (Question q : questions) {
            String selectedKey = String.valueOf(q.getId());
            String selected = HttpUtils.parseJsonValue(body.contains("\"answers\"") ?
                extractAnswersObject(body) : body, selectedKey);
            boolean isCorrect = selected != null && selected.equalsIgnoreCase(q.getCorrectAnswer());
            if (isCorrect) {
                score += q.getMarks();
                correctCount++;
            }
            resultDao.saveAnswer(resultId, q.getId(), selected, isCorrect);
        }

        boolean passed = score >= exam.getPassingMarks();

        // Update result with score
        try (var conn = com.examportal.utils.DatabaseManager.getInstance().getConnection()) {
            var ps = conn.prepareStatement(
                "UPDATE results SET score=?, passed=?, correct_answers=? WHERE id=?"
            );
            ps.setInt(1, score);
            ps.setInt(2, passed ? 1 : 0);
            ps.setInt(3, correctCount);
            ps.setInt(4, resultId);
            ps.executeUpdate();
        }

        Result savedResult = resultDao.findById(resultId);
        HttpUtils.sendResponse(exchange, 201, "{\"success\":true,\"data\":" + savedResult.toJson() + "}");
    }


    private String extractAnswersObject(String body) {
        int idx = body.indexOf("\"answers\"");
        if (idx == -1) return body;
        int start = body.indexOf("{", idx + 9);
        if (start == -1) return "";
        int depth = 0;
        int end = start;
        while (end < body.length()) {
            char c = body.charAt(end);
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) break; }
            end++;
        }
        return body.substring(start, end + 1);
    }

    private void handleListResults(HttpExchange exchange, User user) throws Exception {
        List<Result> results;
        if ("admin".equals(user.getRole())) {
            results = resultDao.findAll();
        } else {
            results = resultDao.findByUserId(user.getId());
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(results.get(i).toJson());
        }
        sb.append("]");
        HttpUtils.sendSuccess(exchange, sb.toString());
    }

    private void handleGetResult(HttpExchange exchange, int resultId, User user) throws Exception {
        Result result = resultDao.findById(resultId);
        if (result == null) { HttpUtils.sendError(exchange, 404, "Result not found"); return; }
        if (!"admin".equals(user.getRole()) && result.getUserId() != user.getId()) {
            HttpUtils.sendError(exchange, 403, "Access denied");
            return;
        }
        HttpUtils.sendSuccess(exchange, result.toJson());
    }

    private void handleExamResults(HttpExchange exchange, int examId, User user) throws Exception {
        if (!"admin".equals(user.getRole())) { HttpUtils.sendError(exchange, 403, "Admin access required"); return; }
        List<Result> results = resultDao.findByExamId(examId);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < results.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(results.get(i).toJson());
        }
        sb.append("]");
        HttpUtils.sendSuccess(exchange, sb.toString());
    }

    private void handleDashboard(HttpExchange exchange, User user) throws Exception {
        if ("admin".equals(user.getRole())) {
            int totalStudents = resultDao.countTotalStudents();
            int totalExams = resultDao.countTotalExams();
            int totalAttempts = resultDao.countTotalAttempts();
            List<Result> recentResults = resultDao.findAll();
            int limit = Math.min(5, recentResults.size());

            StringBuilder sb = new StringBuilder();
            sb.append("{\"totalStudents\":").append(totalStudents)
              .append(",\"totalExams\":").append(totalExams)
              .append(",\"totalAttempts\":").append(totalAttempts)
              .append(",\"recentResults\":[");
            for (int i = 0; i < limit; i++) {
                if (i > 0) sb.append(",");
                sb.append(recentResults.get(i).toJson());
            }
            sb.append("]}");
            HttpUtils.sendSuccess(exchange, sb.toString());
        } else {
            List<Result> myResults = resultDao.findByUserId(user.getId());
            int totalAttempts = myResults.size();
            int totalPassed = (int) myResults.stream().filter(Result::isPassed).count();
            double avgScore = myResults.stream().mapToDouble(r ->
                r.getTotalMarks() > 0 ? (double) r.getScore() / r.getTotalMarks() * 100 : 0
            ).average().orElse(0);

            StringBuilder sb = new StringBuilder();
            sb.append("{\"totalAttempts\":").append(totalAttempts)
              .append(",\"totalPassed\":").append(totalPassed)
              .append(",\"avgScore\":").append(String.format("%.2f", avgScore))
              .append(",\"recentResults\":[");
            int limit = Math.min(5, myResults.size());
            for (int i = 0; i < limit; i++) {
                if (i > 0) sb.append(",");
                sb.append(myResults.get(i).toJson());
            }
            sb.append("]}");
            HttpUtils.sendSuccess(exchange, sb.toString());
        }
    }
}
