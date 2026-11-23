document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("modalIncidente");
    const catalogCache = new Map();
    const getCatalog = async (name, parent = "") => {
        const key = `${name}:${parent}`;
        if (!catalogCache.has(key)) {
            const query = parent ? `?padre=${encodeURIComponent(parent)}` : "";
            catalogCache.set(key, fetch(`/incidentes/catalogos/${name}${query}`, {
                headers: {Accept: "application/json"}
            }).then(async response => {
                if (!response.ok) throw new Error(`No se pudo cargar el catálogo ${name}.`);
                return response.json();
            }));
        }
        return catalogCache.get(key);
    };
    const fillOptions = (select, items, placeholder, parentKey) => {
        const previous = select.value;
        const options = items.map(item => {
            const option = new Option(item.etiqueta ?? item.nombreEstado ?? item.nombre ?? "", item.id);
            if (parentKey && item.relacionId != null) option.dataset[parentKey] = item.relacionId;
            return option;
        });
        select.replaceChildren(new Option(placeholder, ""), ...options);
        if (options.some(option => option.value === previous)) select.value = previous;
    };
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
    [
        ['#modalIncidente select[name="idTipoIncidente"]', "tipos", "Seleccione tipo"],
        ['#modalIncidente select[name="idPrioridad"]', "prioridades", "Seleccione prioridad"],
        ['#modalIncidente select[name="idEstado"]', "estados", "Seleccione estado"]
    ].forEach(([selector, catalog, prompt]) => {
        const select = document.querySelector(selector);
        select?.addEventListener("focus", async () => {
            try { fillOptions(select, await getCatalog(catalog), prompt); }
            catch (error) { console.error(error); }
        }, {once: true});
    });
    ["incidente", "cliente"].forEach(prefix => {
        const province = document.getElementById(`${prefix}Provincia`);
        const canton = document.getElementById(`${prefix}Canton`);
        const district = document.getElementById(`${prefix}Distrito`);
        if (!province || !canton || !district) return;
        province.addEventListener("focus", async () => {
            try { fillOptions(province, await getCatalog("provincias"), "Seleccione provincia"); }
            catch (error) { console.error(error); }
        }, {once: true});
        province.addEventListener("change", async () => {
            canton.replaceChildren(new Option("Seleccione cantón", ""));
            district.replaceChildren(new Option("Seleccione distrito", ""));
            canton.disabled = !province.value;
            district.disabled = true;
            if (!province.value) return;
            try { fillOptions(canton, await getCatalog("cantones", province.value), "Seleccione cantón", "provincia"); }
            catch (error) { console.error(error); }
        });
        canton.addEventListener("change", async () => {
            district.replaceChildren(new Option("Seleccione distrito", ""));
            district.disabled = !canton.value;
            if (!canton.value) return;
            try { fillOptions(district, await getCatalog("distritos", canton.value), "Seleccione distrito", "canton"); }
            catch (error) { console.error(error); }
        });
    });
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
    const optionLabels = [
        {filter: filters[0], key: "tipoEtiqueta", prompt: "Todos los tipos"},
        {filter: filters[1], key: "prioridadEtiqueta", prompt: "Todas las prioridades"},
        {filter: filters[2], key: "estadoEtiqueta", prompt: "Todos los estados"}
    ];
    optionLabels.forEach(({filter, key, prompt}) => {
        if (!filter) return;
        const labels = [...new Set(rows.map(row => row.dataset[key]).filter(Boolean))]
            .sort((a, b) => a.localeCompare(b, "es"));
        filter.replaceChildren(new Option(prompt, ""), ...labels.map(label => new Option(label, label)));
    });
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
                && (!type || row.dataset.tipoEtiqueta === type)
                && (!priority || row.dataset.prioridadEtiqueta === priority)
                && (!status || row.dataset.estadoEtiqueta === status)
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
    const ownerMessage = document.getElementById("propietarioVehiculo");
    const vehicleModal = document.getElementById("modalVehiculo");
    const vehicleForm = document.getElementById("formVehiculo");
    const vehicleError = document.getElementById("vehiculoError");
    const inputModal = document.getElementById("modalIncidente");
    let vehicleButtons = [...document.querySelectorAll(".vehicle-suggestion")];
    let vehiclesLoaded = vehicleButtons.length > 0;
    const loadVehicles = async () => {
        if (vehiclesLoaded) return;
        const vehicles = await getCatalog("vehiculos");
        vehicleButtons = vehicles.map(vehicle => {
            const button = document.createElement("button");
            button.type = "button";
            button.className = "vehicle-suggestion";
            button.dataset.id = vehicle.id;
            button.dataset.placa = vehicle.etiqueta;
            button.dataset.ocupado = String(vehicle.incidenteEnProceso);
            button.textContent = vehicle.etiqueta;
            return button;
        });
        vehiclesLoaded = true;
    };
    const normalize = value => value.replace(/[^a-zA-Z0-9]/g, "").toUpperCase();
    const cleanPlateInput = input => {
        const caret = input.selectionStart;
        const position = caret == null ? null : normalize(input.value.slice(0, caret)).length;
        input.value = normalize(input.value);
        if (position != null) input.setSelectionRange(position, position);
    };
    const newPlateInput = document.getElementById("nuevaPlaca");
    newPlateInput?.addEventListener("input", () => cleanPlateInput(newPlateInput));
    const chooseVehicle = (id, plate, ocupado = false) => {
        if (plateConfirmed) plateConfirmed.hidden = ocupado;
        vehicleId.value = id;
        plateInput.value = normalize(plate);
        suggestions.hidden = true;
        addVehicle.hidden = true;
        selectedMessage.textContent = ocupado
            ? "Esta placa ya está ligada a otro incidente en proceso."
            : `Vehículo seleccionado: ${plate}`;
        selectedMessage.classList.add("selected");
        selectedMessage.classList.toggle("occupied", ocupado);
        if (ownerMessage) {
            ownerMessage.hidden = false;
            ownerMessage.textContent = "Cargando cliente propietario...";
            fetch(`/incidentes/vehiculos/${id}/propietario`, {headers: {Accept: "application/json"}})
                .then(async response => {
                    if (!response.ok) throw new Error("No se pudo cargar el propietario del vehículo.");
                    return response.json();
                })
                .then(data => {
                    if (vehicleId.value === String(id)) ownerMessage.textContent = `Cliente propietario: ${data.nombre}`;
                })
                .catch(() => {
                    if (vehicleId.value === String(id)) ownerMessage.textContent = "No se pudo cargar el propietario del vehículo.";
                });
        }
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
        if (current) chooseVehicle(current.dataset.id, current.dataset.placa, current.dataset.ocupado === "true");
    }
    const updatePlateSearch = () => {
        if (plateConfirmed) plateConfirmed.hidden = true;
        cleanPlateInput(plateInput);
        vehicleId.value = "";
        if (ownerMessage) { ownerMessage.hidden = true; ownerMessage.textContent = ""; }
        selectedMessage.textContent = "";
        selectedMessage.classList.remove("selected");
        selectedMessage.classList.remove("occupied");
        const query = normalize(plateInput.value);
        const exactMatch = query ? matchPlate(query) : null;
        const matches = vehicleButtons.filter(button => normalize(button.dataset.placa).includes(query));
        suggestions.replaceChildren(...matches.slice(0, 8));
        suggestions.hidden = !query || matches.length === 0;
        addVehicle.hidden = !query || Boolean(exactMatch);
        selectedMessage.textContent = !query
            ? "Seleccione una placa existente."
            : exactMatch
                ? exactMatch.dataset.ocupado === "true"
                    ? "La placa ya está ligada a otro incidente en proceso."
                    : "Seleccione el vehículo de la lista."
                : `La placa ${query} no aparece entre los vehículos activos. Puede registrarla.`;
        selectedMessage.classList.toggle("occupied", exactMatch?.dataset.ocupado === "true");
    };
    plateInput?.addEventListener("input", async () => {
        try { await loadVehicles(); updatePlateSearch(); } catch (error) { console.error(error); }
    });
    plateInput?.addEventListener("change", async () => {
        try { await loadVehicles(); updatePlateSearch(); } catch (error) { console.error(error); }
    });
    plateInput?.addEventListener("focus", async () => {
        try { await loadVehicles(); } catch (error) { console.error(error); return; }
        if (!plateInput.value.trim()) {
            suggestions.replaceChildren(...vehicleButtons.slice(0, 8));
            suggestions.hidden = vehicleButtons.length === 0;
        }
    });
    suggestions?.addEventListener("click", event => {
        const button = event.target.closest(".vehicle-suggestion");
        if (button) chooseVehicle(button.dataset.id, button.dataset.placa, button.dataset.ocupado === "true");
    });
    addVehicle?.addEventListener("click", async () => {
        document.getElementById("nuevaPlaca").value = plateInput.value.trim().toLocaleUpperCase("es");
        vehicleError.hidden = true;
        vehicleError.textContent = "";
        try {
            const [brands, models, years] = await Promise.all([
                getCatalog("marcas"), getCatalog("modelos"), getCatalog("anios")
            ]);
            fillOptions(document.getElementById("idMarca"), brands, "Seleccione marca");
            fillOptions(document.getElementById("idModelo"), models, "Seleccione modelo", "marca");
            fillOptions(document.getElementById("idAnio"), years, "Seleccione año", "modelo");
        } catch (error) { console.error(error); }
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
