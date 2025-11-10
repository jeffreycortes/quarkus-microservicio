package com.dian.pokemon.infrastructure;

import com.dian.common.ErrorResponse;
import com.dian.pokemon.application.PokemonListResponse;
import com.dian.pokemon.application.PokemonService;
import com.dian.pokemon.domain.Pokemon;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.extern.jbosslog.JBossLog;
import java.util.Arrays;
import java.util.List;

@Path("/api/pokemon")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@JBossLog
//@AllArgsConstructor
public class PokemonResource {

    //@Inject
    private final PokemonService pokemonService;

    PokemonResource(PokemonService pokemonService) {
        this.pokemonService = pokemonService;
    }

    @GET
    @Path("/{idOrName}")
    public Response getPokemon(
            @PathParam("idOrName") String idOrName,
            @HeaderParam("Authorization") @DefaultValue("") String token
    ) {
        try {
            String cleanToken = extractToken(token);
            Pokemon pokemon = pokemonService.getPokemonByIdOrName(idOrName, cleanToken);
            return Response.ok(pokemon).build();

        } catch (PokemonNotFoundException e) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("POKEMON_NOT_FOUND", e.getMessage()))
                    .build();
        } catch (PokemonApiAuthException e) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new ErrorResponse("UNAUTHORIZED", e.getMessage()))
                    .build();
        } catch (PokemonApiException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new ErrorResponse("API_ERROR", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/batch")
    public Response getMultiplePokemons(
            @QueryParam("ids") String ids,
            @HeaderParam("Authorization") @DefaultValue("") String token
    ) {
        try {
            if (ids == null || ids.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                        .entity(new ErrorResponse("BAD_REQUEST", "El parámetro 'ids' es requerido"))
                        .build();
            }

            List<String> idList = Arrays.asList(ids.split(","));
            String cleanToken = extractToken(token);
            List<Pokemon> pokemons = pokemonService.getMultiplePokemons(idList, cleanToken);

            return Response.ok(pokemons).build();

        } catch (Exception e) {
            log.errorf("Error en consulta batch: %s", e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                    .entity(new ErrorResponse("BATCH_ERROR", "Error al obtener múltiples pokémons"))
                    .build();
        }
    }

    @GET
    public Response getPokemonList(
            @QueryParam("limit") @DefaultValue("20") Integer limit,
            @QueryParam("offset") @DefaultValue("0") Integer offset,
            @HeaderParam("Authorization") @DefaultValue("") String token
    ) {
        try {
            if (limit > 100) limit = 100;

            String cleanToken = extractToken(token);
            PokemonListResponse response = pokemonService.getPokemonList(limit, offset, cleanToken);

            return Response.ok(response).build();

        } catch (PokemonApiException e) {
            return Response.status(Response.Status.BAD_GATEWAY)
                    .entity(new ErrorResponse("API_ERROR", e.getMessage()))
                    .build();
        }
    }

    private String extractToken(String authorizationHeader) {
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        return authorizationHeader.isEmpty() ? null : authorizationHeader;
    }
}