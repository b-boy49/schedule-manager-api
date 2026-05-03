const app = document.querySelector(".app-shell");
const currentUsername = app?.dataset?.currentUsername || "";

const friendRequestForm = document.getElementById("friendRequestForm");
const friendUsernameInput = document.getElementById("friendUsername");
const friendMessage = document.getElementById("friendMessage");
const friendList = document.getElementById("friendList");
const incomingRequestList = document.getElementById("incomingRequestList");
const outgoingRequestList = document.getElementById("outgoingRequestList");
const levelRankingList = document.getElementById("levelRankingList");
const rankingTabs = Array.from(document.querySelectorAll(".ranking-tab"));
const directMessageForm = document.getElementById("directMessageForm");
const messageRecipientSelect = document.getElementById("messageRecipient");
const messageBodyInput = document.getElementById("messageBody");
const directMessageList = document.getElementById("directMessageList");

let currentRankingPeriod = "week";
let currentFriends = [];

friendRequestForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    friendMessage.textContent = "";

    try {
        await fetchJson("/api/friends/requests", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username: friendUsernameInput.value })
        });

        friendMessage.style.color = "#087057";
        friendMessage.textContent = "フレンド申請を送信しました。";
        friendUsernameInput.value = "";
        await loadFriendDashboard();
    } catch (error) {
        friendMessage.style.color = "#be2f2f";
        friendMessage.textContent = error.message;
    }
});

rankingTabs.forEach((tab) => {
    tab.addEventListener("click", async () => {
        currentRankingPeriod = tab.dataset.period || "week";
        rankingTabs.forEach((node) => node.classList.remove("active"));
        tab.classList.add("active");
        await loadTaskRanking(currentRankingPeriod);
    });
});

if (directMessageForm) {
    directMessageForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        friendMessage.textContent = "";

        try {
            await fetchJson("/api/friends/messages", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    recipientUsername: messageRecipientSelect.value,
                    body: messageBodyInput.value
                })
            });
            messageBodyInput.value = "";
            friendMessage.style.color = "#087057";
            friendMessage.textContent = "メッセージを送信しました。";
            await loadDirectMessages();
        } catch (error) {
            friendMessage.style.color = "#be2f2f";
            friendMessage.textContent = error.message;
        }
    });
}

async function loadFriendDashboard() {
    try {
        const data = await fetchJson("/api/friends");
        currentFriends = Array.isArray(data.friends) ? data.friends : [];

        renderFriendList(friendList, data.friends, "フレンドがまだいません。", (friend) => {
            return `${friend.displayName} (@${friend.username})`;
        });

        renderIncomingRequests(data.incomingRequests || []);
        renderFriendList(outgoingRequestList, data.outgoingRequests, "送信中の申請はありません。", (request) => {
            return `${request.requesterDisplayName} (@${request.requesterUsername})`;
        });

        renderMessageRecipientOptions(currentFriends);
        renderDirectMessageList(data.messages || []);
    } catch (error) {
        friendMessage.style.color = "#be2f2f";
        friendMessage.textContent = error.message;
    }
}

async function loadTaskRanking(period) {
    try {
        const data = await fetchJson(`/api/friends/ranking?period=${encodeURIComponent(period)}`);
        renderLevelRanking(data.rows || []);
    } catch (error) {
        friendMessage.style.color = "#be2f2f";
        friendMessage.textContent = error.message;
    }
}

async function loadDirectMessages() {
    try {
        const rows = await fetchJson("/api/friends/messages?limit=100");
        renderDirectMessageList(rows || []);
    } catch (error) {
        friendMessage.style.color = "#be2f2f";
        friendMessage.textContent = error.message;
    }
}

function renderIncomingRequests(incomingRequests) {
    incomingRequestList.innerHTML = "";
    if (!Array.isArray(incomingRequests) || incomingRequests.length === 0) {
        incomingRequestList.innerHTML = "<li>受信した申請はありません。</li>";
        return;
    }

    incomingRequests.forEach((request) => {
        const li = document.createElement("li");
        li.textContent = `${request.requesterDisplayName} (@${request.requesterUsername})`;

        const acceptButton = document.createElement("button");
        acceptButton.type = "button";
        acceptButton.textContent = "承認";
        acceptButton.addEventListener("click", async () => {
            try {
                await fetchJson(`/api/friends/requests/${request.id}/accept`, { method: "POST" });
                friendMessage.style.color = "#087057";
                friendMessage.textContent = "フレンド申請を承認しました。";
                await loadFriendDashboard();
                await loadTaskRanking(currentRankingPeriod);
            } catch (error) {
                friendMessage.style.color = "#be2f2f";
                friendMessage.textContent = error.message;
            }
        });

        li.appendChild(acceptButton);
        incomingRequestList.appendChild(li);
    });
}

function renderFriendList(targetElement, list, emptyText, itemTextBuilder) {
    targetElement.innerHTML = "";
    if (!Array.isArray(list) || list.length === 0) {
        targetElement.innerHTML = `<li>${emptyText}</li>`;
        return;
    }

    list.forEach((item) => {
        const li = document.createElement("li");
        li.textContent = itemTextBuilder(item);
        targetElement.appendChild(li);
    });
}

function renderMessageRecipientOptions(friends) {
    if (!messageRecipientSelect) {
        return;
    }
    messageRecipientSelect.innerHTML = "";

    if (!Array.isArray(friends) || friends.length === 0) {
        const empty = document.createElement("option");
        empty.value = "";
        empty.textContent = "フレンドがいません";
        messageRecipientSelect.appendChild(empty);
        messageRecipientSelect.disabled = true;
        return;
    }

    messageRecipientSelect.disabled = false;
    friends.forEach((friend) => {
        const option = document.createElement("option");
        option.value = friend.username;
        option.textContent = `${friend.displayName} (@${friend.username})`;
        messageRecipientSelect.appendChild(option);
    });
}

function renderDirectMessageList(rows) {
    if (!directMessageList) {
        return;
    }
    directMessageList.innerHTML = "";

    if (!Array.isArray(rows) || rows.length === 0) {
        directMessageList.innerHTML = "<li>メッセージはまだありません。</li>";
        return;
    }

    rows.forEach((row) => {
        const li = document.createElement("li");
        const sender = row.senderUsername || "unknown";
        const recipient = row.recipientUsername || "unknown";
        const direction = sender === currentUsername ? `→ ${recipient}` : `← ${sender}`;
        li.textContent = `${formatDateTime(row.createdAt)} ${direction} : ${row.body || ""}`;
        directMessageList.appendChild(li);
    });
}

function renderLevelRanking(rankingRows) {
    levelRankingList.innerHTML = "";
    if (!Array.isArray(rankingRows) || rankingRows.length === 0) {
        levelRankingList.innerHTML = "<li>ランキング対象がいません。</li>";
        return;
    }

    rankingRows.forEach((row) => {
        const li = document.createElement("li");
        li.className = "ranking-item";
        if (row.currentUser) {
            li.classList.add("current-user");
        }

        const medal = document.createElement("div");
        medal.className = "ranking-medal";
        medal.textContent = medalText(row.rank);

        const avatar = document.createElement("div");
        avatar.className = "ranking-avatar";
        avatar.textContent = (row.avatarInitial || "?").slice(0, 1);

        const body = document.createElement("div");
        body.className = "ranking-body";

        const title = document.createElement("div");
        title.className = "ranking-title";
        const name = row.displayName || row.username || "Unknown";
        title.textContent = `${row.rank}位 ${name}${row.currentUser ? " (あなた)" : ""}`;

        const meta = document.createElement("div");
        meta.className = "ranking-meta";
        meta.textContent = `達成数: ${row.completedCount}`;

        const progressTrack = document.createElement("div");
        progressTrack.className = "ranking-progress-track";
        const progressBar = document.createElement("div");
        progressBar.className = "ranking-progress-bar";
        progressBar.style.width = `${row.progressPercent || 0}%`;
        progressTrack.appendChild(progressBar);

        body.appendChild(title);
        body.appendChild(meta);
        body.appendChild(progressTrack);

        li.appendChild(medal);
        li.appendChild(avatar);
        li.appendChild(body);
        levelRankingList.appendChild(li);
    });
}

function medalText(rank) {
    if (rank === 1) {
        return "1";
    }
    if (rank === 2) {
        return "2";
    }
    if (rank === 3) {
        return "3";
    }
    return `${rank}`;
}

function formatDateTime(value) {
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

async function initialize() {
    await loadFriendDashboard();
    await loadTaskRanking(currentRankingPeriod);
}

initialize();
