package com.rescatevial.domain;

import java.io.Serializable;
import lombok.Data;

@Data
public class Rol implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String nombre;

    private Long estado;
}
