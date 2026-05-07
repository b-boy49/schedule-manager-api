package com.example.schedulemanager.controller;

import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.service.FriendshipService;
import com.example.schedulemanager.service.LabelColorService;
import com.example.schedulemanager.service.UserAccountService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class FriendPageController {
    private final UserAccountService userAccountService;
    private final LabelColorService labelColorService;
    private final FriendshipService friendshipService;

    public FriendPageController(
            UserAccountService userAccountService,
            LabelColorService labelColorService,
            FriendshipService friendshipService) {
        this.userAccountService = userAccountService;
        this.labelColorService = labelColorService;
        this.friendshipService = friendshipService;
    }

    @GetMapping("/friends")
    public String friends(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        model.addAttribute("currentUsername", user.getUsername());
        model.addAttribute("currentDisplayName", user.getDisplayName());
        model.addAttribute("labelColorStyle", labelColorService.toInlineStyle(user.getId()));
        return "friends";
    }

    @GetMapping("/friends/profile/{username}")
    public String friendProfile(
            @PathVariable("username") String username,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {
        AppUser viewer = userAccountService.getByUsername(userDetails.getUsername());
        AppUser target = userAccountService.getByUsername(username);
        if (!friendshipService.areFriendsOrSelf(viewer.getId(), target.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "フレンドのみ閲覧できます。");
        }
        model.addAttribute("currentUsername", viewer.getUsername());
        model.addAttribute("currentDisplayName", viewer.getDisplayName());
        model.addAttribute("labelColorStyle", labelColorService.toInlineStyle(viewer.getId()));
        model.addAttribute("friend", target);
        return "friend-profile";
    }
}
