const app = document.querySelector(".app-shell");
const initialToday = app.dataset.today;
const currentUsername = app.dataset.currentUsername;
const currentDisplayName = app.dataset.currentDisplayName;
const DEFAULT_PROFILE_ICON_COLOR = "#BFD6FF";

const state = {
    currentMonth: toDate(initialToday),
    selectedDate: initialToday,
    monthMarkersByDate: new Map(),
    friendShareCandidates: [],
    selectedFriendShareUserIds: new Set()
};

const weekdays = ["日", "月", "火", "水", "木", "金", "土"];

const monthLabel = document.getElementById("monthLabel");
const weekdaysContainer = document.getElementById("weekdays");
const calendarGrid = document.getElementById("calendarGrid");
const selectedDateLabel = document.getElementById("selectedDateLabel");
const scheduleList = document.getElementById("scheduleList");

const form = document.getElementById("scheduleForm");
const formTitle = document.getElementById("formTitle");
const scheduleIdInput = document.getElementById("scheduleId");
const scheduleDateInput = document.getElementById("scheduleDate");
const titleInput = document.getElementById("title");
const titleSuggestions = document.getElementById("titleSuggestions");
const priorityInput = document.getElementById("priority");
const deviceTypeInput = document.getElementById("deviceType");
const startTimeInput = document.getElementById("startTime");
const endTimeInput = document.getElementById("endTime");
const descriptionInput = document.getElementById("description");
const sharedWithFriendsInput = document.getElementById("sharedWithFriends");
const joinableInput = document.getElementById("joinable");
const friendShareScopeSection = document.getElementById("friendShareScopeSection");
const friendShareScopeAllInput = document.getElementById("friendShareScopeAll");
const friendShareScopeSelectedInput = document.getElementById("friendShareScopeSelected");
const openFriendSharePickerButton = document.getElementById("openFriendSharePickerButton");
const friendShareSelectionSummary = document.getElementById("friendShareSelectionSummary");
const friendSharePickerScreen = document.getElementById("friendSharePickerScreen");
const friendSharePickerList = document.getElementById("friendSharePickerList");
const friendSharePickerApplyButton = document.getElementById("friendSharePickerApplyButton");
const friendSharePickerCancelButton = document.getElementById("friendSharePickerCancelButton");
const messageShareableInput = document.getElementById("messageShareable");
const recruitmentLimitInput = document.getElementById("recruitmentLimit");
const formMessage = document.getElementById("formMessage");
const csvImportForm = document.getElementById("csvImportForm");
const csvImportFileInput = document.getElementById("csvImportFile");
const csvImportMessage = document.getElementById("csvImportMessage");
const notificationPermissionButton = document.getElementById("notificationPermissionButton");
const notificationEnabledInput = document.getElementById("notificationEnabled");
const notificationIntervalMinutesInput = document.getElementById("notificationIntervalMinutes");
const notificationStatus = document.getElementById("notificationStatus");

const NOTIFICATION_SETTINGS_KEY = "schedule_notification_settings";
let reminderTimerId = null;

document.getElementById("prevMonth").addEventListener("click", async () => {
    await changeMonth(-1);
});

document.getElementById("nextMonth").addEventListener("click", async () => {
    await changeMonth(1);
});

document.getElementById("newScheduleButton").addEventListener("click", () => {
    resetFormForCreate();
});

document.getElementById("cancelEditButton").addEventListener("click", () => {
    resetFormForCreate();
});

joinableInput.addEventListener("change", () => {
    syncJoinableOptions();
});
friendShareScopeAllInput.addEventListener("change", () => {
    syncFriendShareScopeOptions();
});
friendShareScopeSelectedInput.addEventListener("change", () => {
    syncFriendShareScopeOptions();
});
openFriendSharePickerButton.addEventListener("click", async () => {
    await openFriendSharePickerScreen();
});
friendSharePickerApplyButton.addEventListener("click", () => {
    closeFriendSharePickerScreen();
    syncFriendShareSelectionSummary();
});
friendSharePickerCancelButton.addEventListener("click", () => {
    closeFriendSharePickerScreen();
});

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    formMessage.textContent = "";

    const payload = {
        scheduleDate: scheduleDateInput.value,
        priority: priorityInput.value,
        deviceType: deviceTypeInput.value,
        title: titleInput.value,
        startTime: startTimeInput.value || null,
        endTime: endTimeInput.value || null,
        description: descriptionInput.value,
        sharedWithFriends: sharedWithFriendsInput.checked,
        joinable: joinableInput.checked,
        messageShareable: joinableInput.checked && messageShareableInput.checked,
        recruitmentLimit: parseRecruitmentLimit()
    };
    if (joinableInput.checked && friendShareScopeSelectedInput.checked && state.selectedFriendShareUserIds.size === 0) {
        formMessage.style.color = "#be2f2f";
        formMessage.textContent = "公開するフレンドを1人以上選択してください。";
        return;
    }

    const id = scheduleIdInput.value;
    const endpoint = id ? `/api/schedules/${id}` : "/api/schedules";
    const method = id ? "PUT" : "POST";

    try {
        await fetchJson(endpoint, {
            method,
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload)
        });

        await loadSchedules(state.selectedDate);
        await loadTitleSuggestions();
        try {
            await loadMonthMarkers();
        } catch (error) {
            formMessage.style.color = "#be2f2f";
            formMessage.textContent = error.message;
        }
        renderCalendar();
        resetFormForCreate();
    } catch (error) {
        formMessage.textContent = error.message;
    }
});

if (csvImportForm) {
    csvImportForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        csvImportMessage.textContent = "";

        const file = csvImportFileInput.files && csvImportFileInput.files[0];
        if (!file) {
            csvImportMessage.textContent = "CSV file is required.";
            return;
        }

        const formData = new FormData();
        formData.append("file", file);

        try {
            const result = await fetchJson("/api/schedules/import", {
                method: "POST",
                body: formData
            });

            const errors = Array.isArray(result.errors) ? result.errors : [];
            if (errors.length === 0) {
                csvImportMessage.style.color = "#087057";
                csvImportMessage.textContent = `Imported ${result.insertedRows}/${result.totalRows} rows.`;
            } else {
                const firstFive = errors
                    .slice(0, 5)
                    .map((e) => `row ${e.rowNumber}: ${e.message}`)
                    .join(" | ");
                const more = errors.length > 5 ? ` ... and ${errors.length - 5} more.` : "";
                csvImportMessage.style.color = "#be2f2f";
                csvImportMessage.textContent =
                    `Imported ${result.insertedRows}/${result.totalRows} rows with ${errors.length} errors. ${firstFive}${more}`;
            }

            csvImportForm.reset();
            await loadSchedules(state.selectedDate);
            await loadMonthMarkers();
            renderCalendar();
        } catch (error) {
            csvImportMessage.style.color = "#be2f2f";
            csvImportMessage.textContent = error.message;
        }
    });
}

if (notificationPermissionButton) {
    notificationPermissionButton.addEventListener("click", async () => {
        if (!("Notification" in window)) {
            notificationStatus.textContent = "このブラウザは通知に対応していません。";
            return;
        }
        const permission = await Notification.requestPermission();
        notificationStatus.textContent = `通知権限: ${permission}`;
        syncReminderTimer();
    });
}

if (notificationEnabledInput) {
    notificationEnabledInput.addEventListener("change", () => {
        saveNotificationSettings();
        syncReminderTimer();
    });
}

if (notificationIntervalMinutesInput) {
    notificationIntervalMinutesInput.addEventListener("change", () => {
        saveNotificationSettings();
        syncReminderTimer();
    });
}

async function changeMonth(offset) {
    state.currentMonth = new Date(state.currentMonth.getFullYear(), state.currentMonth.getMonth() + offset, 1);
    try {
        await loadMonthMarkers();
    } catch (error) {
        state.monthMarkersByDate = new Map();
        formMessage.style.color = "#be2f2f";
        formMessage.textContent = error.message;
    }
    renderCalendar();
}

function renderCalendar() {
    if (weekdaysContainer.children.length === 0) {
        weekdays.forEach((day, index) => {
            const cell = document.createElement("div");
            cell.classList.add("weekday-label");
            if (index === 0) {
                cell.classList.add("weekday-sunday");
            } else if (index === 6) {
                cell.classList.add("weekday-saturday");
            }
            cell.textContent = day;
            weekdaysContainer.appendChild(cell);
        });
    }

    const year = state.currentMonth.getFullYear();
    const month = state.currentMonth.getMonth();
    monthLabel.textContent = `${year}年 ${month + 1}月`;
    calendarGrid.innerHTML = "";

    const firstDay = new Date(year, month, 1);
    const firstDayIndex = firstDay.getDay();
    const daysInMonth = new Date(year, month + 1, 0).getDate();
    const daysInPrevMonth = new Date(year, month, 0).getDate();

    const cells = 42;
    for (let i = 0; i < cells; i += 1) {
        const dayCell = document.createElement("div");
        dayCell.className = "calendar-day";
        dayCell.setAttribute("role", "button");
        dayCell.tabIndex = 0;

        let cellDate;
        let dayText;
        if (i < firstDayIndex) {
            const day = daysInPrevMonth - firstDayIndex + i + 1;
            cellDate = new Date(year, month - 1, day);
            dayCell.classList.add("outside");
            dayText = String(day);
        } else if (i >= firstDayIndex + daysInMonth) {
            const day = i - firstDayIndex - daysInMonth + 1;
            cellDate = new Date(year, month + 1, day);
            dayCell.classList.add("outside");
            dayText = String(day);
        } else {
            const day = i - firstDayIndex + 1;
            cellDate = new Date(year, month, day);
            dayText = String(day);
        }

        const dateKey = formatDate(cellDate);
        const holidayName = getJapaneseHolidayName(cellDate);
        setDayCellContent(dayCell, dayText, holidayName);
        renderDayOwnerMarkers(dayCell, dateKey);

        const dayOfWeek = cellDate.getDay();
        if (dayOfWeek === 6) {
            dayCell.classList.add("saturday");
        }
        if (dayOfWeek === 0 || holidayName !== null) {
            dayCell.classList.add("holiday");
        }

        if (dateKey === state.selectedDate) {
            dayCell.classList.add("selected");
        }
        if (dateKey === initialToday) {
            dayCell.classList.add("today");
        }

        dayCell.addEventListener("click", async () => {
            await selectDate(dateKey, null);
        });
        dayCell.addEventListener("keydown", async (event) => {
            if (event.key !== "Enter" && event.key !== " ") {
                return;
            }
            event.preventDefault();
            await selectDate(dateKey, null);
        });

        calendarGrid.appendChild(dayCell);
    }
}

async function selectDate(dateKey, focusScheduleId) {
    const selectedDate = toDate(dateKey);
    const nextMonth = new Date(selectedDate.getFullYear(), selectedDate.getMonth(), 1);
    const monthChanged = state.currentMonth.getFullYear() !== nextMonth.getFullYear()
        || state.currentMonth.getMonth() !== nextMonth.getMonth();
    state.currentMonth = nextMonth;
    state.selectedDate = dateKey;
    scheduleDateInput.value = dateKey;
    if (monthChanged) {
        try {
            await loadMonthMarkers();
        } catch (error) {
            state.monthMarkersByDate = new Map();
            formMessage.style.color = "#be2f2f";
            formMessage.textContent = error.message;
        }
    }
    renderCalendar();
    await loadSchedules(dateKey, focusScheduleId);
}

async function loadSchedules(dateKey, focusScheduleId = null) {
    selectedDateLabel.textContent = `${dateKey} の予定`;
    scheduleDateInput.value = dateKey;
    scheduleList.innerHTML = "<li>読み込み中...</li>";

    try {
        const schedules = await fetchJson(`/api/schedules?date=${encodeURIComponent(dateKey)}`);
        scheduleList.innerHTML = "";

        if (!Array.isArray(schedules) || schedules.length === 0) {
            scheduleList.innerHTML = "<li>この日の予定はまだありません。</li>";
            return;
        }

        schedules.forEach((item) => {
            const li = document.createElement("li");
            li.className = "schedule-card";
            li.dataset.scheduleId = String(item.id ?? "");

            const owner = document.createElement("p");
            owner.className = "schedule-owner";
            owner.textContent = scheduleOwnerText(item);

            const title = document.createElement("h4");
            title.textContent = item.title;

            const time = document.createElement("div");
            time.className = "schedule-time";
            time.textContent = timeText(item.startTime, item.endTime);

            const description = document.createElement("p");
            description.textContent = item.description || "";

            const priority = document.createElement("p");
            priority.className = "schedule-priority";
            priority.textContent = `優先度: ${priorityLabel(item.priority)}`;
            const deviceType = document.createElement("p");
            deviceType.className = "schedule-priority";
            deviceType.textContent = `デバイス: ${deviceTypeLabel(item.deviceType)}`;

            const completed = document.createElement("p");
            completed.className = item.completed ? "schedule-complete done" : "schedule-complete";
            completed.textContent = item.completed ? "状態: 完了" : "状態: 未完了";

            li.append(owner, title, priority, deviceType, completed, time, description);

            if (item.joinable && item.messageShareable) {
                const shareable = document.createElement("p");
                shareable.className = "schedule-priority";
                shareable.textContent = "この募集メッセージは再シェア可能";
                li.appendChild(shareable);
            }
            if (item.sourceOwnerUserId) {
                const sourceOwner = item.sourceOwnerDisplayName || "フレンド";
                const sourceMeta = document.createElement("p");
                sourceMeta.className = "schedule-owner";
                sourceMeta.textContent = `共有元: ${sourceOwner}`;
                li.appendChild(sourceMeta);
            }

            if (item.joinable) {
                const participationBadge = document.createElement("p");
                participationBadge.className = "schedule-participation";
                participationBadge.textContent = scheduleParticipationBadgeText(item);
                li.appendChild(participationBadge);

                li.appendChild(renderParticipants(item.participants));
                li.appendChild(renderJoinAction(item));
            }

            if (item.editable) {
                const actions = document.createElement("div");
                actions.className = "schedule-actions";

                const editButton = document.createElement("button");
                editButton.type = "button";
                editButton.className = "edit-btn";
                editButton.textContent = "編集";
                editButton.addEventListener("click", () => fillFormForEdit(item));

                const deleteButton = document.createElement("button");
                deleteButton.type = "button";
                deleteButton.className = "delete-btn";
                deleteButton.textContent = "削除";
                deleteButton.addEventListener("click", async () => {
                    const confirmed = window.confirm("この予定を削除しますか？");
                    if (!confirmed) {
                        return;
                    }

                    try {
                        await fetchJson(`/api/schedules/${item.id}`, { method: "DELETE" });
                        await loadSchedules(state.selectedDate);
                        try {
                            await loadMonthMarkers();
                        } catch (error) {
                            formMessage.style.color = "#be2f2f";
                            formMessage.textContent = error.message;
                        }
                        renderCalendar();
                        resetFormForCreate();
                    } catch (error) {
                        formMessage.textContent = error.message;
                    }
                });

                const completeButton = document.createElement("button");
                completeButton.type = "button";
                completeButton.className = item.completed ? "uncomplete-btn" : "complete-btn";
                completeButton.textContent = item.completed ? "未完了に戻す" : "完了";
                completeButton.addEventListener("click", async () => {
                    try {
                        await fetchJson(`/api/schedules/${item.id}/complete`, {
                            method: item.completed ? "DELETE" : "POST"
                        });
                        await loadSchedules(state.selectedDate);
                    } catch (error) {
                        formMessage.textContent = error.message;
                    }
                });

                actions.append(editButton, completeButton, deleteButton);
                li.appendChild(actions);
            } else if (item.joinable && item.messageShareable) {
                const shareActions = document.createElement("div");
                shareActions.className = "schedule-actions";

                const shareButton = document.createElement("button");
                shareButton.type = "button";
                shareButton.className = "share-btn";
                shareButton.textContent = "この募集をシェア";
                shareButton.addEventListener("click", async () => {
                    try {
                        await fetchJson(`/api/schedules/${item.id}/share`, { method: "POST" });
                        formMessage.style.color = "#087057";
                        formMessage.textContent = "募集メッセージをフレンドへシェアしました。";
                        await loadSchedules(state.selectedDate);
                        try {
                            await loadMonthMarkers();
                        } catch (error) {
                            formMessage.style.color = "#be2f2f";
                            formMessage.textContent = error.message;
                        }
                        renderCalendar();
                    } catch (error) {
                        formMessage.style.color = "#be2f2f";
                        formMessage.textContent = error.message;
                    }
                });

                shareActions.appendChild(shareButton);
                li.appendChild(shareActions);
            }

            scheduleList.appendChild(li);
        });
        if (focusScheduleId != null) {
            focusScheduleCard(focusScheduleId);
        }
    } catch (error) {
        scheduleList.innerHTML = `<li>${error.message}</li>`;
    }
}

function focusScheduleCard(scheduleId) {
    const target = scheduleList.querySelector(`.schedule-card[data-schedule-id="${scheduleId}"]`);
    if (!target) {
        return;
    }
    target.classList.add("focused");
    target.scrollIntoView({ behavior: "smooth", block: "nearest" });
    window.setTimeout(() => target.classList.remove("focused"), 1400);
}

async function loadMonthMarkers() {
    const year = state.currentMonth.getFullYear();
    const month = state.currentMonth.getMonth() + 1;
    const rows = await fetchJson(`/api/schedules/month?year=${year}&month=${month}`);
    state.monthMarkersByDate = buildMonthMarkerMap(rows);
}

async function loadTitleSuggestions() {
    const titles = await fetchJson("/api/schedules/title-suggestions?limit=30");
    if (!Array.isArray(titles)) {
        return;
    }

    titleSuggestions.innerHTML = "";
    titles
        .map((title) => String(title || "").trim())
        .filter((title, index, array) => title !== "" && array.indexOf(title) === index)
        .forEach((title) => {
            const option = document.createElement("option");
            option.value = title;
            titleSuggestions.appendChild(option);
        });
}

function buildMonthMarkerMap(rows) {
    const byDate = new Map();
    if (!Array.isArray(rows)) {
        return byDate;
    }

    rows.forEach((row) => {
        if (!row || !row.scheduleDate) {
            return;
        }
        const dateKey = row.scheduleDate;
        let ownerMap = byDate.get(dateKey);
        if (!ownerMap) {
            ownerMap = new Map();
            byDate.set(dateKey, ownerMap);
        }

        const ownerKey = String(row.ownerUserId ?? row.ownerUsername ?? "unknown");
        if (!ownerMap.has(ownerKey)) {
            ownerMap.set(ownerKey, {
                ownerUserId: row.ownerUserId,
                ownerDisplayName: row.ownerDisplayName || row.ownerUsername || "不明",
                ownerProfileIconColor: normalizeColorValue(row.ownerProfileIconColor),
                ownerHasProfileImage: Boolean(row.ownerHasProfileImage),
                scheduleId: row.id,
                count: 1
            });
            return;
        }
        const marker = ownerMap.get(ownerKey);
        marker.count += 1;
    });

    const result = new Map();
    byDate.forEach((ownerMap, dateKey) => {
        result.set(dateKey, Array.from(ownerMap.values()));
    });
    return result;
}

function renderDayOwnerMarkers(dayCell, dateKey) {
    const markers = state.monthMarkersByDate.get(dateKey) || [];
    if (markers.length === 0) {
        return;
    }

    const wrapper = document.createElement("div");
    wrapper.className = "day-owner-markers";

    markers.slice(0, 4).forEach((marker) => {
        const avatarButton = document.createElement("button");
        avatarButton.type = "button";
        avatarButton.className = "day-owner-avatar";
        const countText = marker.count > 1 ? `（${marker.count}件）` : "";
        avatarButton.title = `${marker.ownerDisplayName}${countText}`;
        avatarButton.addEventListener("click", async (event) => {
            event.stopPropagation();
            await selectDate(dateKey, marker.scheduleId);
        });

        const image = document.createElement("img");
        image.alt = `${marker.ownerDisplayName}のアイコン`;
        image.loading = "lazy";
        const fallbackDataUrl = buildDefaultProfileDataUrl(marker.ownerProfileIconColor);
        image.src = marker.ownerHasProfileImage && marker.ownerUserId
            ? `/api/users/${marker.ownerUserId}/profile-image`
            : fallbackDataUrl;
        image.addEventListener("error", () => {
            image.src = fallbackDataUrl;
        });
        avatarButton.appendChild(image);
        wrapper.appendChild(avatarButton);
    });

    if (markers.length > 4) {
        const more = document.createElement("span");
        more.className = "day-owner-more";
        more.textContent = `+${markers.length - 4}`;
        wrapper.appendChild(more);
    }

    dayCell.appendChild(wrapper);
}

function scheduleOwnerText(item) {
    const ownerName = item.ownerDisplayName || item.ownerUsername || "不明";
    if (item.ownerUsername === currentUsername) {
        if (item.joinable) {
            return item.recruitmentClosed ? "あなたの募集予定（締切）" : "あなたの募集予定（参加受付中）";
        }
        if (item.sharedWithFriends) {
            return "あなたの予定（フレンド共有中）";
        }
        return "あなたの予定";
    }
    if (item.joinable) {
        return item.recruitmentClosed ? `募集締切: ${ownerName}` : `募集予定: ${ownerName}`;
    }
    return `共有予定: ${ownerName}`;
}

function fillFormForEdit(item) {
    if (!item.editable) {
        return;
    }

    formTitle.textContent = "予定の編集";
    scheduleIdInput.value = item.id;
    scheduleDateInput.value = item.scheduleDate;
    titleInput.value = item.title ?? "";
    priorityInput.value = item.priority ?? "LOW";
    deviceTypeInput.value = item.deviceType ?? "PC";
    startTimeInput.value = toTimeInput(item.startTime);
    endTimeInput.value = toTimeInput(item.endTime);
    descriptionInput.value = item.description ?? "";
    sharedWithFriendsInput.checked = item.sharedWithFriends === true;
    joinableInput.checked = item.joinable === true;
    messageShareableInput.checked = item.messageShareable === true;
    recruitmentLimitInput.value = item.recruitmentLimit ?? "";
    syncJoinableOptions();
    formMessage.style.color = "#087057";
    formMessage.textContent = "編集モードです。内容を更新して保存してください。";
    form.scrollIntoView({ behavior: "smooth", block: "start" });
    titleInput.focus();
}

function resetFormForCreate() {
    formTitle.textContent = "予定の追加";
    scheduleIdInput.value = "";
    scheduleDateInput.value = state.selectedDate;
    titleInput.value = "";
    priorityInput.value = "LOW";
    deviceTypeInput.value = "PC";
    startTimeInput.value = "";
    endTimeInput.value = "";
    descriptionInput.value = "";
    sharedWithFriendsInput.checked = false;
    joinableInput.checked = false;
    messageShareableInput.checked = false;
    recruitmentLimitInput.value = "";
    syncJoinableOptions();
    formMessage.textContent = "";
}

function toTimeInput(value) {
    if (!value) {
        return "";
    }
    return String(value).slice(0, 5);
}

function timeText(startTime, endTime) {
    const start = toTimeInput(startTime);
    const end = toTimeInput(endTime);
    if (!start && !end) {
        return "時刻指定なし";
    }
    if (start && end) {
        return `${start} - ${end}`;
    }
    return start || end;
}

function priorityLabel(priority) {
    const normalized = String(priority || "LOW").toUpperCase();
    if (normalized === "HIGH") {
        return "HIGH";
    }
    if (normalized === "MEDIUM") {
        return "MEDIUM";
    }
    return "LOW";
}

function deviceTypeLabel(deviceType) {
    const normalized = String(deviceType || "PC").toUpperCase();
    if (normalized === "CONSOLE") {
        return "家庭用ゲーム機";
    }
    return "PC";
}

function scheduleParticipationBadgeText(item) {
    const participantCount = Number(item.participantCount ?? 0);
    const recruitmentLimit = item.recruitmentLimit;
    const remainingSlots = item.remainingRecruitmentSlots;
    if (recruitmentLimit == null) {
        return `参加者 ${participantCount}人`;
    }
    if (item.recruitmentClosed) {
        return `参加者 ${participantCount}/${recruitmentLimit}人（締切）`;
    }
    return `参加者 ${participantCount}/${recruitmentLimit}人（残り${remainingSlots}人）`;
}

function renderParticipants(participants) {
    const wrapper = document.createElement("p");
    wrapper.className = "schedule-participants";

    if (!Array.isArray(participants) || participants.length === 0) {
        wrapper.textContent = "まだ参加者はいません。";
        return wrapper;
    }

    const names = participants.map((user) => user.displayName || user.username || "不明");
    wrapper.textContent = `参加中: ${names.join(" / ")}`;
    return wrapper;
}

function renderJoinAction(item) {
    const actionArea = document.createElement("div");
    actionArea.className = "join-actions";

    if (item.editable) {
        const ownerHint = document.createElement("span");
        ownerHint.textContent = "あなたが募集主です";
        actionArea.appendChild(ownerHint);
        return actionArea;
    }

    if (item.recruitmentClosed && !item.joinedByCurrentUser) {
        const closedLabel = document.createElement("span");
        closedLabel.className = "closed-label";
        closedLabel.textContent = "募集は締め切られました";
        actionArea.appendChild(closedLabel);
        return actionArea;
    }

    const joinButton = document.createElement("button");
    joinButton.type = "button";
    joinButton.className = item.joinedByCurrentUser ? "leave-btn" : "join-btn";
    joinButton.textContent = item.joinedByCurrentUser ? "参加を取り消す" : "この枠に参加する";
    joinButton.addEventListener("click", async () => {
        try {
            if (item.joinedByCurrentUser) {
                await fetchJson(`/api/schedules/${item.id}/join`, { method: "DELETE" });
            } else {
                await fetchJson(`/api/schedules/${item.id}/join`, { method: "POST" });
            }
            await loadSchedules(state.selectedDate);
        } catch (error) {
            formMessage.textContent = error.message;
        }
    });
    actionArea.appendChild(joinButton);
    return actionArea;
}

function syncJoinableOptions() {
    if (joinableInput.checked) {
        sharedWithFriendsInput.checked = true;
        sharedWithFriendsInput.disabled = true;
        messageShareableInput.disabled = false;
        recruitmentLimitInput.disabled = false;
        recruitmentLimitInput.required = true;
        return;
    }
    sharedWithFriendsInput.disabled = false;
    messageShareableInput.checked = false;
    messageShareableInput.disabled = true;
    recruitmentLimitInput.disabled = true;
    recruitmentLimitInput.required = false;
    recruitmentLimitInput.value = "";
}

function parseRecruitmentLimit() {
    if (!joinableInput.checked) {
        return null;
    }
    const value = recruitmentLimitInput.value;
    if (value == null || value.trim() === "") {
        return null;
    }
    return Number.parseInt(value, 10);
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
    const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="120" height="120" viewBox="0 0 240 240" fill="none"><rect width="240" height="240" rx="28" fill="#ECF4FF"/><circle cx="120" cy="88" r="44" fill="${color}"/><path d="M36 201C40 160 73 130 120 130C167 130 200 160 204 201" fill="${color}"/></svg>`;
    return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
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

function loadNotificationSettings() {
    const defaults = { enabled: false, intervalMinutes: 5 };
    try {
        const raw = localStorage.getItem(NOTIFICATION_SETTINGS_KEY);
        if (!raw) {
            return defaults;
        }
        const parsed = JSON.parse(raw);
        return {
            enabled: Boolean(parsed.enabled),
            intervalMinutes: normalizeInterval(parsed.intervalMinutes)
        };
    } catch (error) {
        return defaults;
    }
}

function saveNotificationSettings() {
    const settings = {
        enabled: Boolean(notificationEnabledInput && notificationEnabledInput.checked),
        intervalMinutes: normalizeInterval(notificationIntervalMinutesInput ? notificationIntervalMinutesInput.value : 5)
    };
    localStorage.setItem(NOTIFICATION_SETTINGS_KEY, JSON.stringify(settings));
}

function applyNotificationSettings() {
    const settings = loadNotificationSettings();
    if (notificationEnabledInput) {
        notificationEnabledInput.checked = settings.enabled;
    }
    if (notificationIntervalMinutesInput) {
        notificationIntervalMinutesInput.value = String(settings.intervalMinutes);
    }
    if (notificationStatus) {
        notificationStatus.textContent = "通知設定を読み込みました。";
    }
}

function normalizeInterval(value) {
    const parsed = Number.parseInt(String(value ?? "5"), 10);
    if (Number.isNaN(parsed)) {
        return 5;
    }
    return Math.max(1, Math.min(parsed, 60));
}

function syncReminderTimer() {
    if (reminderTimerId != null) {
        window.clearInterval(reminderTimerId);
        reminderTimerId = null;
    }
    const settings = loadNotificationSettings();
    if (!settings.enabled) {
        notificationStatus.textContent = "通知は無効です。";
        return;
    }
    if (!("Notification" in window)) {
        notificationStatus.textContent = "このブラウザは通知に対応していません。";
        return;
    }
    if (Notification.permission !== "granted") {
        notificationStatus.textContent = "通知を有効化するには「通知を許可」を押してください。";
        return;
    }
    reminderTimerId = window.setInterval(() => {
        checkDueSoonTasks(settings.intervalMinutes).catch(() => {
            notificationStatus.textContent = "通知チェックに失敗しました。";
        });
    }, settings.intervalMinutes * 60 * 1000);
    checkDueSoonTasks(settings.intervalMinutes).catch(() => {
        notificationStatus.textContent = "通知チェックに失敗しました。";
    });
    notificationStatus.textContent = `通知は有効です（${settings.intervalMinutes}分ごと）`;
}

async function checkDueSoonTasks(intervalMinutes) {
    const windowMinutes = Math.max(15, intervalMinutes * 2);
    const reminders = await fetchJson(`/api/schedules/reminders?windowMinutes=${windowMinutes}`);
    if (!Array.isArray(reminders) || reminders.length === 0) {
        return;
    }

    const seen = loadSeenReminderMap();
    reminders.forEach((item) => {
        const key = `${item.id}:${item.dueAt}`;
        if (seen[key]) {
            return;
        }
        const title = item.title || "予定";
        const minutesLeft = Number(item.minutesLeft ?? 0);
        const body = minutesLeft <= 0
            ? "期限時刻です。"
            : `あと${minutesLeft}分で期限です。`;
        new Notification(`リマインダー: ${title}`, { body });
        seen[key] = Date.now();
    });
    saveSeenReminderMap(seen);
}

function loadSeenReminderMap() {
    const key = "schedule_notification_seen";
    try {
        const raw = localStorage.getItem(key);
        const parsed = raw ? JSON.parse(raw) : {};
        return parsed && typeof parsed === "object" ? parsed : {};
    } catch (error) {
        return {};
    }
}

function saveSeenReminderMap(map) {
    const key = "schedule_notification_seen";
    const entries = Object.entries(map);
    const sorted = entries.sort((a, b) => Number(b[1]) - Number(a[1])).slice(0, 300);
    const trimmed = Object.fromEntries(sorted);
    localStorage.setItem(key, JSON.stringify(trimmed));
}

function toDate(yyyyMmDd) {
    const [year, month, day] = yyyyMmDd.split("-").map(Number);
    return new Date(year, month - 1, day);
}

function formatDate(date) {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

function setDayCellContent(button, dayText, holidayName) {
    button.innerHTML = "";

    const dayNumberElement = document.createElement("span");
    dayNumberElement.className = "day-number";
    dayNumberElement.textContent = dayText;
    button.appendChild(dayNumberElement);

    if (holidayName !== null) {
        const holidayNameElement = document.createElement("span");
        holidayNameElement.className = "holiday-name";
        holidayNameElement.textContent = holidayName;
        button.appendChild(holidayNameElement);
    }
}

function getJapaneseHolidayName(date) {
    const baseHolidayName = getBaseHolidayName(date);
    if (baseHolidayName !== null) {
        return baseHolidayName;
    }

    if (isCitizensHoliday(date)) {
        return "国民の休日";
    }

    if (isSubstituteHoliday(date)) {
        return "振替休日";
    }

    return null;
}

function isBaseHoliday(date) {
    return getBaseHolidayName(date) !== null;
}

function getBaseHolidayName(date) {
    const year = date.getFullYear();
    const month = date.getMonth() + 1;
    const day = date.getDate();

    if (year < 1948) {
        return null;
    }

    if (isSameDate(date, 1959, 4, 10)) {
        return "皇太子明仁親王の結婚の儀";
    }
    if (isSameDate(date, 1989, 2, 24)) {
        return "昭和天皇の大喪の礼";
    }
    if (isSameDate(date, 1990, 11, 12)) {
        return "即位礼正殿の儀";
    }
    if (isSameDate(date, 1993, 6, 9)) {
        return "皇太子徳仁親王の結婚の儀";
    }
    if (isSameDate(date, 2019, 5, 1)) {
        return "即位の日";
    }
    if (isSameDate(date, 2019, 10, 22)) {
        return "即位礼正殿の儀";
    }

    if (month === 1) {
        if (day === 1) {
            return "元日";
        }
        if (year >= 2000) {
            if (day === nthWeekdayOfMonth(year, 1, 1, 2)) {
                return "成人の日";
            }
            return null;
        }
        if (day === 15) {
            return "成人の日";
        }
        return null;
    }

    if (month === 2) {
        if (year >= 1967 && day === 11) {
            return "建国記念の日";
        }
        if (year >= 2020 && day === 23) {
            return "天皇誕生日";
        }
        return null;
    }

    if (month === 3 && day === vernalEquinoxDay(year)) {
        return "春分の日";
    }

    if (month === 4 && day === 29) {
        if (year >= 2007) {
            return "昭和の日";
        }
        if (year >= 1989) {
            return "みどりの日";
        }
        return "天皇誕生日";
    }

    if (month === 5) {
        if (day === 3 || day === 5) {
            return day === 3 ? "憲法記念日" : "こどもの日";
        }
        if (year >= 2007 && day === 4) {
            return "みどりの日";
        }
        return null;
    }

    if (month === 7) {
        if (year === 2020) {
            if (day === 23) {
                return "海の日";
            }
            return null;
        }
        if (year === 2021) {
            if (day === 22) {
                return "海の日";
            }
            return null;
        }
        if (year >= 2003) {
            if (day === nthWeekdayOfMonth(year, 7, 1, 3)) {
                return "海の日";
            }
            return null;
        }
        if (year >= 1996 && day === 20) {
            return "海の日";
        }
        return null;
    }

    if (month === 8) {
        if (year === 2020) {
            if (day === 10) {
                return "山の日";
            }
            return null;
        }
        if (year === 2021) {
            if (day === 8) {
                return "山の日";
            }
            return null;
        }
        if (year >= 2016 && day === 11) {
            return "山の日";
        }
        return null;
    }

    if (month === 9) {
        if (year >= 2003 && day === nthWeekdayOfMonth(year, 9, 1, 3)) {
            return "敬老の日";
        }
        if (year >= 1966 && year <= 2002 && day === 15) {
            return "敬老の日";
        }
        if (day === autumnalEquinoxDay(year)) {
            return "秋分の日";
        }
        return null;
    }

    if (month === 10) {
        if (year === 2020 && day === 24) {
            return "スポーツの日";
        }
        if (year === 2021 && day === 23) {
            return "スポーツの日";
        }
        if (year >= 2000) {
            if (day === nthWeekdayOfMonth(year, 10, 1, 2)) {
                if (year >= 2020) {
                    return "スポーツの日";
                }
                return "体育の日";
            }
            return null;
        }
        if (year >= 1966 && day === 10) {
            return "体育の日";
        }
        return null;
    }

    if (month === 11) {
        if (day === 3) {
            return "文化の日";
        }
        if (day === 23) {
            return "勤労感謝の日";
        }
        return null;
    }

    if (month === 12) {
        if (year >= 1989 && year <= 2018 && day === 23) {
            return "天皇誕生日";
        }
        return null;
    }

    return null;
}

function isCitizensHoliday(date) {
    if (date < new Date(1986, 0, 1)) {
        return false;
    }

    if (isBaseHoliday(date)) {
        return false;
    }

    const previousDate = addDays(date, -1);
    const nextDate = addDays(date, 1);
    return isBaseHoliday(previousDate) && isBaseHoliday(nextDate);
}

function isSubstituteHoliday(date) {
    const ruleStartDate = new Date(1973, 3, 30);
    if (date < ruleStartDate || isHolidayWithoutSubstitute(date)) {
        return false;
    }

    const year = date.getFullYear();

    if (year < 2007) {
        const previousDate = addDays(date, -1);
        return date.getDay() === 1
            && previousDate.getDay() === 0
            && isHolidayWithoutSubstitute(previousDate);
    }

    let cursor = addDays(date, -1);
    let foundSundayHoliday = false;

    while (isHolidayWithoutSubstitute(cursor)) {
        if (cursor.getDay() === 0) {
            foundSundayHoliday = true;
        }
        cursor = addDays(cursor, -1);
    }

    return foundSundayHoliday;
}

function isHolidayWithoutSubstitute(date) {
    return isBaseHoliday(date) || isCitizensHoliday(date);
}

function vernalEquinoxDay(year) {
    if (year <= 1979) {
        return Math.floor(20.8357 + 0.242194 * (year - 1980) - Math.floor((year - 1983) / 4));
    }
    return Math.floor(20.8431 + 0.242194 * (year - 1980) - Math.floor((year - 1980) / 4));
}

function autumnalEquinoxDay(year) {
    if (year <= 1979) {
        return Math.floor(23.2588 + 0.242194 * (year - 1980) - Math.floor((year - 1983) / 4));
    }
    return Math.floor(23.2488 + 0.242194 * (year - 1980) - Math.floor((year - 1980) / 4));
}

function nthWeekdayOfMonth(year, month, weekday, nth) {
    const firstDay = new Date(year, month - 1, 1).getDay();
    const offset = (7 + weekday - firstDay) % 7;
    return 1 + offset + (nth - 1) * 7;
}

function addDays(date, days) {
    const copiedDate = new Date(date);
    copiedDate.setDate(copiedDate.getDate() + days);
    return copiedDate;
}

function isSameDate(date, year, month, day) {
    return date.getFullYear() === year
        && date.getMonth() + 1 === month
        && date.getDate() === day;
}

async function initializeCalendarPage() {
    applyNotificationSettings();
    syncReminderTimer();
    resetFormForCreate();
    try {
        await loadTitleSuggestions();
    } catch (error) {
        // 候補取得失敗時も入力は可能にする。
    }
    try {
        await loadMonthMarkers();
    } catch (error) {
        state.monthMarkersByDate = new Map();
        formMessage.style.color = "#be2f2f";
        formMessage.textContent = error.message;
    }
    renderCalendar();
    await loadSchedules(state.selectedDate);
}

initializeCalendarPage();
