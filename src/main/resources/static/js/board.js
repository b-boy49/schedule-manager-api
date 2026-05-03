const app = document.querySelector(".app-shell");
const currentUsername = app.dataset.currentUsername;
const searchKeyword = (app.dataset.searchKeyword || "").trim();

const threadForm = document.getElementById("threadForm");
const threadGameTitleInput = document.getElementById("threadGameTitle");
const boardMessage = document.getElementById("boardMessage");
const threadList = document.getElementById("threadList");
const selectedThreadLabel = document.getElementById("selectedThreadLabel");
const postForm = document.getElementById("postForm");
const postBodyInput = document.getElementById("postBody");
const postScheduleDateInput = document.getElementById("postScheduleDate");
const postStartTimeInput = document.getElementById("postStartTime");
const postRecruitmentLimitInput = document.getElementById("postRecruitmentLimit");
const postList = document.getElementById("postList");

const state = {
    selectedThreadId: null,
    selectedThreadTitle: ""
};

threadForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    boardMessage.textContent = "";

    try {
        const created = await fetchJson("/api/board/threads", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ gameTitle: threadGameTitleInput.value })
        });
        threadGameTitleInput.value = "";
        boardMessage.style.color = "#087057";
        boardMessage.textContent = "スレッドを作成しました。";
        await loadThreads();
        await selectThread(created.id, created.gameTitle);
    } catch (error) {
        boardMessage.style.color = "#be2f2f";
        boardMessage.textContent = error.message;
    }
});

postForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!state.selectedThreadId) {
        boardMessage.style.color = "#be2f2f";
        boardMessage.textContent = "先にスレッドを選択してください。";
        return;
    }

    try {
        await fetchJson(`/api/board/threads/${state.selectedThreadId}/posts`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                body: postBodyInput.value,
                scheduleDate: postScheduleDateInput.value || null,
                startTime: postStartTimeInput.value || null,
                recruitmentLimit: parseRecruitmentLimit()
            })
        });
        postBodyInput.value = "";
        postScheduleDateInput.value = "";
        postStartTimeInput.value = "";
        postRecruitmentLimitInput.value = "";
        boardMessage.style.color = "#087057";
        boardMessage.textContent = "募集投稿を作成しました。";
        await loadThreads();
        await loadPosts(state.selectedThreadId);
    } catch (error) {
        boardMessage.style.color = "#be2f2f";
        boardMessage.textContent = error.message;
    }
});

async function loadThreads() {
    const query = searchKeyword ? `?keyword=${encodeURIComponent(searchKeyword)}` : "";
    const threads = await fetchJson(`/api/board/threads${query}`);
    threadList.innerHTML = "";

    if (!Array.isArray(threads) || threads.length === 0) {
        threadList.innerHTML = "<li>まだスレッドがありません。</li>";
        return;
    }

    threads.forEach((thread) => {
        const li = document.createElement("li");

        const selectButton = document.createElement("button");
        selectButton.type = "button";
        selectButton.className = "secondary";
        selectButton.textContent = "開く";
        selectButton.addEventListener("click", async () => {
            await selectThread(thread.id, thread.gameTitle);
        });

        const ownerName = thread.ownerDisplayName || thread.ownerUsername || "不明";
        const postCount = Number(thread.postCount ?? 0);
        li.textContent = `${thread.gameTitle} | 作成者: ${ownerName} | 投稿: ${postCount}件 `;
        li.appendChild(selectButton);
        threadList.appendChild(li);
    });
}

async function selectThread(threadId, threadTitle) {
    state.selectedThreadId = threadId;
    state.selectedThreadTitle = threadTitle || "";
    selectedThreadLabel.textContent = `スレッド: ${state.selectedThreadTitle}`;
    await loadPosts(threadId);
}

async function loadPosts(threadId) {
    const posts = await fetchJson(`/api/board/threads/${threadId}/posts`);
    postList.innerHTML = "";

    if (!Array.isArray(posts) || posts.length === 0) {
        postList.innerHTML = "<li>まだ募集投稿がありません。</li>";
        return;
    }

    posts.forEach((post) => {
        const li = document.createElement("li");
        const authorName = post.authorDisplayName || post.authorUsername || "不明";
        const scheduleDate = post.scheduleDate ? ` / 日付: ${post.scheduleDate}` : "";
        const startTime = post.startTime ? ` / 時刻: ${String(post.startTime).slice(0, 5)}` : "";
        const limit = post.recruitmentLimit ? ` / 募集人数: ${post.recruitmentLimit}` : "";
        li.textContent = `${authorName}: ${post.body}${scheduleDate}${startTime}${limit}`;

        if (post.authorUsername && post.authorUsername !== currentUsername) {
            const friendButton = document.createElement("button");
            friendButton.type = "button";
            friendButton.textContent = "フレンド申請";
            friendButton.addEventListener("click", async () => {
                try {
                    await fetchJson("/api/friends/requests", {
                        method: "POST",
                        headers: { "Content-Type": "application/json" },
                        body: JSON.stringify({ username: post.authorUsername })
                    });
                    boardMessage.style.color = "#087057";
                    boardMessage.textContent = `${authorName} さんへフレンド申請を送信しました。`;
                } catch (error) {
                    boardMessage.style.color = "#be2f2f";
                    boardMessage.textContent = error.message;
                }
            });
            li.appendChild(friendButton);
        }

        postList.appendChild(li);
    });
}

function parseRecruitmentLimit() {
    const value = postRecruitmentLimitInput.value;
    if (value == null || value.trim() === "") {
        return null;
    }
    return Number.parseInt(value, 10);
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

async function initializeBoard() {
    bindThreadOpenButtons();
    await loadThreads();
}

initializeBoard();

function bindThreadOpenButtons() {
    const buttons = threadList.querySelectorAll("button[data-thread-id]");
    buttons.forEach((button) => {
        button.addEventListener("click", async () => {
            const threadId = Number(button.dataset.threadId);
            const threadTitle = button.dataset.threadTitle || "";
            if (!Number.isFinite(threadId)) {
                return;
            }
            await selectThread(threadId, threadTitle);
        });
    });
}
