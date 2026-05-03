package com.example.schedulemanager.model;

import java.time.LocalDateTime;

public class DmConversation {
    private Long id;
    private Long userAId;
    private Long userBId;
    private String partnerUsername;
    private String partnerDisplayName;
    private String lastMessageBody;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserAId() {
        return userAId;
    }

    public void setUserAId(Long userAId) {
        this.userAId = userAId;
    }

    public Long getUserBId() {
        return userBId;
    }

    public void setUserBId(Long userBId) {
        this.userBId = userBId;
    }

    public String getPartnerUsername() {
        return partnerUsername;
    }

    public void setPartnerUsername(String partnerUsername) {
        this.partnerUsername = partnerUsername;
    }

    public String getPartnerDisplayName() {
        return partnerDisplayName;
    }

    public void setPartnerDisplayName(String partnerDisplayName) {
        this.partnerDisplayName = partnerDisplayName;
    }

    public String getLastMessageBody() {
        return lastMessageBody;
    }

    public void setLastMessageBody(String lastMessageBody) {
        this.lastMessageBody = lastMessageBody;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
