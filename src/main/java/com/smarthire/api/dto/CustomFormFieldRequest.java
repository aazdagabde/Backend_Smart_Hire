package com.smarthire.api.dto;

// DTO pour créer un champ de formulaire
public record CustomFormFieldRequest(
        String label,
        String fieldType, // "TEXT", "TEXTAREA", "RADIO", "CHECKBOX"
        String options, // "Option 1;Option 2;Option 3"
        boolean isRequired
) {}