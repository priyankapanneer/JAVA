package com.examportal.handlers;

import com.examportal.dao.ExamDao;
import com.examportal.dao.QuestionDao;
import com.examportal.dao.UserDao;
import com.examportal.models.Exam;
import com.examportal.models.Question;
import com.examportal.models.User;
import com.examportal.utils.HttpUtils;
import com.examportal.utils.SessionManager;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.util.List;

public class ExamHandler implements HttpHandler {
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
        if (userId == null) {
            HttpUtils.sendError(exchange, 401, "Unauthorized");
            return;
        }

        try {
            User user = userDao.findById(userId);
            if (user == null) { HttpUtils.sendError(exchange, 401, "Unauthorized"); return; }

            // Route matching
            // /api/exams  GET/POST
            // /api/exams/{id}  GET/PUT/DELETE
            // /api/exams/{id}/questions  GET/POST
            // /api/exams/{id}/questions/{qid}  PUT/DELETE
            // /api/exams/active  GET

            String[] parts = path.split("/");
            // parts: ["", "api", "exams", ...]

            if (parts.length == 3 && "exams".equals(parts[2])) {
                if ("GET".equals(method)) handleListExams(exchange, user);
                else if ("POST".equals(method)) handleCreateExam(exchange, user);
                else HttpUtils.sendError(exchange, 405, "Method not allowed");

            } else if (parts.length == 4 && "exams".equals(parts[2])) {
                if ("active".equals(parts[3])) {
                    handleActiveExams(exchange);
                    return;
                }
                int examId = Integer.parseInt(parts[3]);
                if ("GET".equals(method)) handleGetExam(exchange, examId, user);
                else if ("PUT".equals(method)) handleUpdateExam(exchange, examId, user);
                else if ("DELETE".equals(method)) handleDeleteExam(exchange, examId, user);
                else HttpUtils.sendError(exchange, 405, "Method not allowed");

            } else if (parts.length == 5 && "exams".equals(parts[2]) && "questions".equals(parts[4])) {
                int examId = Integer.parseInt(parts[3]);
                if ("GET".equals(method)) handleListQuestions(exchange, examId, user);
                else if ("POST".equals(method)) handleCreateQuestion(exchange, examId, user);
                else HttpUtils.sendError(exchange, 405, "Method not allowed");

            } else if (parts.length == 6 && "exams".equals(parts[2]) && "questions".equals(parts[4])) {
                int examId = Integer.parseInt(parts[3]);
                int questionId = Integer.parseInt(parts[5]);
                if ("PUT".equals(method)) handleUpdateQuestion(exchange, examId, questionId, user);
                else if ("DELETE".equals(method)) handleDeleteQuestion(exchange, examId, questionId, user);
                else HttpUtils.sendError(exchange, 405, "Method not allowed");

            } else {
                HttpUtils.sendError(exchange, 404, "Not found");
            }

        } catch (NumberFormatException e) {
            HttpUtils.sendError(exchange, 400, "Invalid ID format");
        } catch (Exception e) {
            HttpUtils.sendError(exchange, 500, "Server error: " + e.getMessage());
        }
    }

    private void handleListExams(HttpExchange exchange, User user) throws Exception {
        List<Exam> exams = examDao.findAll();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < exams.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(exams.get(i).toJson());
        }
        sb.append("]");
        HttpUtils.sendSuccess(exchange, sb.toString());
    }

    private void handleActiveExams(HttpExchange exchange) throws Exception {
        List<Exam> exams = examDao.findActive();
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < exams.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(exams.get(i).toJson());
        }
        sb.append("]");
        HttpUtils.sendSuccess(exchange, sb.toString());
    }

    private void handleGetExam(HttpExchange exchange, int examId, User user) throws Exception {
        Exam exam = examDao.findById(examId);
        if (exam == null) { HttpUtils.sendError(exchange, 404, "Exam not found"); return; }
        HttpUtils.sendSuccess(exchange, exam.toJson());
    }

    private void handleCreateExam(HttpExchange exchange, User user) throws Exception {
        if (!"admin".equals(user.getRole())) {
            HttpUtils.sendError(exchange, 403, "Admin access required");
            return;
        }
        String body = HttpUtils.readRequestBody(exchange);
        Exam exam = new Exam();
        exam.setTitle(HttpUtils.parseJsonValue(body, "title"));
        exam.setDescription(HttpUtils.parseJsonValue(body, "description"));
        String duration = HttpUtils.parseJsonValue(body, "duration");
        String passingMarks = HttpUtils.parseJsonValue(body, "passingMarks");
        String status = HttpUtils.parseJsonValue(body, "status");
        exam.setDuration(duration != null ? Integer.parseInt(duration) : 60);
        exam.setPassingMarks(passingMarks != null ? Integer.parseInt(passingMarks) : 0);
        exam.setTotalMarks(0);
        exam.setCreatedBy(user.getId());
        exam.setStatus(status != null ? status : "draft");

        if (exam.getTitle() == null || exam.getTitle().isBlank()) {
            HttpUtils.sendError(exchange, 400, "Title is required");
            return;
        }

        int id = examDao.create(exam);
        exam = examDao.findById(id);
        HttpUtils.sendResponse(exchange, 201, "{\"success\":true,\"data\":" + exam.toJson() + "}");
    }

    private void handleUpdateExam(HttpExchange exchange, int examId, User user) throws Exception {
        if (!"admin".equals(user.getRole())) { HttpUtils.sendError(exchange, 403, "Admin access required"); return; }
        Exam exam = examDao.findById(examId);
        if (exam == null) { HttpUtils.sendError(exchange, 404, "Exam not found"); return; }

        String body = HttpUtils.readRequestBody(exchange);
        String title = HttpUtils.parseJsonValue(body, "title");
        String description = HttpUtils.parseJsonValue(body, "description");
        String duration = HttpUtils.parseJsonValue(body, "duration");
        String passingMarks = HttpUtils.parseJsonValue(body, "passingMarks");
        String status = HttpUtils.parseJsonValue(body, "status");

        if (title != null && !title.isBlank()) exam.setTitle(title);
        if (description != null) exam.setDescription(description);
        if (duration != null) exam.setDuration(Integer.parseInt(duration));
        if (passingMarks != null) exam.setPassingMarks(Integer.parseInt(passingMarks));
        if (status != null) exam.setStatus(status);

        examDao.update(exam);
        exam = examDao.findById(examId);
        HttpUtils.sendSuccess(exchange, exam.toJson());
    }

    private void handleDeleteExam(HttpExchange exchange, int examId, User user) throws Exception {
        if (!"admin".equals(user.getRole())) { HttpUtils.sendError(exchange, 403, "Admin access required"); return; }
        boolean deleted = examDao.delete(examId);
        if (!deleted) { HttpUtils.sendError(exchange, 404, "Exam not found"); return; }
        HttpUtils.sendSuccessMessage(exchange, "Exam deleted successfully");
    }

    private void handleListQuestions(HttpExchange exchange, int examId, User user) throws Exception {
        Exam exam = examDao.findById(examId);
        if (exam == null) { HttpUtils.sendError(exchange, 404, "Exam not found"); return; }
        List<Question> questions = questionDao.findByExamId(examId);
        boolean isAdmin = "admin".equals(user.getRole());
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < questions.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(questions.get(i).toJson(isAdmin));
        }
        sb.append("]");
        HttpUtils.sendSuccess(exchange, sb.toString());
    }

    private void handleCreateQuestion(HttpExchange exchange, int examId, User user) throws Exception {
        if (!"admin".equals(user.getRole())) { HttpUtils.sendError(exchange, 403, "Admin access required"); return; }
        Exam exam = examDao.findById(examId);
        if (exam == null) { HttpUtils.sendError(exchange, 404, "Exam not found"); return; }

        String body = HttpUtils.readRequestBody(exchange);
        Question q = new Question();
        q.setExamId(examId);
        q.setQuestionText(HttpUtils.parseJsonValue(body, "questionText"));
        q.setOptionA(HttpUtils.parseJsonValue(body, "optionA"));
        q.setOptionB(HttpUtils.parseJsonValue(body, "optionB"));
        q.setOptionC(HttpUtils.parseJsonValue(body, "optionC"));
        q.setOptionD(HttpUtils.parseJsonValue(body, "optionD"));
        q.setCorrectAnswer(HttpUtils.parseJsonValue(body, "correctAnswer"));
        String marks = HttpUtils.parseJsonValue(body, "marks");
        String order = HttpUtils.parseJsonValue(body, "orderNum");
        q.setMarks(marks != null ? Integer.parseInt(marks) : 1);
        q.setOrderNum(order != null ? Integer.parseInt(order) : questionDao.countByExamId(examId) + 1);

        int id = questionDao.create(q);
        examDao.updateTotalMarks(examId);
        q = questionDao.findById(id);
        HttpUtils.sendResponse(exchange, 201, "{\"success\":true,\"data\":" + q.toJson(true) + "}");
    }

    private void handleUpdateQuestion(HttpExchange exchange, int examId, int questionId, User user) throws Exception {
        if (!"admin".equals(user.getRole())) { HttpUtils.sendError(exchange, 403, "Admin access required"); return; }
        Question q = questionDao.findById(questionId);
        if (q == null || q.getExamId() != examId) { HttpUtils.sendError(exchange, 404, "Question not found"); return; }

        String body = HttpUtils.readRequestBody(exchange);
        String questionText = HttpUtils.parseJsonValue(body, "questionText");
        if (questionText != null) q.setQuestionText(questionText);
        String optA = HttpUtils.parseJsonValue(body, "optionA"); if (optA != null) q.setOptionA(optA);
        String optB = HttpUtils.parseJsonValue(body, "optionB"); if (optB != null) q.setOptionB(optB);
        String optC = HttpUtils.parseJsonValue(body, "optionC"); if (optC != null) q.setOptionC(optC);
        String optD = HttpUtils.parseJsonValue(body, "optionD"); if (optD != null) q.setOptionD(optD);
        String ca = HttpUtils.parseJsonValue(body, "correctAnswer"); if (ca != null) q.setCorrectAnswer(ca);
        String marks = HttpUtils.parseJsonValue(body, "marks"); if (marks != null) q.setMarks(Integer.parseInt(marks));

        questionDao.update(q);
        examDao.updateTotalMarks(examId);
        q = questionDao.findById(questionId);
        HttpUtils.sendSuccess(exchange, q.toJson(true));
    }

    private void handleDeleteQuestion(HttpExchange exchange, int examId, int questionId, User user) throws Exception {
        if (!"admin".equals(user.getRole())) { HttpUtils.sendError(exchange, 403, "Admin access required"); return; }
        boolean deleted = questionDao.delete(questionId);
        if (!deleted) { HttpUtils.sendError(exchange, 404, "Question not found"); return; }
        examDao.updateTotalMarks(examId);
        HttpUtils.sendSuccessMessage(exchange, "Question deleted");
    }
}
