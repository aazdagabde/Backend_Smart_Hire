package com.smarthire.api.repository;

import com.smarthire.api.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Spring Data JPA va automatiquement comprendre cette méthode
    // et générer la requête SQL pour "SELECT * FROM users WHERE email = ?"
    Optional<User> findByEmail(String email);
    // Spring Data JPA comprend aussi celle-ci pour vérifier si un email existe
    Boolean existsByEmail(String email);
}