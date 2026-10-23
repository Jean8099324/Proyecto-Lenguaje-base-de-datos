package com.rescatevial.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class Usuario {
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
    @NotBlank @Size(min = 8, max = 100)
    private String password;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
    private LocalDateTime fechaEliminacion;
    @NotNull @Positive
    private Long tipoUsuario;
    @NotNull @Positive
    private Long estado;
    private String tipoUsuarioNombre;
    private String estadoNombre;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(String primerApellido) { this.primerApellido = primerApellido; }
    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(String segundoApellido) { this.segundoApellido = segundoApellido; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public LocalDateTime getFechaModificacion() { return fechaModificacion; }
    public void setFechaModificacion(LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }
    public LocalDateTime getFechaEliminacion() { return fechaEliminacion; }
    public void setFechaEliminacion(LocalDateTime fechaEliminacion) { this.fechaEliminacion = fechaEliminacion; }
    public Long getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(Long tipoUsuario) { this.tipoUsuario = tipoUsuario; }
    public Long getEstado() { return estado; }
    public void setEstado(Long estado) { this.estado = estado; }
    public String getTipoUsuarioNombre() { return tipoUsuarioNombre; }
    public void setTipoUsuarioNombre(String tipoUsuarioNombre) { this.tipoUsuarioNombre = tipoUsuarioNombre; }
    public String getEstadoNombre() { return estadoNombre; }
    public void setEstadoNombre(String estadoNombre) { this.estadoNombre = estadoNombre; }
    public String getNombreCompleto() {
        return String.join(" ", nombre == null ? "" : nombre,
                primerApellido == null ? "" : primerApellido,
                segundoApellido == null ? "" : segundoApellido).trim().replaceAll("\\s+", " ");
    }
}
