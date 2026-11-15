package com.rescatevial.domain;

import lombok.Data;

/** Información de teléfono, correo o dirección vinculada a un usuario. */
@Data
public class DatoContacto {
    private Long idUsuario;
    private String valor;          // Teléfono o correo actual
    private String valorAnterior;  // Clave original para editar
    private Long idProvincia;
    private Long idCanton;
    private Long idDistrito;
    private String otrasSenas;
    private Long idEstado;
    private String nombreEstado;
    private String nombreProvincia;
    private String nombreCanton;
    private String nombreDistrito;
}
