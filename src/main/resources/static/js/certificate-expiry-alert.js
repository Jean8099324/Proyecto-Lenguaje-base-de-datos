(() => {
    if (window.location.protocol !== "https:") return;

    const warningWindowMs = 24 * 60 * 60 * 1000;
    const pollIntervalMs = 60 * 1000;
    const requestTimeoutMs = 4000;
    const aboutPanel = document.querySelector("[data-certificate-about]");
    let expiresAt = null;
    let unavailable = false;
    let footerStatus;

    const stylesheet = document.createElement("link");
    stylesheet.rel = "stylesheet";
    stylesheet.href = "/css/certificate-expiry-alert.css";
    document.head.appendChild(stylesheet);

    if (!aboutPanel) {
        const mainContent = document.querySelector(".main-content");
        const loginFooter = document.querySelector(".login-footer");
        if (mainContent || loginFooter) {
            footerStatus = document.createElement("p");
            footerStatus.className = "certificate-status-footer";
            footerStatus.setAttribute("role", "status");
            footerStatus.setAttribute("aria-live", "polite");
            if (mainContent) mainContent.appendChild(footerStatus);
            else loginFooter.after(footerStatus);
        }
    }

    function formatDuration(milliseconds) {
        const totalSeconds = Math.floor(Math.abs(milliseconds) / 1000);
        const days = Math.floor(totalSeconds / 86400);
        const hours = Math.floor((totalSeconds % 86400) / 3600);
        const minutes = Math.floor((totalSeconds % 3600) / 60);
        const seconds = totalSeconds % 60;
        const two = value => String(value).padStart(2, "0");
        const clock = `${two(hours)}:${two(minutes)}:${two(seconds)}`;
        const value = days > 0 ? `${days} d ${clock}` : clock;
        return milliseconds < 0 ? `-${value}` : value;
    }

    function renderAbout(remaining) {
        const state = aboutPanel.querySelector("[data-certificate-state]");
        const countdown = aboutPanel.querySelector("[data-certificate-countdown]");
        aboutPanel.classList.remove(
            "certificate-status-panel--valid",
            "certificate-status-panel--warning",
            "certificate-status-panel--expired",
            "certificate-status-panel--unavailable"
        );

        if (unavailable) {
            aboutPanel.classList.add("certificate-status-panel--unavailable");
            state.textContent = "No se pudo verificar el certificado";
            countdown.textContent = "Contador no disponible";
            return;
        }

        countdown.textContent = formatDuration(remaining);
        if (remaining < 0) {
            aboutPanel.classList.add("certificate-status-panel--expired");
            state.textContent = "Certificado vencido · renovación pendiente";
        } else if (remaining <= warningWindowMs) {
            aboutPanel.classList.add("certificate-status-panel--warning");
            state.textContent = "Certificado vigente · vence en 24 horas o menos";
        } else {
            aboutPanel.classList.add("certificate-status-panel--valid");
            state.textContent = "Certificado vigente";
        }
    }

    function renderFooter(remaining) {
        if (!footerStatus) return;
        if (unavailable) {
            footerStatus.textContent = "Estado del certificado HTTPS no disponible.";
        } else if (remaining < 0) {
            footerStatus.textContent = `Certificado HTTPS vencido · renovación pendiente · ${formatDuration(remaining)}`;
        } else {
            footerStatus.textContent = `Certificado HTTPS vigente · vence en ${formatDuration(remaining)}`;
        }
    }

    function render() {
        if (expiresAt === null && !unavailable) return;
        const remaining = expiresAt === null ? null : expiresAt - Date.now();
        if (aboutPanel) renderAbout(remaining);
        else renderFooter(remaining);
    }

    async function refreshCertificateStatus() {
        const controller = new AbortController();
        const timeoutId = window.setTimeout(() => controller.abort(), requestTimeoutMs);
        try {
            const response = await fetch("/api/certificate-status", {
                cache: "no-store",
                signal: controller.signal
            });
            if (!response.ok) throw new Error("Certificate status unavailable");
            const status = await response.json();
            const parsedExpiry = Date.parse(status.expiresAt);
            if (!Number.isFinite(parsedExpiry)) throw new Error("Invalid certificate expiry");
            expiresAt = parsedExpiry;
            unavailable = false;
        } catch {
            if (expiresAt === null) unavailable = true;
        } finally {
            window.clearTimeout(timeoutId);
        }
        render();
    }

    refreshCertificateStatus();
    window.setInterval(refreshCertificateStatus, pollIntervalMs);
    window.setInterval(render, 1000);
})();
