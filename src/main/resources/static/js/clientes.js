document.addEventListener("DOMContentLoaded", function () {

    // =========================
    // MENÚ LATERAL
    // =========================

    const sidebar = document.getElementById("sidebar");
    const menuButton = document.getElementById("menuButton");
    const closeSidebar = document.getElementById("closeSidebar");
    const sidebarOverlay = document.getElementById("sidebarOverlay");

    function abrirSidebar() {
        sidebar.classList.add("open");
        sidebarOverlay.classList.add("show");
    }

    function cerrarSidebar() {
        sidebar.classList.remove("open");
        sidebarOverlay.classList.remove("show");
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

    window.addEventListener("resize", function () {

        if (window.innerWidth > 1050) {
            cerrarSidebar();
        }

    });


    // =========================
    // MODAL NUEVO CLIENTE
    // =========================

    const modalCliente = document.getElementById("modalCliente");
    const btnNuevoCliente = document.getElementById("btnNuevoCliente");
    const cerrarModal = document.getElementById("cerrarModal");
    const cancelarCliente = document.getElementById("cancelarCliente");
    const modalBackdrop = document.getElementById("modalBackdrop");
    const formCliente = document.getElementById("formCliente");

    function abrirModalCliente() {

        modalCliente.classList.add("show");

        document.body.style.overflow = "hidden";

    }

    function cerrarModalCliente() {

        modalCliente.classList.remove("show");

        document.body.style.overflow = "";

    }

    btnNuevoCliente.addEventListener("click", abrirModalCliente);

    cerrarModal.addEventListener("click", cerrarModalCliente);

    cancelarCliente.addEventListener("click", cerrarModalCliente);

    modalBackdrop.addEventListener("click", cerrarModalCliente);


    // =========================
    // GUARDADO TEMPORAL
    // =========================

    formCliente.addEventListener("submit", function (event) {

        event.preventDefault();

        const identificacion =
            document.getElementById("identificacion").value.trim();

        const nombre =
            document.getElementById("nombre").value.trim();

        const primerApellido =
            document.getElementById("primerApellido").value.trim();

        const segundoApellido =
            document.getElementById("segundoApellido").value.trim();

        const telefono =
            document.getElementById("telefono").value.trim();

        const correo =
            document.getElementById("correo").value.trim();

        if (
            identificacion === "" ||
            nombre === "" ||
            primerApellido === "" ||
            telefono === "" ||
            correo === ""
        ) {
            return;
        }

        /*
         * Guardado temporal.
         *
         * Posteriormente este formulario enviará los datos
         * al backend de Spring Boot y finalmente Oracle
         * realizará el registro mediante la lógica
         * correspondiente.
         */

        alert(
            "Cliente registrado temporalmente.\n\n" +
            nombre + " " +
            primerApellido + " " +
            segundoApellido
        );

        formCliente.reset();

        cerrarModalCliente();

    });


    // =========================
    // BUSCADOR
    // =========================

    const buscarCliente = document.getElementById("buscarCliente");
    const filtroEstado = document.getElementById("filtroEstado");
    const filasClientes = document.querySelectorAll("#tablaClientes tr");
    const cantidadVisible = document.getElementById("cantidadVisible");

    function filtrarClientes() {

        const texto =
            buscarCliente.value.toLowerCase().trim();

        const estado =
            filtroEstado.value;

        let visibles = 0;

        filasClientes.forEach(function (fila) {

            const contenido =
                fila.textContent.toLowerCase();

            const estadoFila =
                fila.dataset.estado;

            const coincideTexto =
                contenido.includes(texto);

            const coincideEstado =
                estado === "todos" ||
                estado === estadoFila;

            if (coincideTexto && coincideEstado) {

                fila.style.display = "";

                visibles++;

            } else {

                fila.style.display = "none";

            }

        });

        cantidadVisible.textContent = visibles;

    }

    buscarCliente.addEventListener(
        "input",
        filtrarClientes
    );

    filtroEstado.addEventListener(
        "change",
        filtrarClientes
    );


    // =========================
    // CERRAR MODAL CON ESC
    // =========================

    document.addEventListener("keydown", function (event) {

        if (
            event.key === "Escape" &&
            modalCliente.classList.contains("show")
        ) {
            cerrarModalCliente();
        }

    });

});