package com.smarthire.api.controller;

import com.smarthire.api.dto.CustomFormFieldRequest;
import com.smarthire.api.dto.CustomFormFieldResponse;
import com.smarthire.api.service.CustomFormService;
import jakarta.persistence.EntityNotFoundException;
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
@RequestMapping("/api/offers/{offerId}/custom-fields") // Endpoints nichés sous l'offre
@RequiredArgsConstructor
public class CustomFormController {

    private final CustomFormService customFormService;

    /**
     * Endpoint pour un RH pour AJOUTER un champ personnalisé à une offre.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> createFormField(
            @PathVariable Long offerId,
            @RequestBody CustomFormFieldRequest request) {
        try {
            String rhEmail = getAuthenticatedUserEmail();
            CustomFormFieldResponse response = customFormService.createFormField(offerId, request, rhEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(createSuccessResponse(response, "Champ ajouté avec succès."));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur est survenue.", e.getMessage());
        }
    }

    /**
     * Endpoint PUBLIC pour récupérer les champs de formulaire d'une offre.
     * (Nécessaire pour que le candidat puisse construire le formulaire)
     */
    @GetMapping
    public ResponseEntity<?> getFormFieldsForOffer(@PathVariable Long offerId) {
        try {
            List<CustomFormFieldResponse> fields = customFormService.getFormFieldsForOffer(offerId);
            return ResponseEntity.ok(createSuccessResponse(fields, "Champs récupérés avec succès."));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        }
    }

    /**
     * Endpoint pour un RH pour SUPPRIMER un champ personnalisé.
     */
    @DeleteMapping("/{fieldId}")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> deleteFormField(
            @PathVariable Long offerId, // offerId est dans l'URL mais non utilisé, on garde pour la cohérence REST
            @PathVariable Long fieldId) {
        try {
            String rhEmail = getAuthenticatedUserEmail();
            customFormService.deleteFormField(fieldId, rhEmail);
            return ResponseEntity.ok(createSuccessResponse(null, "Champ supprimé avec succès."));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        }
    }


    // --- Méthodes utilitaires (copiées) ---

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