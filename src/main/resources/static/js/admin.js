const adminSearchForm = document.getElementById("adminSearchForm");
const adminKeywordInput = document.getElementById("adminKeyword");
const adminTargetUserIdSelect = document.getElementById("adminTargetUserId");
const adminBanButton = document.getElementById("adminBanButton");
const adminUnbanButton = document.getElementById("adminUnbanButton");
const adminMessage = document.getElementById("adminMessage");
const adminReportList = document.getElementById("adminReportList");
const adminBoardPostList = document.getElementById("adminBoardPostList");
const adminBoardInterestList = document.getElementById("adminBoardInterestList");
const adminDirectMessageList = document.getElementById("adminDirectMessageList");

let users = [];

adminSearchForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    await loadAll();
});

adminTargetUserIdSelect.addEventListener("change", async () => {
    await loadAll();
});

adminBanButton.addEventListener("click", async () => {
    const userId = Number(adminTargetUserIdSelect.value);
    if (!Number.isFinite(userId)) {
        return;
    }
    if (!window.confirm("このユーザーをBANしますか？")) {
        return;
    }
    await fetchJson(`/api/admin/moderation/users/${userId}/ban`, { method: "DELETE" });
    adminMessage.style.color = "#087057";
    adminMessage.textContent = "BANしました。";
    await loadUsers();
});

adminUnbanButton.addEventListener("click", async () => {
    const userId = Number(adminTargetUserIdSelect.value);
    if (!Number.isFinite(userId)) {
        return;
    }
    await fetchJson(`/api/admin/moderation/users/${userId}/unban`, { method: "DELETE" });
    adminMessage.style.color = "#087057";
    adminMessage.textContent = "BAN解除しました。";
    await loadUsers();
});

async function loadUsers() {
    users = await fetchJson("/api/admin/moderation/users");
    adminTargetUserIdSelect.innerHTML = "";
    (users || []).forEach((u) => {
        const option = document.createElement("option");
        option.value = String(u.id);
        option.textContent = `${u.displayName || u.username} (@${u.username}) ${u.enabled ? "" : "[BAN中]"}`;
        adminTargetUserIdSelect.appendChild(option);
    });
}

async function loadAll() {
    const targetUserId = Number(adminTargetUserIdSelect.value);
    if (!Number.isFinite(targetUserId)) {
        adminBoardPostList.innerHTML = "<li>対象ユーザーを選択してください。</li>";
        adminBoardInterestList.innerHTML = "<li>対象ユーザーを選択してください。</li>";
        adminDirectMessageList.innerHTML = "<li>対象ユーザーを選択してください。</li>";
        return;
    }

    const keyword = String(adminKeywordInput.value || "").trim();
    const query = new URLSearchParams();
    query.set("targetUserId", String(targetUserId));
    if (keyword) {
        query.set("keyword", keyword);
    }

    try {
        const [reports, posts, interests, messages] = await Promise.all([
            fetchJson(`/api/admin/moderation/reports`),
            fetchJson(`/api/admin/moderation/board-posts?${query.toString()}`),
            fetchJson(`/api/admin/moderation/board-interests?${query.toString()}`),
            fetchJson(`/api/admin/moderation/direct-messages?${query.toString()}`)
        ]);
        renderReports(reports || []);
        renderBoardPosts(posts || []);
        renderBoardInterests(interests || []);
        renderDirectMessages(messages || []);
        adminMessage.style.color = "#087057";
        adminMessage.textContent = "読み込みました。";
    } catch (error) {
        adminMessage.style.color = "#be2f2f";
        adminMessage.textContent = error.message;
    }
}

function renderReports(rows) {
    adminReportList.innerHTML = "";
    if (!rows.length) {
        adminReportList.innerHTML = "<li>未対応の通報はありません。</li>";
        return;
    }
    rows.forEach((row) => {
        const li = document.createElement("li");
        const reporter = row.reporterDisplayName || row.reporterUsername || "不明";
        const target = row.targetDisplayName || row.targetUsername || "不明";
        const category = row.category || "OTHER";
        const note = row.note || row.reason || "";
        li.textContent = `通報者: ${reporter} / 対象: ${target} / 種別: ${row.sourceType} / カテゴリ: ${category} / 備考: ${note}`;

        const openButton = document.createElement("button");
        openButton.type = "button";
        openButton.className = "secondary";
        openButton.textContent = "対象を開く";
        openButton.addEventListener("click", async () => {
            adminTargetUserIdSelect.value = String(row.targetUserId);
            await loadAll();
        });

        const resolveButton = document.createElement("button");
        resolveButton.type = "button";
        resolveButton.className = "secondary";
        resolveButton.textContent = "対応済み";
        resolveButton.addEventListener("click", async () => {
            await fetchJson(`/api/admin/moderation/reports/${row.id}/resolve`, { method: "DELETE" });
            await loadAll();
        });

        li.appendChild(openButton);
        li.appendChild(resolveButton);
        adminReportList.appendChild(li);
    });
}

function renderBoardPosts(rows) {
    adminBoardPostList.innerHTML = "";
    if (!rows.length) {
        adminBoardPostList.innerHTML = "<li>該当なし</li>";
        return;
    }
    rows.forEach((row) => {
        const li = document.createElement("li");
        const game = row.gameTitle || "不明ゲーム";
        const name = row.authorDisplayName || row.authorUsername || "不明";
        li.textContent = `[${game}] ${name}: ${row.body}`;
        li.appendChild(deleteButton(async () => {
            await fetchJson(`/api/admin/moderation/board-posts/${row.id}`, { method: "DELETE" });
            await loadAll();
        }));
        adminBoardPostList.appendChild(li);
    });
}

function renderBoardInterests(rows) {
    adminBoardInterestList.innerHTML = "";
    if (!rows.length) {
        adminBoardInterestList.innerHTML = "<li>該当なし</li>";
        return;
    }
    rows.forEach((row) => {
        const name = row.requesterDisplayName || row.requesterUsername || "不明";
        const li = document.createElement("li");
        li.textContent = `${name}: ${row.comment}`;
        li.appendChild(deleteButton(async () => {
            await fetchJson(`/api/admin/moderation/board-interests/${row.id}`, { method: "DELETE" });
            await loadAll();
        }));
        adminBoardInterestList.appendChild(li);
    });
}

function renderDirectMessages(rows) {
    adminDirectMessageList.innerHTML = "";
    if (!rows.length) {
        adminDirectMessageList.innerHTML = "<li>該当なし</li>";
        return;
    }
    rows.forEach((row) => {
        const sender = row.senderDisplayName || row.senderUsername || "不明";
        const recipient = row.recipientDisplayName || row.recipientUsername || "不明";
        const li = document.createElement("li");
        li.textContent = `${sender} → ${recipient}: ${row.body}`;
        li.appendChild(deleteButton(async () => {
            await fetchJson(`/api/admin/moderation/direct-messages/${row.id}`, { method: "DELETE" });
            await loadAll();
        }));
        adminDirectMessageList.appendChild(li);
    });
}

function deleteButton(onDelete) {
    const button = document.createElement("button");
    button.type = "button";
    button.className = "secondary";
    button.textContent = "削除";
    button.addEventListener("click", async () => {
        if (!window.confirm("この投稿を削除しますか？")) {
            return;
        }
        try {
            await onDelete();
            adminMessage.style.color = "#087057";
            adminMessage.textContent = "削除しました。";
        } catch (error) {
            adminMessage.style.color = "#be2f2f";
            adminMessage.textContent = error.message;
        }
    });
    return button;
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

(async () => {
    await loadUsers();
    await loadAll();
})();
