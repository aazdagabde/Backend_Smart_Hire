package com.smarthire.api.dto;

import com.smarthire.api.model.Application;
import java.time.Instant;

// DTO pour afficher les détails d'une candidature
public record ApplicationResponse(
        Long id,
        Long jobOfferId,
        String jobOfferTitle,
        Long applicantId,
        String applicantName,
        String status,
        String cvFileName,
        String cvFileType,
        Instant appliedAt
) {
    // Méthode de 'fabrique' pour convertir l'entité Application en DTO
    public static ApplicationResponse fromEntity(Application app) {
        if (app == null) return null;

        String applicantName = (app.getApplicant() != null)
                ? app.getApplicant().getFirstName() + " " + app.getApplicant().getLastName()
                : "Candidat inconnu";

        String jobOfferTitle = (app.getJobOffer() != null)
                ? app.getJobOffer().getTitle()
                : "Offre inconnue";

        return new ApplicationResponse(
                app.getId(),
                app.getJobOffer() != null ? app.getJobOffer().getId() : null,
                jobOfferTitle,
                app.getApplicant() != null ? app.getApplicant().getId() : null,
                applicantName,
                app.getStatus().name(),
                app.getCvFileName(),
                app.getCvFileType(),
                app.getAppliedAt()
        );
    }
}