package com.dian;

public class PokemonApiException extends RuntimeException {
    public PokemonApiException(String message) { super(message); }
    public PokemonApiException(String message, Throwable cause) { super(message, cause); }
}
