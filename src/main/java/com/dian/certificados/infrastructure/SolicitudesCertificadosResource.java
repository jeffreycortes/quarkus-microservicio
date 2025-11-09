package com.dian.certificados.infrastructure;

import com.dian.common.Filtros;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;


@Path("/solicitudes")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SolicitudesCertificadosResource {

    @GET
    @Path("/certificados/{id}")
    public Response obtenerUsuario(
            // Parámetro de path (URI) - REQUERIDO
            @PathParam("id") Long id,

            // Parámetros de query string - OPCIONALES
            @QueryParam("incluirDetalles") Boolean incluirDetalles,
            @QueryParam("formato") String formato,

            // Bean con múltiples parámetros de query
            @BeanParam Filtros queryParams,

            // Headers
            @HeaderParam("Authorization") String authorization,
            @HeaderParam("Origin") String origin,

            // Context para acceder a todos los headers
            @Context HttpHeaders headers) {

        // Validar headers requeridos
        if (authorization == null || authorization.trim().isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Header Authorization requerido\"}")
                    .build();
        }

        // Procesar autenticación
        if (!validarToken(authorization)) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Token inválido\"}")
                    .build();
        }

        // Procesar parámetros
        String respuesta = String.format(
                "Solicitud ID: %d, Incluir detalles: %s, Formato: %s, " +
                        "Filtro: %s, Página: %d, Tamaño: %d, " +
                        "Origin: %s, Token: %s",
                id,
                incluirDetalles != null ? incluirDetalles : false,
                formato != null ? formato : "json",
                queryParams.getFiltro(),
                queryParams.getPagina() != null ? queryParams.getPagina() : 1,
                queryParams.getTamaño() != null ? queryParams.getTamaño() : 10,
                origin,
                authorization.substring(0, Math.min(10, authorization.length())) + "..."
        );

        return Response.ok("{\"mensaje\": \"" + respuesta + "\"}").build();
    }

    @POST
    @Path("/certificados/{categoria}/{subcategoria}")
    public Response crearProducto(
            // Múltiples parámetros de path
            @PathParam("categoria") String categoria,
            @PathParam("subcategoria") String subcategoria,

            // Parámetros de query string - REQUERIDOS y OPCIONALES
            @QueryParam("nombre") String nombre,
            @QueryParam("precio") @DefaultValue("0.0") Double precio,
            @QueryParam("cantidad") @DefaultValue("0") Integer cantidad,

            // Headers
            @HeaderParam("Authorization") String authorization,
            @HeaderParam("Origin") String origin) {

        // Validar parámetros requeridos
        if (nombre == null || nombre.trim().isEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"El parámetro 'nombre' es requerido\"}")
                    .build();
        }

        // Validar headers
        if (authorization == null) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Header Authorization requerido\"}")
                    .build();
        }

        String respuesta = String.format(
                "Solicitud creada - Categoría: %s, Subcategoría: %s, " +
                        "Nombre: %s, Precio: %.2f, Cantidad: %d, Origin: %s",
                categoria, subcategoria, nombre, precio, cantidad, origin
        );

        return Response.status(Response.Status.CREATED)
                .entity("{\"resultado\": \"" + respuesta + "\"}")
                .build();
    }

    @GET
    @Path("/reportes")
    public Response generarReporte(
            // Parámetros de query con validación
            @QueryParam("tipo") @DefaultValue("resumen") String tipo,
            @QueryParam("fechaInicio") String fechaInicio,
            @QueryParam("fechaFin") String fechaFin,
            @QueryParam("limite") @DefaultValue("100") Integer limite,

            // Todos los headers
            @Context HttpHeaders headers) {

        // Acceder a headers específicos
        String authHeader = headers.getHeaderString("Authorization");
        String originHeader = headers.getHeaderString("Origin");
        String userAgent = headers.getHeaderString("User-Agent");

        // Validar fechas si se proporcionan
        if ((fechaInicio != null && fechaFin == null) ||
                (fechaInicio == null && fechaFin != null)) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity("{\"error\": \"Ambas fechas (inicio y fin) deben proporcionarse\"}")
                    .build();
        }

        String respuesta = String.format(
                "Reporte generado - Tipo: %s, Fecha Inicio: %s, " +
                        "Fecha Fin: %s, Límite: %d, Origin: %s, User-Agent: %s",
                tipo,
                fechaInicio != null ? fechaInicio : "No especificada",
                fechaFin != null ? fechaFin : "No especificada",
                limite,
                originHeader,
                userAgent
        );

        return Response.ok("{\"reporte\": \"" + respuesta + "\"}").build();
    }

    private boolean validarToken(String authorization) {
        // Lógica de validación del token
        return authorization != null &&
                authorization.startsWith("Bearer ") &&
                authorization.length() > 10;
    }
}
