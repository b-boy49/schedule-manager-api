package com.example.schedulemanager.dto;

public class BoardRecruitmentCreateRequest {
    private String body;
    private String scheduleDate;
    private String startTime;
    private String rankBand;
    private Integer recruitmentLimit;

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getScheduleDate() {
        return scheduleDate;
    }

    public void setScheduleDate(String scheduleDate) {
        this.scheduleDate = scheduleDate;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public Integer getRecruitmentLimit() {
        return recruitmentLimit;
    }

    public void setRecruitmentLimit(Integer recruitmentLimit) {
        this.recruitmentLimit = recruitmentLimit;
    }

    public String getRankBand() {
        return rankBand;
    }

    public void setRankBand(String rankBand) {
        this.rankBand = rankBand;
    }
}
