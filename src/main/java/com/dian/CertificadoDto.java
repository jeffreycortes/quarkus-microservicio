package com.dian;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CertificadoDto {
    private Long id;

    @NotBlank(message = "campo nombre es obligatorio")
    private String nombre;

    @Min(value = 0, message = "campo costo debe ser mayor o igual a cero (0)")
    private Double costo;

    public CertificadoDto() {}

    public CertificadoDto(Long id, String nombre, Double costo) {
        this.id = id;
        this.nombre = nombre;
        this.costo = costo;
    }
}
