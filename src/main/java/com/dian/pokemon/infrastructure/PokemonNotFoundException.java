package com.dian.pokemon.infrastructure;

public class PokemonNotFoundException extends PokemonApiException {
    public PokemonNotFoundException(String message) { super(message); }
}
