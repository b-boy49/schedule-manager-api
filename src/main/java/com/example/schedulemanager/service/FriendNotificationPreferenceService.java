package com.example.schedulemanager.service;

import com.example.schedulemanager.mapper.FriendNotificationPreferenceMapper;
import com.example.schedulemanager.mapper.FriendshipMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendNotificationPreferenceService {
    private final FriendNotificationPreferenceMapper preferenceMapper;
    private final FriendshipMapper friendshipMapper;

    public FriendNotificationPreferenceService(
            FriendNotificationPreferenceMapper preferenceMapper,
            FriendshipMapper friendshipMapper) {
        this.preferenceMapper = preferenceMapper;
        this.friendshipMapper = friendshipMapper;
    }

    @Transactional(readOnly = true)
    public List<Long> listEnabledFriendUserIds(Long userId) {
        return preferenceMapper.findEnabledFriendUserIds(userId);
    }

    @Transactional
    public void setPreference(Long userId, Long friendUserId, boolean enabled) {
        if (userId == null || friendUserId == null) {
            throw new IllegalArgumentException("ユーザーIDが不正です。");
        }
        if (userId.equals(friendUserId)) {
            throw new IllegalArgumentException("自分自身には設定できません。");
        }
        if (!friendshipMapper.existsAcceptedFriendship(userId, friendUserId)) {
            throw new IllegalArgumentException("フレンド関係が必要です。");
        }
        preferenceMapper.deletePreference(userId, friendUserId);
        if (enabled) {
            preferenceMapper.insertEnabled(userId, friendUserId);
        }
    }

    @Transactional(readOnly = true)
    public List<Long> findRecipientsByActor(Long actorUserId) {
        if (actorUserId == null) {
            return List.of();
        }
        return preferenceMapper.findRecipientUserIdsByActor(actorUserId);
    }
}
