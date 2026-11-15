document.addEventListener("DOMContentLoaded", () => {
    const sidebar = document.getElementById("sidebar");
    const overlay = document.getElementById("sidebarOverlay");
    const cerrar = () => { sidebar?.classList.remove("open"); overlay?.classList.remove("show"); };
    document.getElementById("menuButton")?.addEventListener("click", () => {
        sidebar?.classList.add("open"); overlay?.classList.add("show");
    });
    document.getElementById("closeSidebar")?.addEventListener("click", cerrar);
    overlay?.addEventListener("click", cerrar);

    const buscador = document.getElementById("buscarPersonal");
    const filas = [...document.querySelectorAll("#tablaPersonal tr")].filter(fila => fila.cells.length === 6);
    const visible = document.getElementById("cantidadVisible");
    const filtrar = () => {
        const texto = buscador?.value.toLowerCase().trim() ?? "";
        let total = 0;
        filas.forEach(fila => {
            const coincide = fila.textContent.toLowerCase().includes(texto);
            fila.hidden = !coincide;
            if (coincide) total++;
        });
        if (visible) visible.textContent = total;
    };
    buscador?.addEventListener("input", filtrar);
});
