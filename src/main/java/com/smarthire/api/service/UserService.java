package com.smarthire.api.service;

import com.smarthire.api.dto.ProfileUpdateDTO; // AJOUT
import com.smarthire.api.dto.ProfileViewDTO; // AJOUT
import com.smarthire.api.model.User;
import com.smarthire.api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    // Limite de taille pour la photo (ex: 5MB)
    // Cohérent avec votre application.properties
    private final long MAX_PROFILE_PICTURE_SIZE = 5 * 1024 * 1024;

    /**
     * Récupère les informations de profil (publiques) d'un utilisateur par son email.
     */
    @Transactional(readOnly = true)
    public ProfileViewDTO getUserProfile(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable pour l'email: " + userEmail));

        return ProfileViewDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .hasProfilePicture(user.getProfilePicture() != null)
                .build();
    }

    /**
     * Met à jour les informations de profil d'un utilisateur.
     */
    @Transactional
    public ProfileViewDTO updateUserProfile(String userEmail, ProfileUpdateDTO updateDTO) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable pour l'email: " + userEmail));

        // Mettre à jour uniquement les champs fournis (s'ils ne sont pas null)
        // Note : Si vous voulez permettre de mettre à null, la logique doit être ajustée.
        if (updateDTO.getFirstName() != null) {
            user.setFirstName(updateDTO.getFirstName());
        }
        if (updateDTO.getLastName() != null) {
            user.setLastName(updateDTO.getLastName());
        }
        if (updateDTO.getPhoneNumber() != null) {
            user.setPhoneNumber(updateDTO.getPhoneNumber());
        }

        User updatedUser = userRepository.save(user);

        // Renvoyer le profil mis à jour
        return ProfileViewDTO.builder()
                .id(updatedUser.getId())
                .email(updatedUser.getEmail())
                .firstName(updatedUser.getFirstName())
                .lastName(updatedUser.getLastName())
                .phoneNumber(updatedUser.getPhoneNumber())
                .hasProfilePicture(updatedUser.getProfilePicture() != null)
                .build();
    }

    /**
     * Uploade ou met à jour la photo de profil d'un utilisateur.
     */
    @Transactional // Ajout de @Transactional pour la session
    public void uploadProfilePicture(String userEmail, MultipartFile file) throws IOException {

        // --- Étape 1 : Validation ---
        List<String> allowedTypes = List.of("image/jpeg", "image/png");

        if (file.isEmpty()) {
            throw new IllegalArgumentException("Le fichier ne peut pas être vide.");
        }
        if (file.getSize() > MAX_PROFILE_PICTURE_SIZE) {
            throw new IllegalArgumentException("Le fichier ne doit pas dépasser 5MB.");
        }
        if (!allowedTypes.contains(file.getContentType())) {
            throw new IllegalArgumentException("Le fichier doit être au format JPEG ou PNG.");
        }

        // --- Étape 2 : Logique métier ---
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable pour l'email: " + userEmail));

        byte[] pictureData = file.getBytes();

        user.setProfilePicture(pictureData);
        user.setProfilePictureType(file.getContentType());

        userRepository.save(user);
    }

    /**
     * Récupère l'entité User complète pour un ID donné.
     * Nécessaire pour que le contrôleur puisse accéder aux données de l'image.
     */
    @Transactional(readOnly = true) // readOnly = true pour la performance en lecture
    public User getProfilePictureForUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable: " + userId));

        // Note : Grâce à @Transactional, la session reste ouverte,
        // ce qui permettra au contrôleur d'accéder au champ 'profilePicture'
        // même s'il est en FetchType.LAZY.
        return user;
    }
}