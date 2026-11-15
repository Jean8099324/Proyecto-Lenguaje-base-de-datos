document.addEventListener('DOMContentLoaded', () => {
    const menuButton = document.getElementById('menuButton');
    const closeSidebar = document.getElementById('closeSidebar');
    const overlay = document.getElementById('sidebarOverlay');
    const sidebar = document.getElementById('sidebar');
    const abrir = () => { sidebar?.classList.add('open'); overlay?.classList.add('show'); };
    const cerrar = () => { sidebar?.classList.remove('open'); overlay?.classList.remove('show'); };
    menuButton?.addEventListener('click', abrir);
    closeSidebar?.addEventListener('click', cerrar);
    overlay?.addEventListener('click', cerrar);

    const provincia = document.getElementById('provincia');
    const canton = document.getElementById('canton');
    const distrito = document.getElementById('distrito');
    if (!provincia || !canton || !distrito) return;

    const filtrar = (select, idPadre) => {
        Array.from(select.options).forEach(opcion => {
            if (opcion.value === '') return;
            const corresponde = opcion.dataset.padre === idPadre && idPadre !== '';
            opcion.hidden = !corresponde;
            opcion.disabled = !corresponde;
        });
        const actual = select.options[select.selectedIndex];
        if (actual && actual.disabled) select.value = '';
    };
    const actualizarDistritos = () => filtrar(distrito, canton.value);
    const actualizarCantones = () => {filtrar(canton, provincia.value); actualizarDistritos();};
    provincia.addEventListener('change', actualizarCantones);
    canton.addEventListener('change', actualizarDistritos);
    actualizarCantones();
});
