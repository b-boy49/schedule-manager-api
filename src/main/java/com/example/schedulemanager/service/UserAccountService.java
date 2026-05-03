package com.example.schedulemanager.service;

import com.example.schedulemanager.dto.RegisterRequest;
import com.example.schedulemanager.dto.ProfileUpdateRequest;
import com.example.schedulemanager.mapper.UserMapper;
import com.example.schedulemanager.model.AppUser;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserAccountService {
    public static final String DEFAULT_PROFILE_ICON_COLOR = "#BFD6FF";

    private static final int MAX_PROFILE_IMAGE_SIZE_BYTES = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED_PROFILE_IMAGE_TYPES =
            Set.of("image/png", "image/jpeg", "image/webp", "image/gif");

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public AppUser getByUsername(String username) {
        AppUser user = userMapper.findByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("ユーザーが存在しません。");
        }
        return user;
    }

    @Transactional(readOnly = true)
    public AppUser getById(Long userId) {
        AppUser user = userMapper.findById(userId);
        if (user == null) {
            throw new NoSuchElementException("ユーザーが見つかりません。");
        }
        return user;
    }

    @Transactional
    public AppUser register(RegisterRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("登録情報が空です。");
        }

        String username = normalize(request.getUsername());
        String password = normalize(request.getPassword());
        String email = normalizeEmail(request.getEmail());
        String displayName = normalize(request.getDisplayName());

        validateUsername(username);
        validatePassword(password);
        validateEmail(email, true);

        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("表示名は必須です。");
        }
        if (displayName.length() > 100) {
            throw new IllegalArgumentException("表示名は100文字以内で入力してください。");
        }

        if (userMapper.findByUsername(username) != null) {
            throw new IllegalArgumentException("そのユーザー名はすでに使われています。");
        }
        if (userMapper.findByEmail(email) != null) {
            throw new IllegalArgumentException("そのメールアドレスはすでに使われています。");
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setEmail(email);
        user.setTotalPoints(0);
        user.setDisplayName(displayName);
        user.setProfileIconColor(DEFAULT_PROFILE_ICON_COLOR);
        user.setEnabled(true);
        userMapper.insert(user);
        return userMapper.findById(user.getId());
    }

    @Transactional
    public AppUser updateProfile(String username, ProfileUpdateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("更新情報が空です。");
        }
        return updateProfile(
                username,
                request.getDisplayName(),
                request.getEmail(),
                request.getProfileBio(),
                request.getXUrl(),
                request.getStreamUrl(),
                request.getProfileIconColor(),
                null,
                false);
    }

    @Transactional
    public AppUser updateProfile(
            String username,
            String displayNameValue,
            String emailValue,
            String profileBioValue,
            String xUrlValue,
            String streamUrlValue,
            String profileIconColorValue,
            MultipartFile profileImageFile,
            boolean removeProfileImage) {
        AppUser user = userMapper.findByUsername(username);
        if (user == null) {
            throw new NoSuchElementException("ユーザーが見つかりません。");
        }

        String displayName = normalize(displayNameValue);
        String email = normalizeEmail(emailValue);
        String profileBio = normalize(profileBioValue);
        String xUrl = normalizeUrl(xUrlValue, "X URL");
        String streamUrl = normalizeUrl(streamUrlValue, "配信先URL");
        String profileIconColor = normalizeProfileIconColor(profileIconColorValue);
        if (profileIconColor == null) {
            profileIconColor = normalizeProfileIconColor(user.getProfileIconColor());
        }
        if (profileIconColor == null) {
            profileIconColor = DEFAULT_PROFILE_ICON_COLOR;
        }

        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("表示名は必須です。");
        }
        if (displayName.length() > 100) {
            throw new IllegalArgumentException("表示名は100文字以内で入力してください。");
        }
        validateEmail(email, false);
        if (profileBio != null && profileBio.length() > 500) {
            throw new IllegalArgumentException("自己紹介は500文字以内で入力してください。");
        }

        if (email != null) {
            AppUser emailOwner = userMapper.findByEmail(email);
            if (emailOwner != null && !emailOwner.getId().equals(user.getId())) {
                throw new IllegalArgumentException("そのメールアドレスはすでに使われています。");
            }
        }

        boolean hasUpload = profileImageFile != null && !profileImageFile.isEmpty();
        if (removeProfileImage && hasUpload) {
            throw new IllegalArgumentException("画像の削除とアップロードは同時に指定できません。");
        }

        user.setDisplayName(displayName);
        user.setEmail(email);
        user.setProfileBio(profileBio);
        user.setXUrl(xUrl);
        user.setStreamUrl(streamUrl);
        user.setProfileIconColor(profileIconColor);

        if (removeProfileImage) {
            user.setProfileImageUrl(null);
            user.setProfileImageData(null);
            user.setProfileImageContentType(null);
        } else if (hasUpload) {
            applyUploadedProfileImage(user, profileImageFile);
            user.setProfileImageUrl(null);
        }

        userMapper.updateProfile(user);
        return userMapper.findById(user.getId());
    }

    private void validateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("ユーザー名は必須です。");
        }
        if (!username.matches("^[a-zA-Z0-9_]{4,30}$")) {
            throw new IllegalArgumentException("ユーザー名は4〜30文字の英数字/アンダースコアで入力してください。");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("パスワードは必須です。");
        }
        if (password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException("パスワードは8〜72文字で入力してください。");
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private String normalizeEmail(String value) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        return normalized.toLowerCase(Locale.ROOT);
    }

    private void applyUploadedProfileImage(AppUser user, MultipartFile profileImageFile) {
        if (profileImageFile.getSize() > MAX_PROFILE_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("プロフィール画像は2MB以内でアップロードしてください。");
        }

        String contentType = normalizeContentType(profileImageFile.getContentType());
        if (!ALLOWED_PROFILE_IMAGE_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("画像形式は PNG / JPEG / WEBP / GIF のみ対応しています。");
        }

        byte[] imageBytes;
        try {
            imageBytes = profileImageFile.getBytes();
        } catch (IOException ex) {
            throw new IllegalArgumentException("画像の読み込みに失敗しました。");
        }

        if (imageBytes.length == 0) {
            throw new IllegalArgumentException("空の画像ファイルはアップロードできません。");
        }

        user.setProfileImageData(imageBytes);
        user.setProfileImageContentType(contentType);
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return "";
        }
        return contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeProfileIconColor(String colorValue) {
        String normalized = normalize(colorValue);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        if (!normalized.matches("^#[0-9a-fA-F]{6}$")) {
            throw new IllegalArgumentException("アイコン色は #RRGGBB 形式で指定してください。");
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private void validateEmail(String email, boolean required) {
        if (email == null || email.isBlank()) {
            if (required) {
                throw new IllegalArgumentException("メールアドレスは必須です。");
            }
            return;
        }
        if (email.length() > 254) {
            throw new IllegalArgumentException("メールアドレスは254文字以内で入力してください。");
        }
        if (!email.matches("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            throw new IllegalArgumentException("メールアドレスの形式が正しくありません。");
        }
    }

    private String normalizeUrl(String value, String fieldLabel) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > 1000) {
            throw new IllegalArgumentException(fieldLabel + "は1000文字以内で入力してください。");
        }
        try {
            URI uri = new URI(normalized);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null) {
                throw new IllegalArgumentException(fieldLabel + "はURL形式で入力してください。");
            }
            String lowerScheme = scheme.toLowerCase(Locale.ROOT);
            if (!"http".equals(lowerScheme) && !"https".equals(lowerScheme)) {
                throw new IllegalArgumentException(fieldLabel + "はhttp/httpsのみ対応しています。");
            }
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(fieldLabel + "はURL形式で入力してください。");
        }
        return normalized;
    }
}
