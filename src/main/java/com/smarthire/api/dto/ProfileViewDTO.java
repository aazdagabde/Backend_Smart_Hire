package com.smarthire.api.dto;

import lombok.Builder;
import lombok.Data;

// DTO pour AFFICHER les informations de profil (sans mot de passe)
@Data
@Builder
public class ProfileViewDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    // On peut ajouter un champ pour l'URL de l'image de profil si on veut
    private boolean hasProfilePicture;
}