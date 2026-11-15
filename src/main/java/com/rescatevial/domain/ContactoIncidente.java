package com.rescatevial.domain;

import lombok.Data;

/** Relación entre contactos e incidentes desde FIDE_INCIDENTES_CONTACTOS_TB. */
@Data
public class ContactoIncidente {
    private Long idIncidente;
    private Long idContacto;
    private Long idEstado;
    private String nombreEstado;
}
