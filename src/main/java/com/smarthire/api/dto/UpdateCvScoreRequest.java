package com.smarthire.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCvScoreRequest(
        @NotNull(message = "La note est obligatoire")
        @Min(value = 0, message = "La note doit être au moins 0")
        @Max(value = 100, message = "La note ne doit pas dépasser 100") // Ou une autre valeur max
        Integer score
) {}