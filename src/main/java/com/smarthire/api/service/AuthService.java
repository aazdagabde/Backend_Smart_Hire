package com.smarthire.api.service;

import com.smarthire.api.dto.LoginRequest;
import com.smarthire.api.dto.LoginResponse;
import com.smarthire.api.dto.RegisterRequest;
import com.smarthire.api.model.Role;
import com.smarthire.api.model.User;
import com.smarthire.api.repository.RoleRepository;
import com.smarthire.api.repository.UserRepository;
import com.smarthire.api.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    @Transactional
    public void register(RegisterRequest request) {
        validateRegisterRequest(request);

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Cet email est déjà utilisé");
        }

        // Assigner le rôle "ROLE_CANDIDAT" par défaut (basé sur DataInitializer)
        Role userRole = roleRepository.findByName("ROLE_CANDIDAT")
                .orElseThrow(() -> new RuntimeException("Erreur: Rôle 'ROLE_CANDIDAT' non trouvé."));
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .phoneNumber(request.phoneNumber()) // Gère le numéro de téléphone optionnel
                .roles(roles)
                .build();

        userRepository.save(user);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        validateLoginRequest(request);

        try {
            // 1. Authentifier l'utilisateur via Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );

            // 2. Placer l'authentification dans le contexte de sécurité
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 3. Récupérer l'utilisateur depuis la BDD (nécessaire pour la réponse)
            User user = userRepository.findByEmail(request.email())
                    .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable"));

            // 4. Générer le token
            String token = jwtProvider.generateToken(authentication);

            // 5. Récupérer les noms des rôles pour la réponse
            List<String> roles = user.getRoles().stream()
                    .map(Role::getName)
                    .collect(Collectors.toList());

            // 6. Construire la réponse
            // --- CORRECTION APPLIQUÉE ICI ---
            // Remplacement du .builder() par le constructeur du record
            return new LoginResponse(
                    token,              // Le 'jwt'
                    user.getId(),       // Le 'id' manquant
                    user.getEmail(),
                    user.getFirstName(),// Le 'firstName' manquant
                    user.getLastName(), // Le 'lastName' manquant
                    roles
            );
            // --- FIN DE LA CORRECTION ---

        } catch (Exception e) {
            // Si l'authentification échoue, une exception est levée
            throw new IllegalArgumentException("Email ou mot de passe incorrect");
        }
    }

    // --- Méthodes de validation (inchangées) ---

    private void validateRegisterRequest(RegisterRequest request) {
        if (request.firstName() == null || request.firstName().trim().isEmpty()) {
            throw new IllegalArgumentException("Le prénom est obligatoire");
        }
        if (request.lastName() == null || request.lastName().trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom est obligatoire");
        }
        if (request.email() == null || request.email().trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }
        if (request.password() == null || request.password().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        }
        if (request.password().length() < 6) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caractères");
        }
        // Pas de validation pour le phoneNumber, il est optionnel
    }

    private void validateLoginRequest(LoginRequest request) {
        if (request.email() == null || request.email().trim().isEmpty()) {
            throw new IllegalArgumentException("L'email est obligatoire");
        }
        if (request.password() == null || request.password().isEmpty()) {
            throw new IllegalArgumentException("Le mot de passe est obligatoire");
        }
    }
}