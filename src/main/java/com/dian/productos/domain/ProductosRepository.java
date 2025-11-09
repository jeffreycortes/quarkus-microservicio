package com.dian;

import java.util.List;
import java.util.Optional;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ProductosRepository implements PanacheRepository<ProductoEntity> {

    public List<ProductoEntity> findActiveProducts() {
        return list("active", true);
    }

    public Optional<ProductoEntity> findByName(String name) {
        return find("name", name).firstResultOptional();
    }
}
