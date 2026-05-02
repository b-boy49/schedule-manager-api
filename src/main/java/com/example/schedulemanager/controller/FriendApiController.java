package com.example.schedulemanager.controller;

import com.example.schedulemanager.dto.FriendRequestCreateRequest;
import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.service.FriendshipService;
import com.example.schedulemanager.service.GamificationService;
import com.example.schedulemanager.service.UserAccountService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/friends")
public class FriendApiController {
    private final FriendshipService friendshipService;
    private final UserAccountService userAccountService;
    private final GamificationService gamificationService;

    public FriendApiController(
            FriendshipService friendshipService,
            UserAccountService userAccountService,
            GamificationService gamificationService) {
        this.friendshipService = friendshipService;
        this.userAccountService = userAccountService;
        this.gamificationService = gamificationService;
    }

    @GetMapping
    public Map<String, Object> friendDashboard(@AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        return Map.of(
                "friends", friendshipService.listFriends(user.getId()),
                "incomingRequests", friendshipService.listIncomingPending(user.getId()),
                "outgoingRequests", friendshipService.listOutgoingPending(user.getId()),
                "taskRanking", gamificationService.buildTaskCompletionRanking(user.getId(), "all"));
    }

    @GetMapping("/ranking")
    public Map<String, Object> taskRanking(
            @RequestParam(value = "period", defaultValue = "all") String period,
            @AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        return Map.of("period", period, "rows", gamificationService.buildTaskCompletionRanking(user.getId(), period));
    }

    @PostMapping("/requests")
    public ResponseEntity<Void> sendRequest(
            @RequestBody FriendRequestCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        friendshipService.createRequest(user.getId(), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<Void> acceptRequest(
            @PathVariable("requestId") Long requestId,
            @AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        friendshipService.acceptRequest(user.getId(), requestId);
        return ResponseEntity.noContent().build();
    }
}
