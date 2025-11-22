package com.smarthire.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record JobOfferRequest(
        @NotBlank(message = "Le titre est obligatoire")
        @Size(min = 5, message = "Le titre doit contenir au moins 5 caractères")
        String title,

        @NotBlank(message = "La description est obligatoire")
        @Size(min = 20, message = "La description doit contenir au moins 20 caractères")
        String description,

        @NotBlank(message = "La localisation est obligatoire")
        String location,

        // Nouveau champ optionnel
        LocalDate deadline,

        @NotBlank(message = "Le type de contrat est obligatoire")
        String contractType,

        @NotBlank(message = "Le statut est obligatoire")
        String status
) {
}