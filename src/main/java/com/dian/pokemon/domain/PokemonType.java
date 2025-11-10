package com.dian.pokemon.domain;

import lombok.Data;

@Data
public class PokemonType {
    private Integer slot;
    private Type type;

    @Data
    public static class Type {
        private String name;
        private String url;
    }
}