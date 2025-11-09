package com.dian.common;

import com.dian.ResponseApi;
import com.dian.ResponseApiError;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Provider
public class ValidationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(ConstraintViolationException exception) {
        var responseApi = new ResponseApi();
        responseApi.setStatus(Response.Status.BAD_REQUEST.getStatusCode());
        responseApi.setTimestamp(Instant.now().toString());
        responseApi.setPath(uriInfo.getPath());
        responseApi.setSucces(false);
        responseApi.setCodigo("Petición invalida");
        responseApi.setMensaje("Error de validación en los datos de entrada");

        var responseApiError = ResponseApiError.builder()
                .mensaje(exception.getMessage())
                .codigo("CERT-ERR-V1")
                .build();
        responseApi.setError(responseApiError);

        List<String> errors = exception.getConstraintViolations()
                .stream()
                .map(violation -> {
                    String propertyPath = violation.getPropertyPath().toString();
                    // Simplificar el path si es muy largo
                    if (propertyPath.contains(".")) {
                        propertyPath = propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
                    }
                    return String.format("%s: %s", propertyPath, violation.getMessage());
                })
                .collect(Collectors.toList());

        responseApiError.setDetalles(errors);

        return Response.status(Response.Status.BAD_REQUEST)
                .entity(responseApi)
                .build();
    }
}
