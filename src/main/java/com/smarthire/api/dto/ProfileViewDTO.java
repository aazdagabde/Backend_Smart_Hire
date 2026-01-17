package com.smarthire.api.dto;

import lombok.Builder;
import lombok.Data;

// DTO pour AFFICHER les informations de profil
@Data
@Builder
public class ProfileViewDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String role ;
    private boolean hasProfilePicture;
}