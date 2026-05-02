package com.example.schedulemanager.service;

import com.example.schedulemanager.mapper.LabelColorMapper;
import com.example.schedulemanager.model.LabelColorSetting;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LabelColorService {
    public static final String KEY_LABEL_TEXT = "labelText";
    public static final String KEY_LABEL_HEADING = "labelHeading";
    public static final String KEY_LABEL_MUTED = "labelMuted";

    private static final Map<String, String> DEFAULTS = Map.of(
            KEY_LABEL_TEXT, "#2B4E93",
            KEY_LABEL_HEADING, "#7D35D8",
            KEY_LABEL_MUTED, "#445A86");

    private final LabelColorMapper labelColorMapper;

    public LabelColorService(LabelColorMapper labelColorMapper) {
        this.labelColorMapper = labelColorMapper;
    }

    @Transactional(readOnly = true)
    public Map<String, String> getResolvedColors(Long userId) {
        Map<String, String> result = new LinkedHashMap<>(DEFAULTS);
        if (userId == null) {
            return result;
        }
        List<LabelColorSetting> rows = labelColorMapper.findByUserId(userId);
        for (LabelColorSetting row : rows) {
            String key = normalizeKey(row.getLabelKey());
            if (key == null || !DEFAULTS.containsKey(key)) {
                continue;
            }
            String color = normalizeColor(row.getColorHex());
            if (color != null) {
                result.put(key, color);
            }
        }
        return result;
    }

    @Transactional
    public Map<String, String> saveColor(Long userId, String labelKey, String colorHex) {
        if (userId == null) {
            throw new IllegalArgumentException("User not found.");
        }
        String key = normalizeKey(labelKey);
        if (key == null || !DEFAULTS.containsKey(key)) {
            throw new IllegalArgumentException("Unknown label key.");
        }
        String color = normalizeColor(colorHex);
        if (color == null) {
            throw new IllegalArgumentException("Color must be #RRGGBB.");
        }
        labelColorMapper.upsert(userId, key, color);
        return getResolvedColors(userId);
    }

    @Transactional
    public Map<String, String> resetOne(Long userId, String labelKey) {
        if (userId == null) {
            throw new IllegalArgumentException("User not found.");
        }
        String key = normalizeKey(labelKey);
        if (key == null || !DEFAULTS.containsKey(key)) {
            throw new IllegalArgumentException("Unknown label key.");
        }
        labelColorMapper.deleteOne(userId, key);
        return getResolvedColors(userId);
    }

    @Transactional
    public Map<String, String> resetAll(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User not found.");
        }
        labelColorMapper.deleteAll(userId);
        return getResolvedColors(userId);
    }

    @Transactional(readOnly = true)
    public String toInlineStyle(Long userId) {
        Map<String, String> colors = getResolvedColors(userId);
        return "--label-text-color:" + colors.get(KEY_LABEL_TEXT)
                + ";--label-heading-color:" + colors.get(KEY_LABEL_HEADING)
                + ";--label-muted-color:" + colors.get(KEY_LABEL_MUTED) + ";";
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        return normalized;
    }

    private String normalizeColor(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("^#[0-9A-F]{6}$")) {
            return null;
        }
        return normalized;
    }
}
