package com.example.schedulemanager.service;

import com.example.schedulemanager.dto.DmMessageSendRequest;
import com.example.schedulemanager.dto.DmStartRequest;
import com.example.schedulemanager.mapper.DirectMessageMapper;
import com.example.schedulemanager.mapper.DmConversationMapper;
import com.example.schedulemanager.mapper.UserMapper;
import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.model.DirectMessage;
import com.example.schedulemanager.model.DmConversation;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DmService {
    private final DmConversationMapper dmConversationMapper;
    private final DirectMessageMapper directMessageMapper;
    private final UserMapper userMapper;
    private final FriendshipService friendshipService;

    public DmService(
            DmConversationMapper dmConversationMapper,
            DirectMessageMapper directMessageMapper,
            UserMapper userMapper,
            FriendshipService friendshipService) {
        this.dmConversationMapper = dmConversationMapper;
        this.directMessageMapper = directMessageMapper;
        this.userMapper = userMapper;
        this.friendshipService = friendshipService;
    }

    @Transactional
    public DmConversation startOrGetConversation(Long currentUserId, DmStartRequest request) {
        if (request == null || request.getPartnerUserId() == null) {
            throw new IllegalArgumentException("相手ユーザーIDを指定してください。");
        }
        Long partnerUserId = request.getPartnerUserId();
        if (currentUserId.equals(partnerUserId)) {
            throw new IllegalArgumentException("自分自身との会話は作成できません。");
        }
        AppUser partner = userMapper.findById(partnerUserId);
        if (partner == null) {
            throw new NoSuchElementException("相手ユーザーが見つかりません。");
        }
        if (!friendshipService.areFriendsOrSelf(currentUserId, partnerUserId)) {
            throw new IllegalArgumentException("フレンド同士のみDMを開始できます。");
        }

        DmConversation existing = dmConversationMapper.findPair(currentUserId, partnerUserId);
        if (existing != null) {
            return existing;
        }

        DmConversation conversation = new DmConversation();
        long userA = Math.min(currentUserId, partnerUserId);
        long userB = Math.max(currentUserId, partnerUserId);
        conversation.setUserAId(userA);
        conversation.setUserBId(userB);
        dmConversationMapper.insert(conversation);
        return dmConversationMapper.findById(conversation.getId());
    }

    @Transactional(readOnly = true)
    public List<DmConversation> listConversations(Long currentUserId) {
        return dmConversationMapper.findByUser(currentUserId);
    }

    @Transactional(readOnly = true)
    public List<DirectMessage> listMessages(Long currentUserId, Long conversationId) {
        DmConversation conversation = requireAccessibleConversation(currentUserId, conversationId);
        return directMessageMapper.findByConversationId(conversation.getId());
    }

    @Transactional
    public DirectMessage sendMessage(Long currentUserId, Long conversationId, DmMessageSendRequest request) {
        DmConversation conversation = requireAccessibleConversation(currentUserId, conversationId);
        String body = normalize(request == null ? null : request.getBody());
        if (body == null) {
            throw new IllegalArgumentException("メッセージ本文を入力してください。");
        }
        if (body.length() > 1000) {
            throw new IllegalArgumentException("メッセージ本文は1000文字以内で入力してください。");
        }

        Long recipientUserId = conversation.getUserAId().equals(currentUserId)
                ? conversation.getUserBId()
                : conversation.getUserAId();

        DirectMessage message = new DirectMessage();
        message.setConversationId(conversationId);
        message.setSenderUserId(currentUserId);
        message.setRecipientUserId(recipientUserId);
        message.setBody(body);
        message.setRead(false);
        directMessageMapper.insert(message);
        return directMessageMapper.findByConversationId(conversationId).stream()
                .reduce((a, b) -> b)
                .orElseThrow();
    }

    private DmConversation requireAccessibleConversation(Long currentUserId, Long conversationId) {
        if (conversationId == null) {
            throw new IllegalArgumentException("会話IDを指定してください。");
        }
        DmConversation conversation = dmConversationMapper.findById(conversationId);
        if (conversation == null) {
            throw new NoSuchElementException("会話が見つかりません。");
        }
        boolean member = currentUserId.equals(conversation.getUserAId()) || currentUserId.equals(conversation.getUserBId());
        if (!member) {
            throw new IllegalArgumentException("この会話にはアクセスできません。");
        }
        return conversation;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
