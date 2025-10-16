package com.dian;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Path("/certificados")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CertificadosResource {
    private Long currentId = 1L;
    private Map<Long, Certificado> certificados = new HashMap<>();

    public CertificadosResource() {
        // Datos de ejemplo
        certificados.put(currentId, new Certificado(currentId++, "Ingresos y Retenciones", 5000.00));
        certificados.put(currentId, new Certificado(currentId++, "RUT", 3500.00));
    }


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String ping() {
        return "Certificados v1.0 funcionando! 🚀";
    }

    @GET
    @Path("/json")
    public PingOutput pingJson() {
        return new PingOutput("Certificados funcionando", "1.0");
    }

    @GET
    public List<Certificado> getAll() {
        return new ArrayList<>(certificados.values());
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        Certificado certificado = certificados.get(id);
        if (certificado == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        return Response.ok(certificado).build();
    }

    @POST
    public Response create(@Valid Certificado certificado) {
        certificado.setId(currentId++);
        certificados.put(certificado.getId(), certificado);
        return Response.status(Response.Status.CREATED).entity(certificado).build();
    }

    @PUT
    @Path("/{id}")
    public Response update(@PathParam("id") Long id, @Valid Certificado product) {
        if (!certificados.containsKey(id)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        product.setId(id);
        certificados.put(id, product);
        return Response.ok(product).build();
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

    public record PingOutput(String message, String version) {}

    @Data
    public class Certificado {
        private Long id;

        @NotBlank(message = "campo nombre es obligatorio")
        private String nombre;

        @Min(value = 0, message = "campo costo debe ser mayor o igual a cero (0)")
        private Double costo;

        public Certificado() {}

        public Certificado(Long id, String name, Double price) {
            this.id = id;
            this.nombre = nombre;
            this.costo = costo;
        }
    }
}
