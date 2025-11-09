package com.dian.common;

import com.dian.ResponseApi;
import com.dian.ResponseApiError;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.ArrayList;

//@Provider
public class GlobalExceptionMapper implements ExceptionMapper<RuntimeException> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(RuntimeException exception) {
        return handleGenericException(exception);
    }

    private Response handleGenericException(RuntimeException ex) {
        // Log del error completo para debugging
        ex.printStackTrace();

        var responseApi = new ResponseApi();
        responseApi.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
        responseApi.setTimestamp(Instant.now().toString());
        responseApi.setPath(uriInfo.getPath());
        responseApi.setSucces(false);
        responseApi.setCodigo("ERR-NOCONTROLADO");
        responseApi.setMensaje("Error de servidor");

        var responseApiError = ResponseApiError.builder()
                .mensaje("Error interno del servidor")
                .codigo("INTERNAL_SERVER_ERROR")
                .build();

        responseApi.setPath(uriInfo.getPath());
        responseApi.setError(responseApiError);

        // En producción, no exponer detalles internos
        if ("dev".equals(System.getProperty("quarkus.profile"))) {
            var errores = new ArrayList<String>();
            errores.add(ex.getMessage());
            responseApiError.setDetalles(errores);
        }

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(responseApi)
                .build();
    }
}