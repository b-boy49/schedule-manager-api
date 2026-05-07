package com.example.schedulemanager.service;

import com.example.schedulemanager.dto.PlayerReportCreateRequest;
import com.example.schedulemanager.mapper.PlayerReportMapper;
import com.example.schedulemanager.mapper.UserMapper;
import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.model.PlayerReport;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerReportService {
    private static final Set<String> ALLOWED_CATEGORIES = Set.of(
            "HARASSMENT", "HATE_SPEECH", "SPAM", "SEXUAL", "VIOLENCE", "OTHER"
    );

    private final PlayerReportMapper playerReportMapper;
    private final UserMapper userMapper;
    private final AdminGuardService adminGuardService;

    public PlayerReportService(PlayerReportMapper playerReportMapper, UserMapper userMapper, AdminGuardService adminGuardService) {
        this.playerReportMapper = playerReportMapper;
        this.userMapper = userMapper;
        this.adminGuardService = adminGuardService;
    }

    @Transactional
    public void create(String reporterUsername, PlayerReportCreateRequest request) {
        AppUser reporter = userMapper.findByUsername(reporterUsername);
        if (reporter == null) {
            throw new IllegalArgumentException("通報者が見つかりません。");
        }
        if (request == null || request.getTargetUserId() == null) {
            throw new IllegalArgumentException("通報対象が不正です。");
        }
        if (reporter.getId().equals(request.getTargetUserId())) {
            throw new IllegalArgumentException("自分自身は通報できません。");
        }
        AppUser target = userMapper.findById(request.getTargetUserId());
        if (target == null) {
            throw new IllegalArgumentException("通報対象ユーザーが見つかりません。");
        }

        String sourceType = normalize(request.getSourceType());
        if (sourceType == null) {
            throw new IllegalArgumentException("通報種別が不正です。");
        }
        String category = normalize(request.getCategory());
        if (category == null || !ALLOWED_CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("通報カテゴリが不正です。");
        }
        String note = normalize(request.getNote());
        if (note == null || note.length() < 5) {
            throw new IllegalArgumentException("備考は5文字以上で入力してください。");
        }
        if (note.length() > 500) {
            throw new IllegalArgumentException("備考は500文字以内で入力してください。");
        }

        PlayerReport report = new PlayerReport();
        report.setReporterUserId(reporter.getId());
        report.setTargetUserId(request.getTargetUserId());
        report.setSourceType(sourceType);
        report.setSourceId(request.getSourceId());
        report.setCategory(category);
        report.setNote(note);
        report.setReason(category + ": " + note);
        report.setStatus("OPEN");
        playerReportMapper.insert(report);
    }

    @Transactional(readOnly = true)
    public List<PlayerReport> listOpenReports(String adminUsername) {
        adminGuardService.requireAdmin(adminUsername);
        return playerReportMapper.findByStatus("OPEN");
    }

    @Transactional
    public void resolveReport(String adminUsername, Long reportId) {
        adminGuardService.requireAdmin(adminUsername);
        playerReportMapper.updateStatus(reportId, "RESOLVED");
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
