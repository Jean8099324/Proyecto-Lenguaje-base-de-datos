document.addEventListener("DOMContentLoaded", () => {
    const sidebar = document.getElementById("sidebar");
    const overlay = document.getElementById("sidebarOverlay");
    const cerrar = () => { sidebar?.classList.remove("open"); overlay?.classList.remove("show"); };
    document.getElementById("menuButton")?.addEventListener("click", () => {
        sidebar?.classList.add("open"); overlay?.classList.add("show");
    });
    document.getElementById("closeSidebar")?.addEventListener("click", cerrar);
    overlay?.addEventListener("click", cerrar);

    const buscar = document.getElementById("buscarVehiculo");
    const filas = [...document.querySelectorAll("#tablaVehiculos tr")].filter(f => f.cells.length === 8);
    const cantidad = document.getElementById("cantidadVisible");
    buscar?.addEventListener("input", () => {
        let total = 0;
        const texto = buscar.value.trim().toLowerCase();
        filas.forEach(fila => {
            const coincide = fila.textContent.toLowerCase().includes(texto);
            fila.hidden = !coincide;
            if (coincide) total++;
        });
        if (cantidad) cantidad.textContent = String(total);
    });

    const marca = document.getElementById("idMarca");
    const modelo = document.getElementById("idModelo");
    const anio = document.getElementById("idAnio");
    if (!marca || !modelo || !anio) return;

    function filtrar(select, idPadre) {
        let valido = false;
        [...select.options].forEach(opcion => {
            if (!opcion.value) return;
            const coincide = !!idPadre && opcion.dataset.padre === idPadre;
            opcion.hidden = !coincide;
            opcion.disabled = !coincide;
            if (coincide && select.value === opcion.value) valido = true;
        });
        if (!valido) select.value = "";
        select.disabled = !idPadre;
    }
    function actualizar() {
        filtrar(modelo, marca.value);
        filtrar(anio, modelo.value);
    }
    marca.addEventListener("change", () => {
        modelo.value = "";
        anio.value = "";
        actualizar();
    });
    modelo.addEventListener("change", () => {
        anio.value = "";
        actualizar();
    });
    actualizar();
});
