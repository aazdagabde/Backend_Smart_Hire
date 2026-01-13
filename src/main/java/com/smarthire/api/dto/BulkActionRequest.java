package com.smarthire.api.dto;

public record BulkActionRequest(
        int topCount,          // Le "N" (ex: 5 premiers candidats)
        String actionType,     // "INTERVIEW" ou "ACCEPT"
        String message         // Le message personnalisé pour l'email
) {}