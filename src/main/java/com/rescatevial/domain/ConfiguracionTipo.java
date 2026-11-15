package com.rescatevial.domain;

import java.util.List;

/** Catálogos habilitados: las claves provienen del código, no de parámetros SQL del navegador. */
public enum ConfiguracionTipo {
    PROVINCIAS("provincias", "Provincia", "Provincias", null),
    CANTONES("cantones", "Cantón", "Cantones", "provincias"),
    DISTRITOS("distritos", "Distrito", "Distritos", "cantones"),
    TIPOS_USUARIOS("tipos-usuarios", "Tipo de usuario", "Tipos de usuarios", null),
    ESTADOS("estados", "Estado", "Estados", null);

    private final String clave;
    private final String singular;
    private final String plural;
    private final String padre;

    ConfiguracionTipo(String clave, String singular, String plural, String padre) {
        this.clave=clave; this.singular=singular; this.plural=plural; this.padre=padre;
    }
    public String getClave() { return clave; }
    public String getSingular() { return singular; }
    public String getPlural() { return plural; }
    public String getPadre() { return padre; }
    public boolean isTienePadre() { return padre != null; }
    public boolean isIdManual() { return this == PROVINCIAS || this == CANTONES || this == DISTRITOS; }
    public boolean isEstadoSistema() { return this == ESTADOS; }
    public static List<ConfiguracionTipo> todos() { return List.of(values()); }
    public static ConfiguracionTipo desdeRuta(String clave) {
        for (ConfiguracionTipo tipo : values()) if (tipo.clave.equals(clave)) return tipo;
        throw new IllegalArgumentException("Catálogo desconocido.");
    }
}
