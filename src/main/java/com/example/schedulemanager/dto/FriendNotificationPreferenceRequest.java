package com.example.schedulemanager.dto;

public class FriendNotificationPreferenceRequest {
    private Long friendUserId;
    private Boolean enabled;

    public Long getFriendUserId() {
        return friendUserId;
    }

    public void setFriendUserId(Long friendUserId) {
        this.friendUserId = friendUserId;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
