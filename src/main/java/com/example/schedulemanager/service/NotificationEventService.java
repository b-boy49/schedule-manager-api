package com.example.schedulemanager.service;

import com.example.schedulemanager.mapper.NotificationEventMapper;
import com.example.schedulemanager.mapper.UserMapper;
import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.model.NotificationEvent;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationEventService {
    private final NotificationEventMapper notificationEventMapper;
    private final UserMapper userMapper;
    private final WebPushService webPushService;

    public NotificationEventService(
            NotificationEventMapper notificationEventMapper,
            UserMapper userMapper,
            WebPushService webPushService) {
        this.notificationEventMapper = notificationEventMapper;
        this.userMapper = userMapper;
        this.webPushService = webPushService;
    }

    @Transactional
    public void publish(Long recipientUserId, Long actorUserId, String eventType, String title, String body) {
        if (recipientUserId == null || title == null || body == null || eventType == null) {
            return;
        }
        NotificationEvent event = new NotificationEvent();
        event.setRecipientUserId(recipientUserId);
        event.setActorUserId(actorUserId);
        event.setEventType(eventType);
        event.setTitle(trimTo(title, 200));
        event.setBody(trimTo(body, 1000));
        notificationEventMapper.insert(event);
        webPushService.pushToUser(recipientUserId, event.getTitle(), event.getBody(), "/calendar");
    }

    @Transactional(readOnly = true)
    public List<NotificationEvent> listSince(String username, Long sinceId, Integer limit) {
        AppUser user = userMapper.findByUsername(username);
        if (user == null) {
            throw new NoSuchElementException("ログインユーザーが見つかりません。");
        }
        long safeSinceId = sinceId == null ? 0L : Math.max(0L, sinceId);
        int safeLimit = limit == null ? 30 : Math.max(1, Math.min(limit, 100));
        return notificationEventMapper.findSince(user.getId(), safeSinceId, safeLimit);
    }

    private String trimTo(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }
}
