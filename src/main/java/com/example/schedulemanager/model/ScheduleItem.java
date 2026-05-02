package com.example.schedulemanager.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class ScheduleItem {
    private Long id;
    private Long ownerUserId;
    private String ownerUsername;
    private String ownerDisplayName;
    private String ownerProfileIconColor;
    private Boolean ownerHasProfileImage;
    private LocalDate scheduleDate;
    private String priority;
    private String deviceType;
    private Boolean completed;
    private LocalDateTime completedAt;
    private Boolean messageShareable;
    private Long sourceScheduleItemId;
    private Long sourceOwnerUserId;
    private String sourceOwnerDisplayName;
    private String title;
    private LocalTime startTime;
    private LocalTime endTime;
    private String description;
    private Boolean sharedWithFriends;
    private Boolean joinable;
    private Integer recruitmentLimit;
    private Boolean editable;
    private Integer participantCount;
    private Integer remainingRecruitmentSlots;
    private Boolean recruitmentClosed;
    private Boolean joinedByCurrentUser;
    private List<FriendUser> participants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    public void setOwnerDisplayName(String ownerDisplayName) {
        this.ownerDisplayName = ownerDisplayName;
    }

    public String getOwnerProfileIconColor() {
        return ownerProfileIconColor;
    }

    public void setOwnerProfileIconColor(String ownerProfileIconColor) {
        this.ownerProfileIconColor = ownerProfileIconColor;
    }

    public Boolean getOwnerHasProfileImage() {
        return ownerHasProfileImage;
    }

    public void setOwnerHasProfileImage(Boolean ownerHasProfileImage) {
        this.ownerHasProfileImage = ownerHasProfileImage;
    }

    public LocalDate getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(LocalDate scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public Boolean getMessageShareable() {
        return messageShareable;
    }

    public void setMessageShareable(Boolean messageShareable) {
        this.messageShareable = messageShareable;
    }

    public Long getSourceScheduleItemId() {
        return sourceScheduleItemId;
    }

    public void setSourceScheduleItemId(Long sourceScheduleItemId) {
        this.sourceScheduleItemId = sourceScheduleItemId;
    }

    public Long getSourceOwnerUserId() {
        return sourceOwnerUserId;
    }

    public void setSourceOwnerUserId(Long sourceOwnerUserId) {
        this.sourceOwnerUserId = sourceOwnerUserId;
    }

    public String getSourceOwnerDisplayName() {
        return sourceOwnerDisplayName;
    }

    public void setSourceOwnerDisplayName(String sourceOwnerDisplayName) {
        this.sourceOwnerDisplayName = sourceOwnerDisplayName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    public LocalTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalTime endTime) {
        this.endTime = endTime;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getSharedWithFriends() {
        return sharedWithFriends;
    }

    public void setSharedWithFriends(Boolean sharedWithFriends) {
        this.sharedWithFriends = sharedWithFriends;
    }

    public Boolean getJoinable() {
        return joinable;
    }

    public void setJoinable(Boolean joinable) {
        this.joinable = joinable;
    }

    public Integer getRecruitmentLimit() {
        return recruitmentLimit;
    }

    public void setRecruitmentLimit(Integer recruitmentLimit) {
        this.recruitmentLimit = recruitmentLimit;
    }

    public Boolean getEditable() {
        return editable;
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    public Integer getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(Integer participantCount) {
        this.participantCount = participantCount;
    }

    public Integer getRemainingRecruitmentSlots() {
        return remainingRecruitmentSlots;
    }

    public void setRemainingRecruitmentSlots(Integer remainingRecruitmentSlots) {
        this.remainingRecruitmentSlots = remainingRecruitmentSlots;
    }

    public Boolean getRecruitmentClosed() {
        return recruitmentClosed;
    }

    public void setRecruitmentClosed(Boolean recruitmentClosed) {
        this.recruitmentClosed = recruitmentClosed;
    }

    public Boolean getJoinedByCurrentUser() {
        return joinedByCurrentUser;
    }

    public void setJoinedByCurrentUser(Boolean joinedByCurrentUser) {
        this.joinedByCurrentUser = joinedByCurrentUser;
    }

    public List<FriendUser> getParticipants() {
        return participants;
    }

    public void setParticipants(List<FriendUser> participants) {
        this.participants = participants;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
