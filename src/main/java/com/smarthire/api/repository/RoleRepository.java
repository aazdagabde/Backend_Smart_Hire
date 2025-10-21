package com.smarthire.api.repository;

import com.smarthire.api.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // Une méthode pour trouver un rôle par son nom (ex: "ROLE_RH")
    Optional<Role> findByName(String name);
}