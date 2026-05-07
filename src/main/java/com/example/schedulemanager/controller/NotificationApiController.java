package com.example.schedulemanager.controller;

import com.example.schedulemanager.model.NotificationEvent;
import com.example.schedulemanager.service.NotificationEventService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
public class NotificationApiController {
    private final NotificationEventService notificationEventService;

    public NotificationApiController(NotificationEventService notificationEventService) {
        this.notificationEventService = notificationEventService;
    }

    @GetMapping("/events")
    public List<NotificationEvent> listEvents(
            @RequestParam(value = "sinceId", required = false) Long sinceId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @AuthenticationPrincipal UserDetails userDetails) {
        return notificationEventService.listSince(userDetails.getUsername(), sinceId, limit);
    }
}
