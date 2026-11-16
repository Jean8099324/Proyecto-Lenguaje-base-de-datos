document.addEventListener("DOMContentLoaded", () => {
    // Algunas vistas antiguas declaran la barra con clase, pero sin id.
    const sidebar = document.getElementById("sidebar") || document.querySelector(".sidebar");
    const overlay = document.getElementById("sidebarOverlay");
    const cerrar = () => {
        sidebar?.classList.remove("open");
        overlay?.classList.remove("show");
    };
    document.getElementById("menuButton")?.addEventListener("click", () => {
        sidebar?.classList.add("open");
        overlay?.classList.add("show");
    });
    document.getElementById("closeSidebar")?.addEventListener("click", cerrar);
    overlay?.addEventListener("click", cerrar);
    const ruta = window.location.pathname;
    sidebar?.querySelectorAll(".nav-link[href]").forEach(enlace => {
        const destino = enlace.getAttribute("href");
        const activo = destino === "/dashboard" ? ruta === destino
            : destino !== "#" && ruta.startsWith(destino.replace(/\/listado$/, ""));
        enlace.classList.toggle("active", activo);
        if (activo) enlace.setAttribute("aria-current", "page");
    });
});
