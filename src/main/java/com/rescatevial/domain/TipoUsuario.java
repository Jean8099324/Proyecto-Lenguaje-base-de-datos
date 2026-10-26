package com.rescatevial.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.Data;

@Data
@Entity
@Table(name = "FIDE_TIPOS_USUARIOS_TB")
public class TipoUsuario implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID_TIPO_USUARIO")
    private Long id;

    @Column(name = "NOMBRE_TIPO_USUARIO", length = 100)
    private String nombreTipoUsuario;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ID_ESTADO", nullable = false)
    private Estado estado;
}
