package com.dian;

import com.dian.pokemon.domain.Pokemon;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.Data;
import lombok.extern.jbosslog.JBossLog;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@ApplicationScoped
@JBossLog
public class PokemonCacheService {

    private static final long CACHE_DURATION_MINUTES = 30;
    private final ConcurrentHashMap<String, CacheEntry<Pokemon>> pokemonCache = new ConcurrentHashMap<>();

    public Pokemon getPokemon(String key) {
        CacheEntry<Pokemon> entry = pokemonCache.get(key);
        if (entry != null && !entry.isExpired()) {
            return entry.getValue();
        }

        if (entry != null) {
            pokemonCache.remove(key);
        }

        return null;
    }

    public void cachePokemon(String key, Pokemon pokemon) {
        CacheEntry<Pokemon> entry = new CacheEntry<>(pokemon, CACHE_DURATION_MINUTES, TimeUnit.MINUTES);
        pokemonCache.put(key, entry);
        log.debugf("Pokémon %s almacenado en cache", key);
    }

    public void clearCache() {
        pokemonCache.clear();
        log.info("Cache limpiado");
    }

    @Data
    private static class CacheEntry<T> {
        private final T value;
        private final long expiryTime;

        public CacheEntry(T value, long duration, TimeUnit unit) {
            this.value = value;
            this.expiryTime = System.currentTimeMillis() + unit.toMillis(duration);
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}