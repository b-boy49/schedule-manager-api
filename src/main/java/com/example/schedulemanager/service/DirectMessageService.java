package com.example.schedulemanager.service;

import com.example.schedulemanager.dto.DirectMessageSendRequest;
import com.example.schedulemanager.mapper.DirectMessageMapper;
import com.example.schedulemanager.mapper.UserMapper;
import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.model.DirectMessage;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DirectMessageService {
    private final DirectMessageMapper directMessageMapper;
    private final UserMapper userMapper;
    private final FriendshipService friendshipService;

    public DirectMessageService(
            DirectMessageMapper directMessageMapper,
            UserMapper userMapper,
            FriendshipService friendshipService) {
        this.directMessageMapper = directMessageMapper;
        this.userMapper = userMapper;
        this.friendshipService = friendshipService;
    }

    @Transactional(readOnly = true)
    public List<DirectMessage> listRecentMessages(Long userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 200));
        return directMessageMapper.findRecentByUserId(userId, safeLimit);
    }

    @Transactional
    public DirectMessage send(Long senderUserId, DirectMessageSendRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("メッセージ送信内容が不正です。");
        }
        String recipientUsername = normalize(request.getRecipientUsername());
        String body = normalize(request.getBody());
        if (recipientUsername == null) {
            throw new IllegalArgumentException("送信先ユーザー名を指定してください。");
        }
        if (body == null) {
            throw new IllegalArgumentException("メッセージ本文を入力してください。");
        }
        if (body.length() > 1000) {
            throw new IllegalArgumentException("メッセージ本文は1000文字以内で入力してください。");
        }

        AppUser recipient = userMapper.findByUsername(recipientUsername);
        if (recipient == null) {
            throw new NoSuchElementException("送信先ユーザーが見つかりません。");
        }
        if (senderUserId.equals(recipient.getId())) {
            throw new IllegalArgumentException("自分自身には送信できません。");
        }
        if (!friendshipService.areFriendsOrSelf(senderUserId, recipient.getId())) {
            throw new IllegalArgumentException("フレンド同士のみメッセージ送信できます。");
        }

        DirectMessage message = new DirectMessage();
        message.setSenderUserId(senderUserId);
        message.setRecipientUserId(recipient.getId());
        message.setBody(body);
        message.setRelatedScheduleItemId(request.getRelatedScheduleItemId());
        directMessageMapper.insert(message);
        return directMessageMapper.findRecentByUserId(senderUserId, 1).get(0);
    }

    @Transactional
    public void markReceivedAsRead(Long userId) {
        directMessageMapper.markAllReceivedAsRead(userId);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
