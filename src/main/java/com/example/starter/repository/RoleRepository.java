package com.example.starter.repository;

import com.example.starter.entity.Role;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class RoleRepository implements PanacheRepository<Role> {
    public List<Role> findAllActive() {
        return listAll();
    }

    public boolean existsByName(String name) {
        return find("name", name).count() > 0;
    }
}
