package com.dian;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

@Provider
public class PokemonApiExceptionMapper implements ExceptionMapper<WebApplicationException> {

    private static final Logger LOG = Logger.getLogger(PokemonApiExceptionMapper.class);

    @Override
    public Response toResponse(WebApplicationException exception) {
        Response originalResponse = exception.getResponse();

        LOG.errorf("Error calling Pokemon API: Status %d - %s",
                originalResponse.getStatus(), exception.getMessage());

        if (originalResponse.getStatus() == 404) {
            throw new PokemonNotFoundException("Pokémon no encontrado");
        } else if (originalResponse.getStatus() == 401) {
            throw new PokemonApiAuthException("No autorizado para acceder a la API");
        } else if (originalResponse.getStatus() >= 500) {
            throw new PokemonApiServerException("Error del servidor de Pokemon API");
        } else {
            throw new PokemonApiException("Error al consumir Pokemon API: " + exception.getMessage());
        }
    }
}