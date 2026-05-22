package com.examportal.models;

public class Exam {
    private int id;
    private String title;
    private String description;
    private int duration; // in minutes
    private int totalMarks;
    private int passingMarks;
    private int createdBy;
    private String createdByName;
    private String status; // "active", "inactive", "draft"
    private String createdAt;
    private int questionCount;

    public Exam() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public int getTotalMarks() { return totalMarks; }
    public void setTotalMarks(int totalMarks) { this.totalMarks = totalMarks; }
    public int getPassingMarks() { return passingMarks; }
    public void setPassingMarks(int passingMarks) { this.passingMarks = passingMarks; }
    public int getCreatedBy() { return createdBy; }
    public void setCreatedBy(int createdBy) { this.createdBy = createdBy; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public int getQuestionCount() { return questionCount; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }

    public String toJson() {
        return String.format(
            "{\"id\":%d,\"title\":\"%s\",\"description\":\"%s\",\"duration\":%d,\"totalMarks\":%d,\"passingMarks\":%d,\"createdBy\":%d,\"createdByName\":\"%s\",\"status\":\"%s\",\"createdAt\":\"%s\",\"questionCount\":%d}",
            id, escapeJson(title), escapeJson(description), duration, totalMarks, passingMarks,
            createdBy, escapeJson(createdByName), escapeJson(status), escapeJson(createdAt), questionCount
        );
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
