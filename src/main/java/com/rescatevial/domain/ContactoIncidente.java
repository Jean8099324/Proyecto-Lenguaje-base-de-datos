package com.rescatevial.domain;

import lombok.Data;

@Data
public class ContactoIncidente {
    private Long id;
    private String nombre;
    private String primerApellido;
    private String segundoApellido;
    private String telefono;
    private String correo;
    private Long idIncidente;
private Long idContacto;
private Long idEstado;
private String nombreEstado;

    public String getNombreCompleto() {
        return java.util.stream.Stream.of(nombre, primerApellido, segundoApellido)
                .filter(value -> value != null && !value.isBlank())
                .collect(java.util.stream.Collectors.joining(" "));
    }
}
