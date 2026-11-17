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

    const placa = document.getElementById("placa");
    const placaDuplicada = document.getElementById("placaDuplicada");
    const formularioVehiculo = placa?.form;
    const idVehiculo = document.getElementById("id");
    if (placa && placaDuplicada && formularioVehiculo) {
        let temporizador;
        let solicitud;
        let version = 0;
        let duplicada = false;
        let verificando = false;
        let envioPendiente = false;

        const normalizarPlaca = valor => valor.replace(/[^a-z0-9]/gi, "").toUpperCase();
        const verificarPlaca = async (valor, versionActual) => {
            solicitud = new AbortController();
            const parametros = new URLSearchParams({ placa: valor });
            if (idVehiculo?.value) parametros.set("id", idVehiculo.value);
            try {
                const endpoint = formularioVehiculo.dataset.plateCheckUrl || "/vehiculos/validar-placa";
                const respuesta = await fetch(`${endpoint}?${parametros}`, {
                    headers: { Accept: "application/json" },
                    signal: solicitud.signal,
                    cache: "no-store"
                });
                if (!respuesta.ok) throw new Error("No se pudo verificar la placa.");
                const resultado = await respuesta.json();
                if (versionActual !== version || normalizarPlaca(placa.value) !== valor) return;
                duplicada = resultado.duplicada === true;
                placaDuplicada.hidden = !duplicada;
                placa.setCustomValidity(duplicada ? "La placa ya está registrada." : "");
            } catch (error) {
                if (error.name !== "AbortError" && versionActual === version) {
                    // El servidor vuelve a validar la placa al guardar.
                    console.error(error);
                }
            } finally {
                if (versionActual === version) {
                    verificando = false;
                    if (envioPendiente) {
                        envioPendiente = false;
                        if (duplicada) placa.reportValidity();
                        else formularioVehiculo.requestSubmit();
                    }
                }
            }
        };

        placa.addEventListener("input", () => {
            const valor = normalizarPlaca(placa.value);
            clearTimeout(temporizador);
            solicitud?.abort();
            version++;
            duplicada = false;
            placa.setCustomValidity("");
            placaDuplicada.hidden = true;
            if (!valor) {
                verificando = false;
                envioPendiente = false;
                return;
            }
            verificando = true;
            const versionActual = version;
            temporizador = setTimeout(() => verificarPlaca(valor, versionActual), 400);
        });

        formularioVehiculo.addEventListener("submit", event => {
            if (duplicada) {
                event.preventDefault();
                placa.reportValidity();
            } else if (verificando) {
                event.preventDefault();
                envioPendiente = true;
            }
        });
    }

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
