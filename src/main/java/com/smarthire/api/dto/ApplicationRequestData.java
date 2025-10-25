package com.smarthire.api.dto;

// DTO simple pour parser le JSON des réponses du candidat
// On utilise une classe simple au lieu d'un record pour la compatibilité ObjectMapper
public class ApplicationRequestData {
    private Long fieldId;
    private String value;

    // Getters et Setters nécessaires pour ObjectMapper
    public Long getFieldId() { return fieldId; }
    public void setFieldId(Long fieldId) { this.fieldId = fieldId; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}