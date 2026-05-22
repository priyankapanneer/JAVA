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
            "INSERT INTO results (user_id, exam_id, score, total_marks, passing_marks, time_taken, passed, correct_answers, total_questions, cheating_flag, cheat_count, question_times) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
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
        ps.setInt(10, result.isCheatingFlag() ? 1 : 0);
        ps.setInt(11, result.getCheatCount());
        ps.setString(12, result.getQuestionTimes() != null ? result.getQuestionTimes() : "{}");
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

    /** Returns leaderboard rows: each row is {userId, username, fullName, avgScore, totalAttempts, totalPassed, bestScore} */
    public List<String> getLeaderboard() throws SQLException {
        String sql = """
            SELECT u.id, u.username, u.full_name,
                   COUNT(r.id) as total_attempts,
                   SUM(r.passed) as total_passed,
                   ROUND(AVG(CASE WHEN r.total_marks>0 THEN CAST(r.score AS REAL)/r.total_marks*100 ELSE 0 END),1) as avg_score,
                   MAX(CASE WHEN r.total_marks>0 THEN CAST(r.score AS REAL)/r.total_marks*100 ELSE 0 END) as best_score
            FROM users u
            JOIN results r ON r.user_id = u.id
            WHERE u.role='student'
            GROUP BY u.id
            ORDER BY avg_score DESC, total_passed DESC
            LIMIT 20
        """;
        List<String> rows = new ArrayList<>();
        ResultSet rs = conn().createStatement().executeQuery(sql);
        int rank = 1;
        while (rs.next()) {
            rows.add(String.format(
                "{\"rank\":%d,\"userId\":%d,\"username\":\"%s\",\"fullName\":\"%s\",\"totalAttempts\":%d,\"totalPassed\":%d,\"avgScore\":%.1f,\"bestScore\":%.1f}",
                rank++,
                rs.getInt("id"),
                escape(rs.getString("username")),
                escape(rs.getString("full_name")),
                rs.getInt("total_attempts"),
                rs.getInt("total_passed"),
                rs.getDouble("avg_score"),
                rs.getDouble("best_score")
            ));
        }
        return rows;
    }

    /** Returns per-exam analytics for admin dashboard */
    public List<String> getAnalytics() throws SQLException {
        String sql = """
            SELECT e.title,
                   COUNT(r.id) as total_attempts,
                   SUM(r.passed) as total_passed,
                   ROUND(AVG(CASE WHEN r.total_marks>0 THEN CAST(r.score AS REAL)/r.total_marks*100 ELSE 0 END),1) as avg_score,
                   ROUND(AVG(r.time_taken),0) as avg_time
            FROM exams e
            LEFT JOIN results r ON r.exam_id = e.id
            GROUP BY e.id
            ORDER BY total_attempts DESC
            LIMIT 10
        """;
        List<String> rows = new ArrayList<>();
        ResultSet rs = conn().createStatement().executeQuery(sql);
        while (rs.next()) {
            int attempts = rs.getInt("total_attempts");
            int passed = rs.getInt("total_passed");
            rows.add(String.format(
                "{\"title\":\"%s\",\"totalAttempts\":%d,\"totalPassed\":%d,\"totalFailed\":%d,\"avgScore\":%.1f,\"avgTime\":%d}",
                escape(rs.getString("title")),
                attempts, passed, attempts - passed,
                rs.getDouble("avg_score"),
                rs.getInt("avg_time")
            ));
        }
        return rows;
    }

    private String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
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
        try { r.setCheatingFlag(rs.getInt("cheating_flag") == 1); } catch (Exception ignored) {}
        try { r.setCheatCount(rs.getInt("cheat_count")); } catch (Exception ignored) {}
        try { r.setQuestionTimes(rs.getString("question_times")); } catch (Exception ignored) {}
        return r;
    }
}
