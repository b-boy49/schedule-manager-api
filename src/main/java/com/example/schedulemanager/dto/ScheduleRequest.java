package com.example.schedulemanager.dto;

public class ScheduleRequest {
    private String scheduleDate;
    private String priority;
    private String deviceType;
    private String title;
    private String startTime;
    private String endTime;
    private String description;
    private Boolean sharedWithFriends;
    private Boolean joinable;
    private Boolean messageShareable;
    private Integer recruitmentLimit;

    public String getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(String scheduleDate) {
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
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

    public Boolean getMessageShareable() {
        return messageShareable;
    }

    public void setMessageShareable(Boolean messageShareable) {
        this.messageShareable = messageShareable;
    }

    public Integer getRecruitmentLimit() {
        return recruitmentLimit;
    }

    public void setRecruitmentLimit(Integer recruitmentLimit) {
        this.recruitmentLimit = recruitmentLimit;
    }
}
