
/*
 * PANTALLA 22: REPUESTOS
 * DESCRIPCION: BUSQUEDA Y CONFIRMACION DE ELIMINACION.
 */

document.addEventListener("DOMContentLoaded", function () {

    // BUSCAR REPUESTOS EN LA TABLA
    const buscar = document.getElementById("buscarRepuesto");
    const tabla = document.getElementById("tablaRepuestos");

    if (buscar && tabla) {

        buscar.addEventListener("input", function () {

            const texto = buscar.value.toLowerCase().trim();

            const filas = tabla.querySelectorAll("tbody tr");

            filas.forEach(function (fila) {

                const contenido = fila.textContent.toLowerCase();

                fila.style.display = contenido.includes(texto)
                    ? ""
                    : "none";
            });
        });
    }

    // CONFIRMAR ELIMINACION
    const formularios = document.querySelectorAll(".form-eliminar");

    formularios.forEach(function (formulario) {

        formulario.addEventListener("submit", function (evento) {

            const confirmar = window.confirm(
                "¿Está seguro de que desea eliminar este repuesto?"
            );

            if (!confirmar) {
                evento.preventDefault();
            }
        });
    });

});
