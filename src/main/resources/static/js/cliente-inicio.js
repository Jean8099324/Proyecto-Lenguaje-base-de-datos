document.addEventListener("DOMContentLoaded", () => {
    const sidebar = document.getElementById("sidebar");
    const close = () => {
        sidebar.classList.remove("open");
        document.getElementById("sidebarOverlay").classList.remove("show");
    };
    sidebar.querySelectorAll('a[href*="#"]').forEach(link => link.addEventListener("click", close));
    document.addEventListener("keydown", event => {
        if (event.key === "Escape") close();
    });

});

document.addEventListener("DOMContentLoaded", () => {
    const modal = document.getElementById("incidentModal");
    if (!modal) return;
    const openers = document.querySelectorAll("[data-open-incident]");
    const closers = modal.querySelectorAll("[data-close-incident]");
    let lastFocused;
    const close = () => {
        modal.hidden = true;
        document.body.classList.remove("incident-modal-open");
        lastFocused?.focus();
    };
    const open = event => {
        event?.preventDefault();
        lastFocused = document.activeElement;
        modal.hidden = false;
        document.body.classList.add("incident-modal-open");
        modal.querySelector("form select, form input, form textarea")?.focus();
    };
    openers.forEach(button => button.addEventListener("click", open));
    if (new URLSearchParams(window.location.search).has("abrirIncidente")) open();
    closers.forEach(button => button.addEventListener("click", close));
    document.addEventListener("keydown", event => {
        if (event.key === "Escape" && !modal.hidden) close();
    });
});

document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-location]").forEach(group => {
        const province=group.querySelector("[data-province]");
        const canton=group.querySelector("[data-canton]");
        const district=group.querySelector("[data-district]");
        const filter=(select,parent) => {
            [...select.options].forEach(option => { option.hidden=!!option.value && option.dataset.parent!==parent; option.disabled=option.hidden; });
            select.value="";
        };
        province.addEventListener("change", () => { filter(canton,province.value); filter(district,""); });
        canton.addEventListener("change", () => filter(district,canton.value));
        filter(canton,province.value); filter(district,canton.value);
    });
});
