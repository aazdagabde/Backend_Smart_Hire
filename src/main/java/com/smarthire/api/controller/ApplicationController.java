// Fichier : src/main/java/com/smarthire/api/controller/ApplicationController.java

package com.smarthire.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.smarthire.api.dto.*; // Importe tous les DTOs
import com.smarthire.api.model.Application;
import com.smarthire.api.service.ApplicationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid; // Pour la validation des DTOs
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    // ... (endpoints existants: applyToOffer, getMyApplications, getApplicationsForOffer) ...
    // >>> ASSUREZ-VOUS QUE LES ENDPOINTS EXISTANTS SONT PRÉSENTS ICI <<<
    /**
     * Endpoint pour un CANDIDAT pour postuler à une offre.
     * Le CV est envoyé en tant que 'multipart/form-data'.
     * Les données personnalisées sont envoyées en tant que 'multipart/form-data' dans un champ "customData".
     */
    @PostMapping(value = "/apply/{offerId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasAuthority('ROLE_CANDIDAT')")
    public ResponseEntity<?> applyToOffer(
            @PathVariable Long offerId,
            @RequestPart("cv") MultipartFile cvFile,
            @RequestPart(value = "customData", required = false) String customDataJson // Champ optionnel pour les données JSON
    ) {

        try {
            String candidateEmail = getAuthenticatedUserEmail();
            ApplicationResponse response = applicationService.applyToOffer(offerId, cvFile, customDataJson, candidateEmail);
            return ResponseEntity.status(HttpStatus.CREATED).body(createSuccessResponse(response, "Candidature enregistrée avec succès."));

        } catch (IOException e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du traitement du fichier CV.", e.getMessage());
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur est survenue.", e.getMessage());
        }
    }

    /**
     * Endpoint pour un CANDIDAT pour voir ses propres candidatures.
     */
    @GetMapping("/my-applications")
    @PreAuthorize("hasAuthority('ROLE_CANDIDAT')")
    public ResponseEntity<?> getMyApplications() {
        try {
            String candidateEmail = getAuthenticatedUserEmail();
            List<ApplicationResponse> applications = applicationService.getApplicationsForCandidate(candidateEmail);
            return ResponseEntity.ok(createSuccessResponse(applications, "Candidatures récupérées avec succès."));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        }
    }

    /**
     * Endpoint pour un RH pour voir les candidatures d'une de ses offres.
     */
    @GetMapping("/offer/{offerId}")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> getApplicationsForOffer(@PathVariable Long offerId) {
        try {
            String rhEmail = getAuthenticatedUserEmail();
            List<ApplicationResponse> applications = applicationService.getApplicationsForOffer(offerId, rhEmail);
            return ResponseEntity.ok(createSuccessResponse(applications, "Candidats récupérés avec succès."));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        }
    }

    /**
     * Endpoint pour un RH ou un CANDIDAT pour TÉLÉCHARGER ou AFFICHER un CV.
     * MODIFIÉ (Amélioration 2) : Changé de "attachment" à "inline"
     */
    @GetMapping("/{applicationId}/cv")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_CANDIDAT')")
    public ResponseEntity<Resource> downloadCv(@PathVariable Long applicationId) {
        try {
            String userEmail = getAuthenticatedUserEmail();
            // Le service vérifie les droits d'accès
            Application application = applicationService.getApplicationCv(applicationId, userEmail);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(application.getCvFileType()))
                    // MODIFICATION : "inline" demande au navigateur d'afficher le fichier
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + application.getCvFileName() + "\"")
                    .body(new ByteArrayResource(application.getCvData()));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Endpoint pour un CANDIDAT pour mettre à jour son CV pour une candidature existante.
     */
    @PutMapping(value = "/{applicationId}/cv", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasAuthority('ROLE_CANDIDAT')")
    public ResponseEntity<?> updateApplicationCv(
            @PathVariable Long applicationId,
            @RequestParam("cv") MultipartFile cvFile) {

        try {
            String candidateEmail = getAuthenticatedUserEmail();
            ApplicationResponse response = applicationService.updateApplicationCv(applicationId, cvFile, candidateEmail);
            return ResponseEntity.ok(createSuccessResponse(response, "CV mis à jour avec succès."));

        } catch (IOException e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors du traitement du fichier CV.", e.getMessage());
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur est survenue.", e.getMessage());
        }
    }

    /**
     * Endpoint pour un RH ou un CANDIDAT pour voir les réponses personnalisées d'une candidature.
     */
    @GetMapping("/{applicationId}/custom-data")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_CANDIDAT')")
    public ResponseEntity<?> getApplicationCustomData(@PathVariable Long applicationId) {
        try {
            String userEmail = getAuthenticatedUserEmail();
            List<ApplicationCustomDataResponse> responses = applicationService.getApplicationCustomData(applicationId, userEmail);
            return ResponseEntity.ok(createSuccessResponse(responses, "Données personnalisées récupérées avec succès."));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        }
    }

    // --- ENDPOINTS AJOUTÉS PRÉCÉDEMMENT ---

    // Endpoint : Mettre à jour le statut (RH)
    @PutMapping("/{applicationId}/status")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateApplicationStatusRequest request) { // Utilise le nouveau DTO et la validation
        try {
            String rhEmail = getAuthenticatedUserEmail();
            ApplicationResponse updatedApplication = applicationService.updateApplicationStatus(applicationId, request, rhEmail);
            return ResponseEntity.ok(createSuccessResponse(updatedApplication, "Statut de la candidature mis à jour."));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        } catch (IllegalArgumentException e) { // Pour statut invalide ou message trop long
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur serveur.", e.getMessage());
        }
    }

    // Endpoint : Mettre à jour la note du CV (RH)
    @PutMapping("/{applicationId}/score")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> updateCvScore(
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateCvScoreRequest request) { // Utilise le nouveau DTO et la validation
        try {
            String rhEmail = getAuthenticatedUserEmail();
            ApplicationResponse updatedApplication = applicationService.updateCvScore(applicationId, request, rhEmail);
            return ResponseEntity.ok(createSuccessResponse(updatedApplication, "Note du CV mise à jour."));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, "Données invalides.", e.getMessage());
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur serveur.", e.getMessage());
        }
    }


    // --- NOUVEL ENDPOINT (Amélioration 3) ---

    // Endpoint : Mettre à jour les notes internes (RH)
    @PutMapping("/{applicationId}/notes")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> updateInternalNotes(
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateInternalNotesRequest request) {
        try {
            String rhEmail = getAuthenticatedUserEmail();
            ApplicationResponse updatedApplication = applicationService.updateInternalNotes(applicationId, request, rhEmail);
            return ResponseEntity.ok(createSuccessResponse(updatedApplication, "Notes internes mises à jour."));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur serveur.", e.getMessage());
        }
    }


    // --- Méthodes utilitaires ---

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