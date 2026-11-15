package com.rescatevial.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;
import lombok.ToString;

@Data
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Positive
    private Long id;
    @NotBlank @Size(max = 100)
    private String nombre;
    @NotBlank @Size(max = 100)
    private String primerApellido;
    @Size(max = 100)
    private String segundoApellido;
    private LocalDate fechaNacimiento;
    @NotBlank @Size(max = 100)
    private String username;
    @Pattern(regexp = "^$|.{8,512}$", message = "La contraseña debe tener al menos 8 caracteres.")
    @ToString.Exclude
    private String password;
    private Instant fechaCreacion;
    private Instant fechaModificacion;
    private Instant fechaEliminacion;
    @NotNull @Positive
    private Long tipoUsuario;
    @NotNull @Positive
    private Long estado;
    private String tipoUsuarioNombre;
    private String estadoNombre;
    private Set<String> rolesNombresSeleccionados = new HashSet<>();

    private Set<Rol> roles = new HashSet<>();

}
