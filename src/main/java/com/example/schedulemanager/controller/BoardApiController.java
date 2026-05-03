package com.example.schedulemanager.controller;

import com.example.schedulemanager.dto.BoardRecruitmentCreateRequest;
import com.example.schedulemanager.dto.BoardThreadCreateRequest;
import com.example.schedulemanager.model.BoardPost;
import com.example.schedulemanager.model.BoardThread;
import com.example.schedulemanager.service.BoardService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/board")
public class BoardApiController {
    private final BoardService boardService;

    public BoardApiController(BoardService boardService) {
        this.boardService = boardService;
    }

    @GetMapping("/threads")
    public List<BoardThread> listThreads(@RequestParam(value = "keyword", required = false) String keyword) {
        return boardService.listThreads(keyword);
    }

    @PostMapping("/threads")
    public ResponseEntity<BoardThread> createThread(
            @RequestBody BoardThreadCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boardService.createThread(request, userDetails.getUsername()));
    }

    @GetMapping("/threads/{threadId}/posts")
    public List<BoardPost> listPosts(@PathVariable("threadId") Long threadId) {
        return boardService.listPosts(threadId);
    }

    @PostMapping("/threads/{threadId}/posts")
    public ResponseEntity<BoardPost> createPost(
            @PathVariable("threadId") Long threadId,
            @RequestBody BoardRecruitmentCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(boardService.createRecruitment(threadId, request, userDetails.getUsername()));
    }
}
