package com.dian.pokemon.domain;

import lombok.Data;

@Data
public class PokemonAbility {
    private Boolean is_hidden;
    private Integer slot;
    private Ability ability;

    @Data
    public static class Ability {
        private String name;
        private String url;
    }
}