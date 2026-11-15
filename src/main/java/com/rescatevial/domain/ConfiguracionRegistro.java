package com.rescatevial.domain;

/** Datos de los catálogos: estados, tipos de usuarios, provincias, cantones y distritos. */
public class ConfiguracionRegistro {
    private Long id;
    private String nombre;
    private Long idEstado;
    private Long idPadre;
    private String nombrePadre;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Long getIdEstado() { return idEstado; }
    public void setIdEstado(Long idEstado) { this.idEstado = idEstado; }
    public Long getIdPadre() { return idPadre; }
    public void setIdPadre(Long idPadre) { this.idPadre = idPadre; }
    public String getNombrePadre() { return nombrePadre; }
    public void setNombrePadre(String nombrePadre) { this.nombrePadre = nombrePadre; }
    public boolean isActivo() { return idEstado == null || idEstado == 1L; }
}
