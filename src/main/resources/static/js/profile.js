const profileForm = document.getElementById("profileForm");
const displayNameInput = document.getElementById("profileDisplayName");
const emailInput = document.getElementById("profileEmail");
const xUrlInput = document.getElementById("profileXUrl");
const streamUrlInput = document.getElementById("profileStreamUrl");
const imageFileInput = document.getElementById("profileImageFile");
const removeProfileImageInput = document.getElementById("removeProfileImage");
const profileIconColorInput = document.getElementById("profileIconColor");
const bioInput = document.getElementById("profileBio");
const profileMessage = document.getElementById("profileMessage");
const profileImagePreview = document.getElementById("profileImagePreview");
const profilePreviewName = document.getElementById("profilePreviewName");
const profilePreviewBio = document.getElementById("profilePreviewBio");
const profileLevelText = document.getElementById("profileLevelText");
const profilePointText = document.getElementById("profilePointText");
const profileLevelProgressBar = document.getElementById("profileLevelProgressBar");
const profileLevelNextText = document.getElementById("profileLevelNextText");
const pointHistoryList = document.getElementById("pointHistoryList");
const labelTextColorInput = document.getElementById("labelTextColor");
const labelHeadingColorInput = document.getElementById("labelHeadingColor");
const labelMutedColorInput = document.getElementById("labelMutedColor");
const resetLabelTextColorButton = document.getElementById("resetLabelTextColorButton");
const resetLabelHeadingColorButton = document.getElementById("resetLabelHeadingColorButton");
const resetLabelMutedColorButton = document.getElementById("resetLabelMutedColorButton");
const resetAllLabelColorsButton = document.getElementById("resetAllLabelColorsButton");
const DEFAULT_PROFILE_ICON_COLOR = "#BFD6FF";
const state = {
    profileImageUrl: "",
    profileIconColor: DEFAULT_PROFILE_ICON_COLOR,
    previewObjectUrl: null
};

const LABEL_DEFAULTS = {
    labelText: "#2B4E93",
    labelHeading: "#7D35D8",
    labelMuted: "#445A86"
};

profileForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    profileMessage.textContent = "";

    const formData = new FormData();
    formData.append("displayName", displayNameInput.value);
    formData.append("email", emailInput.value || "");
    formData.append("profileBio", bioInput.value || "");
    formData.append("xUrl", xUrlInput.value || "");
    formData.append("streamUrl", streamUrlInput.value || "");
    formData.append("profileIconColor", normalizeColorValue(profileIconColorInput.value));
    formData.append("removeProfileImage", removeProfileImageInput.checked ? "true" : "false");
    if (imageFileInput.files && imageFileInput.files[0]) {
        formData.append("profileImageFile", imageFileInput.files[0]);
    }

    try {
        const user = await fetchJson("/api/me", {
            method: "PUT",
            body: formData
        });
        renderProfile(user);
        profileMessage.style.color = "#087057";
        profileMessage.textContent = "プロフィールを更新しました。";
    } catch (error) {
        profileMessage.style.color = "#be2f2f";
        profileMessage.textContent = error.message;
    }
});

imageFileInput.addEventListener("change", () => {
    if (imageFileInput.files && imageFileInput.files[0]) {
        removeProfileImageInput.checked = false;
        renderPreviewFile(imageFileInput.files[0]);
        return;
    }
    clearPreviewObjectUrl();
    renderPreviewImage(state.profileImageUrl, state.profileIconColor);
});

removeProfileImageInput.addEventListener("change", () => {
    if (!removeProfileImageInput.checked) {
        renderPreviewImage(state.profileImageUrl, state.profileIconColor);
        return;
    }
    imageFileInput.value = "";
    clearPreviewObjectUrl();
    renderPreviewImage("", state.profileIconColor);
});

profileIconColorInput.addEventListener("input", () => {
    const color = normalizeColorValue(profileIconColorInput.value);
    state.profileIconColor = color;
    if (imageFileInput.files && imageFileInput.files[0]) {
        return;
    }
    if (removeProfileImageInput.checked || !state.profileImageUrl) {
        renderPreviewImage("", color);
    }
});

displayNameInput.addEventListener("input", () => {
    profilePreviewName.textContent = displayNameInput.value.trim() || "表示名";
});

bioInput.addEventListener("input", () => {
    profilePreviewBio.textContent = bioInput.value.trim() || "自己紹介はまだありません。";
});

if (labelTextColorInput) {
    labelTextColorInput.addEventListener("change", async () => {
        await saveLabelColor("labelText", labelTextColorInput.value);
    });
}
if (labelHeadingColorInput) {
    labelHeadingColorInput.addEventListener("change", async () => {
        await saveLabelColor("labelHeading", labelHeadingColorInput.value);
    });
}
if (labelMutedColorInput) {
    labelMutedColorInput.addEventListener("change", async () => {
        await saveLabelColor("labelMuted", labelMutedColorInput.value);
    });
}
if (resetLabelTextColorButton) {
    resetLabelTextColorButton.addEventListener("click", async () => {
        await resetLabelColor("labelText");
    });
}
if (resetLabelHeadingColorButton) {
    resetLabelHeadingColorButton.addEventListener("click", async () => {
        await resetLabelColor("labelHeading");
    });
}
if (resetLabelMutedColorButton) {
    resetLabelMutedColorButton.addEventListener("click", async () => {
        await resetLabelColor("labelMuted");
    });
}
if (resetAllLabelColorsButton) {
    resetAllLabelColorsButton.addEventListener("click", async () => {
        await resetAllLabelColors();
    });
}

async function loadProfile() {
    try {
        const user = await fetchJson("/api/me");
        renderProfile(user);
        await loadLabelColors();
        await loadPointHistory();
    } catch (error) {
        profileMessage.style.color = "#be2f2f";
        profileMessage.textContent = error.message;
    }
}

async function loadLabelColors() {
    const colors = await fetchJson("/api/me/label-colors");
    applyLabelColors(colors);
}

async function saveLabelColor(labelKey, colorValue) {
    try {
        const colors = await fetchJson(`/api/me/label-colors/${encodeURIComponent(labelKey)}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ color: normalizeLabelColor(colorValue, labelKey) })
        });
        applyLabelColors(colors);
        profileMessage.style.color = "#087057";
        profileMessage.textContent = "ラベル色を保存しました。";
    } catch (error) {
        profileMessage.style.color = "#be2f2f";
        profileMessage.textContent = error.message;
    }
}

async function resetLabelColor(labelKey) {
    try {
        const colors = await fetchJson(`/api/me/label-colors/${encodeURIComponent(labelKey)}`, {
            method: "DELETE"
        });
        applyLabelColors(colors);
        profileMessage.style.color = "#087057";
        profileMessage.textContent = "ラベル色を初期化しました。";
    } catch (error) {
        profileMessage.style.color = "#be2f2f";
        profileMessage.textContent = error.message;
    }
}

async function resetAllLabelColors() {
    try {
        const colors = await fetchJson("/api/me/label-colors", { method: "DELETE" });
        applyLabelColors(colors);
        profileMessage.style.color = "#087057";
        profileMessage.textContent = "ラベル色をすべて初期化しました。";
    } catch (error) {
        profileMessage.style.color = "#be2f2f";
        profileMessage.textContent = error.message;
    }
}

function applyLabelColors(colors) {
    const labelText = normalizeLabelColor(colors && colors.labelText, "labelText");
    const labelHeading = normalizeLabelColor(colors && colors.labelHeading, "labelHeading");
    const labelMuted = normalizeLabelColor(colors && colors.labelMuted, "labelMuted");

    document.body.style.setProperty("--label-text-color", labelText);
    document.body.style.setProperty("--label-heading-color", labelHeading);
    document.body.style.setProperty("--label-muted-color", labelMuted);

    if (labelTextColorInput) {
        labelTextColorInput.value = labelText;
    }
    if (labelHeadingColorInput) {
        labelHeadingColorInput.value = labelHeading;
    }
    if (labelMutedColorInput) {
        labelMutedColorInput.value = labelMuted;
    }
}

function normalizeLabelColor(value, labelKey) {
    const normalized = String(value || "").trim().toUpperCase();
    if (/^#[0-9A-F]{6}$/.test(normalized)) {
        return normalized;
    }
    return LABEL_DEFAULTS[labelKey] || "#000000";
}

function renderProfile(user) {
    displayNameInput.value = user.displayName || "";
    emailInput.value = user.email || "";
    bioInput.value = user.profileBio || "";
    xUrlInput.value = user.xUrl || "";
    streamUrlInput.value = user.streamUrl || "";
    state.profileImageUrl = user.profileImageUrl || "";
    state.profileIconColor = normalizeColorValue(user.profileIconColor || DEFAULT_PROFILE_ICON_COLOR);
    profileIconColorInput.value = state.profileIconColor;
    imageFileInput.value = "";
    removeProfileImageInput.checked = false;
    clearPreviewObjectUrl();
    profilePreviewName.textContent = user.displayName || "表示名";
    profilePreviewBio.textContent = user.profileBio || "自己紹介はまだありません。";
    renderPreviewImage(state.profileImageUrl, state.profileIconColor);
    renderGamification(user);
}

function renderPreviewImage(imageUrl, iconColor) {
    const url = (imageUrl || "").trim();
    profileImagePreview.src = url || buildDefaultProfileDataUrl(iconColor);
}

function renderPreviewFile(file) {
    clearPreviewObjectUrl();
    state.previewObjectUrl = URL.createObjectURL(file);
    profileImagePreview.src = state.previewObjectUrl;
}

function clearPreviewObjectUrl() {
    if (!state.previewObjectUrl) {
        return;
    }
    URL.revokeObjectURL(state.previewObjectUrl);
    state.previewObjectUrl = null;
}

function normalizeColorValue(value) {
    const normalized = String(value || "").trim().toUpperCase();
    if (/^#[0-9A-F]{6}$/.test(normalized)) {
        return normalized;
    }
    return DEFAULT_PROFILE_ICON_COLOR;
}

function buildDefaultProfileDataUrl(iconColor) {
    const color = normalizeColorValue(iconColor);
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="240" height="240" viewBox="0 0 240 240" fill="none"><rect width="240" height="240" rx="28" fill="#ECF4FF"/><circle cx="120" cy="88" r="44" fill="${color}"/><path d="M36 201C40 160 73 130 120 130C167 130 200 160 204 201" fill="${color}"/></svg>`;
    return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
}

function renderGamification(user) {
    const level = Number(user.level ?? 1);
    const totalPoints = Number(user.totalPoints ?? 0);
    const currentLevelPoints = Number(user.currentLevelPoints ?? 0);
    const requiredPointsForNextLevel = Number(user.requiredPointsForNextLevel ?? 100);
    const pointsToNextLevel = Number(user.pointsToNextLevel ?? requiredPointsForNextLevel);
    const levelProgressPercent = Math.max(0, Math.min(100, Number(user.levelProgressPercent ?? 0)));

    profileLevelText.textContent = `レベル ${level}`;
    profilePointText.textContent = `累計ポイント: ${totalPoints}pt`;
    profileLevelProgressBar.style.width = `${levelProgressPercent}%`;
    profileLevelNextText.textContent = `次レベルまで ${pointsToNextLevel}pt（${currentLevelPoints}/${requiredPointsForNextLevel}）`;
}

async function loadPointHistory() {
    const histories = await fetchJson("/api/me/points/history");
    pointHistoryList.innerHTML = "";
    if (!Array.isArray(histories) || histories.length === 0) {
        pointHistoryList.innerHTML = "<li>まだポイント履歴はありません。</li>";
        return;
    }

    histories.forEach((history) => {
        const li = document.createElement("li");
        const actionLabel = history.actionLabel || history.actionType || "ポイント";
        const points = Number(history.points ?? 0);
        const sign = points >= 0 ? "+" : "";
        const createdAt = formatHistoryDate(history.createdAt);
        li.textContent = `${createdAt} | ${actionLabel} | ${sign}${points}pt`;
        pointHistoryList.appendChild(li);
    });
}

function formatHistoryDate(value) {
    if (!value) {
        return "-";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return String(value);
    }
    const y = date.getFullYear();
    const m = String(date.getMonth() + 1).padStart(2, "0");
    const d = String(date.getDate()).padStart(2, "0");
    const h = String(date.getHours()).padStart(2, "0");
    const min = String(date.getMinutes()).padStart(2, "0");
    return `${y}-${m}-${d} ${h}:${min}`;
}

async function fetchJson(url, options = {}) {
    const response = await fetch(url, options);
    const contentType = response.headers.get("content-type") || "";
    const isJson = contentType.includes("application/json");
    const data = isJson ? await response.json() : null;

    if (!response.ok) {
        const message = data && data.message ? data.message : "通信に失敗しました。";
        throw new Error(message);
    }

    return data;
}

profileImagePreview.addEventListener("error", () => {
    profileImagePreview.src = buildDefaultProfileDataUrl(state.profileIconColor);
});

loadProfile();
