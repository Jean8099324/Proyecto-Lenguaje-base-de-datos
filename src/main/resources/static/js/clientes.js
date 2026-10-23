document.addEventListener("DOMContentLoaded", () => {
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
    document.getElementById("closeSidebar")?.addEventListener("click", closeSidebar);
    overlay?.addEventListener("click", closeSidebar);
    window.addEventListener("resize", () => { if (window.innerWidth > 1050) closeSidebar(); });

    const modal = document.getElementById("modalCliente");
    const showModal = () => {
        modal?.classList.add("show");
        document.body.style.overflow = "hidden";
    };
    const hideModal = () => {
        modal?.classList.remove("show");
        document.body.style.overflow = "";
    };
    document.getElementById("btnNuevoCliente")?.addEventListener("click", showModal);
    document.getElementById("cerrarModal")?.addEventListener("click", hideModal);
    document.getElementById("cancelarCliente")?.addEventListener("click", hideModal);
    document.getElementById("modalBackdrop")?.addEventListener("click", hideModal);
    if (document.getElementById("modalAbierto")?.value === "true") showModal();
    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && modal?.classList.contains("show")) hideModal();
    });

    document.querySelectorAll("[data-cliente-id]").forEach(button => {
        button.addEventListener("click", async () => {
            try {
                const response = await fetch(`/clientes/${button.dataset.clienteId}/detalle`);
                if (!response.ok) throw new Error("No se pudo cargar el cliente.");
                const cliente = await response.json();
                alert(`${cliente.nombre}\nUsuario: ${cliente.username}\nTeléfono: ${cliente.telefono}\nCorreo: ${cliente.correo}\nVehículos: ${cliente.vehiculos}\nEstado: ${cliente.estado}`);
            } catch (error) {
                alert(error.message);
            }
        });
    });

    const search = document.getElementById("buscarCliente");
    const stateFilter = document.getElementById("filtroEstado");
    const rows = document.querySelectorAll("#tablaClientes tr[data-estado]");
    const visibleCount = document.getElementById("cantidadVisible");
    const filterRows = () => {
        const text = search?.value.toLowerCase().trim() ?? "";
        const state = stateFilter?.value ?? "todos";
        let visible = 0;
        rows.forEach(row => {
            const matches = row.textContent.toLowerCase().includes(text)
                && (state === "todos" || row.dataset.estado === state);
            row.style.display = matches ? "" : "none";
            if (matches) visible++;
        });
        if (visibleCount) visibleCount.textContent = visible;
    };
    search?.addEventListener("input", filterRows);
    stateFilter?.addEventListener("change", filterRows);
    filterRows();
});
