document.addEventListener("DOMContentLoaded", () => {
    const sidebar = document.getElementById("sidebar");
    const overlay = document.getElementById("sidebarOverlay");
    const cerrar = () => { sidebar?.classList.remove("open"); overlay?.classList.remove("show"); };
    document.getElementById("menuButton")?.addEventListener("click", () => {
        sidebar?.classList.add("open"); overlay?.classList.add("show");
    });
    document.getElementById("closeSidebar")?.addEventListener("click", cerrar);
    overlay?.addEventListener("click", cerrar);

    const buscador = document.getElementById("buscarCliente");
    const filas = [...document.querySelectorAll("#tablaClientes tr")].filter(fila => fila.cells.length === 7);
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

    const username = document.querySelector("[data-username-check]");
    const usernameStatus = document.getElementById("usernameStatus");
    const clienteId = document.querySelector('input[name="id"]')?.value;
    const formularioCliente = username?.closest("form");
    let usernameDisponible = false;
    let usernameTimer;
    const comprobarUsername = async () => {
        const valor = username.value.trim();
        usernameDisponible = false;
        if (!valor) {
            usernameStatus.textContent = "";
            usernameStatus.className = "username-status";
            return;
        }
        usernameStatus.textContent = "Comprobando...";
        usernameStatus.className = "username-status checking";
        try {
            const params = new URLSearchParams({username: valor});
            if (clienteId) params.set("id", clienteId);
            const response = await fetch(`/clientes/username-disponible?${params}`, {
                headers: {Accept: "application/json"}
            });
            if (!response.ok) throw new Error("No se pudo comprobar el username.");
            const result = await response.json();
            usernameDisponible = result.disponible === true;
            usernameStatus.textContent = result.mensaje;
            usernameStatus.className = `username-status ${usernameDisponible ? "available" : "unavailable"}`;
        } catch {
            usernameStatus.textContent = "No se pudo comprobar la disponibilidad.";
            usernameStatus.className = "username-status unavailable";
        }
    };
    username?.addEventListener("input", () => {
        clearTimeout(usernameTimer);
        usernameTimer = setTimeout(comprobarUsername, 350);
    });
    if (username?.value.trim()) comprobarUsername();
    formularioCliente?.addEventListener("submit", event => {
        if (username && !usernameDisponible) {
            event.preventDefault();
            usernameStatus.textContent = username.value.trim()
                ? "Espera a que se confirme la disponibilidad."
                : "Ingrese un username.";
            usernameStatus.className = "username-status unavailable";
            username.focus();
        }
    });

    document.querySelectorAll(".add-contact").forEach(button => {
        button.addEventListener("click", () => {
            const list = document.getElementById(button.dataset.target);
            const first = list?.querySelector("input");
            if (!list || !first) return;
            const row = document.createElement("div");
            row.className = "contact-input-row";
            const input = first.cloneNode();
            input.value = "";
            const remove = document.createElement("button");
            remove.type = "button";
            remove.className = "remove-contact";
            remove.title = "Quitar contacto";
            remove.innerHTML = '<i class="bi bi-dash-lg"></i>';
            remove.addEventListener("click", () => row.remove());
            row.append(input, remove);
            list.append(row);
            input.focus();
        });
    });

    const configurarDireccion = entry => {
        const provincia = entry.querySelector(".address-provincia");
        const canton = entry.querySelector(".address-canton");
        const distrito = entry.querySelector(".address-distrito");
        if (!provincia || !canton || !distrito) return;
        const filtrar = (select, padre) => {
            [...select.options].forEach(option => {
                if (!option.value) return;
                const visible = option.dataset.padre === padre && padre !== "";
                option.hidden = !visible;
                option.disabled = !visible;
            });
            if (select.selectedOptions[0]?.disabled) select.value = "";
        };
        provincia.addEventListener("change", () => {
            filtrar(canton, provincia.value);
            filtrar(distrito, "");
        });
        canton.addEventListener("change", () => filtrar(distrito, canton.value));
        filtrar(canton, provincia.value);
        filtrar(distrito, canton.value);
    };

    document.querySelectorAll(".address-entry").forEach(configurarDireccion);
    document.querySelector(".add-address")?.addEventListener("click", event => {
        const list = document.getElementById("direccionesNuevas");
        const original = list?.querySelector(".address-entry");
        if (!list || !original) return;
        const clone = original.cloneNode(true);
        clone.querySelectorAll("select, textarea").forEach(control => control.value = "");
        list.insertBefore(clone, event.currentTarget);
        configurarDireccion(clone);
    });
});
