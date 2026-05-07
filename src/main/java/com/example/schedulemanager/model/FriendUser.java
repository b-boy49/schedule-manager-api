package com.example.schedulemanager.model;

public class FriendUser {
    private Long id;
    private String username;
    private String displayName;
    private String profileIconColor;
    private Boolean hasProfileImage;
    private Integer totalPoints;
    private Integer level;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getTotalPoints() {
        return totalPoints;
    }

    public void setTotalPoints(Integer totalPoints) {
        this.totalPoints = totalPoints;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public String getProfileIconColor() {
        return profileIconColor;
    }

    public void setProfileIconColor(String profileIconColor) {
        this.profileIconColor = profileIconColor;
    }

    public Boolean getHasProfileImage() {
        return hasProfileImage;
    }

    public void setHasProfileImage(Boolean hasProfileImage) {
        this.hasProfileImage = hasProfileImage;
    }
}
