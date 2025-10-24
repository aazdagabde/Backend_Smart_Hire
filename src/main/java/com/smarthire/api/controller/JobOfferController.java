package com.smarthire.api.controller;

import com.smarthire.api.dto.JobOfferRequest;
import com.smarthire.api.dto.JobOfferResponse;
import com.smarthire.api.service.JobOfferService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*; // Assurez-vous d'avoir @RequestParam

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
// @CrossOrigin(origins = "http://localhost:3000", maxAge = 3600, allowCredentials = "true") // Déjà géré globalement dans SecurityConfig
public class JobOfferController {

    private final JobOfferService jobOfferService;

    // ======================================================
    // ENDPOINTS PUBLICS (pour candidats et visiteurs)
    // ======================================================

    @GetMapping
    public ResponseEntity<?> getAllPublicOffers(
            // Ajouter @RequestParam pour le terme de recherche, non obligatoire (required = false)
            @RequestParam(required = false) String searchTerm
    ) {
        try {
            // Passer le searchTerm (qui peut être null) au service
            List<JobOfferResponse> offers = jobOfferService.getAllPublicOffers(searchTerm);
            return ResponseEntity.ok(createSuccessResponse(offers, "Offres récupérées avec succès"));
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la récupération des offres", e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPublicOfferById(@PathVariable Long id) {
        try {
            JobOfferResponse offer = jobOfferService.getPublicOfferById(id);
            return ResponseEntity.ok(createSuccessResponse(offer, "Offre récupérée avec succès"));
        } catch (EntityNotFoundException e) {
            // Retourne 404 si l'offre n'est pas trouvée OU n'est pas publiée
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            // Optionnel : si le service lance AccessDeniedException pour les offres non publiées
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la récupération de l'offre", e.getMessage());
        }
    }

    // ======================================================
    // ENDPOINTS SÉCURISÉS (pour RH)
    // ======================================================

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_RH')") // Sécurise l'endpoint pour les RH
    public ResponseEntity<?> createOffer(@Valid @RequestBody JobOfferRequest request) {
        try {
            String hrEmail = getAuthenticatedUserEmail();
            JobOfferResponse newOffer = jobOfferService.createOffer(request, hrEmail);
            // Utilise HttpStatus.CREATED pour la réponse de création
            Map<String, Object> response = createSuccessResponse(newOffer, "Offre créée avec succès");
            response.put("status", HttpStatus.CREATED.value()); // Met à jour le statut dans la réponse
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            // Erreurs de validation ou utilisateur non trouvé
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), null);
        } catch (Exception e) {
            // Autres erreurs serveur
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la création de l'offre", e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_RH')") // Sécurise l'endpoint
    public ResponseEntity<?> updateOffer(@PathVariable Long id, @Valid @RequestBody JobOfferRequest request) {
        try {
            String hrEmail = getAuthenticatedUserEmail();
            JobOfferResponse updatedOffer = jobOfferService.updateOffer(id, request, hrEmail);
            return ResponseEntity.ok(createSuccessResponse(updatedOffer, "Offre mise à jour avec succès"));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la mise à jour", e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_RH')") // Sécurise l'endpoint
    public ResponseEntity<?> deleteOffer(@PathVariable Long id) {
        try {
            String hrEmail = getAuthenticatedUserEmail();
            jobOfferService.deleteOffer(id, hrEmail);
            // Pas besoin de retourner de données, juste un message de succès
            // Spécifie explicitement HttpStatus.OK pour la suppression réussie sans contenu à retourner.
            Map<String, Object> response = createSuccessResponse(null, "Offre supprimée avec succès");
            response.put("status", HttpStatus.OK.value()); // Maintient OK pour la suppression
            return ResponseEntity.ok(response);
            // Alternative: Si vous préférez retourner 204 No Content (pas de corps de réponse)
            // return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la suppression", e.getMessage());
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_RH')") // Sécurise l'endpoint
    public ResponseEntity<?> getMyOffers() {
        try {
            String hrEmail = getAuthenticatedUserEmail();
            List<JobOfferResponse> offers = jobOfferService.getOffersByRecruiter(hrEmail);
            return ResponseEntity.ok(createSuccessResponse(offers, "Mes offres récupérées avec succès"));
        } catch (EntityNotFoundException e) {
            // Spécifique si l'utilisateur RH n'est pas trouvé (peu probable si authentifié)
            return createErrorResponse(HttpStatus.NOT_FOUND, "Utilisateur RH non trouvé", e.getMessage());
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la récupération de mes offres", e.getMessage());
        }
    }


    // --- Méthodes utilitaires pour les réponses JSON (style AuthController) ---

    // Récupère l'email de l'utilisateur authentifié
    private String getAuthenticatedUserEmail() {
        // Ajouter une vérification pour s'assurer que l'authentification n'est pas nulle
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("Aucun utilisateur authentifié trouvé.");
        }
        return authentication.getName();
    }

    // Crée une réponse JSON standardisée pour les succès
    private Map<String, Object> createSuccessResponse(Object data, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        // Ne pas ajouter 'data' si null pour éviter "data": null dans la réponse JSON
        if (data != null) {
            response.put("data", data);
        }
        response.put("status", HttpStatus.OK.value()); // Statut par défaut OK, sera ajusté si nécessaire (ex: CREATED)
        return response;
    }

    // Crée une réponse JSON standardisée pour les erreurs
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