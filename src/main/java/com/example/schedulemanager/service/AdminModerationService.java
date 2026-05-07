package com.example.schedulemanager.service;

import com.example.schedulemanager.mapper.AdminModerationMapper;
import com.example.schedulemanager.mapper.UserMapper;
import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.model.BoardPost;
import com.example.schedulemanager.model.BoardPostInterest;
import com.example.schedulemanager.model.DirectMessage;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminModerationService {
    private final AdminModerationMapper adminModerationMapper;
    private final AdminGuardService adminGuardService;
    private final UserMapper userMapper;

    public AdminModerationService(
            AdminModerationMapper adminModerationMapper,
            AdminGuardService adminGuardService,
            UserMapper userMapper) {
        this.adminModerationMapper = adminModerationMapper;
        this.adminGuardService = adminGuardService;
        this.userMapper = userMapper;
    }

    @Transactional(readOnly = true)
    public List<AppUser> listUsers(String adminUsername) {
        adminGuardService.requireAdmin(adminUsername);
        return adminModerationMapper.findUsers();
    }

    @Transactional(readOnly = true)
    public List<BoardPost> listBoardPosts(String adminUsername, String keyword, Long targetUserId) {
        adminGuardService.requireAdmin(adminUsername);
        return adminModerationMapper.findBoardPosts(normalize(keyword), targetUserId);
    }

    @Transactional(readOnly = true)
    public List<BoardPostInterest> listBoardInterests(String adminUsername, String keyword, Long targetUserId) {
        adminGuardService.requireAdmin(adminUsername);
        return adminModerationMapper.findBoardInterests(normalize(keyword), targetUserId);
    }

    @Transactional(readOnly = true)
    public List<DirectMessage> listDirectMessages(String adminUsername, String keyword, Long targetUserId) {
        adminGuardService.requireAdmin(adminUsername);
        return adminModerationMapper.findDirectMessages(normalize(keyword), targetUserId);
    }

    @Transactional
    public void deleteBoardPost(String adminUsername, Long id) {
        adminGuardService.requireAdmin(adminUsername);
        adminModerationMapper.deleteBoardInterestsByPostId(id);
        adminModerationMapper.deleteBoardPost(id);
    }

    @Transactional
    public void deleteBoardInterest(String adminUsername, Long id) {
        adminGuardService.requireAdmin(adminUsername);
        adminModerationMapper.deleteBoardInterest(id);
    }

    @Transactional
    public void deleteDirectMessage(String adminUsername, Long id) {
        adminGuardService.requireAdmin(adminUsername);
        adminModerationMapper.deleteDirectMessage(id);
    }

    @Transactional
    public void setUserBan(String adminUsername, Long targetUserId, boolean banned) {
        adminGuardService.requireAdmin(adminUsername);
        AppUser user = userMapper.findById(targetUserId);
        if (user == null) {
            throw new IllegalArgumentException("対象ユーザーが見つかりません。");
        }
        if (adminGuardService.isAdmin(user.getUsername())) {
            throw new IllegalArgumentException("管理者ユーザーはBANできません。");
        }
        userMapper.updateEnabled(targetUserId, !banned);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
