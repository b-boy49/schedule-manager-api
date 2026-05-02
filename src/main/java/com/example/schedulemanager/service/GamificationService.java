package com.example.schedulemanager.service;

import com.example.schedulemanager.mapper.FriendshipMapper;
import com.example.schedulemanager.mapper.PointHistoryMapper;
import com.example.schedulemanager.mapper.ScheduleMapper;
import com.example.schedulemanager.mapper.UserMapper;
import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.model.FriendUser;
import com.example.schedulemanager.model.PointHistory;
import com.example.schedulemanager.model.ScheduleItem;
import com.example.schedulemanager.model.TaskCompletionRankingRow;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GamificationService {
    public static final int POINT_DAILY_LOGIN = 3;
    public static final int POINT_SCHEDULE_CREATE = 5;
    public static final int POINT_SCHEDULE_COMPLETE_BASE = 10;
    public static final int POINT_PRIORITY_HIGH_BONUS = 10;
    public static final int POINT_PRIORITY_MEDIUM_BONUS = 5;
    public static final int POINT_ON_TIME_BONUS = 5;

    private final UserMapper userMapper;
    private final PointHistoryMapper pointHistoryMapper;
    private final FriendshipMapper friendshipMapper;
    private final ScheduleMapper scheduleMapper;

    @Value("${app.gamification.zone:Asia/Tokyo}")
    private String gamificationZone;

    public GamificationService(
            UserMapper userMapper,
            PointHistoryMapper pointHistoryMapper,
            FriendshipMapper friendshipMapper,
            ScheduleMapper scheduleMapper) {
        this.userMapper = userMapper;
        this.pointHistoryMapper = pointHistoryMapper;
        this.friendshipMapper = friendshipMapper;
        this.scheduleMapper = scheduleMapper;
    }

    @Transactional
    public int awardDailyLogin(String username) {
        AppUser user = userMapper.findByUsername(username);
        if (user == null) {
            return 0;
        }
        LocalDate today = LocalDate.now(ZoneId.of(gamificationZone));
        String actionKey = "DAILY_LOGIN:" + today;
        return awardPoints(
                user.getId(),
                "DAILY_LOGIN",
                "デイリーログインボーナス",
                POINT_DAILY_LOGIN,
                actionKey);
    }

    @Transactional
    public int awardScheduleCreated(Long userId, String title, Long scheduleId) {
        if (userId == null || scheduleId == null) {
            return 0;
        }
        String actionKey = "SCHEDULE_CREATE:" + scheduleId;
        String label = "予定作成: " + safeTitle(title);
        return awardPoints(userId, "SCHEDULE_CREATE", label, POINT_SCHEDULE_CREATE, actionKey);
    }

    @Transactional
    public int awardScheduleCompleted(Long userId, ScheduleItem item) {
        if (userId == null || item == null || item.getId() == null) {
            return 0;
        }
        int points = POINT_SCHEDULE_COMPLETE_BASE
                + priorityBonus(item.getPriority())
                + onTimeBonus(item);
        String actionKey = "SCHEDULE_COMPLETE:" + item.getId();
        String label = "予定完了: " + safeTitle(item.getTitle());
        return awardPoints(userId, "SCHEDULE_COMPLETE", label, points, actionKey);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> buildUserProgressSummary(Long userId) {
        AppUser user = userMapper.findById(userId);
        int totalPoints = user == null || user.getTotalPoints() == null ? 0 : user.getTotalPoints();
        LevelProgress levelProgress = toLevelProgress(totalPoints);

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("totalPoints", totalPoints);
        map.put("level", levelProgress.level());
        map.put("currentLevelPoints", levelProgress.currentLevelPoints());
        map.put("requiredPointsForNextLevel", levelProgress.requiredPointsForNextLevel());
        map.put("pointsToNextLevel", levelProgress.pointsToNextLevel());
        map.put("levelProgressPercent", levelProgress.progressPercent());
        return map;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPointHistories(Long userId) {
        List<PointHistory> histories = pointHistoryMapper.findRecentByUserId(userId);
        List<Map<String, Object>> response = new ArrayList<>();
        for (PointHistory history : histories) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("createdAt", history.getCreatedAt());
            row.put("actionType", history.getActionType());
            row.put("actionLabel", history.getActionLabel());
            row.put("points", history.getPoints());
            response.add(row);
        }
        return response;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> buildFriendRanking(Long currentUserId) {
        Set<Long> userIds = new LinkedHashSet<>();
        userIds.add(currentUserId);
        List<FriendUser> friends = friendshipMapper.findFriends(currentUserId);
        for (FriendUser friend : friends) {
            userIds.add(friend.getId());
        }

        List<RankingCandidate> candidates = new ArrayList<>();
        for (Long userId : userIds) {
            AppUser user = userMapper.findById(userId);
            if (user == null) {
                continue;
            }
            int totalPoints = user.getTotalPoints() == null ? 0 : user.getTotalPoints();
            int level = toLevelProgress(totalPoints).level();
            candidates.add(new RankingCandidate(
                    user.getId(),
                    user.getUsername(),
                    user.getDisplayName(),
                    totalPoints,
                    level));
        }

        candidates.sort(
                Comparator.comparingInt(RankingCandidate::level).reversed()
                        .thenComparingInt(RankingCandidate::totalPoints).reversed()
                        .thenComparing(candidate -> normalizeText(candidate.displayName()))
                        .thenComparing(candidate -> normalizeText(candidate.username())));

        List<Map<String, Object>> result = new ArrayList<>();
        int rank = 1;
        for (RankingCandidate candidate : candidates) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("rank", rank++);
            row.put("id", candidate.id());
            row.put("username", candidate.username());
            row.put("displayName", candidate.displayName());
            row.put("level", candidate.level());
            row.put("totalPoints", candidate.totalPoints());
            row.put("currentUser", Objects.equals(candidate.id(), currentUserId));
            result.add(row);
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> buildTaskCompletionRanking(Long currentUserId, String period) {
        Set<Long> userIds = new LinkedHashSet<>();
        userIds.add(currentUserId);
        List<FriendUser> friends = friendshipMapper.findFriends(currentUserId);
        for (FriendUser friend : friends) {
            userIds.add(friend.getId());
        }

        TimeRange range = resolvePeriodRange(period);
        List<TaskCompletionRankingRow> rows = scheduleMapper.findTaskCompletionRanking(
                new ArrayList<>(userIds),
                range.startAt(),
                range.endAt());

        int maxCompleted = rows.stream()
                .map(TaskCompletionRankingRow::getCompletedCount)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        List<Map<String, Object>> result = new ArrayList<>();
        for (TaskCompletionRankingRow row : rows) {
            int completedCount = row.getCompletedCount() == null ? 0 : row.getCompletedCount();
            int percent = maxCompleted <= 0 ? 0 : (int) Math.round((completedCount * 100.0) / maxCompleted);

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("rank", row.getRank());
            item.put("id", row.getUserId());
            item.put("username", row.getUsername());
            item.put("displayName", row.getDisplayName());
            item.put("completedCount", completedCount);
            item.put("progressPercent", Math.min(100, Math.max(0, percent)));
            item.put("avatarInitial", extractAvatarInitial(row.getDisplayName(), row.getUsername()));
            item.put("currentUser", Objects.equals(row.getUserId(), currentUserId));
            result.add(item);
        }
        return result;
    }

    private TimeRange resolvePeriodRange(String period) {
        String key = period == null ? "all" : period.trim().toLowerCase(Locale.ROOT);
        LocalDate today = LocalDate.now(ZoneId.of(gamificationZone));
        if ("week".equals(key)) {
            LocalDate startDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            return new TimeRange(startDate.atStartOfDay(), startDate.plusWeeks(1).atStartOfDay());
        }
        if ("month".equals(key)) {
            LocalDate startDate = today.with(TemporalAdjusters.firstDayOfMonth());
            return new TimeRange(startDate.atStartOfDay(), startDate.plusMonths(1).atStartOfDay());
        }
        return new TimeRange(null, null);
    }

    private String extractAvatarInitial(String displayName, String username) {
        String source = normalizeText(displayName);
        if (source.isBlank()) {
            source = normalizeText(username);
        }
        if (source.isBlank()) {
            return "?";
        }
        return source.substring(0, 1).toUpperCase(Locale.ROOT);
    }

    private int awardPoints(Long userId, String actionType, String actionLabel, int points, String actionKey) {
        if (points == 0) {
            return 0;
        }
        PointHistory history = new PointHistory();
        history.setUserId(userId);
        history.setActionType(actionType);
        history.setActionLabel(actionLabel);
        history.setPoints(points);
        history.setActionKey(actionKey);

        try {
            pointHistoryMapper.insert(history);
        } catch (DataIntegrityViolationException ex) {
            return 0;
        }

        userMapper.addPoints(userId, points);
        return points;
    }

    private int priorityBonus(String priority) {
        String normalized = normalizePriority(priority);
        if ("HIGH".equals(normalized)) {
            return POINT_PRIORITY_HIGH_BONUS;
        }
        if ("MEDIUM".equals(normalized)) {
            return POINT_PRIORITY_MEDIUM_BONUS;
        }
        return 0;
    }

    private int onTimeBonus(ScheduleItem item) {
        if (item.getCompletedAt() == null || item.getScheduleDate() == null) {
            return 0;
        }
        return item.getCompletedAt().toLocalDate().isAfter(item.getScheduleDate()) ? 0 : POINT_ON_TIME_BONUS;
    }

    private String safeTitle(String title) {
        String normalized = normalizeText(title);
        return normalized.isBlank() ? "(タイトルなし)" : normalized;
    }

    private String normalizePriority(String value) {
        if (value == null || value.isBlank()) {
            return "LOW";
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("HIGH".equals(normalized) || "MEDIUM".equals(normalized)) {
            return normalized;
        }
        return "LOW";
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }
        return value.trim();
    }

    private LevelProgress toLevelProgress(int totalPoints) {
        int level = 1;
        int requiredPointsForNextLevel = 100;
        int currentLevelPoints = Math.max(totalPoints, 0);

        while (currentLevelPoints >= requiredPointsForNextLevel) {
            currentLevelPoints -= requiredPointsForNextLevel;
            level += 1;
            requiredPointsForNextLevel = 100 + (level - 1) * 50;
        }

        int pointsToNextLevel = requiredPointsForNextLevel - currentLevelPoints;
        int progressPercent = (int) Math.floor((currentLevelPoints * 100.0) / requiredPointsForNextLevel);
        return new LevelProgress(
                level,
                currentLevelPoints,
                requiredPointsForNextLevel,
                pointsToNextLevel,
                progressPercent);
    }

    private record LevelProgress(
            int level,
            int currentLevelPoints,
            int requiredPointsForNextLevel,
            int pointsToNextLevel,
            int progressPercent) {
    }

    private record RankingCandidate(
            Long id,
            String username,
            String displayName,
            int totalPoints,
            int level) {
    }

    private record TimeRange(LocalDateTime startAt, LocalDateTime endAt) {
    }
}
