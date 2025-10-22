package com.smarthire.api.controller;

import com.smarthire.api.dto.LoginRequest;
import com.smarthire.api.dto.LoginResponse;
import com.smarthire.api.dto.RegisterRequest;
import com.smarthire.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000", maxAge = 3600, allowCredentials = "true")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        try {
            authService.register(registerRequest);

            // Réponse structurée pour le frontend
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Utilisateur enregistré avec succès!");
            response.put("status", HttpStatus.CREATED.value());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            // Gère l'erreur si l'email existe déjà ou validation échoue
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("status", HttpStatus.BAD_REQUEST.value());

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            // Gère les autres erreurs potentielles
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Une erreur est survenue lors de l'inscription.");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse loginResponse = authService.login(loginRequest);

            // Réponse structurée pour le frontend
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Connexion réussie!");
            response.put("data", loginResponse);
            response.put("status", HttpStatus.OK.value());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Gère les erreurs de validation métier
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            // Gère spécifiquement les mauvais identifiants
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Email ou mot de passe incorrect.");
            errorResponse.put("status", HttpStatus.UNAUTHORIZED.value());

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (Exception e) {
            // Gère les autres erreurs
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Une erreur est survenue lors de la connexion.");
            errorResponse.put("error", e.getMessage());
            errorResponse.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // Endpoint optionnel pour vérifier la santé de l'API
    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Auth service is running");
        response.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(response);
    }

    // Endpoint optionnel pour déconnexion (côté client généralement)
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Déconnexion réussie");
        response.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(response);
    }
}