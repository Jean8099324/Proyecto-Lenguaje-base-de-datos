document.addEventListener("DOMContentLoaded", () => {
    // MENU LATERAL
    const sidebar = document.getElementById("sidebar");
    const overlay = document.getElementById("sidebarOverlay");

    const closeSidebar = () => {
        sidebar?.classList.remove("open");
        overlay?.classList.remove("show");
    };

    document.getElementById("menuButton")?.addEventListener("click", () => {
        sidebar?.classList.add("open");
        overlay?.classList.add("show");
    });

    document
        .getElementById("closeSidebar")
        ?.addEventListener("click", closeSidebar);

    overlay?.addEventListener("click", closeSidebar);

    // VENTANA MODAL PARA REGISTRAR REPARACIONES
    const modal = document.getElementById("modalReparacion");

    const show = () => {
        modal?.classList.add("show");
        document.body.style.overflow = "hidden";
    };

    const hide = () => {
        modal?.classList.remove("show");
        document.body.style.overflow = "";
    };

    document
        .getElementById("btnNuevaReparacion")
        ?.addEventListener("click", show);

    document.getElementById("cerrarModal")?.addEventListener("click", hide);

    document
        .getElementById("cancelarReparacion")
        ?.addEventListener("click", hide);

    document.getElementById("modalBackdrop")?.addEventListener("click", hide);

    // ABRIR MODAL SI EL SERVIDOR DEVUELVE ERRORES
    if (document.getElementById("modalAbierto")?.value === "true") {
        show();
    }

    document.addEventListener("keydown", (event) => {
        if (event.key === "Escape") {
            hide();
        }
    });

    // BUSCADOR DE REPARACIONES
    const search = document.getElementById("buscarReparacion");

    const rows = [...document.querySelectorAll("#tablaReparaciones tr")].filter(
        (row) => row.hasAttribute("data-reparacion"),
    );

    const visible = document.getElementById("cantidadVisible");

    const filter = () => {
        const query = search?.value.toLowerCase().trim() ?? "";

        let count = 0;

        rows.forEach((row) => {
            const match = row.textContent.toLowerCase().includes(query);

            row.hidden = !match;

            if (match) {
                count++;
            }
        });

        if (visible) {
            visible.textContent = count;
        }
    };

    search?.addEventListener("input", filter);

    filter();
});
