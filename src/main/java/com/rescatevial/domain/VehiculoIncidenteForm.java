package com.rescatevial.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VehiculoIncidenteForm {
    public static String normalizarPlaca(String placa) {
        return placa == null ? null : placa.replaceAll("[^a-zA-Z0-9]", "")
                .toUpperCase(java.util.Locale.ROOT);
    }

    public void setPlaca(String placa) {
        this.placa = normalizarPlaca(placa);
    }

    @NotBlank @Size(max = 10) private String placa;
    @Size(max = 17) private String codigoChasis;
    @NotNull private Long idMarca;
    @NotNull private Long idModelo;
    @NotNull private Long idAnio;
    @NotNull private Long idEstado;
    private Long idUsuario;
}
