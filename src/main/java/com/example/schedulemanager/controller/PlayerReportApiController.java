package com.example.schedulemanager.controller;

import com.example.schedulemanager.dto.PlayerReportCreateRequest;
import com.example.schedulemanager.service.PlayerReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class PlayerReportApiController {
    private final PlayerReportService playerReportService;

    public PlayerReportApiController(PlayerReportService playerReportService) {
        this.playerReportService = playerReportService;
    }

    @PostMapping
    public ResponseEntity<Void> create(
            @RequestBody PlayerReportCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        playerReportService.create(userDetails.getUsername(), request);
        return ResponseEntity.noContent().build();
    }
}
