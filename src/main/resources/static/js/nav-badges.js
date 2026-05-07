async function loadNavBadges() {
    try {
        const response = await fetch("/api/nav/badges");
        if (!response.ok) {
            return;
        }
        const data = await response.json();
        setNavBadge("friends", Number(data.friends || 0));
        setNavBadge("dm", Number(data.dm || 0));
        setNavBadge("calendar", Number(data.joins || 0));
    } catch (error) {
        // ignore
    }
}

function setNavBadge(key, count) {
    const node = document.querySelector(`[data-nav-badge="${key}"]`);
    if (!node) {
        return;
    }
    const label = node.dataset.navLabel || node.textContent;
    node.textContent = count > 0 ? `${label}${toCircledCount(count)}` : label;
}

function toCircledCount(count) {
    const n = Number(count || 0);
    if (n <= 0) {
        return "";
    }
    const map = ["", "①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨", "⑩", "⑪", "⑫", "⑬", "⑭", "⑮", "⑯", "⑰", "⑱", "⑲", "⑳"];
    if (n <= 20) {
        return map[n];
    }
    return `(${n})`;
}

loadNavBadges();
