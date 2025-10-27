// Fichier : src/main/java/com/smarthire/api/dto/UpdateInternalNotesRequest.java

package com.smarthire.api.dto;

import jakarta.validation.constraints.Size;

public record UpdateInternalNotesRequest(

        // @Size est optionnel mais recommandé pour éviter des notes trop longues
        @Size(max = 5000, message = "Les notes ne doivent pas dépasser 5000 caractères")
        String notes // Peut être null ou une chaîne vide pour effacer les notes
) {}