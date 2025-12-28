package com.example.starter.repository;

import com.example.starter.entity.Inventory;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class InventoryRepository implements PanacheRepository<Inventory> {
    public List<Inventory> findAllActive() {
        return listAll();
    }

    public boolean existsByName(String name) {
        return find("name", name).count() > 0;
    }
}
