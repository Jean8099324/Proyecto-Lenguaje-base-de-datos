document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("modalIncidente");
    document.querySelectorAll("#tablaIncidentes .status-pill").forEach((pill) => {
        const estado = pill.textContent.trim().toUpperCase();
        const clases = {
            "REPORTADO": "status-reported",
            "ASIGNADO": "status-assigned",
            "UNIDAD EN CAMINO": "status-dispatched",
            "EN ATENCION": "status-attending",
            "TRASLADO": "status-transfer",
            "FINALIZADO": "status-completed"
        };
        pill.classList.add(clases[estado] || "status-default");
    });
    const configurarDireccion = (prefijo) => {
        const provincia = document.getElementById(`${prefijo}Provincia`);
        const canton = document.getElementById(`${prefijo}Canton`);
        const distrito = document.getElementById(`${prefijo}Distrito`);
        if (!provincia || !canton || !distrito) return;
        const cantones = [...canton.options].filter(option => option.value);
        const distritos = [...distrito.options].filter(option => option.value);
        const filtrar = (select, opciones, atributo, padre, etiqueta) => {
            const anterior = select.value;
            const disponibles = opciones.filter(option => padre && option.dataset[atributo] === padre);
            select.replaceChildren(new Option(etiqueta, ""), ...disponibles);
            select.value = disponibles.some(option => option.value === anterior) ? anterior : "";
            select.disabled = !padre;
        };
        const actualizarDistritos = () =>
            filtrar(distrito, distritos, "canton", canton.value, "Seleccione distrito");
        const actualizarCantones = () => {
            filtrar(canton, cantones, "provincia", provincia.value, "Seleccione cantón");
            actualizarDistritos();
        };
        provincia.addEventListener("change", () => {
            canton.value = "";
            distrito.value = "";
            actualizarCantones();
        });
        canton.addEventListener("change", () => {
            distrito.value = "";
            actualizarDistritos();
        });
        actualizarCantones();
    };
    configurarDireccion("incidente");
    configurarDireccion("cliente");
    const clienteIncidente = document.getElementById("clienteIncidente");
    const usernameCliente = document.getElementById("usernameCliente");
    const clientDependent = [...document.querySelectorAll(".client-dependent")];
    const toggleClientFields = () => {
        const hasClient = Boolean(clienteIncidente?.value);
        clientDependent.forEach(container => {
            container.classList.toggle("client-dependent-hidden", !hasClient);
            container.querySelectorAll("input, select, textarea").forEach(field => {
                field.disabled = !hasClient;
            });
        });
    };
    const renderClientContacts = (tipo, valores) => {
        const list = document.querySelector(`[data-contact-list="${tipo}"]`);
        if (!list) return;
        list.replaceChildren();
        if (!valores.length) {
            const empty = document.createElement("p");
            empty.className = "client-contact-empty";
            empty.textContent = `Sin ${tipo === "correo" ? "correos" : "teléfonos"} registrados.`;
            list.appendChild(empty);
            return;
        }
        valores.forEach(valor => {
            const link = document.createElement("a");
            link.className = "client-contact-value";
            link.href = `${tipo === "correo" ? "mailto:" : "tel:"}${valor}`;
            link.textContent = valor;
            list.appendChild(link);
        });
    };
    const cargarCliente = async id => {
        if (!id) return;
        const response = await fetch(`/incidentes/clientes/${id}`, {headers: {Accept: "application/json"}});
        if (!response.ok) throw new Error("No se pudieron cargar los datos del cliente.");
        return response.json();
    };
    clienteIncidente?.addEventListener("change", async () => {
        const opcion = clienteIncidente.selectedOptions[0];
        if (usernameCliente) usernameCliente.value = opcion?.dataset.username ?? "";
        const nombre = document.getElementById("nombreCliente");
        const primerApellido = document.getElementById("primerApellidoCliente");
        const segundoApellido = document.getElementById("segundoApellidoCliente");
        if (nombre) nombre.value = opcion?.dataset.nombre ?? "";
        if (primerApellido) primerApellido.value = opcion?.dataset.primerApellido ?? "";
        if (segundoApellido) segundoApellido.value = opcion?.dataset.segundoApellido ?? "";
        const idContacto = document.getElementById("idContactoIncidente");
        const contactoFields = ["nombreContactoIncidente", "primerApellidoContactoIncidente",
            "segundoApellidoContactoIncidente", "telefonoContactoIncidente"];
        if (idContacto) idContacto.value = "";
        contactoFields.forEach(id => { const field = document.getElementById(id); if (field) field.value = ""; });
        if (clienteIncidente.value) {
            try {
                const cliente = await cargarCliente(clienteIncidente.value);
                const direccion = cliente.direccion;
                const provincia = document.getElementById("clienteProvincia");
                const canton = document.getElementById("clienteCanton");
                const distrito = document.getElementById("clienteDistrito");
                const senas = document.getElementById("otrasSenasCliente");
                if (provincia) { provincia.value = direccion?.provincia ?? ""; provincia.dispatchEvent(new Event("change")); }
                if (canton) { canton.value = direccion?.canton ?? ""; canton.dispatchEvent(new Event("change")); }
                if (distrito) distrito.value = direccion?.distrito ?? "";
                if (senas) senas.value = direccion?.otrasSenas ?? "";
                renderClientContacts("correo", cliente.correos ?? []);
                renderClientContacts("telefono", cliente.telefonos ?? []);
            } catch (error) {
                console.error(error);
            }
        } else {
            renderClientContacts("correo", []);
            renderClientContacts("telefono", []);
        }
        toggleClientFields();
    });
    toggleClientFields();
    const fillReportDateTime = () => {
        const parts = Object.fromEntries(new Intl.DateTimeFormat("en-GB", {
            timeZone: "America/Costa_Rica", year: "numeric", month: "2-digit",
            day: "2-digit", hour: "2-digit", minute: "2-digit", hourCycle: "h23"
        }).formatToParts(new Date()).map(part => [part.type, part.value]));
        const date = modal?.querySelector('[name="fechaReporte"]');
        const time = modal?.querySelector('[name="horaReporte"]');
        if (date && !date.value) date.value = `${parts.year}-${parts.month}-${parts.day}`;
        if (time && !time.value) time.value = `${parts.hour}:${parts.minute}`;
    };
    const open = () => {
        fillReportDateTime();
        modal?.classList.add("show");
        document.body.style.overflow = "hidden";
    };
    const close = () => { modal?.classList.remove("show"); document.body.style.overflow = ""; };
    if (modal?.classList.contains("show")) open();
    document.getElementById("btnNuevoIncidente")?.addEventListener("click", open);
    document.getElementById("cerrarModal")?.addEventListener("click", close);
    document.getElementById("cancelarIncidente")?.addEventListener("click", close);
    document.getElementById("modalBackdrop")?.addEventListener("click", close);

    const search = document.getElementById("buscarIncidente");
    const filters = ["filtroTipo", "filtroPrioridad", "filtroEstado"]
        .map(id => document.getElementById(id)).filter(Boolean);
    const rows = [...document.querySelectorAll("#tablaIncidentes tr[data-tipo]")];
    const noResults = document.getElementById("sinResultados");
    const dateFrom = document.getElementById("filtroFechaDesde");
    const dateTo = document.getElementById("filtroFechaHasta");
    const applyFilters = () => {
        const query = search?.value.trim().toLocaleLowerCase("es") ?? "";
        const [type, priority, status] = filters.map(filter => filter.value);
        const from = dateFrom?.value ?? "";
        const to = dateTo?.value ?? "";
        let visible = 0;
        rows.forEach(row => {
            const reportDate = row.dataset.fecha || "";
            const matches = row.textContent.toLocaleLowerCase("es").includes(query)
                && (!type || row.dataset.tipo === type)
                && (!priority || row.dataset.prioridad === priority)
                && (!status || row.dataset.estado === status)
                && (!from || (reportDate && reportDate >= from))
                && (!to || (reportDate && reportDate <= to));
            row.hidden = !matches;
            if (matches) visible++;
        });
        if (noResults) noResults.hidden = visible > 0 || rows.length === 0;
    };
    search?.addEventListener("input", applyFilters);
    filters.forEach(filter => filter.addEventListener("change", applyFilters));
    [dateFrom, dateTo].forEach(filter => filter?.addEventListener("change", applyFilters));
    document.getElementById("limpiarFechas")?.addEventListener("click", () => {
        dateFrom.value = "";
        dateTo.value = "";
        applyFilters();
    });

    const tableBody = document.getElementById("tablaIncidentes");
    const sortButtons = [...document.querySelectorAll(".sort-button")];
    const sortState = { key: "", direction: "asc" };
    const sortValue = (row, key) => key === "fecha"
        ? `${row.dataset.fecha || ""}T${row.dataset.hora || ""}`
        : row.dataset[`${key}Etiqueta`]?.toLocaleLowerCase("es") || "";
    sortButtons.forEach(button => button.addEventListener("click", () => {
        const key = button.dataset.sort;
        sortState.direction = sortState.key === key && sortState.direction === "asc" ? "desc" : "asc";
        sortState.key = key;
        rows.sort((first, second) => sortValue(first, key).localeCompare(sortValue(second, key), "es", { numeric: true })
            * (sortState.direction === "asc" ? 1 : -1));
        rows.forEach(row => tableBody.insertBefore(row, noResults));
        sortButtons.forEach(item => {
            const icon = item.querySelector("i");
            item.classList.toggle("active", item === button);
            item.setAttribute("aria-sort", item === button ? (sortState.direction === "asc" ? "ascending" : "descending") : "none");
            icon.className = item === button
                ? `bi ${sortState.direction === "asc" ? "bi-arrow-up" : "bi-arrow-down"}`
                : "bi bi-arrow-down-up";
        });
    }));
    rows.forEach(row => row.addEventListener("dblclick", event => {
        if (event.target.closest(".incident-actions")) return;
        if (row.dataset.detalleUrl) window.location.href = row.dataset.detalleUrl;
    }));

    document.querySelectorAll(".add-client-contact").forEach(button => {
        button.addEventListener("click", () => {
            const tipo = button.dataset.contactAdd;
            const list = document.querySelector(`[data-contact-list="${tipo}"]`);
            if (!list) return;
            const input = document.createElement("input");
            input.type = tipo === "correo" ? "email" : "tel";
            input.name = tipo === "correo" ? "correosClienteNuevos" : "telefonosClienteNuevos";
            input.maxLength = tipo === "correo" ? 150 : 8;
            if (tipo === "telefono") input.pattern = "[0-9]{8}";
            input.placeholder = tipo === "correo" ? "correo@ejemplo.com" : "Ej. 88888888";
            input.className = "new-client-contact";
            list.appendChild(input);
            input.focus();
        });
    });

    const plateInput = document.querySelector("#modalIncidente #placaVehiculo");
    const plateConfirmed = document.getElementById("placaConfirmada");
    const vehicleId = document.querySelector('#modalIncidente input[name="idVehiculo"]');
    const suggestions = document.getElementById("vehiculosSugeridos");
    const addVehicle = document.getElementById("btnAbrirVehiculo");
    const selectedMessage = document.getElementById("vehiculoSeleccionado");
    const vehicleModal = document.getElementById("modalVehiculo");
    const vehicleForm = document.getElementById("formVehiculo");
    const vehicleError = document.getElementById("vehiculoError");
    const inputModal = document.getElementById("modalIncidente");
    const vehicleButtons = [...document.querySelectorAll(".vehicle-suggestion")];
    const normalize = value => value.replace(/[^a-zA-Z0-9]/g, "").toUpperCase();
    const cleanPlateInput = input => {
        const caret = input.selectionStart;
        const position = caret == null ? null : normalize(input.value.slice(0, caret)).length;
        input.value = normalize(input.value);
        if (position != null) input.setSelectionRange(position, position);
    };
    const newPlateInput = document.getElementById("nuevaPlaca");
    newPlateInput?.addEventListener("input", () => cleanPlateInput(newPlateInput));
    const chooseVehicle = (id, plate) => {
        if (plateConfirmed) plateConfirmed.hidden = false;
        vehicleId.value = id;
        plateInput.value = normalize(plate);
        suggestions.hidden = true;
        addVehicle.hidden = true;
        selectedMessage.textContent = `Vehículo seleccionado: ${plate}`;
        selectedMessage.classList.add("selected");
    };
    const findById = id => vehicleButtons.find(button => button.dataset.id === id);
    const matchPlate = plate => vehicleButtons.find(button => normalize(button.dataset.placa) === normalize(plate));
    plateInput?.closest("form")?.addEventListener("submit", event => {
        if (!vehicleId.value) {
            event.preventDefault();
            selectedMessage.textContent = "Seleccione una placa existente o registre el vehículo con el botón +.";
            selectedMessage.classList.remove("selected");
            plateInput.focus();
        }
    });

    if (vehicleId?.value) {
        const current = findById(vehicleId.value);
        if (current) chooseVehicle(current.dataset.id, current.dataset.placa);
    }
    const updatePlateSearch = () => {
        if (plateConfirmed) plateConfirmed.hidden = true;
        cleanPlateInput(plateInput);
        vehicleId.value = "";
        selectedMessage.textContent = "";
        selectedMessage.classList.remove("selected");
        const query = normalize(plateInput.value);
        const exactMatch = query ? matchPlate(query) : null;
        const matches = vehicleButtons.filter(button => normalize(button.dataset.placa).includes(query));
        suggestions.replaceChildren(...matches.slice(0, 8));
        suggestions.hidden = !query || matches.length === 0;
        addVehicle.hidden = !query || Boolean(exactMatch);
        selectedMessage.textContent = !query
            ? "Seleccione una placa existente."
            : exactMatch
                ? "Seleccione el vehículo de la lista."
                : `La placa ${query} no aparece entre los vehículos activos. Puede registrarla.`;
    };
    plateInput?.addEventListener("input", updatePlateSearch);
    plateInput?.addEventListener("change", updatePlateSearch);
    plateInput?.addEventListener("focus", () => {
        if (!plateInput.value.trim()) {
            suggestions.replaceChildren(...vehicleButtons.slice(0, 8));
            suggestions.hidden = vehicleButtons.length === 0;
        }
    });
    suggestions?.addEventListener("click", event => {
        const button = event.target.closest(".vehicle-suggestion");
        if (button) chooseVehicle(button.dataset.id, button.dataset.placa);
    });
    addVehicle?.addEventListener("click", () => {
        document.getElementById("nuevaPlaca").value = plateInput.value.trim().toLocaleUpperCase("es");
        vehicleError.hidden = true;
        vehicleError.textContent = "";
        vehicleModal.classList.add("show");
    });
    const closeVehicleModal = () => {
        vehicleModal?.classList.remove("show");
        if (inputModal?.classList.contains("show")) document.body.style.overflow = "hidden";
    };
    ["cerrarVehiculo", "cancelarVehiculo", "vehiculoBackdrop"].forEach(id =>
        document.getElementById(id)?.addEventListener("click", closeVehicleModal));

    const brand = document.getElementById("idMarca");
    const model = document.getElementById("idModelo");
    const year = document.getElementById("idAnio");
    const syncCatalog = (select, dataKey, parentValue) => {
        if (!select) return;
        select.value = "";
        [...select.options].forEach(option => {
            if (!option.value) return;
            option.hidden = !parentValue || option.dataset[dataKey] !== parentValue;
            option.disabled = option.hidden;
        });
        select.disabled = !parentValue;
    };
    brand?.addEventListener("change", () => {
        syncCatalog(model, "marca", brand.value);
        syncCatalog(year, "modelo", "");
    });
    model?.addEventListener("change", () => syncCatalog(year, "modelo", model.value));
    if (brand && model && year) {
        model.disabled = true;
        year.disabled = true;
    }

    const editVehicleModal = document.getElementById("modalEditarVehiculo");
    const editVehicleButton = document.getElementById("btnEditarVehiculo");
    const editVehicleBrand = document.getElementById("marcaVehiculoModal");
    const editVehicleModel = document.getElementById("modeloVehiculoModal");
    const editVehicleYear = document.getElementById("anioVehiculoModal");
    const filterVehicleOptions = (select, attribute, parentValue, selectedValue) => {
        if (!select) return;
        [...select.options].forEach(option => {
            const available = !option.value || (parentValue && option.dataset[attribute] === parentValue);
            option.hidden = !available;
            option.disabled = !available;
        });
        if (selectedValue && [...select.options].some(option => option.value === selectedValue && !option.disabled))
            select.value = selectedValue;
    };
    const syncEditVehicleCatalog = () => {
        const modelValue = editVehicleModel?.value;
        filterVehicleOptions(editVehicleModel, "marca", editVehicleBrand?.value, modelValue);
        filterVehicleOptions(editVehicleYear, "modelo", editVehicleModel?.value, editVehicleYear?.value);
        if (editVehicleModel) editVehicleModel.disabled = !editVehicleBrand?.value;
        if (editVehicleYear) editVehicleYear.disabled = !editVehicleModel?.value;
    };
    editVehicleBrand?.addEventListener("change", () => {
        if (editVehicleModel) editVehicleModel.value = "";
        if (editVehicleYear) editVehicleYear.value = "";
        syncEditVehicleCatalog();
    });
    editVehicleModel?.addEventListener("change", () => {
        if (editVehicleYear) editVehicleYear.value = "";
        syncEditVehicleCatalog();
    });
    syncEditVehicleCatalog();
    const closeEditVehicleModal = () => {
        editVehicleModal?.classList.remove("show");
        document.body.style.overflow = "";
    };
    editVehicleButton?.addEventListener("click", () => {
        editVehicleModal?.classList.add("show");
        document.body.style.overflow = "hidden";
    });
    ["cerrarEditarVehiculo", "cancelarEditarVehiculo", "editarVehiculoBackdrop"].forEach(id =>
        document.getElementById(id)?.addEventListener("click", closeEditVehicleModal));

    vehicleForm?.addEventListener("submit", async event => {
        event.preventDefault();
        const submit = document.getElementById("guardarVehiculo");
        submit.disabled = true;
        vehicleError.hidden = true;
        try {
            const response = await fetch(vehicleForm.action, {
                method: "POST",
                headers: { "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8", "Accept": "application/json" },
                body: new URLSearchParams(new FormData(vehicleForm))
            });
            const result = await response.json();
            if (!response.ok) throw new Error(result.error || "No se pudo guardar el vehículo.");
            const option = document.createElement("button");
            option.type = "button";
            option.className = "vehicle-suggestion";
            option.dataset.id = result.id;
            option.dataset.placa = result.placa;
            option.textContent = result.placa;
            vehicleButtons.push(option);
            chooseVehicle(result.id, result.placa);
            closeVehicleModal();
            vehicleForm.reset();
            syncCatalog(model, "marca", "");
            syncCatalog(year, "modelo", "");
        } catch (error) {
            vehicleError.textContent = error.message;
            vehicleError.hidden = false;
        } finally {
            submit.disabled = false;
        }
    });
});
