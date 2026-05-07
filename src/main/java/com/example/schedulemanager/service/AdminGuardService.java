package com.example.schedulemanager.service;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

@Service
public class AdminGuardService {

    private final Set<String> adminUsernames;

    public AdminGuardService(@Value("${app.admin.usernames:}") String adminUsernamesRaw) {
        this.adminUsernames = Arrays.stream(adminUsernamesRaw.split(","))
                .map(String::trim)
                .filter((v) -> !v.isBlank())
                .collect(Collectors.toSet());
    }

    public boolean isAdmin(String username) {
        if (username == null) {
            return false;
        }
        return adminUsernames.contains(username.trim());
    }

    public void requireAdmin(String username) {
        if (!isAdmin(username)) {
            throw new AccessDeniedException("管理者権限が必要です。");
        }
    }
}
