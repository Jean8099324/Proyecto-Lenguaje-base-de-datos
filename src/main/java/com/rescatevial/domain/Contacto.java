package com.rescatevial.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** Datos de FIDE_CONTACTOS_TB; no se confunden con los datos de contacto de usuarios. */
@Data
public class Contacto {
    private Long id;

    @NotBlank(message = "El nombre es obligatorio.")
    @Size(max = 100)
    private String nombreContacto;

    @NotBlank(message = "El primer apellido es obligatorio.")
    @Size(max = 100)
    private String primerApellido;

    @Size(max = 100)
    private String segundoApellido;

    @Size(max = 20)
    private String telefonoContacto;

    @Email(message = "El correo no tiene un formato válido.")
    @Size(max = 150)
    private String correoContacto;

    private Long idEstado;
    private String nombreEstado;
}
