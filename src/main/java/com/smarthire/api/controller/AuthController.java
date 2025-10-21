package com.smarthire.api.controller;

import com.smarthire.api.dto.LoginRequest;
import com.smarthire.api.dto.LoginResponse;
import com.smarthire.api.dto.RegisterRequest;
import com.smarthire.api.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

// Importez @Valid si vous utilisez la validation
// import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
// Optionnel: Si votre frontend est sur un port différent (ex: React sur 3000, Angular sur 4200)
// @CrossOrigin(origins = "*", maxAge = 3600) // Autorise toutes les origines pour le dev
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(/*@Valid*/ @RequestBody RegisterRequest registerRequest) {
        try {
            authService.register(registerRequest);
            // Vous pouvez retourner un message simple ou juste un statut 201 Created
            return ResponseEntity.status(HttpStatus.CREATED).body("Utilisateur enregistré avec succès!");
        } catch (IllegalArgumentException e) {
            // Gère l'erreur si l'email existe déjà
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            // Gère les autres erreurs potentielles
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Une erreur est survenue.");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(/*@Valid*/ @RequestBody LoginRequest loginRequest) {
        try {
            LoginResponse loginResponse = authService.login(loginRequest);
            return ResponseEntity.ok(loginResponse);
        } catch (Exception e) { // Spring Security lance des exceptions spécifiques (BadCredentialsException...)
            // Pour simplifier, on retourne juste 401 Unauthorized
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email ou mot de passe incorrect.");
        }
    }
}