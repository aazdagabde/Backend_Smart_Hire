package com.smarthire.api.dto;

import com.smarthire.api.model.ApplicationCustomData;

// DTO pour afficher une réponse personnalisée
public record ApplicationCustomDataResponse(
        Long id,
        String label, // Le label de la question
        String value  // La réponse donnée
) {
    public static ApplicationCustomDataResponse fromEntity(ApplicationCustomData data) {
        return new ApplicationCustomDataResponse(
                data.getId(),
                data.getCustomFormField().getLabel(),
                data.getValue()
        );
    }
}