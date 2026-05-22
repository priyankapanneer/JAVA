package com.examportal.models;

public class Result {
    private int id;
    private int userId;
    private int examId;
    private String username;
    private String fullName;
    private String examTitle;
    private int score;
    private int totalMarks;
    private int passingMarks;
    private int timeTaken; // seconds
    private boolean passed;
    private String submittedAt;
    private int correctAnswers;
    private int totalQuestions;
    private boolean cheatingFlag;
    private int cheatCount;
    private String questionTimes;

    public Result() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public int getExamId() { return examId; }
    public void setExamId(int examId) { this.examId = examId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getExamTitle() { return examTitle; }
    public void setExamTitle(String examTitle) { this.examTitle = examTitle; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public int getTotalMarks() { return totalMarks; }
    public void setTotalMarks(int totalMarks) { this.totalMarks = totalMarks; }
    public int getPassingMarks() { return passingMarks; }
    public void setPassingMarks(int passingMarks) { this.passingMarks = passingMarks; }
    public int getTimeTaken() { return timeTaken; }
    public void setTimeTaken(int timeTaken) { this.timeTaken = timeTaken; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public String getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(String submittedAt) { this.submittedAt = submittedAt; }
    public int getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(int correctAnswers) { this.correctAnswers = correctAnswers; }
    public int getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(int totalQuestions) { this.totalQuestions = totalQuestions; }
    public boolean isCheatingFlag() { return cheatingFlag; }
    public void setCheatingFlag(boolean cheatingFlag) { this.cheatingFlag = cheatingFlag; }
    public int getCheatCount() { return cheatCount; }
    public void setCheatCount(int cheatCount) { this.cheatCount = cheatCount; }
    public String getQuestionTimes() { return questionTimes; }
    public void setQuestionTimes(String questionTimes) { this.questionTimes = questionTimes; }

    public String toJson() {
        double percentage = totalMarks > 0 ? (double) score / totalMarks * 100 : 0;
        return String.format(
            "{\"id\":%d,\"userId\":%d,\"examId\":%d,\"username\":\"%s\",\"fullName\":\"%s\",\"examTitle\":\"%s\"," +
            "\"score\":%d,\"totalMarks\":%d,\"passingMarks\":%d,\"timeTaken\":%d,\"passed\":%b," +
            "\"submittedAt\":\"%s\",\"correctAnswers\":%d,\"totalQuestions\":%d,\"percentage\":%.2f," +
            "\"cheatingFlag\":%b,\"cheatCount\":%d,\"questionTimes\":%s}",
            id, userId, examId, escapeJson(username), escapeJson(fullName), escapeJson(examTitle),
            score, totalMarks, passingMarks, timeTaken, passed, escapeJson(submittedAt),
            correctAnswers, totalQuestions, percentage,
            cheatingFlag, cheatCount,
            (questionTimes != null && !questionTimes.isEmpty()) ? questionTimes : "{}"
        );
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
