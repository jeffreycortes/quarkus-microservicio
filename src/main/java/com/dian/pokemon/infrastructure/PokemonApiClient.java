package com.dian;

import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "pokemon-api")
@Path("/pokemon")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RegisterProvider(PokemonApiExceptionMapper.class)
public interface PokemonApiClient {

    @GET
    @Path("/{idOrName}")
    Pokemon getPokemonByIdOrName(
            @PathParam("idOrName") String idOrName,
            @HeaderParam("Authorization") String authorization
    );

    @GET
    PokemonListResponse getAllPokemons(
            @QueryParam("limit") Integer limit,
            @QueryParam("offset") Integer offset,
            @HeaderParam("Authorization") String authorization
    );
}
