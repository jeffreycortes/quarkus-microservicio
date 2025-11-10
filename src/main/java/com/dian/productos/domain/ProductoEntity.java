package com.dian.productos.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.List;

// Entity con Panache
@Entity
@Table(name = "products")
public class ProductoEntity extends PanacheEntity {

    @Column(nullable = false)
    public String name;

    @Column(nullable = false)
    public Double price;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    // Métodos de negocio
    public static List<ProductoEntity> findByName(String name) {
        return list("name", name);
    }

    public static List<ProductoEntity> findByPriceLessThan(Double maxPrice) {
        return list("price < ?1", maxPrice);
    }
}
