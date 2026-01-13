package com.smarthire.api.model.enums;

public enum ApplicationStatus {
    PENDING,  // En attente d'examen
    REVIEWED,
    INTERVIEW_SCHEDULED,// CV examiné
    ACCEPTED, // Candidature acceptée pour entretien
    REJECTED  // Candidature rejetée
}