(function () {
    const STORAGE_KEY = "themeMode";
    const DARK_CLASS = "dark-mode";

    function applyTheme(mode) {
        const isDark = mode === "dark";
        document.body.classList.toggle(DARK_CLASS, isDark);
        const button = document.getElementById("themeToggleButton");
        if (button) {
            button.textContent = isDark ? "☀" : "🌙";
            button.setAttribute("aria-pressed", String(isDark));
            button.setAttribute("aria-label", isDark ? "ライトモードへ切替" : "ダークモードへ切替");
            button.setAttribute("title", isDark ? "ライトモードへ切替" : "ダークモードへ切替");
        }
    }

    function currentTheme() {
        const saved = localStorage.getItem(STORAGE_KEY);
        if (saved === "dark" || saved === "light") {
            return saved;
        }
        return "light";
    }

    function buildToggleButton() {
        const button = document.createElement("button");
        button.id = "themeToggleButton";
        button.type = "button";
        button.className = "theme-toggle";
        button.addEventListener("click", () => {
            const next = document.body.classList.contains(DARK_CLASS) ? "light" : "dark";
            localStorage.setItem(STORAGE_KEY, next);
            applyTheme(next);
        });
        document.body.appendChild(button);
    }

    document.addEventListener("DOMContentLoaded", () => {
        buildToggleButton();
        applyTheme(currentTheme());
    });
})();
