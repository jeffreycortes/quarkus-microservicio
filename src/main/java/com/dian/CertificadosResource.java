package com.dian;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/certificados")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CertificadosResource {
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

    public record PingOutput(String message, String version) {}
}
