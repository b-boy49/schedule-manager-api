package com.example.schedulemanager.controller;

import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.service.LabelColorService;
import com.example.schedulemanager.service.UserAccountService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DmPageController {
    private final UserAccountService userAccountService;
    private final LabelColorService labelColorService;

    public DmPageController(UserAccountService userAccountService, LabelColorService labelColorService) {
        this.userAccountService = userAccountService;
        this.labelColorService = labelColorService;
    }

    @GetMapping("/dm")
    public String dm(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        model.addAttribute("currentUsername", user.getUsername());
        model.addAttribute("currentDisplayName", user.getDisplayName());
        model.addAttribute("labelColorStyle", labelColorService.toInlineStyle(user.getId()));
        return "dm";
    }
}
