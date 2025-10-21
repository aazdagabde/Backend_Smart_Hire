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
import org.springframework.transaction.annotation.Transactional; // Important pour gérer les transactions

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

    @Transactional // Assure que l'opération est atomique
    public void register(RegisterRequest request) {
        // 1. Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Erreur: Email déjà utilisé!");
        }

        // 2. Créer le nouvel utilisateur
        User user = User.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password())) // Hasher le mot de passe
                .build();

        // 3. Assigner un rôle par défaut (ex: ROLE_CANDIDAT)
        // Cherche le rôle, ou le crée s'il n'existe pas (bonne pratique pour le premier lancement)
        Role userRole = roleRepository.findByName("ROLE_CANDIDAT")
                .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_CANDIDAT").build()));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        user.setRoles(roles);

        // 4. Sauvegarder l'utilisateur
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {
        // 1. Authentifier l'utilisateur via Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        // 2. Mettre l'authentification dans le contexte de sécurité
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Générer le token JWT
        String jwt = jwtProvider.generateToken(authentication.getName()); // authentication.getName() retourne l'email

        // 4. Récupérer les détails de l'utilisateur pour la réponse
        // Note: L'authentification réussie garantit que l'utilisateur existe
        User userDetails = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé après authentification réussie - devrait être impossible"));

        List<String> roles = userDetails.getAuthorities().stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toList());

        // 5. Construire et retourner la réponse
        return new LoginResponse(
                jwt,
                userDetails.getId(),
                userDetails.getEmail(),
                userDetails.getFirstName(),
                userDetails.getLastName(),
                roles
        );
    }
}