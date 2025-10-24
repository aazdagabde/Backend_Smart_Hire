package com.smarthire.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// DTO pour la création et la mise à jour d'une offre
public record JobOfferRequest(
        @NotBlank(message = "Le titre est obligatoire")
        @Size(min = 5, message = "Le titre doit contenir au moins 5 caractères")
        String title,

        @NotBlank(message = "La description est obligatoire")
        @Size(min = 20, message = "La description doit contenir au moins 20 caractères")
        String description,

        @NotBlank(message = "La localisation est obligatoire")
        String location,

        @NotBlank(message = "Le type de contrat est obligatoire (ex: CDI, CDD, STAGE...)")
        String contractType, // On le garde en String pour la simplicité de l'API

        @NotBlank(message = "Le statut est obligatoire (ex: PUBLISHED, DRAFT)")
        String status // On le garde en String pour la simplicité
) {
}