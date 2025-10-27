package com.smarthire.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateApplicationStatusRequest(
        @NotBlank(message = "Le statut est obligatoire")
        String status, // Sera validé comme enum dans le service

        @Size(max = 255, message = "Le message ne doit pas dépasser 255 caractères")
        String message // Message optionnel pour le candidat
) {}