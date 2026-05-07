package com.example.schedulemanager.controller;

import com.example.schedulemanager.mapper.DirectMessageMapper;
import com.example.schedulemanager.mapper.FriendshipMapper;
import com.example.schedulemanager.mapper.ScheduleMapper;
import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.service.UserAccountService;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nav")
public class NavBadgeApiController {
    private final UserAccountService userAccountService;
    private final FriendshipMapper friendshipMapper;
    private final DirectMessageMapper directMessageMapper;
    private final ScheduleMapper scheduleMapper;

    public NavBadgeApiController(
            UserAccountService userAccountService,
            FriendshipMapper friendshipMapper,
            DirectMessageMapper directMessageMapper,
            ScheduleMapper scheduleMapper) {
        this.userAccountService = userAccountService;
        this.friendshipMapper = friendshipMapper;
        this.directMessageMapper = directMessageMapper;
        this.scheduleMapper = scheduleMapper;
    }

    @GetMapping("/badges")
    public Map<String, Integer> badges(@AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        return Map.of(
                "friends", friendshipMapper.countIncomingPending(user.getId()),
                "dm", directMessageMapper.countUnreadByRecipient(user.getId()),
                "joins", scheduleMapper.countPendingJoinRequestsForOwner(user.getId()));
    }
}
