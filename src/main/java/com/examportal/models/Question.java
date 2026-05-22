package com.examportal.models;

public class Question {
    private int id;
    private int examId;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String correctAnswer; // "A", "B", "C", or "D"
    private int marks;
    private int orderNum;

    public Question() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getExamId() { return examId; }
    public void setExamId(int examId) { this.examId = examId; }
    public String getQuestionText() { return questionText; }
    public void setQuestionText(String questionText) { this.questionText = questionText; }
    public String getOptionA() { return optionA; }
    public void setOptionA(String optionA) { this.optionA = optionA; }
    public String getOptionB() { return optionB; }
    public void setOptionB(String optionB) { this.optionB = optionB; }
    public String getOptionC() { return optionC; }
    public void setOptionC(String optionC) { this.optionC = optionC; }
    public String getOptionD() { return optionD; }
    public void setOptionD(String optionD) { this.optionD = optionD; }
    public String getCorrectAnswer() { return correctAnswer; }
    public void setCorrectAnswer(String correctAnswer) { this.correctAnswer = correctAnswer; }
    public int getMarks() { return marks; }
    public void setMarks(int marks) { this.marks = marks; }
    public int getOrderNum() { return orderNum; }
    public void setOrderNum(int orderNum) { this.orderNum = orderNum; }

    public String toJson(boolean includeAnswer) {
        return String.format(
            "{\"id\":%d,\"examId\":%d,\"questionText\":\"%s\",\"optionA\":\"%s\",\"optionB\":\"%s\",\"optionC\":\"%s\",\"optionD\":\"%s\"%s,\"marks\":%d,\"orderNum\":%d}",
            id, examId, escapeJson(questionText), escapeJson(optionA), escapeJson(optionB),
            escapeJson(optionC), escapeJson(optionD),
            includeAnswer ? ",\"correctAnswer\":\"" + escapeJson(correctAnswer) + "\"" : "",
            marks, orderNum
        );
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
