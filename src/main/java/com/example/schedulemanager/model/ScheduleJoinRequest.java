package com.example.schedulemanager.model;

import java.time.LocalDateTime;

public class ScheduleJoinRequest {
    private Long id;
    private Long scheduleItemId;
    private Long requesterUserId;
    private String requesterUsername;
    private String requesterDisplayName;
    private String requesterProfileIconColor;
    private Boolean requesterHasProfileImage;
    private String comment;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getScheduleItemId() { return scheduleItemId; }
    public void setScheduleItemId(Long scheduleItemId) { this.scheduleItemId = scheduleItemId; }
    public Long getRequesterUserId() { return requesterUserId; }
    public void setRequesterUserId(Long requesterUserId) { this.requesterUserId = requesterUserId; }
    public String getRequesterUsername() { return requesterUsername; }
    public void setRequesterUsername(String requesterUsername) { this.requesterUsername = requesterUsername; }
    public String getRequesterDisplayName() { return requesterDisplayName; }
    public void setRequesterDisplayName(String requesterDisplayName) { this.requesterDisplayName = requesterDisplayName; }
    public String getRequesterProfileIconColor() { return requesterProfileIconColor; }
    public void setRequesterProfileIconColor(String requesterProfileIconColor) { this.requesterProfileIconColor = requesterProfileIconColor; }
    public Boolean getRequesterHasProfileImage() { return requesterHasProfileImage; }
    public void setRequesterHasProfileImage(Boolean requesterHasProfileImage) { this.requesterHasProfileImage = requesterHasProfileImage; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
