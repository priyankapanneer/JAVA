package com.examportal.dao;

import com.examportal.models.Result;
import com.examportal.utils.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResultDao {
    private Connection conn() { return DatabaseManager.getInstance().getConnection(); }

    public int create(Result result) throws SQLException {
        PreparedStatement ps = conn().prepareStatement(
            "INSERT INTO results (user_id, exam_id, score, total_marks, passing_marks, time_taken, passed, correct_answers, total_questions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        );
        ps.setInt(1, result.getUserId());
        ps.setInt(2, result.getExamId());
        ps.setInt(3, result.getScore());
        ps.setInt(4, result.getTotalMarks());
        ps.setInt(5, result.getPassingMarks());
        ps.setInt(6, result.getTimeTaken());
        ps.setInt(7, result.isPassed() ? 1 : 0);
        ps.setInt(8, result.getCorrectAnswers());
        ps.setInt(9, result.getTotalQuestions());
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        return keys.next() ? keys.getInt(1) : -1;
    }

    public void saveAnswer(int resultId, int questionId, String selectedAnswer, boolean isCorrect) throws SQLException {
        PreparedStatement ps = conn().prepareStatement(
            "INSERT INTO answers (result_id, question_id, selected_answer, is_correct) VALUES (?, ?, ?, ?)"
        );
        ps.setInt(1, resultId);
        ps.setInt(2, questionId);
        ps.setString(3, selectedAnswer);
        ps.setInt(4, isCorrect ? 1 : 0);
        ps.executeUpdate();
    }

    public List<Result> findByUserId(int userId) throws SQLException {
        List<Result> results = new ArrayList<>();
        String sql = """
            SELECT r.*, u.username, u.full_name, e.title as exam_title
            FROM results r
            JOIN users u ON r.user_id = u.id
            JOIN exams e ON r.exam_id = e.id
            WHERE r.user_id = ?
            ORDER BY r.submitted_at DESC
        """;
        PreparedStatement ps = conn().prepareStatement(sql);
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) results.add(mapRow(rs));
        return results;
    }

    public List<Result> findByExamId(int examId) throws SQLException {
        List<Result> results = new ArrayList<>();
        String sql = """
            SELECT r.*, u.username, u.full_name, e.title as exam_title
            FROM results r
            JOIN users u ON r.user_id = u.id
            JOIN exams e ON r.exam_id = e.id
            WHERE r.exam_id = ?
            ORDER BY r.score DESC, r.submitted_at ASC
        """;
        PreparedStatement ps = conn().prepareStatement(sql);
        ps.setInt(1, examId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) results.add(mapRow(rs));
        return results;
    }

    public List<Result> findAll() throws SQLException {
        List<Result> results = new ArrayList<>();
        String sql = """
            SELECT r.*, u.username, u.full_name, e.title as exam_title
            FROM results r
            JOIN users u ON r.user_id = u.id
            JOIN exams e ON r.exam_id = e.id
            ORDER BY r.submitted_at DESC
        """;
        ResultSet rs = conn().createStatement().executeQuery(sql);
        while (rs.next()) results.add(mapRow(rs));
        return results;
    }

    public Result findById(int id) throws SQLException {
        String sql = """
            SELECT r.*, u.username, u.full_name, e.title as exam_title
            FROM results r
            JOIN users u ON r.user_id = u.id
            JOIN exams e ON r.exam_id = e.id
            WHERE r.id = ?
        """;
        PreparedStatement ps = conn().prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapRow(rs);
        return null;
    }

    public boolean hasAttempted(int userId, int examId) throws SQLException {
        PreparedStatement ps = conn().prepareStatement(
            "SELECT COUNT(*) FROM results WHERE user_id=? AND exam_id=?"
        );
        ps.setInt(1, userId);
        ps.setInt(2, examId);
        ResultSet rs = ps.executeQuery();
        return rs.next() && rs.getInt(1) > 0;
    }

    public int countTotalStudents() throws SQLException {
        ResultSet rs = conn().createStatement().executeQuery("SELECT COUNT(*) FROM users WHERE role='student'");
        return rs.next() ? rs.getInt(1) : 0;
    }

    public int countTotalExams() throws SQLException {
        ResultSet rs = conn().createStatement().executeQuery("SELECT COUNT(*) FROM exams");
        return rs.next() ? rs.getInt(1) : 0;
    }

    public int countTotalAttempts() throws SQLException {
        ResultSet rs = conn().createStatement().executeQuery("SELECT COUNT(*) FROM results");
        return rs.next() ? rs.getInt(1) : 0;
    }

    private Result mapRow(ResultSet rs) throws SQLException {
        Result r = new Result();
        r.setId(rs.getInt("id"));
        r.setUserId(rs.getInt("user_id"));
        r.setExamId(rs.getInt("exam_id"));
        r.setUsername(rs.getString("username"));
        r.setFullName(rs.getString("full_name"));
        r.setExamTitle(rs.getString("exam_title"));
        r.setScore(rs.getInt("score"));
        r.setTotalMarks(rs.getInt("total_marks"));
        r.setPassingMarks(rs.getInt("passing_marks"));
        r.setTimeTaken(rs.getInt("time_taken"));
        r.setPassed(rs.getInt("passed") == 1);
        r.setSubmittedAt(rs.getString("submitted_at"));
        r.setCorrectAnswers(rs.getInt("correct_answers"));
        r.setTotalQuestions(rs.getInt("total_questions"));
        return r;
    }
}
