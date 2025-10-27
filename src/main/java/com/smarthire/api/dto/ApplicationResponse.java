// Fichier : src/main/java/com/smarthire/api/dto/ApplicationResponse.java

package com.smarthire.api.dto;

import com.smarthire.api.model.Application;
import java.time.Instant;

public record ApplicationResponse(
        Long id,
        Long jobOfferId,
        String jobOfferTitle,
        Long applicantId,
        String applicantName,
        String applicantEmail,
        String applicantPhoneNumber,
        String status,
        String cvFileName,
        String cvFileType,
        Instant appliedAt,

        // Champs de la réponse précédente
        Integer cvScore,
        String candidateMessage,

        // NOUVEAU CHAMP (Amélioration 3)
        String internalNotes
) {
    public static ApplicationResponse fromEntity(Application app) {
        if (app == null) return null;

        String applicantName = (app.getApplicant() != null)
                ? app.getApplicant().getFirstName() + " " + app.getApplicant().getLastName()
                : "Candidat inconnu";
        String applicantEmail = (app.getApplicant() != null)
                ? app.getApplicant().getEmail()
                : "Email inconnu";
        String applicantPhoneNumber = (app.getApplicant() != null)
                ? app.getApplicant().getPhoneNumber()
                : null;
        String jobOfferTitle = (app.getJobOffer() != null)
                ? app.getJobOffer().getTitle()
                : "Offre inconnue";

        return new ApplicationResponse(
                app.getId(),
                app.getJobOffer() != null ? app.getJobOffer().getId() : null,
                jobOfferTitle,
                app.getApplicant() != null ? app.getApplicant().getId() : null,
                applicantName,
                applicantEmail,
                applicantPhoneNumber,
                app.getStatus().name(),
                app.getCvFileName(),
                app.getCvFileType(),
                app.getAppliedAt(),

                // Champs de la réponse précédente
                app.getCvScore(),
                app.getCandidateMessage(),

                // NOUVEAU CHAMP (Amélioration 3)
                app.getInternalNotes()
        );
    }
}