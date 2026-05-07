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
    node.textContent = count > 0 ? `${label} (${count})` : label;
}

loadNavBadges();
