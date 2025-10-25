package com.smarthire.api.dto;

import com.smarthire.api.model.CustomFormField;

// DTO pour afficher un champ de formulaire
public record CustomFormFieldResponse(
        Long id,
        String label,
        String fieldType,
        String options,
        boolean isRequired
) {
    public static CustomFormFieldResponse fromEntity(CustomFormField field) {
        return new CustomFormFieldResponse(
                field.getId(),
                field.getLabel(),
                field.getFieldType().name(),
                field.getOptions(),
                field.isRequired()
        );
    }
}