package com.example.schedulemanager.controller;

import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.model.BoardThread;
import com.example.schedulemanager.service.BoardService;
import com.example.schedulemanager.service.LabelColorService;
import com.example.schedulemanager.service.UserAccountService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class BoardPageController {
    private final UserAccountService userAccountService;
    private final LabelColorService labelColorService;
    private final BoardService boardService;

    public BoardPageController(
            UserAccountService userAccountService,
            LabelColorService labelColorService,
            BoardService boardService) {
        this.userAccountService = userAccountService;
        this.labelColorService = labelColorService;
        this.boardService = boardService;
    }

    @GetMapping("/board")
    public String board(
            @RequestParam(value = "keyword", required = false) String keyword,
            Model model,
            @AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        List<BoardThread> threads = boardService.listThreads(keyword);
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        model.addAttribute("currentUsername", user.getUsername());
        model.addAttribute("currentDisplayName", user.getDisplayName());
        model.addAttribute("labelColorStyle", labelColorService.toInlineStyle(user.getId()));
        model.addAttribute("keyword", normalizedKeyword);
        model.addAttribute("threadSearchResultCount", threads.size());
        model.addAttribute("threads", threads);
        return "board";
    }
}
