package com.example.schedulemanager.service;

import com.example.schedulemanager.dto.ScheduleRequest;
import com.example.schedulemanager.dto.ScheduleCsvImportError;
import com.example.schedulemanager.dto.ScheduleCsvImportResult;
import com.example.schedulemanager.dto.ScheduleJoinRequestCreateRequest;
import com.example.schedulemanager.mapper.ScheduleMapper;
import com.example.schedulemanager.mapper.UserMapper;
import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.model.ScheduleItem;
import com.example.schedulemanager.model.ScheduleJoinRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.LinkedHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ScheduleService {
    private static final List<String> CSV_TEMPLATE_HEADERS = List.of(
            "scheduleDate", "title", "priority", "deviceType", "rankBand", "startTime", "endTime",
            "description", "sharedWithFriends", "joinable", "messageShareable", "recruitmentLimit");

    private final ScheduleMapper scheduleMapper;
    private final UserMapper userMapper;
    private final GamificationService gamificationService;
    private final NotificationEventService notificationEventService;
    private final FriendNotificationPreferenceService friendNotificationPreferenceService;

    public ScheduleService(
            ScheduleMapper scheduleMapper,
            UserMapper userMapper,
            GamificationService gamificationService,
            NotificationEventService notificationEventService,
            FriendNotificationPreferenceService friendNotificationPreferenceService) {
        this.scheduleMapper = scheduleMapper;
        this.userMapper = userMapper;
        this.gamificationService = gamificationService;
        this.notificationEventService = notificationEventService;
        this.friendNotificationPreferenceService = friendNotificationPreferenceService;
    }

    @Transactional
    public ScheduleCsvImportResult importFromCsv(MultipartFile file, String currentUsername) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("CSV file is empty.");
        }

        AppUser currentUser = getCurrentUser(currentUsername);
        List<List<String>> records = parseCsv(file);
        if (records.isEmpty()) {
            throw new IllegalArgumentException("CSV has no data.");
        }

        Map<String, Integer> headerIndex = buildHeaderIndex(records.get(0));
        validateHeaders(headerIndex);

        List<ScheduleItem> validItems = new ArrayList<>();
        List<ScheduleCsvImportError> errors = new ArrayList<>();
        int totalRows = 0;

        for (int i = 1; i < records.size(); i += 1) {
            List<String> row = records.get(i);
            int rowNumber = i + 1;
            if (isEmptyRow(row)) {
                continue;
            }
            totalRows += 1;
            try {
                ScheduleItem item = parseCsvRow(row, headerIndex, rowNumber);
                item.setOwnerUserId(currentUser.getId());
                item.setCompleted(false);
                item.setCompletedAt(null);
                item.setSourceScheduleItemId(null);
                item.setSourceOwnerUserId(null);
                validItems.add(item);
            } catch (IllegalArgumentException ex) {
                errors.add(new ScheduleCsvImportError(rowNumber, ex.getMessage()));
            }
        }

        if (!validItems.isEmpty()) {
            scheduleMapper.bulkInsert(validItems);
        }

        ScheduleCsvImportResult result = new ScheduleCsvImportResult();
        result.setTotalRows(totalRows);
        result.setValidRows(validItems.size());
        result.setInsertedRows(validItems.size());
        result.setErrors(errors);
        return result;
    }

    public String buildCsvTemplate() {
        String header = String.join(",", CSV_TEMPLATE_HEADERS);
        String sample = "2026-05-02,Sample Task,MEDIUM,PC,,10:00,11:00,\"Sample description\",false,false,false,";
        return header + System.lineSeparator() + sample + System.lineSeparator();
    }

    @Transactional(readOnly = true)
    public List<ScheduleItem> getByDate(LocalDate date, String currentUsername) {
        AppUser currentUser = getCurrentUser(currentUsername);
        List<ScheduleItem> items = scheduleMapper.findVisibleByDate(date, currentUser.getId());
        decorateForViewer(items, currentUser.getId());
        return items;
    }

    @Transactional(readOnly = true)
    public List<ScheduleItem> getByMonth(int year, int month, String currentUsername) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("月は1〜12で指定してください。");
        }
        if (year < 1900 || year > 3000) {
            throw new IllegalArgumentException("年の指定が不正です。");
        }

        AppUser currentUser = getCurrentUser(currentUsername);
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endExclusiveDate = startDate.plusMonths(1);
        List<ScheduleItem> rows = scheduleMapper.findVisibleInRange(startDate, endExclusiveDate, currentUser.getId());

        // 日付セルに載せるマーカー用途なので、参加情報の重い付加処理は避ける。
        List<ScheduleItem> markers = new ArrayList<>();
        for (ScheduleItem row : rows) {
            ScheduleItem marker = new ScheduleItem();
            marker.setId(row.getId());
            marker.setOwnerUserId(row.getOwnerUserId());
            marker.setOwnerUsername(row.getOwnerUsername());
            marker.setOwnerDisplayName(row.getOwnerDisplayName());
            marker.setOwnerProfileIconColor(row.getOwnerProfileIconColor());
            marker.setOwnerHasProfileImage(row.getOwnerHasProfileImage());
            marker.setScheduleDate(row.getScheduleDate());
            marker.setTitle(row.getTitle());
            marker.setJoinable(row.getJoinable());
            markers.add(marker);
        }
        return markers;
    }

    @Transactional(readOnly = true)
    public List<String> getTitleSuggestions(String currentUsername, Integer limit) {
        AppUser currentUser = getCurrentUser(currentUsername);
        int safeLimit = limit == null ? 20 : Math.max(1, Math.min(limit, 100));
        return scheduleMapper.findRecentDistinctTitles(currentUser.getId(), safeLimit);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> findDueSoonReminders(String currentUsername, int windowMinutes) {
        AppUser currentUser = getCurrentUser(currentUsername);
        int safeWindow = Math.max(1, Math.min(windowMinutes, 24 * 60));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold = now.plusMinutes(safeWindow);
        List<ScheduleItem> candidates = scheduleMapper.findOwnedUncompletedInDateRange(
                currentUser.getId(),
                now.toLocalDate(),
                threshold.toLocalDate());

        List<Map<String, Object>> result = new ArrayList<>();
        for (ScheduleItem item : candidates) {
            LocalDateTime dueAt = toDueAt(item);
            if (dueAt.isBefore(now) || dueAt.isAfter(threshold)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("title", item.getTitle());
            row.put("scheduleDate", item.getScheduleDate());
            row.put("startTime", item.getStartTime());
            row.put("dueAt", dueAt);
            row.put("minutesLeft", java.time.Duration.between(now, dueAt).toMinutes());
            result.add(row);
        }
        return result;
    }

    @Transactional
    public ScheduleItem create(ScheduleRequest request, String currentUsername) {
        AppUser currentUser = getCurrentUser(currentUsername);
        ScheduleItem item = fromRequest(request);
        item.setOwnerUserId(currentUser.getId());
        item.setCompleted(false);
        item.setCompletedAt(null);
        scheduleMapper.insert(item);
        ScheduleItem created = scheduleMapper.findVisibleById(item.getId(), currentUser.getId());
        gamificationService.awardScheduleCreated(currentUser.getId(), created.getTitle(), created.getId());
        decorateForViewer(created, currentUser.getId());
        notifyFriendFollowers(currentUser, created.getTitle(), "予定を追加しました。");
        return created;
    }

    @Transactional
    public ScheduleItem update(Long id, ScheduleRequest request, String currentUsername) {
        AppUser currentUser = getCurrentUser(currentUsername);
        ScheduleItem existing = scheduleMapper.findOwnedById(id, currentUser.getId());
        if (existing == null) {
            throw new NoSuchElementException("指定された予定が存在しないか、編集権限がありません。id=" + id);
        }

        ScheduleItem item = fromRequest(request);
        if (Boolean.TRUE.equals(item.getJoinable()) && item.getRecruitmentLimit() != null) {
            int currentParticipants = scheduleMapper.countParticipants(id);
            if (item.getRecruitmentLimit() < currentParticipants) {
                throw new IllegalArgumentException("募集人数は現在の参加者数以上にしてください。");
            }
        }
        item.setId(id);
        item.setOwnerUserId(currentUser.getId());
        int updatedCount = scheduleMapper.update(item);
        if (updatedCount == 0) {
            throw new NoSuchElementException("指定された予定が存在しないか、編集権限がありません。id=" + id);
        }
        if (!Boolean.TRUE.equals(item.getJoinable())) {
            scheduleMapper.deleteAllParticipantsBySchedule(id);
            scheduleMapper.deleteJoinRequestsBySchedule(id);
        }

        ScheduleItem updated = scheduleMapper.findVisibleById(id, currentUser.getId());
        decorateForViewer(updated, currentUser.getId());
        notifyFriendFollowers(currentUser, updated.getTitle(), "予定を更新しました。");
        return updated;
    }

    @Transactional
    public void delete(Long id, String currentUsername) {
        AppUser currentUser = getCurrentUser(currentUsername);
        ScheduleItem existing = scheduleMapper.findOwnedById(id, currentUser.getId());
        if (existing == null) {
            throw new NoSuchElementException("指定された予定が存在しないか、削除権限がありません。id=" + id);
        }

        scheduleMapper.deleteAllParticipantsBySchedule(id);
        scheduleMapper.deleteJoinRequestsBySchedule(id);
        int deletedCount = scheduleMapper.delete(id, currentUser.getId());
        if (deletedCount == 0) {
            throw new NoSuchElementException("指定された予定が存在しないか、削除権限がありません。id=" + id);
        }
    }

    @Transactional
    public void join(Long id, String currentUsername, ScheduleJoinRequestCreateRequest request) {
        AppUser currentUser = getCurrentUser(currentUsername);
        ScheduleItem target = scheduleMapper.findVisibleById(id, currentUser.getId());
        validateJoinable(target, currentUser);

        if (scheduleMapper.existsParticipant(id, currentUser.getId())) {
            throw new IllegalArgumentException("すでに参加しています。");
        }
        if (isRecruitmentClosed(target, scheduleMapper.countParticipants(id))) {
            throw new IllegalArgumentException("この募集は締め切られています。");
        }

        String comment = normalizeJoinRequestComment(request == null ? null : request.getComment());
        ScheduleJoinRequest existing = scheduleMapper.findJoinRequestByScheduleAndRequester(id, currentUser.getId());
        if (existing == null) {
            scheduleMapper.insertJoinRequest(id, currentUser.getId(), comment, "PENDING");
        } else {
            scheduleMapper.updateJoinRequest(existing.getId(), comment, "PENDING");
        }
        if (target.getOwnerUserId() != null && !target.getOwnerUserId().equals(currentUser.getId())) {
            String actorName = currentUser.getDisplayName() == null ? currentUser.getUsername() : currentUser.getDisplayName();
            String scheduleTitle = target.getTitle() == null ? "予定" : target.getTitle();
            notificationEventService.publish(
                    target.getOwnerUserId(),
                    currentUser.getId(),
                    "SCHEDULE_JOIN_REQUEST",
                    "参加希望通知",
                    actorName + " さんが「" + scheduleTitle + "」に参加希望を送信しました。");
        }
    }

    @Transactional
    public void cancelJoin(Long id, String currentUsername) {
        AppUser currentUser = getCurrentUser(currentUsername);
        ScheduleItem target = scheduleMapper.findVisibleById(id, currentUser.getId());
        validateJoinable(target, currentUser);

        int deletedCount = scheduleMapper.deleteParticipant(id, currentUser.getId());
        if (deletedCount == 0) {
            ScheduleJoinRequest existing = scheduleMapper.findJoinRequestByScheduleAndRequester(id, currentUser.getId());
            if (existing == null || !"PENDING".equalsIgnoreCase(existing.getStatus())) {
                throw new NoSuchElementException("参加情報が見つかりません。");
            }
            scheduleMapper.updateJoinRequestStatus(existing.getId(), "CANCELED");
        }
    }

    @Transactional
    public void decideJoinRequest(Long scheduleId, Long joinRequestId, boolean approve, String currentUsername) {
        AppUser currentUser = getCurrentUser(currentUsername);
        ScheduleItem owned = scheduleMapper.findOwnedById(scheduleId, currentUser.getId());
        if (owned == null) {
            throw new NoSuchElementException("対象の募集が見つかりません。");
        }
        List<ScheduleJoinRequest> pending = scheduleMapper.findPendingJoinRequestsBySchedule(scheduleId);
        ScheduleJoinRequest target = pending.stream().filter(row -> row.getId().equals(joinRequestId)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("参加希望が見つかりません。"));

        if (approve) {
            if (isRecruitmentClosed(owned, scheduleMapper.countParticipants(scheduleId))) {
                throw new IllegalArgumentException("この募集は締め切られているため承認できません。");
            }
            if (!scheduleMapper.existsParticipant(scheduleId, target.getRequesterUserId())) {
                scheduleMapper.insertParticipant(scheduleId, target.getRequesterUserId());
            }
            scheduleMapper.updateJoinRequestStatus(joinRequestId, "APPROVED");
            notificationEventService.publish(
                    target.getRequesterUserId(),
                    currentUser.getId(),
                    "SCHEDULE_JOIN_APPROVED",
                    "参加承認",
                    "「" + owned.getTitle() + "」への参加希望が承認されました。");
            return;
        }

        scheduleMapper.updateJoinRequestStatus(joinRequestId, "REJECTED");
        notificationEventService.publish(
                target.getRequesterUserId(),
                currentUser.getId(),
                "SCHEDULE_JOIN_REJECTED",
                "参加見送り",
                "「" + owned.getTitle() + "」への参加希望は見送られました。");
    }

    @Transactional
    public ScheduleItem complete(Long id, String currentUsername) {
        AppUser currentUser = getCurrentUser(currentUsername);
        ScheduleItem existing = scheduleMapper.findOwnedById(id, currentUser.getId());
        if (existing == null) {
            throw new NoSuchElementException("指定された予定が存在しないか、編集権限がありません。id=" + id);
        }
        if (Boolean.TRUE.equals(existing.getCompleted())) {
            ScheduleItem alreadyCompleted = scheduleMapper.findVisibleById(id, currentUser.getId());
            decorateForViewer(alreadyCompleted, currentUser.getId());
            return alreadyCompleted;
        }

        int updated = scheduleMapper.markCompleted(id, currentUser.getId(), LocalDateTime.now());
        if (updated == 0) {
            throw new NoSuchElementException("指定された予定が存在しないか、編集権限がありません。id=" + id);
        }

        ScheduleItem completed = scheduleMapper.findVisibleById(id, currentUser.getId());
        gamificationService.awardScheduleCompleted(currentUser.getId(), completed);
        decorateForViewer(completed, currentUser.getId());
        return completed;
    }

    @Transactional
    public ScheduleItem uncomplete(Long id, String currentUsername) {
        AppUser currentUser = getCurrentUser(currentUsername);
        ScheduleItem existing = scheduleMapper.findOwnedById(id, currentUser.getId());
        if (existing == null) {
            throw new NoSuchElementException("指定された予定が存在しないか、編集権限がありません。id=" + id);
        }

        int updated = scheduleMapper.markIncomplete(id, currentUser.getId());
        if (updated == 0) {
            throw new NoSuchElementException("指定された予定が存在しないか、編集権限がありません。id=" + id);
        }
        ScheduleItem uncompleted = scheduleMapper.findVisibleById(id, currentUser.getId());
        decorateForViewer(uncompleted, currentUser.getId());
        return uncompleted;
    }

    @Transactional
    public ScheduleItem shareToFriends(Long sourceScheduleId, String currentUsername) {
        AppUser currentUser = getCurrentUser(currentUsername);
        ScheduleItem source = scheduleMapper.findVisibleById(sourceScheduleId, currentUser.getId());
        if (source == null) {
            throw new NoSuchElementException("共有対象の予定が見つかりません。");
        }
        if (currentUser.getId().equals(source.getOwnerUserId())) {
            throw new IllegalArgumentException("自分の予定は再シェアできません。");
        }
        if (!Boolean.TRUE.equals(source.getJoinable())) {
            throw new IllegalArgumentException("参加募集予定のみ再シェアできます。");
        }
        if (!Boolean.TRUE.equals(source.getMessageShareable())) {
            throw new IllegalArgumentException("この募集メッセージは再シェア不可です。");
        }
        if (scheduleMapper.existsSharedCopyBySource(currentUser.getId(), source.getId())) {
            throw new IllegalArgumentException("この募集はすでにシェア済みです。");
        }

        ScheduleItem copy = new ScheduleItem();
        copy.setOwnerUserId(currentUser.getId());
        copy.setScheduleDate(source.getScheduleDate());
        copy.setPriority(source.getPriority());
        copy.setDeviceType(source.getDeviceType());
        copy.setRankBand(source.getRankBand());
        copy.setCompleted(false);
        copy.setCompletedAt(null);
        copy.setMessageShareable(true);
        copy.setSourceScheduleItemId(source.getId());
        copy.setSourceOwnerUserId(source.getOwnerUserId());
        copy.setTitle(source.getTitle());
        copy.setStartTime(source.getStartTime());
        copy.setEndTime(source.getEndTime());
        copy.setDescription(source.getDescription());
        copy.setSharedWithFriends(true);
        copy.setJoinable(true);
        copy.setRecruitmentLimit(source.getRecruitmentLimit());

        scheduleMapper.insert(copy);
        ScheduleItem created = scheduleMapper.findVisibleById(copy.getId(), currentUser.getId());
        gamificationService.awardScheduleCreated(currentUser.getId(), created.getTitle(), created.getId());
        decorateForViewer(created, currentUser.getId());
        return created;
    }

    private List<List<String>> parseCsv(MultipartFile file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder content = new StringBuilder();
            char[] buffer = new char[2048];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                content.append(buffer, 0, read);
            }
            return splitCsvRecords(content.toString());
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to read CSV file.");
        }
    }

    private List<List<String>> splitCsvRecords(String raw) {
        List<List<String>> records = new ArrayList<>();
        List<String> currentRow = new ArrayList<>();
        StringBuilder currentCell = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < raw.length(); i += 1) {
            char ch = raw.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < raw.length() && raw.charAt(i + 1) == '"') {
                        currentCell.append('"');
                        i += 1;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    currentCell.append(ch);
                }
                continue;
            }

            if (ch == '"') {
                inQuotes = true;
                continue;
            }
            if (ch == ',') {
                currentRow.add(currentCell.toString());
                currentCell.setLength(0);
                continue;
            }
            if (ch == '\n') {
                currentRow.add(currentCell.toString());
                currentCell.setLength(0);
                records.add(currentRow);
                currentRow = new ArrayList<>();
                continue;
            }
            if (ch != '\r') {
                currentCell.append(ch);
            }
        }

        currentRow.add(currentCell.toString());
        if (!currentRow.isEmpty() && !(currentRow.size() == 1 && currentRow.get(0).isBlank())) {
            records.add(currentRow);
        }
        return records;
    }

    private Map<String, Integer> buildHeaderIndex(List<String> headerRow) {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < headerRow.size(); i += 1) {
            String rawHeader = headerRow.get(i);
            String header = normalize(rawHeader);
            if (header != null && !header.isBlank()) {
                if (i == 0 && !header.isEmpty() && header.charAt(0) == '\uFEFF') {
                    header = header.substring(1);
                }
                index.put(header.toLowerCase(Locale.ROOT), i);
            }
        }
        return index;
    }

    private void validateHeaders(Map<String, Integer> headerIndex) {
        for (String required : List.of("scheduledate", "title")) {
            if (!headerIndex.containsKey(required)) {
                throw new IllegalArgumentException("Missing required CSV header: " + required);
            }
        }
    }

    private boolean isEmptyRow(List<String> row) {
        for (String cell : row) {
            if (cell != null && !cell.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private ScheduleItem parseCsvRow(List<String> row, Map<String, Integer> headerIndex, int rowNumber) {
        String scheduleDateRaw = getCsvValue(row, headerIndex, "scheduledate");
        String titleRaw = getCsvValue(row, headerIndex, "title");
        String priorityRaw = getCsvValue(row, headerIndex, "priority");
        String deviceTypeRaw = getCsvValue(row, headerIndex, "devicetype");
        String rankBandRaw = getCsvValue(row, headerIndex, "rankband");
        String startTimeRaw = getCsvValue(row, headerIndex, "starttime");
        String endTimeRaw = getCsvValue(row, headerIndex, "endtime");
        String descriptionRaw = getCsvValue(row, headerIndex, "description");
        String sharedWithFriendsRaw = getCsvValue(row, headerIndex, "sharedwithfriends");
        String joinableRaw = getCsvValue(row, headerIndex, "joinable");
        String messageShareableRaw = getCsvValue(row, headerIndex, "messageshareable");
        String recruitmentLimitRaw = getCsvValue(row, headerIndex, "recruitmentlimit");

        LocalDate scheduleDate = parseRequiredDate(scheduleDateRaw, rowNumber, "scheduleDate");
        String title = normalize(titleRaw);
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("row " + rowNumber + ": title is required.");
        }
        if (title.length() > 200) {
            throw new IllegalArgumentException("row " + rowNumber + ": title must be <= 200 chars.");
        }

        String priority = normalizePriority(priorityRaw);
        String deviceType = normalizeDeviceType(deviceTypeRaw);
        String rankBand = normalizeRankBand(rankBandRaw);
        LocalTime startTime = parseOptionalTime(startTimeRaw, rowNumber, "startTime");
        LocalTime endTime = parseOptionalTime(endTimeRaw, rowNumber, "endTime");
        String description = normalize(descriptionRaw);
        if (description != null && description.length() > 1000) {
            throw new IllegalArgumentException("row " + rowNumber + ": description must be <= 1000 chars.");
        }

        boolean joinable = parseOptionalBoolean(joinableRaw, rowNumber, "joinable", false);
        boolean sharedWithFriends = parseOptionalBoolean(sharedWithFriendsRaw, rowNumber, "sharedWithFriends", false);
        boolean messageShareable = parseOptionalBoolean(messageShareableRaw, rowNumber, "messageShareable", false);
        Integer recruitmentLimit = parseOptionalInt(recruitmentLimitRaw, rowNumber, "recruitmentLimit");

        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("row " + rowNumber + ": endTime must be after startTime.");
        }
        if (joinable && startTime == null) {
            throw new IllegalArgumentException("row " + rowNumber + ": startTime is required when joinable=true.");
        }
        if (joinable) {
            if (recruitmentLimit == null) {
                throw new IllegalArgumentException("row " + rowNumber + ": recruitmentLimit is required when joinable=true.");
            }
            if (recruitmentLimit < 1) {
                throw new IllegalArgumentException("row " + rowNumber + ": recruitmentLimit must be >= 1.");
            }
            sharedWithFriends = true;
        } else {
            recruitmentLimit = null;
            messageShareable = false;
        }

        ScheduleItem item = new ScheduleItem();
        item.setScheduleDate(scheduleDate);
        item.setTitle(title);
        item.setPriority(priority);
        item.setDeviceType(deviceType);
        item.setRankBand(rankBand);
        item.setStartTime(startTime);
        item.setEndTime(endTime);
        item.setDescription(description);
        item.setSharedWithFriends(sharedWithFriends);
        item.setJoinable(joinable);
        item.setMessageShareable(joinable && messageShareable);
        item.setRecruitmentLimit(recruitmentLimit);
        return item;
    }

    private String getCsvValue(List<String> row, Map<String, Integer> headerIndex, String key) {
        Integer idx = headerIndex.get(key);
        if (idx == null || idx < 0 || idx >= row.size()) {
            return null;
        }
        return row.get(idx);
    }

    private LocalDate parseRequiredDate(String value, int rowNumber, String field) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("row " + rowNumber + ": " + field + " is required.");
        }
        try {
            return LocalDate.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("row " + rowNumber + ": " + field + " must be yyyy-MM-dd.");
        }
    }

    private LocalTime parseOptionalTime(String value, int rowNumber, String field) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        try {
            return LocalTime.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("row " + rowNumber + ": " + field + " must be HH:mm.");
        }
    }

    private boolean parseOptionalBoolean(String value, int rowNumber, String field, boolean defaultValue) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            return defaultValue;
        }
        String lower = normalized.toLowerCase(Locale.ROOT);
        if ("true".equals(lower) || "1".equals(lower) || "yes".equals(lower) || "y".equals(lower)) {
            return true;
        }
        if ("false".equals(lower) || "0".equals(lower) || "no".equals(lower) || "n".equals(lower)) {
            return false;
        }
        throw new IllegalArgumentException("row " + rowNumber + ": " + field + " must be true/false.");
    }

    private Integer parseOptionalInt(String value, int rowNumber, String field) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(normalized);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("row " + rowNumber + ": " + field + " must be integer.");
        }
    }

    private ScheduleItem fromRequest(ScheduleRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("リクエストが空です。");
        }

        String title = normalize(request.getTitle());
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("ゲーム名は必須です。");
        }

        LocalDate scheduleDate;
        try {
            scheduleDate = LocalDate.parse(normalize(request.getScheduleDate()));
        } catch (DateTimeParseException | NullPointerException ex) {
            throw new IllegalArgumentException("日付は YYYY-MM-DD 形式で指定してください。");
        }

        String priority = normalizePriority(request.getPriority());
        String deviceType = normalizeDeviceType(request.getDeviceType());
        String rankBand = normalizeRankBand(request.getRankBand());

        LocalTime startTime = parseTime(request.getStartTime(), "開始時刻");
        LocalTime endTime = parseTime(request.getEndTime(), "終了時刻");
        boolean joinable = Boolean.TRUE.equals(request.getJoinable());
        boolean messageShareable = joinable && Boolean.TRUE.equals(request.getMessageShareable());
        Integer recruitmentLimit = request.getRecruitmentLimit();

        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("終了時刻は開始時刻以降にしてください。");
        }
        if (joinable && startTime == null) {
            throw new IllegalArgumentException("参加募集予定は開始時刻を入力してください。");
        }
        if (joinable) {
            if (recruitmentLimit == null) {
                throw new IllegalArgumentException("募集人数を入力してください。");
            }
            if (recruitmentLimit < 1) {
                throw new IllegalArgumentException("募集人数は1以上で入力してください。");
            }
        } else {
            recruitmentLimit = null;
        }

        ScheduleItem item = new ScheduleItem();
        item.setScheduleDate(scheduleDate);
        item.setPriority(priority);
        item.setDeviceType(deviceType);
        item.setRankBand(rankBand);
        item.setTitle(title);
        item.setStartTime(startTime);
        item.setEndTime(endTime);
        item.setDescription(normalize(request.getDescription()));
        boolean sharedWithFriends = Boolean.TRUE.equals(request.getSharedWithFriends()) || joinable;
        item.setSharedWithFriends(sharedWithFriends);
        item.setJoinable(joinable);
        item.setMessageShareable(messageShareable);
        item.setSourceScheduleItemId(null);
        item.setSourceOwnerUserId(null);
        item.setRecruitmentLimit(recruitmentLimit);
        return item;
    }

    private AppUser getCurrentUser(String username) {
        AppUser user = userMapper.findByUsername(username);
        if (user == null) {
            throw new NoSuchElementException("ログインユーザーが見つかりません。");
        }
        return user;
    }

    private void notifyFriendFollowers(AppUser actor, String scheduleTitle, String suffix) {
        if (actor == null || actor.getId() == null) {
            return;
        }
        List<Long> recipients = friendNotificationPreferenceService.findRecipientsByActor(actor.getId());
        if (recipients.isEmpty()) {
            return;
        }
        String actorName = actor.getDisplayName() == null ? actor.getUsername() : actor.getDisplayName();
        String title = scheduleTitle == null || scheduleTitle.isBlank() ? "予定" : scheduleTitle;
        for (Long recipientUserId : recipients) {
            notificationEventService.publish(
                    recipientUserId,
                    actor.getId(),
                    "FRIEND_SCHEDULE_UPDATE",
                    "フレンド更新通知",
                    actorName + " さんが「" + title + "」" + suffix);
        }
    }

    private LocalTime parseTime(String value, String fieldName) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }

        try {
            return LocalTime.parse(normalized);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException(fieldName + "は HH:mm 形式で指定してください。");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private String normalizePriority(String value) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            return "LOW";
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if ("HIGH".equals(upper) || "MEDIUM".equals(upper) || "LOW".equals(upper)) {
            return upper;
        }
        throw new IllegalArgumentException("優先度は HIGH / MEDIUM / LOW で指定してください。");
    }

    private String normalizeDeviceType(String value) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            return "PC";
        }
        String upper = normalized.toUpperCase(Locale.ROOT);
        if ("PC".equals(upper)) {
            return "PC";
        }
        if ("CONSOLE".equals(upper) || "家庭用ゲーム機".equals(normalized)) {
            return "CONSOLE";
        }
        throw new IllegalArgumentException("デバイスは PC / 家庭用ゲーム機 で指定してください。");
    }

    private String normalizeRankBand(String value) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > 100) {
            throw new IllegalArgumentException("rankBand must be <= 100 chars.");
        }
        return normalized;
    }

    private LocalDateTime toDueAt(ScheduleItem item) {
        LocalDate date = item.getScheduleDate();
        LocalTime time = item.getStartTime() == null ? LocalTime.of(23, 59) : item.getStartTime();
        return LocalDateTime.of(date, time);
    }

    private void decorateForViewer(ScheduleItem item, Long viewerUserId) {
        if (item == null) {
            return;
        }
        item.setEditable(viewerUserId.equals(item.getOwnerUserId()));
        attachParticipationInfo(item, viewerUserId);
    }

    private void decorateForViewer(List<ScheduleItem> items, Long viewerUserId) {
        for (ScheduleItem item : items) {
            decorateForViewer(item, viewerUserId);
        }
    }

    private void attachParticipationInfo(ScheduleItem item, Long viewerUserId) {
        if (!Boolean.TRUE.equals(item.getJoinable())) {
            item.setParticipantCount(0);
            item.setRemainingRecruitmentSlots(null);
            item.setRecruitmentClosed(false);
            item.setJoinedByCurrentUser(false);
            item.setJoinRequestStatusForCurrentUser(null);
            item.setJoinRequestCommentForCurrentUser(null);
            item.setPendingJoinRequests(List.of());
            item.setParticipants(List.of());
            return;
        }
        int participantCount = scheduleMapper.countParticipants(item.getId());
        item.setParticipantCount(participantCount);
        item.setRemainingRecruitmentSlots(calcRemainingRecruitmentSlots(item.getRecruitmentLimit(), participantCount));
        item.setRecruitmentClosed(isRecruitmentClosed(item, participantCount));
        item.setJoinedByCurrentUser(scheduleMapper.existsParticipant(item.getId(), viewerUserId));
        item.setParticipants(scheduleMapper.findParticipants(item.getId()));
        ScheduleJoinRequest ownRequest = scheduleMapper.findJoinRequestByScheduleAndRequester(item.getId(), viewerUserId);
        item.setJoinRequestStatusForCurrentUser(ownRequest == null ? null : ownRequest.getStatus());
        item.setJoinRequestCommentForCurrentUser(ownRequest == null ? null : ownRequest.getComment());
        if (viewerUserId.equals(item.getOwnerUserId())) {
            item.setPendingJoinRequests(scheduleMapper.findPendingJoinRequestsBySchedule(item.getId()));
        } else {
            item.setPendingJoinRequests(List.of());
        }
    }

    private String normalizeJoinRequestComment(String value) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            throw new IllegalArgumentException("参加希望コメントを入力してください。");
        }
        if (normalized.length() > 500) {
            throw new IllegalArgumentException("参加希望コメントは500文字以内で入力してください。");
        }
        return normalized;
    }

    private void validateJoinable(ScheduleItem target, AppUser currentUser) {
        if (target == null) {
            throw new NoSuchElementException("対象の予定が見つかりません。");
        }
        if (!Boolean.TRUE.equals(target.getJoinable())) {
            throw new IllegalArgumentException("この予定は参加募集していません。");
        }
        if (currentUser.getId().equals(target.getOwnerUserId())) {
            throw new IllegalArgumentException("作成者は自動的に参加者として扱われます。");
        }
    }

    private Integer calcRemainingRecruitmentSlots(Integer recruitmentLimit, int participantCount) {
        if (recruitmentLimit == null) {
            return null;
        }
        return Math.max(recruitmentLimit - participantCount, 0);
    }

    private boolean isRecruitmentClosed(ScheduleItem item, int participantCount) {
        Integer recruitmentLimit = item.getRecruitmentLimit();
        if (recruitmentLimit == null) {
            return false;
        }
        return participantCount >= recruitmentLimit;
    }
}
