package com.rescatevial.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.HashSet;
import lombok.Data;
import lombok.ToString;

@Data
@Entity
@Table(name = "FIDE_USUARIOS_TB")
public class Usuario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID_USUARIO")
    @Positive
    private Long id;
    @NotBlank @Size(max = 100) @Column(name = "NOMBRE", nullable = false, length = 100)
    private String nombre;
    @NotBlank @Size(max = 100) @Column(name = "PRIMER_APELLIDO", nullable = false, length = 100)
    private String primerApellido;
    @Size(max = 100) @Column(name = "SEGUNDO_APELLIDO", length = 100)
    private String segundoApellido;
    @Column(name = "FECHA_NACIMIENTO")
    private LocalDate fechaNacimiento;
    @NotBlank @Size(max = 100) @Column(name = "USERNAME", nullable = false, length = 100)
    private String username;
    @Pattern(regexp = "^$|.{8,512}$", message = "La contraseña debe tener al menos 8 caracteres.")
    @Column(name = "PASSWORD", nullable = false, length = 512)
    @ToString.Exclude
    private String password;
    @Column(name = "FECHA_CREACION", insertable = false, updatable = false)
    private LocalDateTime fechaCreacion;
    @Column(name = "FECHA_MODIFICACION", insertable = false, updatable = false)
    private LocalDateTime fechaModificacion;
    @Column(name = "FECHA_ELIMINACION", insertable = false, updatable = false)
    private LocalDateTime fechaEliminacion;
    @NotNull @Positive @Column(name = "ID_TIPO_USUARIO", nullable = false)
    private Long tipoUsuario;
    @NotNull @Positive @Column(name = "ID_ESTADO", nullable = false)
    private Long estado;
    @Transient
    private String tipoUsuarioNombre;
    @Transient
    private String estadoNombre;
    @Transient
    private Set<String> rolesNombresSeleccionados = new HashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "FIDE_USUARIOS_ROLES_TB",
            joinColumns = @JoinColumn(name = "ID_USUARIO"),
            inverseJoinColumns = @JoinColumn(name = "ID_ROL")
    )
    private Set<Rol> roles = new HashSet<>();

}
