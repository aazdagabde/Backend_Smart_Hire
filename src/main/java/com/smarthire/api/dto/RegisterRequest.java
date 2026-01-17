package com.smarthire.api.dto;

// Importez les annotations de validation si vous les utilisez (optionnel pour l'instant)
// import jakarta.validation.constraints.*;

import com.smarthire.api.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
         @NotBlank @Size(min = 3, max = 50)
        String firstName,

         @NotBlank @Size(min = 3, max = 50)
        String lastName,

         @NotBlank @Email
        String email,

        @NotBlank @Size(min = 6) // Ajoutez une validation de taille si désiré
        String password,
        String phoneNumber,
         @NotBlank
         String role
) {}