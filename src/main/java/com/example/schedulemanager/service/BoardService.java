package com.example.schedulemanager.service;

import com.example.schedulemanager.dto.BoardRecruitmentCreateRequest;
import com.example.schedulemanager.dto.BoardThreadCreateRequest;
import com.example.schedulemanager.mapper.BoardMapper;
import com.example.schedulemanager.mapper.UserMapper;
import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.model.BoardPost;
import com.example.schedulemanager.model.BoardThread;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardService {
    private final BoardMapper boardMapper;
    private final UserMapper userMapper;

    public BoardService(BoardMapper boardMapper, UserMapper userMapper) {
        this.boardMapper = boardMapper;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public List<BoardThread> listThreads() {
        return listThreads(null);
    }

    @Transactional(readOnly = true)
    public List<BoardThread> listThreads(String keyword) {
        return boardMapper.findAllThreads(normalize(keyword));
    }

    @Transactional(readOnly = true)
    public List<BoardPost> listPosts(Long threadId) {
        ensureThreadExists(threadId);
        return boardMapper.findPostsByThreadId(threadId);
    }

    @Transactional
    public BoardThread createThread(BoardThreadCreateRequest request, String username) {
        String gameTitle = normalize(request == null ? null : request.getGameTitle());
        if (gameTitle == null || gameTitle.isBlank()) {
            throw new IllegalArgumentException("ゲームタイトルを入力してください。");
        }
        if (gameTitle.length() > 200) {
            throw new IllegalArgumentException("ゲームタイトルは200文字以内で入力してください。");
        }

        AppUser user = findCurrentUser(username);
        BoardThread thread = new BoardThread();
        thread.setOwnerUserId(user.getId());
        thread.setGameTitle(gameTitle);
        boardMapper.insertThread(thread);
        BoardThread created = boardMapper.findThreadViewById(thread.getId());
        if (created == null) {
            throw new NoSuchElementException("作成したスレッドの取得に失敗しました。");
        }
        return created;
    }

    @Transactional
    public BoardPost createRecruitment(Long threadId, BoardRecruitmentCreateRequest request, String username) {
        ensureThreadExists(threadId);
        if (request == null) {
            throw new IllegalArgumentException("募集内容が空です。");
        }

        String body = normalize(request.getBody());
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("募集内容を入力してください。");
        }
        if (body.length() > 1000) {
            throw new IllegalArgumentException("募集内容は1000文字以内で入力してください。");
        }

        LocalDate scheduleDate = parseDate(request.getScheduleDate());
        LocalTime startTime = parseTime(request.getStartTime());
        Integer recruitmentLimit = request.getRecruitmentLimit();
        if (recruitmentLimit != null && recruitmentLimit < 1) {
            throw new IllegalArgumentException("募集人数は1以上で指定してください。");
        }

        AppUser user = findCurrentUser(username);
        BoardPost post = new BoardPost();
        post.setThreadId(threadId);
        post.setAuthorUserId(user.getId());
        post.setBody(body);
        post.setScheduleDate(scheduleDate);
        post.setStartTime(startTime);
        post.setRecruitmentLimit(recruitmentLimit);
        boardMapper.insertPost(post);
        boardMapper.touchThreadUpdatedAt(threadId);

        return boardMapper.findPostsByThreadId(threadId).stream()
                .filter(row -> row.getId().equals(post.getId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("作成した募集投稿の取得に失敗しました。"));
    }

    private void ensureThreadExists(Long threadId) {
        if (threadId == null) {
            throw new IllegalArgumentException("スレッドIDが不正です。");
        }
        BoardThread existing = boardMapper.findThreadById(threadId);
        if (existing == null) {
            throw new NoSuchElementException("指定されたスレッドが見つかりません。");
        }
    }

    private AppUser findCurrentUser(String username) {
        AppUser user = userMapper.findByUsername(username);
        if (user == null) {
            throw new NoSuchElementException("ログインユーザーが見つかりません。");
        }
        return user;
    }

    private LocalDate parseDate(String value) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("日付は YYYY-MM-DD 形式で指定してください。");
        }
    }

    private LocalTime parseTime(String value) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("時刻は HH:mm 形式で指定してください。");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }
}
