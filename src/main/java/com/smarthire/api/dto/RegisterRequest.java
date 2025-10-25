package com.smarthire.api.dto;

// Importez les annotations de validation si vous les utilisez (optionnel pour l'instant)
// import jakarta.validation.constraints.*;

public record RegisterRequest(
        // @NotBlank @Size(min = 3, max = 50)
        String firstName,

        // @NotBlank @Size(min = 3, max = 50)
        String lastName,

        // @NotBlank @Email
        String email,

        // @NotBlank @Size(min = 6) // Ajoutez une validation de taille si désiré
        String password,
        String phoneNumber
) {}