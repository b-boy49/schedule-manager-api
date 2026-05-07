package com.example.schedulemanager.controller;

import com.example.schedulemanager.model.BoardPost;
import com.example.schedulemanager.model.BoardPostInterest;
import com.example.schedulemanager.model.DirectMessage;
import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.service.AdminModerationService;
import com.example.schedulemanager.service.PlayerReportService;
import com.example.schedulemanager.model.PlayerReport;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/moderation")
public class AdminModerationApiController {
    private final AdminModerationService adminModerationService;
    private final PlayerReportService playerReportService;

    public AdminModerationApiController(
            AdminModerationService adminModerationService,
            PlayerReportService playerReportService) {
        this.adminModerationService = adminModerationService;
        this.playerReportService = playerReportService;
    }

    @GetMapping("/board-posts")
    public List<BoardPost> boardPosts(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "targetUserId", required = false) Long targetUserId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return adminModerationService.listBoardPosts(userDetails.getUsername(), keyword, targetUserId);
    }

    @GetMapping("/board-interests")
    public List<BoardPostInterest> boardInterests(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "targetUserId", required = false) Long targetUserId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return adminModerationService.listBoardInterests(userDetails.getUsername(), keyword, targetUserId);
    }

    @GetMapping("/direct-messages")
    public List<DirectMessage> directMessages(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "targetUserId", required = false) Long targetUserId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return adminModerationService.listDirectMessages(userDetails.getUsername(), keyword, targetUserId);
    }

    @GetMapping("/users")
    public List<Map<String, Object>> users(@AuthenticationPrincipal UserDetails userDetails) {
        List<AppUser> users = adminModerationService.listUsers(userDetails.getUsername());
        return users.stream().map((u) -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", u.getId());
            row.put("username", u.getUsername());
            row.put("displayName", u.getDisplayName());
            row.put("enabled", u.getEnabled() != null && u.getEnabled());
            return row;
        }).toList();
    }

    @GetMapping("/reports")
    public List<PlayerReport> reports(@AuthenticationPrincipal UserDetails userDetails) {
        return playerReportService.listOpenReports(userDetails.getUsername());
    }

    @DeleteMapping("/board-posts/{id}")
    public ResponseEntity<Void> deleteBoardPost(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails) {
        adminModerationService.deleteBoardPost(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/board-interests/{id}")
    public ResponseEntity<Void> deleteBoardInterest(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails) {
        adminModerationService.deleteBoardInterest(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/direct-messages/{id}")
    public ResponseEntity<Void> deleteDirectMessage(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails) {
        adminModerationService.deleteDirectMessage(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{id}/ban")
    public ResponseEntity<Void> banUser(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails) {
        adminModerationService.setUserBan(userDetails.getUsername(), id, true);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/users/{id}/unban")
    public ResponseEntity<Void> unbanUser(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails) {
        adminModerationService.setUserBan(userDetails.getUsername(), id, false);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/reports/{id}/resolve")
    public ResponseEntity<Void> resolveReport(@PathVariable("id") Long id, @AuthenticationPrincipal UserDetails userDetails) {
        playerReportService.resolveReport(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
