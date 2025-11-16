package com.smarthire.api.service;

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