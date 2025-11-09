package com.dian.common;

import lombok.Data;

@Data
public class Filtros {
    private String filtro;
    private Integer pagina;
    private Integer tamaño;

    public Filtros() {}

    public Filtros(String filtro, Integer pagina, Integer tamaño) {
        this.filtro = filtro;
        this.pagina = pagina;
        this.tamaño = tamaño;
    }
}