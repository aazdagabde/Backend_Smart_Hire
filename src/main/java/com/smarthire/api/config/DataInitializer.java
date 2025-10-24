package com.smarthire.api.config;

import com.smarthire.api.model.Role;
import com.smarthire.api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j // Pour les logs
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Vérification et initialisation des rôles...");

        // Rôle Candidat
        Optional<Role> candidatRole = roleRepository.findByName("ROLE_CANDIDAT");
        if (candidatRole.isEmpty()) {
            roleRepository.save(Role.builder().name("ROLE_CANDIDAT").build());
            log.info("Rôle ROLE_CANDIDAT créé.");
        }

        // Rôle RH (Recruteur)
        Optional<Role> rhRole = roleRepository.findByName("ROLE_RH");
        if (rhRole.isEmpty()) {
            roleRepository.save(Role.builder().name("ROLE_RH").build());
            log.info("Rôle ROLE_RH créé.");
        }

        log.info("Initialisation des rôles terminée.");
    }
}