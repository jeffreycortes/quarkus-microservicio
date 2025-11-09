package com.dian.common;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/env")
public class EnvironmentResource {

    @Inject
    EnvironmentService environmentService;

    @GET
    @Path("/all")
    @Produces(MediaType.TEXT_PLAIN)
    public String getAllConfig() {
        return environmentService.getAllConfig();
    }

    @GET
    @Path("/app")
    @Produces(MediaType.TEXT_PLAIN)
    public String getAppInfo() {
        return environmentService.getAppInfo();
    }

    @GET
    @Path("/database")
    @Produces(MediaType.TEXT_PLAIN)
    public String getDatabaseConfig() {
        return environmentService.getDatabaseConfig();
    }

    @GET
    @Path("/server")
    @Produces(MediaType.TEXT_PLAIN)
    public String getServerConfig() {
        return environmentService.getServerConfig();
    }

    @GET
    @Path("/features")
    @Produces(MediaType.TEXT_PLAIN)
    public String getFeaturesConfig() {
        return environmentService.getFeaturesConfig();
    }
}
