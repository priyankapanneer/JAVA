package com.examportal.dao;

import com.examportal.models.Exam;
import com.examportal.utils.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamDao {
    private Connection conn() { return DatabaseManager.getInstance().getConnection(); }

    public List<Exam> findAll() throws SQLException {
        List<Exam> exams = new ArrayList<>();
        String sql = """
            SELECT e.*, u.full_name as creator_name,
                   (SELECT COUNT(*) FROM questions q WHERE q.exam_id = e.id) as qcount
            FROM exams e
            JOIN users u ON e.created_by = u.id
            ORDER BY e.created_at DESC
        """;
        ResultSet rs = conn().createStatement().executeQuery(sql);
        while (rs.next()) exams.add(mapRow(rs));
        return exams;
    }

    public List<Exam> findActive() throws SQLException {
        List<Exam> exams = new ArrayList<>();
        String sql = """
            SELECT e.*, u.full_name as creator_name,
                   (SELECT COUNT(*) FROM questions q WHERE q.exam_id = e.id) as qcount
            FROM exams e
            JOIN users u ON e.created_by = u.id
            WHERE e.status='active'
            ORDER BY e.created_at DESC
        """;
        ResultSet rs = conn().createStatement().executeQuery(sql);
        while (rs.next()) exams.add(mapRow(rs));
        return exams;
    }

    public Exam findById(int id) throws SQLException {
        String sql = """
            SELECT e.*, u.full_name as creator_name,
                   (SELECT COUNT(*) FROM questions q WHERE q.exam_id = e.id) as qcount
            FROM exams e
            JOIN users u ON e.created_by = u.id
            WHERE e.id = ?
        """;
        PreparedStatement ps = conn().prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapRow(rs);
        return null;
    }

    public int create(Exam exam) throws SQLException {
        PreparedStatement ps = conn().prepareStatement(
            "INSERT INTO exams (title, description, duration, total_marks, passing_marks, created_by, status) VALUES (?, ?, ?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        );
        ps.setString(1, exam.getTitle());
        ps.setString(2, exam.getDescription());
        ps.setInt(3, exam.getDuration());
        ps.setInt(4, exam.getTotalMarks());
        ps.setInt(5, exam.getPassingMarks());
        ps.setInt(6, exam.getCreatedBy());
        ps.setString(7, exam.getStatus() != null ? exam.getStatus() : "draft");
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        return keys.next() ? keys.getInt(1) : -1;
    }

    public void update(Exam exam) throws SQLException {
        PreparedStatement ps = conn().prepareStatement(
            "UPDATE exams SET title=?, description=?, duration=?, total_marks=?, passing_marks=?, status=? WHERE id=?"
        );
        ps.setString(1, exam.getTitle());
        ps.setString(2, exam.getDescription());
        ps.setInt(3, exam.getDuration());
        ps.setInt(4, exam.getTotalMarks());
        ps.setInt(5, exam.getPassingMarks());
        ps.setString(6, exam.getStatus());
        ps.setInt(7, exam.getId());
        ps.executeUpdate();
    }

    public void updateStatus(int examId, String status) throws SQLException {
        PreparedStatement ps = conn().prepareStatement("UPDATE exams SET status=? WHERE id=?");
        ps.setString(1, status);
        ps.setInt(2, examId);
        ps.executeUpdate();
    }

    public void updateTotalMarks(int examId) throws SQLException {
        PreparedStatement ps = conn().prepareStatement(
            "UPDATE exams SET total_marks=(SELECT COALESCE(SUM(marks),0) FROM questions WHERE exam_id=?) WHERE id=?"
        );
        ps.setInt(1, examId);
        ps.setInt(2, examId);
        ps.executeUpdate();
    }

    public boolean delete(int examId) throws SQLException {
        PreparedStatement ps = conn().prepareStatement("DELETE FROM exams WHERE id=?");
        ps.setInt(1, examId);
        return ps.executeUpdate() > 0;
    }

    private Exam mapRow(ResultSet rs) throws SQLException {
        Exam exam = new Exam();
        exam.setId(rs.getInt("id"));
        exam.setTitle(rs.getString("title"));
        exam.setDescription(rs.getString("description"));
        exam.setDuration(rs.getInt("duration"));
        exam.setTotalMarks(rs.getInt("total_marks"));
        exam.setPassingMarks(rs.getInt("passing_marks"));
        exam.setCreatedBy(rs.getInt("created_by"));
        exam.setCreatedByName(rs.getString("creator_name"));
        exam.setStatus(rs.getString("status"));
        exam.setCreatedAt(rs.getString("created_at"));
        exam.setQuestionCount(rs.getInt("qcount"));
        return exam;
    }
}
