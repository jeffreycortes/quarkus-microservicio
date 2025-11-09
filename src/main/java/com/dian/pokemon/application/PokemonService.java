package com.dian;

import com.dian.pokemon.domain.Pokemon;
import com.dian.pokemon.infrastructure.PokemonApiClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.jbosslog.JBossLog;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
@JBossLog
public class PokemonService {

    //@Inject
    private final PokemonApiClient pokemonApiClient;

    PokemonService(@RestClient PokemonApiClient pokemonApiClient) {
        this.pokemonApiClient = pokemonApiClient;
    }

    @Inject
    PokemonCacheService cacheService;

    public Pokemon getPokemonByIdOrName(String idOrName, String token) {
        try {
            log.infof("Buscando pokémon: %s", idOrName);

            Pokemon cachedPokemon = cacheService.getPokemon(idOrName);
            if (cachedPokemon != null) {
                log.debugf("Pokémon %s encontrado en cache", idOrName);
                return cachedPokemon;
            }

            String authHeader = token != null ? "Bearer " + token : null;
            Pokemon pokemon = pokemonApiClient.getPokemonByIdOrName(idOrName, authHeader);

            cacheService.cachePokemon(idOrName, pokemon);
            return pokemon;

        } catch (PokemonNotFoundException e) {
            log.warnf("Pokémon no encontrado: %s", idOrName);
            throw e;
        } catch (Exception e) {
            log.errorf("Error al obtener pokémon %s: %s", idOrName, e.getMessage());
            throw new PokemonApiException("Error al obtener el pokémon: " + e.getMessage(), e);
        }
    }

    public List<Pokemon> getMultiplePokemons(List<String> idsOrNames, String token) {
        log.infof("Buscando %d pokémons", idsOrNames.size());

        return idsOrNames.parallelStream()
                .map(idOrName -> getPokemonByIdOrName(idOrName, token))
                .collect(Collectors.toList());
    }

    public PokemonListResponse getPokemonList(Integer limit, Integer offset, String token) {
        try {
            log.infof("Obteniendo lista de pokémons - Límite: %d, Offset: %d", limit, offset);

            String authHeader = token != null ? "Bearer " + token : null;
            PokemonListResponse response = pokemonApiClient.getAllPokemons(limit, offset, authHeader);

            log.infof("Encontrados %d pokémons en total", response.getCount());
            return response;

        } catch (Exception e) {
            log.errorf("Error al obtener lista de pokémons: %s", e.getMessage());
            throw new PokemonApiException("Error al obtener la lista de pokémons: " + e.getMessage(), e);
        }
    }
}