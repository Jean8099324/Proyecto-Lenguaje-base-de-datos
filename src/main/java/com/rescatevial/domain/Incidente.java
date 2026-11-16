package com.rescatevial.domain;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import lombok.Data;

@Data
public class Incidente implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long idUsuario;
    @NotNull private Long idVehiculo;
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate fechaReporte;
    private String horaReporte;
    @Size(max = 500) private String detalles;
    @NotNull private Long idProvincia;
    @NotNull private Long idCanton;
    @NotNull private Long idDistrito;
    @Size(max = 200) private String otrasSenas;
    @Size(max = 100) private String nombreReportante;
    @Size(max = 100) private String primerApellidoReportante;
    @Size(max = 100) private String segundoApellidoReportante;
    @jakarta.validation.constraints.Pattern(
            regexp = "^$|\\d{8}",
            message = "El teléfono debe contener exactamente 8 dígitos numéricos.")
    private String telefonoReportante;
    @Size(max = 150) private String correoReportante;
    @NotNull private Long idTipoIncidente;
    @NotNull private Long idPrioridad;
    @NotNull private Long idEstado;
    private LocalDate fechaCierre;
    private String horaCierre;
    @Size(max = 10) private String placaVehiculo;
    private String vehiculoEtiqueta;
    private String ubicacionEtiqueta;
    private String tipoEtiqueta;
    private String prioridadEtiqueta;
    private String estadoEtiqueta;

    // Datos editables del propietario del vehiculo y del contacto del incidente.
    private Long idCliente;
    private String usernameCliente;
    private String nombreCliente;
    private String primerApellidoCliente;
    private String segundoApellidoCliente;
    private Long idProvinciaCliente;
    private Long idCantonCliente;
    private Long idDistritoCliente;
    private Long idProvinciaClienteOriginal;
    private Long idCantonClienteOriginal;
    private Long idDistritoClienteOriginal;
    private String otrasSenasCliente;
    private Long idContactoIncidente;
    private String nombreContactoIncidente;
    private String primerApellidoContactoIncidente;
    private String segundoApellidoContactoIncidente;
    private String telefonoContactoIncidente;
}
