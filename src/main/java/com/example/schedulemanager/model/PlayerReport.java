package com.example.schedulemanager.model;

import java.time.LocalDateTime;

public class PlayerReport {
    private Long id;
    private Long reporterUserId;
    private String reporterUsername;
    private String reporterDisplayName;
    private Long targetUserId;
    private String targetUsername;
    private String targetDisplayName;
    private String sourceType;
    private Long sourceId;
    private String category;
    private String note;
    private String reason;
    private String status;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getReporterUserId() { return reporterUserId; }
    public void setReporterUserId(Long reporterUserId) { this.reporterUserId = reporterUserId; }
    public String getReporterUsername() { return reporterUsername; }
    public void setReporterUsername(String reporterUsername) { this.reporterUsername = reporterUsername; }
    public String getReporterDisplayName() { return reporterDisplayName; }
    public void setReporterDisplayName(String reporterDisplayName) { this.reporterDisplayName = reporterDisplayName; }
    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
    public String getTargetUsername() { return targetUsername; }
    public void setTargetUsername(String targetUsername) { this.targetUsername = targetUsername; }
    public String getTargetDisplayName() { return targetDisplayName; }
    public void setTargetDisplayName(String targetDisplayName) { this.targetDisplayName = targetDisplayName; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
