package com.example.schedulemanager.controller;

import com.example.schedulemanager.dto.PushSubscriptionRequest;
import com.example.schedulemanager.service.WebPushService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push")
public class PushSubscriptionApiController {
    private final WebPushService webPushService;

    public PushSubscriptionApiController(WebPushService webPushService) {
        this.webPushService = webPushService;
    }

    @GetMapping("/public-key")
    public Map<String, String> publicKey() {
        return Map.of("publicKey", webPushService.getPublicKey());
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<Void> subscribe(
            @RequestBody PushSubscriptionRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        webPushService.saveSubscription(
                userDetails.getUsername(),
                request.getEndpoint(),
                request.getP256dh(),
                request.getAuth());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/subscriptions/remove")
    public ResponseEntity<Void> unsubscribe(@RequestBody PushSubscriptionRequest request) {
        webPushService.removeSubscription(request.getEndpoint());
        return ResponseEntity.noContent().build();
    }
}
