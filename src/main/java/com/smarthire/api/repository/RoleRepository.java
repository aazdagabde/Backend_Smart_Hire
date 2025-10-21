package com.smarthire.api.repository;

import com.smarthire.api.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // Ajoutez cette ligne :
    // Spring Data JPA va générer la requête SQL "SELECT * FROM roles WHERE name = ?"
    Optional<Role> findByName(String name);

}