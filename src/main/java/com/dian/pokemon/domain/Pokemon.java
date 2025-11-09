package com.dian;

import lombok.Data;
import java.util.List;

@Data
public class Pokemon {
    private Long id;
    private String name;
    private Integer height;
    private Integer weight;
    private List<PokemonType> types;
    private List<PokemonAbility> abilities;
    private PokemonSprites sprites;

    public Pokemon() {}

    public Pokemon(Long id, String name) {
        this.id = id;
        this.name = name;
    }
}