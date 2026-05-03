package com.example.schedulemanager.controller;

import com.example.schedulemanager.dto.DmMessageSendRequest;
import com.example.schedulemanager.dto.DmStartRequest;
import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.model.DirectMessage;
import com.example.schedulemanager.model.DmConversation;
import com.example.schedulemanager.service.DmService;
import com.example.schedulemanager.service.UserAccountService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dm")
public class DmApiController {
    private final DmService dmService;
    private final UserAccountService userAccountService;

    public DmApiController(DmService dmService, UserAccountService userAccountService) {
        this.dmService = dmService;
        this.userAccountService = userAccountService;
    }

    @PostMapping("/start")
    public DmConversation start(
            @RequestBody DmStartRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        return dmService.startOrGetConversation(user.getId(), request);
    }

    @GetMapping("/conversations")
    public List<DmConversation> conversations(@AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        return dmService.listConversations(user.getId());
    }

    @GetMapping("/conversations/{id}/messages")
    public List<DirectMessage> messages(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        return dmService.listMessages(user.getId(), id);
    }

    @PostMapping("/conversations/{id}/messages")
    public DirectMessage sendMessage(
            @PathVariable("id") Long id,
            @RequestBody DmMessageSendRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        return dmService.sendMessage(user.getId(), id, request);
    }
}
