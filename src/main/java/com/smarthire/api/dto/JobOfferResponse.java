package com.smarthire.api.dto;

import java.time.Instant;

// DTO pour afficher une offre (réponse de l'API)
public record JobOfferResponse(
        Long id,
        String title,
        String description,
        String location,
        String contractType,
        String status,
        Long createdById,
        String createdByFullName,
        Instant createdAt,
        Instant updatedAt
) {
}