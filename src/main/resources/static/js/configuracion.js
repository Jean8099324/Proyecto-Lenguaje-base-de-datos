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
    const buscador = document.getElementById("buscarConfiguracion");
    const filas = [...document.querySelectorAll("#tablaConfiguracion tr[data-registro='true']")];
    const contador = document.getElementById("cantidadVisible");
    buscador?.addEventListener("input", () => {
        const texto = buscador.value.trim().toLowerCase();
        let cantidad = 0;
        filas.forEach(fila => {
            const visible = fila.textContent.toLowerCase().includes(texto);
            fila.hidden = !visible;
            if (visible) cantidad++;
        });
        if (contador) contador.textContent = String(cantidad);
    });
});
