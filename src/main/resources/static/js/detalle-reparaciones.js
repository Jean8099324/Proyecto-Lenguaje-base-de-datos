
/*
 * PROYECTO RESCATE VIAL CR
 *
 * PANTALLA 21: DETALLE DE REPARACIONES
 *
 * FUNCIONES:
 * - ABRIR Y CERRAR EL MODAL
 * - BUSCAR DETALLES DE REPARACIONES
 * - CALCULAR SUBTOTAL
 * - CONTROLAR EL MENU LATERAL
 */

document.addEventListener("DOMContentLoaded", function () {

    // ========================================
    // ELEMENTOS DEL MODAL
    // ========================================

    const modal = document.getElementById("modalDetalle");

    const btnNuevo = document.getElementById("btnNuevoDetalle");

    const btnCerrar = document.getElementById("cerrarModal");

    const btnCancelar = document.getElementById("cancelarDetalle");

    const modalBackdrop = document.getElementById("modalBackdrop");

    // ABRIR MODAL
    function abrirModal() {

        if (modal) {
            modal.classList.add("show");
            document.body.style.overflow = "hidden";
        }
    }

    // CERRAR MODAL
    function cerrarModal() {

        if (modal) {
            modal.classList.remove("show");
            document.body.style.overflow = "";
        }
    }

    if (btnNuevo) {
        btnNuevo.addEventListener("click", abrirModal);
    }

    if (btnCerrar) {
        btnCerrar.addEventListener("click", cerrarModal);
    }

    if (btnCancelar) {
        btnCancelar.addEventListener("click", cerrarModal);
    }

    if (modalBackdrop) {
        modalBackdrop.addEventListener("click", cerrarModal);
    }

    // CERRAR MODAL CON ESCAPE
    document.addEventListener("keydown", function (event) {

        if (event.key === "Escape") {
            cerrarModal();
        }
    });

    // ========================================
    // BUSQUEDA DE DETALLES
    // ========================================

    const buscarDetalle = document.getElementById("buscarDetalle");

    const tablaDetalles = document.getElementById("tablaDetalles");

    const cantidadVisible = document.getElementById("cantidadVisible");

    if (buscarDetalle && tablaDetalles) {

        buscarDetalle.addEventListener("input", function () {

            const texto = buscarDetalle.value
                .toLowerCase()
                .trim();

            const filas = tablaDetalles.querySelectorAll(
                "tr[data-detalle='true']"
            );

            let visibles = 0;

            filas.forEach(function (fila) {

                const contenido = fila.textContent.toLowerCase();

                const coincide = contenido.includes(texto);

                fila.style.display = coincide ? "" : "none";

                if (coincide) {
                    visibles++;
                }
            });

            if (cantidadVisible) {
                cantidadVisible.textContent = visibles;
            }
        });
    }

    // ========================================
    // CALCULAR SUBTOTAL AUTOMATICAMENTE
    // ========================================

    const cantidadRepuestos = document.getElementById(
        "cantidadRepuestos"
    );

    const precioUnitario = document.getElementById(
        "precioUnitarioAplicado"
    );

    const subtotal = document.getElementById("subtotal");

    function calcularSubtotal() {

        if (!cantidadRepuestos || !precioUnitario || !subtotal) {
            return;
        }

        const cantidad = parseFloat(cantidadRepuestos.value) || 0;

        const precio = parseFloat(precioUnitario.value) || 0;

        const total = cantidad * precio;

        subtotal.value = total.toFixed(2);
    }

    if (cantidadRepuestos) {
        cantidadRepuestos.addEventListener(
            "input",
            calcularSubtotal
        );
    }

    if (precioUnitario) {
        precioUnitario.addEventListener(
            "input",
            calcularSubtotal
        );
    }

    calcularSubtotal();

    // ========================================
    // MENU LATERAL RESPONSIVE
    // ========================================

    const sidebar = document.getElementById("sidebar");

    const menuButton = document.getElementById("menuButton");

    const closeSidebar = document.getElementById("closeSidebar");

    const sidebarOverlay = document.getElementById("sidebarOverlay");

    function abrirSidebar() {

        if (sidebar) {
            sidebar.classList.add("show");
        }

        if (sidebarOverlay) {
            sidebarOverlay.classList.add("show");
        }
    }

    function cerrarSidebar() {

        if (sidebar) {
            sidebar.classList.remove("show");
        }

        if (sidebarOverlay) {
            sidebarOverlay.classList.remove("show");
        }
    }

    if (menuButton) {
        menuButton.addEventListener("click", abrirSidebar);
    }

    if (closeSidebar) {
        closeSidebar.addEventListener("click", cerrarSidebar);
    }

    if (sidebarOverlay) {
        sidebarOverlay.addEventListener("click", cerrarSidebar);
    }

});
