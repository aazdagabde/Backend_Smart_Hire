package com.smarthire.api.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

// DTO pour METTRE À JOUR les informations de profil
@Data
public class ProfileUpdateDTO {

    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    private String firstName;

    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String lastName;

    // Vous pouvez ajouter des validations plus strictes pour le téléphone
    @Size(max = 20, message = "Le numéro de téléphone ne doit pas dépasser 20 caractères")
    private String phoneNumber;
}