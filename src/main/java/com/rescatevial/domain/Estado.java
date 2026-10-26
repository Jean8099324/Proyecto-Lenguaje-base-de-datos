package com.rescatevial.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.Data;

@Data
@Entity
@Table(name = "FIDE_ESTADOS_TB")
public class Estado implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "ID_ESTADO")
    private Long id;

    @Column(name = "NOMBRE_ESTADO", length = 100)
    private String nombreEstado;
}
