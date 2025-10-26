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
import org.springframework.security.core.Authentication; // Importer Authentication
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class JobOfferController {

    private final JobOfferService jobOfferService;

    // ======================================================
    // ENDPOINTS PUBLICS
    // ======================================================

    @GetMapping
    public ResponseEntity<?> getAllPublicOffers(@RequestParam(required = false) String searchTerm) {
        try {
            List<JobOfferResponse> offers = jobOfferService.getAllPublicOffers(searchTerm);
            return ResponseEntity.ok(createSuccessResponse(offers, "Offres publiques récupérées"));
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur serveur", e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getPublicOfferById(@PathVariable Long id) {
        try {
            JobOfferResponse offer = jobOfferService.getPublicOfferById(id);
            return ResponseEntity.ok(createSuccessResponse(offer, "Offre récupérée"));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur serveur", e.getMessage());
        }
    }

    // ======================================================
    // ENDPOINTS SÉCURISÉS (pour RH)
    // ======================================================

    // <<< NOUVEL ENDPOINT (Pour Bug 3) >>>
    /**
     * Récupère les détails complets d'une offre pour son propriétaire RH (pour édition),
     * quel que soit le statut de l'offre.
     */
    @GetMapping("/details/{id}")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> getOfferDetailsForOwner(@PathVariable Long id) {
        try {
            String hrEmail = getAuthenticatedUserEmail();
            JobOfferResponse offer = jobOfferService.getOfferDetailsForOwner(id, hrEmail);
            return ResponseEntity.ok(createSuccessResponse(offer, "Détails de l'offre récupérés"));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (AccessDeniedException e) { // Important de catcher AccessDeniedException
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur serveur", e.getMessage());
        }
    }
    // <<< FIN NOUVEL ENDPOINT >>>

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> createOffer(@Valid @RequestBody JobOfferRequest request) {
        try {
            String hrEmail = getAuthenticatedUserEmail();
            JobOfferResponse newOffer = jobOfferService.createOffer(request, hrEmail);
            // Retourne CREATED avec les données
            Map<String, Object> response = createSuccessResponse(newOffer, "Offre créée avec succès");
            response.put("status", HttpStatus.CREATED.value());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) { // Validation métier
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), null);
        } catch (EntityNotFoundException e) { // Utilisateur RH non trouvé
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur serveur", e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> updateOffer(@PathVariable Long id, @Valid @RequestBody JobOfferRequest request) {
        try {
            String hrEmail = getAuthenticatedUserEmail();
            JobOfferResponse updatedOffer = jobOfferService.updateOffer(id, request, hrEmail);
            return ResponseEntity.ok(createSuccessResponse(updatedOffer, "Offre mise à jour"));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur serveur", e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> deleteOffer(@PathVariable Long id) {
        try {
            String hrEmail = getAuthenticatedUserEmail();
            jobOfferService.deleteOffer(id, hrEmail);
            // Pas de contenu à retourner après suppression réussie
            return ResponseEntity.ok(createSuccessResponse(null, "Offre supprimée avec succès"));
            // Alternative: return ResponseEntity.noContent().build();
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur serveur", e.getMessage());
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> getMyOffers() {
        try {
            String hrEmail = getAuthenticatedUserEmail();
            List<JobOfferResponse> offers = jobOfferService.getOffersByRecruiter(hrEmail);
            return ResponseEntity.ok(createSuccessResponse(offers, "Mes offres récupérées"));
        } catch (EntityNotFoundException e) { // Utilisateur non trouvé (peu probable si authentifié)
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur serveur", e.getMessage());
        }
    }


    // --- Méthodes utilitaires ---

    // Récupère l'email de l'utilisateur authentifié
    private String getAuthenticatedUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Vérification plus robuste
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() == null || "anonymousUser".equals(authentication.getPrincipal().toString())) {
            throw new IllegalStateException("Aucun utilisateur authentifié ou valide trouvé.");
        }
        // Principal peut être UserDetails ou juste le nom (String)
        if (authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails) {
            return ((org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal()).getUsername();
        } else {
            return authentication.getPrincipal().toString();
        }
    }

    // Crée une réponse JSON standardisée pour les succès
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