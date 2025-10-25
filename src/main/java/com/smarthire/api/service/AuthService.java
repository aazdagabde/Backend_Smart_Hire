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
        // Validation des données d'entrée
        validateRegisterRequest(request);

        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Erreur: Email déjà utilisé!");
        }

        // Créer le nouvel utilisateur
        User user = User.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(request.email().toLowerCase().trim())
                .password(passwordEncoder.encode(request.password()))
                .phoneNumber(request.phoneNumber()) // NOUVELLE MODIFICATION
                .build();

        // Assigner un rôle par défaut
        Role userRole = roleRepository.findByName("ROLE_CANDIDAT")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CANDIDAT").build()));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        // Sauvegarder l'utilisateur
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        // Validation des données d'entrée
        validateLoginRequest(request);

        try {
            // Authentifier l'utilisateur via Spring Security
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.email().toLowerCase().trim(),
                            request.password()
                    )
            );

            // Mettre l'authentification dans le contexte de sécurité
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Générer le token JWT
            String jwt = jwtProvider.generateToken(authentication.getName());

            // Récupérer les détails de l'utilisateur pour la réponse
            User userDetails = userRepository.findByEmail(request.email().toLowerCase().trim())
                    .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé: " + request.email()));

            List<String> roles = userDetails.getAuthorities().stream()
                    .map(item -> item.getAuthority())
                    .collect(Collectors.toList());

            // Construire et retourner la réponse
            return new LoginResponse(
                    jwt,
                    userDetails.getId(),
                    userDetails.getEmail(),
                    userDetails.getFirstName(),
                    userDetails.getLastName(),
                    roles
            );
        } catch (Exception e) {
            throw new IllegalArgumentException("Email ou mot de passe incorrect");
        }
    }

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