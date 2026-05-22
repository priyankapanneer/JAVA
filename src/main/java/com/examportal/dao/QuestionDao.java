package com.examportal.dao;

import com.examportal.models.Question;
import com.examportal.utils.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionDao {
    private Connection conn() { return DatabaseManager.getInstance().getConnection(); }

    public List<Question> findByExamId(int examId) throws SQLException {
        List<Question> questions = new ArrayList<>();
        PreparedStatement ps = conn().prepareStatement(
            "SELECT * FROM questions WHERE exam_id=? ORDER BY order_num"
        );
        ps.setInt(1, examId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) questions.add(mapRow(rs));
        return questions;
    }

    public Question findById(int id) throws SQLException {
        PreparedStatement ps = conn().prepareStatement("SELECT * FROM questions WHERE id=?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return mapRow(rs);
        return null;
    }

    public int create(Question question) throws SQLException {
        PreparedStatement ps = conn().prepareStatement(
            "INSERT INTO questions (exam_id, question_text, option_a, option_b, option_c, option_d, correct_answer, marks, order_num) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS
        );
        ps.setInt(1, question.getExamId());
        ps.setString(2, question.getQuestionText());
        ps.setString(3, question.getOptionA());
        ps.setString(4, question.getOptionB());
        ps.setString(5, question.getOptionC());
        ps.setString(6, question.getOptionD());
        ps.setString(7, question.getCorrectAnswer());
        ps.setInt(8, question.getMarks() > 0 ? question.getMarks() : 1);
        ps.setInt(9, question.getOrderNum());
        ps.executeUpdate();
        ResultSet keys = ps.getGeneratedKeys();
        return keys.next() ? keys.getInt(1) : -1;
    }

    public void update(Question question) throws SQLException {
        PreparedStatement ps = conn().prepareStatement(
            "UPDATE questions SET question_text=?, option_a=?, option_b=?, option_c=?, option_d=?, correct_answer=?, marks=?, order_num=? WHERE id=?"
        );
        ps.setString(1, question.getQuestionText());
        ps.setString(2, question.getOptionA());
        ps.setString(3, question.getOptionB());
        ps.setString(4, question.getOptionC());
        ps.setString(5, question.getOptionD());
        ps.setString(6, question.getCorrectAnswer());
        ps.setInt(7, question.getMarks());
        ps.setInt(8, question.getOrderNum());
        ps.setInt(9, question.getId());
        ps.executeUpdate();
    }

    public boolean delete(int questionId) throws SQLException {
        PreparedStatement ps = conn().prepareStatement("DELETE FROM questions WHERE id=?");
        ps.setInt(1, questionId);
        return ps.executeUpdate() > 0;
    }

    public int countByExamId(int examId) throws SQLException {
        PreparedStatement ps = conn().prepareStatement("SELECT COUNT(*) FROM questions WHERE exam_id=?");
        ps.setInt(1, examId);
        ResultSet rs = ps.executeQuery();
        return rs.next() ? rs.getInt(1) : 0;
    }

    private Question mapRow(ResultSet rs) throws SQLException {
        Question q = new Question();
        q.setId(rs.getInt("id"));
        q.setExamId(rs.getInt("exam_id"));
        q.setQuestionText(rs.getString("question_text"));
        q.setOptionA(rs.getString("option_a"));
        q.setOptionB(rs.getString("option_b"));
        q.setOptionC(rs.getString("option_c"));
        q.setOptionD(rs.getString("option_d"));
        q.setCorrectAnswer(rs.getString("correct_answer"));
        q.setMarks(rs.getInt("marks"));
        q.setOrderNum(rs.getInt("order_num"));
        return q;
    }
}
