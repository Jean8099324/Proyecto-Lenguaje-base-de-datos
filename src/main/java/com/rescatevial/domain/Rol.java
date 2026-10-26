package com.rescatevial.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.Data;

@Data
@Entity
@Table(name = "FIDE_ROLES_TB")
public class Rol implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID_ROL")
    private Long id;

    @Column(name = "NOMBRE_ROL")
    private String nombre;

    @Column(name = "ID_ESTADO", nullable = false)
    private Long estado;

    @jakarta.persistence.ManyToMany(mappedBy = "roles")
    private java.util.Set<Usuario> usuarios = new java.util.HashSet<>();
}
