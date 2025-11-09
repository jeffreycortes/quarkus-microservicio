package com.dian.certificados.infrastructure;

import com.dian.certificados.application.CertificadoDto;
import com.dian.certificados.domain.CertificadoResourceVersion;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/certificados")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CertificadosResource {
    private Long currentId = 1L;
    private Map<Long, CertificadoDto> certificados = new HashMap<>();

    public CertificadosResource() {
        // Datos de ejemplo
        certificados.put(currentId, new CertificadoDto(currentId++, "Ingresos y Retenciones", 5000.00));
        certificados.put(currentId, new CertificadoDto(currentId++, "RUT", 3500.00));
    }


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "Certificados v1.0 funcionando! 🚀";
    }

    @GET
    @Path("/json")
    public CertificadoResourceVersion pingJson() {
        return new CertificadoResourceVersion("CertificadosResource On", "1.0");
    }

    @GET
    public List<CertificadoDto> getAll() {
        return new ArrayList<>(certificados.values());
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        CertificadoDto certificado = certificados.get(id);
        if (certificado == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(certificado).build();
    }

    @POST
    public Response create(@Valid CertificadoDto certificado) {
        certificado.setId(currentId++);
        certificados.put(certificado.getId(), certificado);
        return Response.status(Response.Status.CREATED).entity(certificado).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, @Valid CertificadoDto certificado) {
        if (!certificados.containsKey(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        certificado.setId(id);
        certificados.put(id, certificado);
        return Response.ok(certificado).build();
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") Long id) {
        if (!certificados.containsKey(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        certificados.remove(id);
        return Response.noContent().build();
    }
}
