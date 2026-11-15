document.addEventListener("DOMContentLoaded", () => {
    const sidebar = document.getElementById("sidebar");
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

    const buscador = document.getElementById("buscarContacto");
    if (!buscador) return;
    const filas = [...document.querySelectorAll("#tablaContactos tr")]
        .filter(fila => fila.cells.length === 7);
    const cantidad = document.getElementById("cantidadVisible");
    buscador.addEventListener("input", () => {
        const texto = buscador.value.trim().toLowerCase();
        let total = 0;
        filas.forEach(fila => {
            const visible = fila.textContent.toLowerCase().includes(texto);
            fila.hidden = !visible;
            if (visible) total++;
        });
        if (cantidad) cantidad.textContent = String(total);
    });
});
