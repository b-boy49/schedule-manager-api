const app = document.querySelector(".app-shell");
const currentUsername = app?.dataset?.currentUsername || "";

const dmMessage = document.getElementById("dmMessage");
const partnerUserIdSelect = document.getElementById("partnerUserId");
const startConversationForm = document.getElementById("startConversationForm");
const conversationList = document.getElementById("conversationList");
const conversationTitle = document.getElementById("conversationTitle");
const messageList = document.getElementById("messageList");
const sendMessageForm = document.getElementById("sendMessageForm");
const messageBody = document.getElementById("messageBody");
const sendButton = document.getElementById("sendButton");

let selectedConversationId = null;
let selectedConversationLabel = "";
let friends = [];
let conversations = [];

startConversationForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        clearStatus();
        const partnerUserId = Number(partnerUserIdSelect.value);
        if (!partnerUserId) {
            throw new Error("会話相手を選択してください。");
        }
        const conversation = await fetchJson("/dm/start", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ partnerUserId })
        });
        await loadConversations();
        await openConversation(conversation.id);
    } catch (error) {
        setError(error.message);
    }
});

sendMessageForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        clearStatus();
        if (!selectedConversationId) {
            throw new Error("会話を選択してください。");
        }
        await fetchJson(`/dm/conversations/${selectedConversationId}/messages`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ body: messageBody.value })
        });
        messageBody.value = "";
        await loadMessages(selectedConversationId);
        await loadConversations();
    } catch (error) {
        setError(error.message);
    }
});

async function initialize() {
    await loadFriends();
    await loadConversations();
}

async function loadFriends() {
    const data = await fetchJson("/api/friends");
    friends = Array.isArray(data.friends) ? data.friends : [];
    partnerUserIdSelect.innerHTML = "";
    if (friends.length === 0) {
        const option = document.createElement("option");
        option.value = "";
        option.textContent = "フレンドがいません";
        partnerUserIdSelect.appendChild(option);
        partnerUserIdSelect.disabled = true;
        return;
    }
    partnerUserIdSelect.disabled = false;
    friends.forEach((friend) => {
        const option = document.createElement("option");
        option.value = String(friend.id);
        option.textContent = `${friend.displayName} (@${friend.username})`;
        partnerUserIdSelect.appendChild(option);
    });
}

async function loadConversations() {
    conversations = await fetchJson("/dm/conversations");
    renderConversationList(conversations);
    if (selectedConversationId) {
        const exists = conversations.some((row) => row.id === selectedConversationId);
        if (!exists) {
            clearConversationSelection();
        }
    }
}

async function openConversation(conversationId) {
    const selected = conversations.find((row) => row.id === conversationId);
    selectedConversationId = conversationId;
    selectedConversationLabel = selected
        ? `${selected.partnerDisplayName || selected.partnerUsername} (@${selected.partnerUsername || "-"})`
        : "会話";
    conversationTitle.textContent = selectedConversationLabel;
    sendButton.disabled = false;
    await loadMessages(conversationId);
    highlightActiveConversation();
}

async function loadMessages(conversationId) {
    const rows = await fetchJson(`/dm/conversations/${conversationId}/messages`);
    messageList.innerHTML = "";
    if (!Array.isArray(rows) || rows.length === 0) {
        messageList.innerHTML = "<li>メッセージはまだありません。</li>";
        return;
    }
    rows.forEach((row) => {
        const li = document.createElement("li");
        const sender = row.senderUsername || "unknown";
        const direction = sender === currentUsername ? "送信" : "受信";
        li.textContent = `${formatDateTime(row.createdAt)} [${direction}] ${sender}: ${row.body || ""}`;
        messageList.appendChild(li);
    });
}

function renderConversationList(rows) {
    conversationList.innerHTML = "";
    if (!Array.isArray(rows) || rows.length === 0) {
        conversationList.innerHTML = "<li>会話はまだありません。</li>";
        return;
    }
    rows.forEach((row) => {
        const li = document.createElement("li");
        li.dataset.conversationId = String(row.id);
        const partnerName = row.partnerDisplayName || row.partnerUsername || "unknown";
        const partnerUser = row.partnerUsername ? ` (@${row.partnerUsername})` : "";
        const preview = row.lastMessageBody ? ` | ${row.lastMessageBody}` : "";
        li.textContent = `${partnerName}${partnerUser}${preview}`;

        const openButton = document.createElement("button");
        openButton.type = "button";
        openButton.textContent = "開く";
        openButton.addEventListener("click", async () => {
            try {
                clearStatus();
                await openConversation(row.id);
            } catch (error) {
                setError(error.message);
            }
        });
        li.appendChild(openButton);
        conversationList.appendChild(li);
    });
    highlightActiveConversation();
}

function highlightActiveConversation() {
    const items = Array.from(conversationList.querySelectorAll("li"));
    items.forEach((node) => {
        if (!selectedConversationId) {
            node.classList.remove("current-user");
            return;
        }
        const hit = Number(node.dataset.conversationId) === selectedConversationId;
        node.classList.toggle("current-user", hit);
    });
}

function clearConversationSelection() {
    selectedConversationId = null;
    selectedConversationLabel = "";
    conversationTitle.textContent = "会話を選択してください";
    sendButton.disabled = true;
    messageList.innerHTML = "<li>会話を選択するとメッセージが表示されます。</li>";
    highlightActiveConversation();
}

function clearStatus() {
    dmMessage.textContent = "";
}

function setError(message) {
    dmMessage.style.color = "#be2f2f";
    dmMessage.textContent = message;
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

initialize().catch((error) => {
    setError(error.message);
    clearConversationSelection();
});
