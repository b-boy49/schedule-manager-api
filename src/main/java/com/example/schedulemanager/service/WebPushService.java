package com.example.schedulemanager.service;

import com.example.schedulemanager.mapper.PushSubscriptionMapper;
import com.example.schedulemanager.mapper.UserMapper;
import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.model.PushSubscription;
import java.security.GeneralSecurityException;
import java.security.Security;
import java.util.List;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WebPushService {
    private final PushSubscriptionMapper pushSubscriptionMapper;
    private final UserMapper userMapper;

    @Value("${app.push.vapid.public-key:}")
    private String vapidPublicKey;

    @Value("${app.push.vapid.private-key:}")
    private String vapidPrivateKey;

    @Value("${app.push.vapid.subject:mailto:admin@example.com}")
    private String vapidSubject;

    public WebPushService(PushSubscriptionMapper pushSubscriptionMapper, UserMapper userMapper) {
        this.pushSubscriptionMapper = pushSubscriptionMapper;
        this.userMapper = userMapper;
    }

    @Transactional
    public void saveSubscription(String username, String endpoint, String p256dh, String auth) {
        AppUser user = requireUser(username);
        validate(endpoint, p256dh, auth);
        pushSubscriptionMapper.deleteByEndpoint(endpoint);
        PushSubscription subscription = new PushSubscription();
        subscription.setUserId(user.getId());
        subscription.setEndpoint(endpoint);
        subscription.setP256dh(p256dh);
        subscription.setAuth(auth);
        pushSubscriptionMapper.insert(subscription);
    }

    @Transactional
    public void removeSubscription(String endpoint) {
        if (endpoint == null || endpoint.isBlank()) {
            return;
        }
        pushSubscriptionMapper.deleteByEndpoint(endpoint.trim());
    }

    @Transactional(readOnly = true)
    public String getPublicKey() {
        return vapidPublicKey == null ? "" : vapidPublicKey.trim();
    }

    @Transactional
    public void pushToUser(Long recipientUserId, String title, String body, String url) {
        if (!isPushEnabled() || recipientUserId == null) {
            return;
        }
        List<PushSubscription> subscriptions = pushSubscriptionMapper.findByUserId(recipientUserId);
        if (subscriptions.isEmpty()) {
            return;
        }

        ensureSecurityProvider();
        PushService pushService;
        try {
            pushService = new PushService()
                    .setSubject(vapidSubject)
                    .setPublicKey(vapidPublicKey)
                    .setPrivateKey(vapidPrivateKey);
        } catch (GeneralSecurityException ex) {
            return;
        }

        String safeTitle = trimTo(title, 120);
        String safeBody = trimTo(body, 400);
        String safeUrl = url == null || url.isBlank() ? "/calendar" : url;
        String payload = "{\"title\":\"" + escapeJson(safeTitle) + "\",\"body\":\"" + escapeJson(safeBody)
                + "\",\"url\":\"" + escapeJson(safeUrl) + "\"}";

        for (PushSubscription subscription : subscriptions) {
            try {
                Notification notification = new Notification(
                        subscription.getEndpoint(),
                        subscription.getP256dh(),
                        subscription.getAuth(),
                        payload);
                pushService.send(notification);
            } catch (Exception ex) {
                pushSubscriptionMapper.deleteByEndpoint(subscription.getEndpoint());
            }
        }
    }

    private AppUser requireUser(String username) {
        AppUser user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("ログインユーザーが見つかりません。");
        }
        return user;
    }

    private void validate(String endpoint, String p256dh, String auth) {
        if (endpoint == null || endpoint.isBlank() || p256dh == null || p256dh.isBlank() || auth == null || auth.isBlank()) {
            throw new IllegalArgumentException("Push購読情報が不足しています。");
        }
    }

    private boolean isPushEnabled() {
        return vapidPublicKey != null && !vapidPublicKey.isBlank()
                && vapidPrivateKey != null && !vapidPrivateKey.isBlank();
    }

    private void ensureSecurityProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private String trimTo(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private String escapeJson(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
