package com.rescatevial.domain;

import java.io.Serializable;
import lombok.Data;

@Data
public class TipoUsuario implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String nombreTipoUsuario;

    private Estado estado;
}
