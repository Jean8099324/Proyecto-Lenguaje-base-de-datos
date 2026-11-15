package com.rescatevial.domain;

import lombok.Data;

@Data
public class OpcionIncidente {
    private Long id;
    private String etiqueta;
    private Long relacionId;
    private Long estadoId;
}
