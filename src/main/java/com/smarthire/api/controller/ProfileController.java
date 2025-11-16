package com.smarthire.api.controller;

import com.smarthire.api.dto.ProfileUpdateDTO; // AJOUT
import com.smarthire.api.dto.ProfileViewDTO; // AJOUT
import com.smarthire.api.model.User;
import com.smarthire.api.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException; // AJOUT

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;

    // --- ENDPOINTS DE DONNÉES TEXTUELLES (JSON) ---

    /**
     * NOUVEAU (MANQUANT)
     * Récupère les détails textuels du profil de l'utilisateur authentifié.
     * C'est ce que ProfileService.getProfile() appelle.
     */
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileViewDTO> getProfile() {
        try {
            String userEmail = getAuthenticatedUserEmail();
            ProfileViewDTO profile = userService.getUserProfile(userEmail);
            return ResponseEntity.ok(profile);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé", e);
        }
    }

    /**
     * NOUVEAU (MANQUANT)
     * Met à jour les détails textuels du profil de l'utilisateur authentifié.
     * C'est ce que ProfileService.updateProfile() appelle.
     */
    @PutMapping("/update")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileViewDTO> updateProfile(@RequestBody ProfileUpdateDTO updateDTO) {
        try {
            String userEmail = getAuthenticatedUserEmail();
            ProfileViewDTO updatedProfile = userService.updateUserProfile(userEmail, updateDTO); // <-- CORRIGÉ
            return ResponseEntity.ok(updatedProfile);
        } catch (EntityNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur non trouvé", e);
        }
    }


    // --- ENDPOINTS DE L'IMAGE (BLOB / MULTIPART) ---
    // Vos endpoints existants, qui sont corrects.

    /**
     * Endpoint pour uploader (POST) ou remplacer (PUT) la photo de profil
     * de l'utilisateur authentifié.
     */
    @PutMapping(value = "/picture", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadProfilePicture(
            @RequestPart("picture") MultipartFile pictureFile) {

        try {
            String userEmail = getAuthenticatedUserEmail();
            userService.uploadProfilePicture(userEmail, pictureFile);

            return ResponseEntity.ok(createSuccessResponse(null, "Photo de profil mise à jour avec succès."));

        } catch (IOException e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du traitement du fichier.", e.getMessage());
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur est survenue.", e.getMessage());
        }
    }

    /**
     * Endpoint pour AFFICHER la photo de profil d'un utilisateur par son ID.
     * C'est un endpoint public (géré par SecurityConfig).
     */
    @GetMapping("/{userId}/picture")
    public ResponseEntity<Resource> getProfilePicture(@PathVariable Long userId) {
        try {
            User user = userService.getProfilePictureForUser(userId);

            if (user.getProfilePicture() == null || user.getProfilePictureType() == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(user.getProfilePictureType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                    .body(new ByteArrayResource(user.getProfilePicture()));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    // --- MÉTHODES UTILITAIRES ---
    // (Inchangées)

    private String getAuthenticatedUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Map<String, Object> createSuccessResponse(Object data, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        if (data != null) {
            response.put("data", data);
        }
        response.put("status", HttpStatus.OK.value());
        return response;
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(HttpStatus status, String message, String errorDetails) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        if (errorDetails != null && !errorDetails.isEmpty()) {
            errorResponse.put("error", errorDetails);
        }
        errorResponse.put("status", status.value());
        return new ResponseEntity<>(errorResponse, status);
    }
}