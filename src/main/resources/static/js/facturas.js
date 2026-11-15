
/*
 * PANTALLA 24: FACTURACION
 *
 * DESCRIPCION:
 * BUSQUEDA DE FACTURAS Y
 * CONFIRMACION DE ELIMINACION.
 *
 * PROYECTO: RESCATE VIAL CR
 */

document.addEventListener("DOMContentLoaded", function () {

    // BUSCAR FACTURAS
    const buscador = document.getElementById("buscarFactura");
    const tabla = document.getElementById("tablaFacturas");

    if (buscador && tabla) {

        buscador.addEventListener("input", function () {

            const texto = buscador.value.toLowerCase().trim();

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
    const formularios =
        document.querySelectorAll(".form-eliminar-factura");

    formularios.forEach(function (formulario) {

        formulario.addEventListener("submit", function (evento) {

            const confirmar = window.confirm(
                "¿Está seguro de que desea eliminar esta factura?"
            );

            if (!confirmar) {
                evento.preventDefault();
            }

        });

    });

});
