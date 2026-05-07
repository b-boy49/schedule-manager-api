package com.example.schedulemanager.controller;

import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.service.AdminGuardService;
import com.example.schedulemanager.service.UserAccountService;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {
    private final UserAccountService userAccountService;
    private final AdminGuardService adminGuardService;

    public AdminPageController(UserAccountService userAccountService, AdminGuardService adminGuardService) {
        this.userAccountService = userAccountService;
        this.adminGuardService = adminGuardService;
    }

    @GetMapping("/admin")
    public String adminPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        String username = userDetails.getUsername();
        adminGuardService.requireAdmin(username);
        AppUser user = userAccountService.getByUsername(username);
        model.addAllAttributes(Map.of(
                "currentUsername", user.getUsername(),
                "currentDisplayName", user.getDisplayName()));
        return "admin";
    }
}
