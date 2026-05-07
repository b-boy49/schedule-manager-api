package com.example.schedulemanager.service;

import com.example.schedulemanager.dto.BoardRecruitmentCreateRequest;
import com.example.schedulemanager.dto.BoardPostInterestCreateRequest;
import com.example.schedulemanager.dto.BoardThreadCreateRequest;
import com.example.schedulemanager.mapper.BoardMapper;
import com.example.schedulemanager.mapper.UserMapper;
import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.model.BoardPost;
import com.example.schedulemanager.model.BoardPostInterest;
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
    private final NotificationEventService notificationEventService;
    private final FriendNotificationPreferenceService friendNotificationPreferenceService;

    public BoardService(
            BoardMapper boardMapper,
            UserMapper userMapper,
            NotificationEventService notificationEventService,
            FriendNotificationPreferenceService friendNotificationPreferenceService) {
        this.boardMapper = boardMapper;
        this.userMapper = userMapper;
        this.notificationEventService = notificationEventService;
        this.friendNotificationPreferenceService = friendNotificationPreferenceService;
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
        String rankBand = normalize(request.getRankBand());
        if (rankBand != null && rankBand.length() > 100) {
            throw new IllegalArgumentException("ランクは100文字以内で入力してください。");
        }
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
        post.setRankBand(rankBand);
        post.setRecruitmentLimit(recruitmentLimit);
        boardMapper.insertPost(post);
        boardMapper.touchThreadUpdatedAt(threadId);
        notifyFriendFollowersForRecruitment(user, threadId);

        return boardMapper.findPostsByThreadId(threadId).stream()
                .filter(row -> row.getId().equals(post.getId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("作成した募集投稿の取得に失敗しました。"));
    }

    @Transactional(readOnly = true)
    public List<BoardPostInterest> listInterests(Long postId) {
        ensurePostExists(postId);
        return boardMapper.findInterestsByPostId(postId);
    }

    @Transactional
    public BoardPostInterest createInterest(Long postId, BoardPostInterestCreateRequest request, String username) {
        BoardPost post = ensurePostExists(postId);
        if (request == null) {
            throw new IllegalArgumentException("参加希望コメントが空です。");
        }
        String comment = normalize(request.getComment());
        if (comment == null || comment.isBlank()) {
            throw new IllegalArgumentException("参加希望コメントを入力してください。");
        }
        if (comment.length() > 500) {
            throw new IllegalArgumentException("参加希望コメントは500文字以内で入力してください。");
        }

        AppUser user = findCurrentUser(username);
        if (post.getAuthorUserId() != null && post.getAuthorUserId().equals(user.getId())) {
            throw new IllegalArgumentException("自分の募集には参加希望を送信できません。");
        }

        BoardPostInterest interest = new BoardPostInterest();
        interest.setPostId(postId);
        interest.setRequesterUserId(user.getId());
        interest.setComment(comment);
        boardMapper.insertPostInterest(interest);
        if (post.getAuthorUserId() != null && !post.getAuthorUserId().equals(user.getId())) {
            BoardThread thread = post.getThreadId() == null ? null : boardMapper.findThreadById(post.getThreadId());
            String threadTitle = thread == null || thread.getGameTitle() == null ? "募集投稿" : thread.getGameTitle();
            String actorName = user.getDisplayName() == null ? user.getUsername() : user.getDisplayName();
            notificationEventService.publish(
                    post.getAuthorUserId(),
                    user.getId(),
                    "BOARD_INTEREST",
                    "参加希望コメント",
                    actorName + " さんが「" + threadTitle + "」に参加希望コメントを投稿しました。");
        }

        return boardMapper.findInterestsByPostId(postId).stream()
                .filter(row -> row.getId().equals(interest.getId()))
                .findFirst()
                .orElseThrow(() -> new NoSuchElementException("作成した参加希望の取得に失敗しました。"));
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

    private BoardPost ensurePostExists(Long postId) {
        if (postId == null) {
            throw new IllegalArgumentException("投稿IDが不正です。");
        }
        BoardPost existing = boardMapper.findPostById(postId);
        if (existing == null) {
            throw new NoSuchElementException("指定された投稿が見つかりません。");
        }
        return existing;
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

    private void notifyFriendFollowersForRecruitment(AppUser actor, Long threadId) {
        if (actor == null || actor.getId() == null) {
            return;
        }
        List<Long> recipients = friendNotificationPreferenceService.findRecipientsByActor(actor.getId());
        if (recipients.isEmpty()) {
            return;
        }
        BoardThread thread = boardMapper.findThreadById(threadId);
        String gameTitle = thread == null || thread.getGameTitle() == null ? "募集投稿" : thread.getGameTitle();
        String actorName = actor.getDisplayName() == null ? actor.getUsername() : actor.getDisplayName();
        for (Long recipientUserId : recipients) {
            notificationEventService.publish(
                    recipientUserId,
                    actor.getId(),
                    "FRIEND_BOARD_UPDATE",
                    "フレンド募集通知",
                    actorName + " さんが「" + gameTitle + "」の募集を投稿しました。");
        }
    }
}
