package com.example.schedulemanager.controller;

import com.example.schedulemanager.dto.ProfileUpdateRequest;
import com.example.schedulemanager.model.AppUser;
import com.example.schedulemanager.service.GamificationService;
import com.example.schedulemanager.service.LabelColorService;
import com.example.schedulemanager.service.UserAccountService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/me")
public class UserApiController {
    private final UserAccountService userAccountService;
    private final GamificationService gamificationService;
    private final LabelColorService labelColorService;

    public UserApiController(
            UserAccountService userAccountService,
            GamificationService gamificationService,
            LabelColorService labelColorService) {
        this.userAccountService = userAccountService;
        this.gamificationService = gamificationService;
        this.labelColorService = labelColorService;
    }

    @GetMapping
    public Map<String, Object> me(@AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        return toMap(user);
    }

    @GetMapping("/profile-image")
    public ResponseEntity<byte[]> profileImage(@AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        byte[] imageData = user.getProfileImageData();
        if (imageData == null || imageData.length == 0) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(parseMediaType(user.getProfileImageContentType()))
                .body(imageData);
    }

    @GetMapping("/points/history")
    public List<Map<String, Object>> pointHistory(@AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        return gamificationService.listPointHistories(user.getId());
    }

    @GetMapping("/label-colors")
    public Map<String, String> labelColors(@AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        return labelColorService.getResolvedColors(user.getId());
    }

    @PutMapping("/label-colors/{labelKey}")
    public Map<String, String> saveLabelColor(
            @AuthenticationPrincipal UserDetails userDetails,
            @org.springframework.web.bind.annotation.PathVariable("labelKey") String labelKey,
            @RequestBody Map<String, String> request) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        String color = request == null ? null : request.get("color");
        return labelColorService.saveColor(user.getId(), labelKey, color);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/label-colors/{labelKey}")
    public Map<String, String> resetLabelColor(
            @AuthenticationPrincipal UserDetails userDetails,
            @org.springframework.web.bind.annotation.PathVariable("labelKey") String labelKey) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        return labelColorService.resetOne(user.getId(), labelKey);
    }

    @org.springframework.web.bind.annotation.DeleteMapping("/label-colors")
    public Map<String, String> resetAllLabelColors(@AuthenticationPrincipal UserDetails userDetails) {
        AppUser user = userAccountService.getByUsername(userDetails.getUsername());
        return labelColorService.resetAll(user.getId());
    }
    @GetMapping("/test-error")
public String testError() {
    throw new RuntimeException("テストエラー");
}

    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> updateMeJson(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody ProfileUpdateRequest request) {
        AppUser user = userAccountService.updateProfile(userDetails.getUsername(), request);
        return toMap(user);
    }

    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> updateMe(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("displayName") String displayName,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "profileBio", required = false) String profileBio,
            @RequestParam(value = "profileIconColor", required = false) String profileIconColor,
            @RequestParam(value = "removeProfileImage", defaultValue = "false") boolean removeProfileImage,
            @RequestPart(value = "profileImageFile", required = false) MultipartFile profileImageFile) {
        AppUser user = userAccountService.updateProfile(
                userDetails.getUsername(),
                displayName,
                email,
                profileBio,
                profileIconColor,
                profileImageFile,
                removeProfileImage);
        return toMap(user);
    }

    private Map<String, Object> toMap(AppUser user) {
        String profileImageUrl = "";
        if (user.getProfileImageData() != null && user.getProfileImageData().length > 0) {
            profileImageUrl = "/api/me/profile-image";
        } else if (user.getProfileImageUrl() != null) {
            profileImageUrl = user.getProfileImageUrl();
        }
        String profileIconColor = user.getProfileIconColor();
        if (profileIconColor == null || profileIconColor.isBlank()) {
            profileIconColor = UserAccountService.DEFAULT_PROFILE_ICON_COLOR;
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("displayName", user.getDisplayName());
        response.put("email", user.getEmail() == null ? "" : user.getEmail());
        response.put("profileBio", user.getProfileBio() == null ? "" : user.getProfileBio());
        response.put("profileImageUrl", profileImageUrl);
        response.put("profileIconColor", profileIconColor);
        response.putAll(gamificationService.buildUserProgressSummary(user.getId()));
        return response;
    }

    private MediaType parseMediaType(String mediaTypeValue) {
        if (mediaTypeValue == null || mediaTypeValue.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(mediaTypeValue);
        } catch (InvalidMediaTypeException ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
