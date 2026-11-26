(() => {
    const footer = document.querySelector(".sidebar-footer, .login-footer");
    if (!footer) return;

    const label = document.createElement("small");
    label.className = "deployment-version";
    label.textContent = "Última actualización: consultando…";
    footer.appendChild(label);

    fetch("/api/deployment-info", { cache: "no-store" })
        .then(response => {
            if (!response.ok) throw new Error("Deployment information unavailable");
            return response.json();
        })
        .then(info => {
            const updatedAt = new Date(info.updatedAt);
            if (!Number.isFinite(updatedAt.getTime())) throw new Error("Invalid deployment date");
            const formattedDate = new Intl.DateTimeFormat("es-CR", {
                dateStyle: "medium",
                timeStyle: "short",
                timeZone: "America/Costa_Rica"
            }).format(updatedAt);
            label.textContent = `Última actualización: ${formattedDate}`;
        })
        .catch(() => {
            label.textContent = "Última actualización: no disponible";
        });
})();
