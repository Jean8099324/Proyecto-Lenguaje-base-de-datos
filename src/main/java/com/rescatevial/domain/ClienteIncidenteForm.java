package com.rescatevial.domain;
import jakarta.validation.constraints.*;
import lombok.Data;
@Data
public class ClienteIncidenteForm {
 @NotBlank @Size(max=100) private String nombre;
 @NotBlank @Size(max=100) private String primerApellido;
 @Size(max=100) private String segundoApellido;
 @NotNull private Long idProvincia;
 @NotNull private Long idCanton;
 @NotNull private Long idDistrito;
 @Size(max=200) private String otrasSenas;
 @Pattern(regexp="^$|\\d{8}") private String telefono;
 @Email @Size(max=150) private String correo;
}
