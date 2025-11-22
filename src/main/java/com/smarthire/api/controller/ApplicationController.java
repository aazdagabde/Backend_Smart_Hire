package com.smarthire.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.smarthire.api.dto.*; // Importe tous les DTOs
import com.smarthire.api.model.Application;
import com.smarthire.api.service.ApplicationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid; // Pour la validation des DTOs
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.internal.util.stereotypes.Lazy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.smarthire.api.service.AIService;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    private  AIService aiService;

    @Autowired
    public void setAiService(AIService aiService) {
        this.aiService = aiService;
    }

    // ==================================================================================
    // ENDPOINTS EXISTANTS
    // ==================================================================================

    /**
     * Endpoint pour un CANDIDAT pour postuler à une offre.
     */
    @PostMapping(value = "/apply/{offerId}", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @PreAuthorize("hasAuthority('ROLE_CANDIDAT')")
    public ResponseEntity<?> applyToOffer(
            @PathVariable Long offerId,
            @RequestPart("cv") MultipartFile cvFile,
            @RequestPart(value = "customData", required = false) String customDataJson
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
     */
    @GetMapping("/{applicationId}/cv")
    @PreAuthorize("hasAnyAuthority('ROLE_RH', 'ROLE_CANDIDAT')")
    public ResponseEntity<Resource> downloadCv(@PathVariable Long applicationId) {
        try {
            String userEmail = getAuthenticatedUserEmail();
            Application application = applicationService.getApplicationCv(applicationId, userEmail);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(application.getCvFileType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + application.getCvFileName() + "\"")
                    .body(new ByteArrayResource(application.getCvData()));

        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * Endpoint pour un CANDIDAT pour mettre à jour son CV.
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
     * Endpoint pour voir les réponses personnalisées.
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

    // ==================================================================================
    // GESTION RH (Statut, Score, Notes)
    // ==================================================================================

    @PutMapping("/{applicationId}/status")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> updateApplicationStatus(
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateApplicationStatusRequest request) {
        try {
            String rhEmail = getAuthenticatedUserEmail();
            ApplicationResponse updatedApplication = applicationService.updateApplicationStatus(applicationId, request, rhEmail);
            return ResponseEntity.ok(createSuccessResponse(updatedApplication, "Statut de la candidature mis à jour."));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur serveur.", e.getMessage());
        }
    }

    @PutMapping("/{applicationId}/score")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> updateCvScore(
            @PathVariable Long applicationId,
            @Valid @RequestBody UpdateCvScoreRequest request) {
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

    // ==================================================================================
    // FONCTIONNALITÉS IA (NOUVEAUX ENDPOINTS AJOUTÉS)
    // ==================================================================================

    // Endpoint pour générer un résumé du profil candidat par IA
    @PostMapping("/{id}/ai-summary")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> generateAiSummary(@PathVariable Long id) {
        try {
            String userEmail = getAuthenticatedUserEmail();
            // Appel à la méthode correspondante dans ApplicationService
            String summary = applicationService.generateAiSummary(id, userEmail);
            return ResponseEntity.ok(createSuccessResponse(summary, "Résumé généré avec succès"));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la génération du résumé", e.getMessage());
        }
    }

    // Endpoint pour générer des questions d'entretien par IA
    @PostMapping("/{id}/ai-questions")
    @PreAuthorize("hasAuthority('ROLE_RH')")
    public ResponseEntity<?> generateAiQuestions(@PathVariable Long id) {
        try {
            String userEmail = getAuthenticatedUserEmail();
            // Appel à la méthode correspondante dans ApplicationService
            String questions = applicationService.generateAiInterviewQuestions(id, userEmail);
            return ResponseEntity.ok(createSuccessResponse(questions, "Questions générées avec succès"));
        } catch (EntityNotFoundException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
        } catch (AccessDeniedException e) {
            return createErrorResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
        } catch (Exception e) {
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur lors de la génération des questions", e.getMessage());
        }
    }

    // Endpoint pour lancer l'analyse globale des CVs
    @PostMapping("/{id}/analyze-cvs")
    @PreAuthorize("hasAuthority('ROLE_RH')") // Sécurisation ajoutée
    public ResponseEntity<?> analyzeAllCvs(@PathVariable Long id, Authentication authentication) {
        // On lance l'analyse (méthode @Async)
        aiService.analyzeAllApplications(id, authentication.getName());

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Analyse IA lancée en arrière-plan. Les résultats apparaîtront progressivement."
        ));
    }

    // ==================================================================================
    // MÉTHODES UTILITAIRES
    // ==================================================================================

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