package com.rescatevial.domain;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class Cliente {
    private Long id;
    @NotBlank @Size(max = 100)
    private String nombre;
    @NotBlank @Size(max = 100)
    private String primerApellido;
    @Size(max = 100)
    private String segundoApellido;
    @NotBlank @Size(max = 100)
    private String username;
    @NotBlank @Size(min = 8, max = 100)
    private String password;
    @NotBlank @Size(max = 20)
    private String telefono;
    @NotBlank @Email @Size(max = 150)
    private String correo;
    private int vehiculos;
    private String estado;

    public Cliente() { }

    public Cliente(Long id, String nombre, String primerApellido, String segundoApellido,
                   String username, String telefono, String correo, int vehiculos, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.primerApellido = primerApellido;
        this.segundoApellido = segundoApellido;
        this.username = username;
        this.telefono = telefono;
        this.correo = correo;
        this.vehiculos = vehiculos;
        this.estado = estado;
    }

    public Long id() { return id; }
    public String nombre() { return nombre; }
    public String primerApellido() { return primerApellido; }
    public String segundoApellido() { return segundoApellido; }
    public String username() { return username; }
    public String telefono() { return telefono; }
    public String correo() { return correo; }
    public int vehiculos() { return vehiculos; }
    public String estado() { return estado; }
    public String nombreCompleto() {
        return String.join(" ", nombre, primerApellido,
                segundoApellido == null ? "" : segundoApellido).trim().replaceAll("\\s+", " ");
    }

    public String iniciales() {
        return (nombre.isBlank() ? "" : nombre.substring(0, 1))
                + (primerApellido.isBlank() ? "" : primerApellido.substring(0, 1));
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getPrimerApellido() { return primerApellido; }
    public void setPrimerApellido(String primerApellido) { this.primerApellido = primerApellido; }
    public String getSegundoApellido() { return segundoApellido; }
    public void setSegundoApellido(String segundoApellido) { this.segundoApellido = segundoApellido; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public int getVehiculos() { return vehiculos; }
    public void setVehiculos(int vehiculos) { this.vehiculos = vehiculos; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
