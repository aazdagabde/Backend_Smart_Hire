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
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> getAllPublicOffers() {
        try {
            List<JobOfferResponse> offers = jobOfferService.getAllPublicOffers();
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
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la récupération de l'offre", e.getMessage());
        }
    }

    // ======================================================
    // ENDPOINTS SÉCURISÉS (pour RH)
    // ======================================================

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> createOffer(@Valid @RequestBody JobOfferRequest request) {
        try {
            String hrEmail = getAuthenticatedUserEmail();
            JobOfferResponse newOffer = jobOfferService.createOffer(request, hrEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(createSuccessResponse(newOffer, "Offre créée avec succès"));
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la création de l'offre", e.getMessage());
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_RH')")
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
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> deleteOffer(@PathVariable Long id) {
        try {
            String hrEmail = getAuthenticatedUserEmail();
            jobOfferService.deleteOffer(id, hrEmail);
            return ResponseEntity.ok(createSuccessResponse(null, "Offre supprimée avec succès"));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la suppression", e.getMessage());
        }
    }

    @GetMapping("/my")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> getMyOffers() {
        try {
            String hrEmail = getAuthenticatedUserEmail();
            // Vous devez ajouter la méthode getOffersByRecruiter au JobOfferService
            List<JobOfferResponse> offers = jobOfferService.getOffersByRecruiter(hrEmail);
            return ResponseEntity.ok(createSuccessResponse(offers, "Mes offres récupérées avec succès"));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, "Utilisateur RH non trouvé", e.getMessage());
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la récupération de mes offres", e.getMessage());
        }
    }

    // --- Méthodes utilitaires pour les réponses JSON (style AuthController) ---

    private String getAuthenticatedUserEmail() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private Map<String, Object> createSuccessResponse(Object data, String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        response.put("status", HttpStatus.OK.value());
        return response;
    }

    private ResponseEntity<Map<String, Object>> createErrorResponse(HttpStatus status, String message, String errorDetails) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message);
        if (errorDetails != null) {
            errorResponse.put("error", errorDetails);
        }
        errorResponse.put("status", status.value());
        return new ResponseEntity<>(errorResponse, status);
    }
}