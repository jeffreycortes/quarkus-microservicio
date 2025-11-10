package com.dian.productos.infrastructure;

import com.dian.productos.domain.ProductoEntity;
import com.dian.productos.domain.ProductosRepository;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDateTime;
import java.util.List;

@Path("/products")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ProductosResource {

    @Inject
    ProductosRepository productosRepository;

    @GET
    public List<ProductoEntity> getAll() {
        return productosRepository.listAll();
    }

    @GET
    @Path("/{id}")
    public Response getById(@PathParam("id") Long id) {
        return productosRepository.findByIdOptional(id)
                .map(product -> Response.ok(product).build())
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @POST
    @Transactional
    public Response create(ProductoEntity product) {
        product.createdAt = LocalDateTime.now();
        productosRepository.persist(product);
        return Response.status(Response.Status.CREATED).entity(product).build();
    }

    @PUT
    @Path("/{id}")
    @Transactional
    public Response update(@PathParam("id") Long id, ProductoEntity productData) {
        return productosRepository.findByIdOptional(id)
                .map(product -> {
                    product.name = productData.name;
                    product.price = productData.price;
                    return Response.ok(product).build();
                })
                .orElse(Response.status(Response.Status.NOT_FOUND).build());
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    public Response delete(@PathParam("id") Long id) {
        boolean deleted = productosRepository.deleteById(id);
        if (deleted) {
            return Response.noContent().build();
        }
        return Response.status(Response.Status.NOT_FOUND).build();
    }
}
