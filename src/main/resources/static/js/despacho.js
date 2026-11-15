document.addEventListener("DOMContentLoaded", () => {
    const search = document.getElementById("buscarDespacho");
    const items = [...document.querySelectorAll(".dispatch-item")];
    const empty = document.getElementById("sinResultadosDespacho");
    const normalize = value => (value || "").normalize("NFD").replace(/[\u0300-\u036f]/g, "").toLowerCase();

    search?.addEventListener("input", () => {
        const query = normalize(search.value.trim());
        let visible = 0;
        items.forEach(item => {
            const matches = !query || normalize(item.dataset.search).includes(query);
            item.hidden = !matches;
            if (matches) visible++;
        });
        if (empty) empty.hidden = visible !== 0 || items.length === 0;
    });
});
