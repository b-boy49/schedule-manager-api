const app = document.querySelector(".app-shell");
const currentUsername = app.dataset.currentUsername;
const searchKeyword = (app.dataset.searchKeyword || "").trim();

const threadForm = document.getElementById("threadForm");
const threadGameTitleInput = document.getElementById("threadGameTitle");
const threadFirstPostBodyInput = document.getElementById("threadFirstPostBody");
const threadFirstPostScheduleDateInput = document.getElementById("threadFirstPostScheduleDate");
const threadFirstPostStartTimeInput = document.getElementById("threadFirstPostStartTime");
const threadFirstPostRankBandInput = document.getElementById("threadFirstPostRankBand");
const threadFirstPostRecruitmentLimitInput = document.getElementById("threadFirstPostRecruitmentLimit");
const threadGameTitleSuggestions = document.getElementById("threadGameTitleSuggestions");
const threadSearchSuggestions = document.getElementById("threadSearchSuggestions");
const boardMessage = document.getElementById("boardMessage");

const openCreateThreadCardButton = document.getElementById("openCreateThreadCard");
const openThreadListCardButton = document.getElementById("openThreadListCard");
const createThreadSection = document.getElementById("createThreadSection");
const threadListSection = document.getElementById("threadListSection");
const postEditorSection = document.getElementById("postEditorSection");
const postListSection = document.getElementById("postListSection");

const gameCardList = document.getElementById("gameCardList");
const selectedThreadLabel = document.getElementById("selectedThreadLabel");
const postSummaryList = document.getElementById("postSummaryList");
const postDetailEmpty = document.getElementById("postDetailEmpty");
const postDetailPanel = document.getElementById("postDetailPanel");
const postDetailMeta = document.getElementById("postDetailMeta");
const postDetailBody = document.getElementById("postDetailBody");
const interestForm = document.getElementById("interestForm");
const interestCommentInput = document.getElementById("interestComment");
const interestSubmitButton = document.getElementById("interestSubmitButton");
const interestList = document.getElementById("interestList");

const state = {
    threads: [],
    threadTitles: [],
    selectedGameTitle: "",
    selectedPosts: [],
    selectedPost: null
};

const POPULAR_GAME_TITLES = [
    "Apex Legends",
    "VALORANT",
    "Overwatch 2",
    "Monster Hunter Wilds",
    "Fortnite",
    "League of Legends",
    "Minecraft",
    "Call of Duty"
];
const GAME_TITLE_HISTORY_KEY = "board_game_title_history";
const GAME_TITLE_HISTORY_LIMIT = 20;

if (openCreateThreadCardButton) {
    openCreateThreadCardButton.addEventListener("click", () => setBoardMode("create"));
}
if (openThreadListCardButton) {
    openThreadListCardButton.addEventListener("click", () => setBoardMode("list"));
}

threadForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    boardMessage.textContent = "";
    const trimmedTitle = normalizeTitle(threadGameTitleInput.value);

    try {
        const created = await fetchJson("/api/board/threads", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ gameTitle: trimmedTitle })
        });
        await fetchJson(`/api/board/threads/${created.id}/posts`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                body: threadFirstPostBodyInput.value,
                scheduleDate: threadFirstPostScheduleDateInput.value || null,
                startTime: threadFirstPostStartTimeInput.value || null,
                rankBand: normalizeTitle(threadFirstPostRankBandInput.value) || null,
                recruitmentLimit: parseIntOrNull(threadFirstPostRecruitmentLimitInput.value)
            })
        });
        pushGameTitleHistory(created.gameTitle || trimmedTitle);
        threadGameTitleInput.value = "";
        threadFirstPostBodyInput.value = "";
        threadFirstPostScheduleDateInput.value = "";
        threadFirstPostStartTimeInput.value = "";
        threadFirstPostRankBandInput.value = "";
        threadFirstPostRecruitmentLimitInput.value = "";
        refreshGameTitleSuggestions();
        boardMessage.style.color = "#087057";
        boardMessage.textContent = "スレッドと募集投稿を作成しました。";
        await loadThreads();
        setBoardMode("list");
        await selectGameTitle(created.gameTitle);
    } catch (error) {
        boardMessage.style.color = "#be2f2f";
        boardMessage.textContent = `作成に失敗しました。${error.message}`;
    }
});

interestForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!state.selectedPost || !state.selectedPost.id) {
        return;
    }
    try {
        interestSubmitButton.disabled = true;
        await fetchJson(`/api/board/posts/${state.selectedPost.id}/interests`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ comment: interestCommentInput.value })
        });
        interestCommentInput.value = "";
        boardMessage.style.color = "#087057";
        boardMessage.textContent = "参加希望を送信しました。";
        await loadInterests(state.selectedPost.id);
    } catch (error) {
        boardMessage.style.color = "#be2f2f";
        boardMessage.textContent = error.message;
    } finally {
        interestSubmitButton.disabled = false;
    }
});

async function loadThreads() {
    const query = searchKeyword ? `?keyword=${encodeURIComponent(searchKeyword)}` : "";
    const threads = await fetchJson(`/api/board/threads${query}`);
    state.threads = Array.isArray(threads) ? threads : [];
    state.threadTitles = state.threads.map((thread) => normalizeTitle(thread.gameTitle)).filter((title) => title);
    refreshGameTitleSuggestions();
    renderGameCards();
}

function renderGameCards() {
    gameCardList.innerHTML = "";
    const groups = new Map();
    state.threads.forEach((thread) => {
        const title = normalizeTitle(thread.gameTitle);
        if (!title) {
            return;
        }
        if (!groups.has(title)) {
            groups.set(title, []);
        }
        groups.get(title).push(thread);
    });

    if (groups.size === 0) {
        gameCardList.innerHTML = "<p class=\"help-text\">該当するゲームがありません。</p>";
        return;
    }

    Array.from(groups.entries()).forEach(([title, threads]) => {
        const totalPosts = threads.reduce((sum, thread) => sum + Number(thread.postCount ?? 0), 0);
        const button = document.createElement("button");
        button.type = "button";
        button.className = "board-game-card";
        button.innerHTML = `<h4>${escapeHtml(title)}</h4><p class=\"help-text\">スレッド: ${threads.length} / 投稿: ${totalPosts}</p>`;
        button.addEventListener("click", async () => {
            await selectGameTitle(title);
        });
        gameCardList.appendChild(button);
    });
}

async function selectGameTitle(gameTitle) {
    state.selectedGameTitle = normalizeTitle(gameTitle);
    selectedThreadLabel.textContent = `${state.selectedGameTitle} の投稿一覧`;
    state.selectedPost = null;
    postDetailPanel.hidden = true;
    postDetailEmpty.hidden = false;
    interestList.innerHTML = "";
    await loadPostsByGameTitle(state.selectedGameTitle);
}

async function loadPostsByGameTitle(gameTitle) {
    const targetThreads = state.threads.filter((thread) => normalizeTitle(thread.gameTitle) === gameTitle);
    if (targetThreads.length === 0) {
        state.selectedPosts = [];
        renderPostSummaries();
        return;
    }

    const postLists = await Promise.all(targetThreads.map((thread) => fetchJson(`/api/board/threads/${thread.id}/posts`)));
    const merged = [];
    postLists.forEach((posts, index) => {
        const thread = targetThreads[index];
        (Array.isArray(posts) ? posts : []).forEach((post) => {
            merged.push({ ...post, threadId: thread.id, threadTitle: thread.gameTitle });
        });
    });
    merged.sort((a, b) => {
        const aKey = `${a.createdAt || ""}`;
        const bKey = `${b.createdAt || ""}`;
        if (aKey === bKey) {
            return Number(b.id) - Number(a.id);
        }
        return bKey.localeCompare(aKey);
    });
    state.selectedPosts = merged;
    renderPostSummaries();
}

function renderPostSummaries() {
    postSummaryList.innerHTML = "";
    if (!Array.isArray(state.selectedPosts) || state.selectedPosts.length === 0) {
        postSummaryList.innerHTML = "<li>このゲームの投稿はまだありません。</li>";
        return;
    }

    state.selectedPosts.forEach((post) => {
        const li = document.createElement("li");
        const button = document.createElement("button");
        button.type = "button";
        button.className = "secondary";
        const authorName = post.authorDisplayName || post.authorUsername || "不明";
        const limit = post.recruitmentLimit ? `${post.recruitmentLimit}人` : "指定なし";
        const timeText = post.startTime ? String(post.startTime).slice(0, 5) : "未指定";
        const rankText = post.rankBand ? post.rankBand : "未指定";
        button.textContent = `募集人数: ${limit} / 時間: ${timeText} / ランク: ${rankText} / 作成者: ${authorName}`;
        button.addEventListener("click", async () => {
            await selectPost(post);
        });
        li.appendChild(button);

        if (post.authorUserId && post.authorUsername !== currentUsername) {
            const reportButton = document.createElement("button");
            reportButton.type = "button";
            reportButton.className = "secondary";
            reportButton.textContent = "通報";
            reportButton.addEventListener("click", async () => {
                await reportUser(post.authorUserId, "BOARD_POST", post.id);
            });
            li.appendChild(reportButton);
        }
        postSummaryList.appendChild(li);
    });
}

async function selectPost(post) {
    state.selectedPost = post;
    postDetailBody.textContent = post.body || "";
    const authorName = post.authorDisplayName || post.authorUsername || "不明";
    const dateText = post.scheduleDate || "未指定";
    const timeText = post.startTime ? String(post.startTime).slice(0, 5) : "未指定";
    const limit = post.recruitmentLimit ? `${post.recruitmentLimit}人` : "指定なし";
    const rankText = post.rankBand ? post.rankBand : "未指定";
    postDetailMeta.textContent = `作成者: ${authorName} / 予定日: ${dateText} / 時間: ${timeText} / ランク: ${rankText} / 募集人数: ${limit}`;
    const isOwnPost = post.authorUsername && post.authorUsername === currentUsername;
    interestCommentInput.disabled = isOwnPost;
    interestSubmitButton.disabled = isOwnPost;
    interestCommentInput.placeholder = isOwnPost ? "自分の募集には参加希望を送信できません" : "例: 参加希望です。21:00から入れます。";
    postDetailPanel.hidden = false;
    postDetailEmpty.hidden = true;
    await loadInterests(post.id);
}

async function loadInterests(postId) {
    const interests = await fetchJson(`/api/board/posts/${postId}/interests`);
    interestList.innerHTML = "";
    if (!Array.isArray(interests) || interests.length === 0) {
        interestList.innerHTML = "<li>まだ参加希望コメントはありません。</li>";
        return;
    }

    interests.forEach((interest) => {
        const li = document.createElement("li");
        const name = interest.requesterDisplayName || interest.requesterUsername || "不明";
        li.textContent = `${name}: ${interest.comment}`;
        if (interest.requesterUserId && interest.requesterUsername !== currentUsername) {
            const reportButton = document.createElement("button");
            reportButton.type = "button";
            reportButton.className = "secondary";
            reportButton.textContent = "通報";
            reportButton.addEventListener("click", async () => {
                await reportUser(interest.requesterUserId, "BOARD_INTEREST", interest.id);
            });
            li.appendChild(reportButton);
        }
        interestList.appendChild(li);
    });
}

function refreshGameTitleSuggestions() {
    if (!threadGameTitleSuggestions && !threadSearchSuggestions) {
        return;
    }

    const merged = [
        ...POPULAR_GAME_TITLES,
        ...loadGameTitleHistory(),
        ...state.threadTitles
    ];
    const normalized = Array.from(new Set(merged.map((title) => normalizeTitle(title)).filter((title) => title)));
    if (threadGameTitleSuggestions) {
        threadGameTitleSuggestions.innerHTML = "";
    }
    if (threadSearchSuggestions) {
        threadSearchSuggestions.innerHTML = "";
    }
    normalized.slice(0, 50).forEach((title) => {
        if (threadGameTitleSuggestions) {
            const threadOption = document.createElement("option");
            threadOption.value = title;
            threadGameTitleSuggestions.appendChild(threadOption);
        }
        if (threadSearchSuggestions) {
            const searchOption = document.createElement("option");
            searchOption.value = title;
            threadSearchSuggestions.appendChild(searchOption);
        }
    });
}

function loadGameTitleHistory() {
    try {
        const raw = localStorage.getItem(GAME_TITLE_HISTORY_KEY);
        if (!raw) {
            return [];
        }
        const parsed = JSON.parse(raw);
        if (!Array.isArray(parsed)) {
            return [];
        }
        return parsed.map((title) => normalizeTitle(title)).filter((title) => title);
    } catch (error) {
        return [];
    }
}

function pushGameTitleHistory(title) {
    const normalizedTitle = normalizeTitle(title);
    if (!normalizedTitle) {
        return;
    }

    const next = [normalizedTitle, ...loadGameTitleHistory().filter((item) => item !== normalizedTitle)]
        .slice(0, GAME_TITLE_HISTORY_LIMIT);
    try {
        localStorage.setItem(GAME_TITLE_HISTORY_KEY, JSON.stringify(next));
    } catch (error) {
        // Ignore storage errors and continue without persistence.
    }
}

function parseIntOrNull(value) {
    if (value == null || value.trim() === "") {
        return null;
    }
    return Number.parseInt(value, 10);
}

function normalizeTitle(value) {
    if (value == null) {
        return "";
    }
    return String(value).trim();
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

function setBoardMode(mode) {
    const showCreate = mode === "create";
    if (createThreadSection) {
        createThreadSection.hidden = !showCreate;
    }
    if (threadListSection) {
        threadListSection.hidden = showCreate;
    }
    if (postEditorSection) {
        postEditorSection.hidden = showCreate;
    }
    if (postListSection) {
        postListSection.hidden = showCreate;
    }
}

async function reportUser(targetUserId, sourceType, sourceId) {
    const categoryInput = window.prompt(
        "通報カテゴリを入力してください: HARASSMENT / HATE_SPEECH / SPAM / SEXUAL / VIOLENCE / OTHER"
    );
    if (!categoryInput) {
        return;
    }
    const category = String(categoryInput).trim().toUpperCase();
    const note = window.prompt("備考を入力してください（5文字以上）");
    if (!note) {
        return;
    }
    try {
        await fetchJson("/api/reports", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                targetUserId,
                sourceType,
                sourceId,
                category,
                note
            })
        });
        boardMessage.style.color = "#087057";
        boardMessage.textContent = "通報を送信しました。";
    } catch (error) {
        boardMessage.style.color = "#be2f2f";
        boardMessage.textContent = error.message;
    }
}

async function initializeBoard() {
    setBoardMode(searchKeyword ? "list" : "create");
    await loadThreads();
}

initializeBoard();

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}
