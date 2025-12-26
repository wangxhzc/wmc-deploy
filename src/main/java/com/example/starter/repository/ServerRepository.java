package com.example.starter.repository;

import com.example.starter.entity.Server;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class ServerRepository implements PanacheRepository<Server> {

    public Server findByName(String name) {
        return find("name", name).firstResult();
    }

    public List<Server> findAllActive() {
        return listAll();
    }
}
