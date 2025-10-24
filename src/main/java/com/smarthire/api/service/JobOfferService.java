package com.smarthire.api.service;

import com.smarthire.api.dto.JobOfferRequest;
import com.smarthire.api.dto.JobOfferResponse;
import com.smarthire.api.model.JobOffer;
import com.smarthire.api.model.User;
import com.smarthire.api.model.enums.ContractType;
import com.smarthire.api.model.enums.OfferStatus;
import com.smarthire.api.repository.JobOfferRepository;
import com.smarthire.api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobOfferService {

    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;

    // --- Méthodes publiques (pour les candidats) ---

    @Transactional(readOnly = true)
    public List<JobOfferResponse> getAllPublicOffers() {
        // Ne retourne que les offres publiées
        return jobOfferRepository.findByStatus(OfferStatus.PUBLISHED).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public JobOfferResponse getPublicOfferById(Long id) {
        JobOffer offer = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offre non trouvée: " + id));

        // Vérifie si l'offre est publiée avant de la retourner
        if (offer.getStatus() != OfferStatus.PUBLISHED) {
            throw new EntityNotFoundException("Offre non trouvée ou non publiée: " + id);
        }
        return convertToResponse(offer);
    }

    // --- Méthodes sécurisées (pour les RH) ---

    @Transactional
    public JobOfferResponse createOffer(JobOfferRequest request, String hrEmail) {
        User hrUser = userRepository.findByEmail(hrEmail)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur RH non trouvé: " + hrEmail));

        JobOffer offer = JobOffer.builder()
                .title(request.title())
                .description(request.description())
                .location(request.location())
                .contractType(validateContractType(request.contractType()))
                .status(validateOfferStatus(request.status()))
                .createdBy(hrUser)
                .build();

        JobOffer savedOffer = jobOfferRepository.save(offer);
        return convertToResponse(savedOffer);
    }

    @Transactional
    public JobOfferResponse updateOffer(Long id, JobOfferRequest request, String hrEmail) {
        User hrUser = userRepository.findByEmail(hrEmail)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur RH non trouvé: " + hrEmail));

        JobOffer offer = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offre non trouvée: " + id));

        // Vérification de sécurité : Seul le créateur de l'offre peut la modifier
        if (!offer.getCreatedBy().equals(hrUser)) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à modifier cette offre.");
        }

        offer.setTitle(request.title());
        offer.setDescription(request.description());
        offer.setLocation(request.location());
        offer.setContractType(validateContractType(request.contractType()));
        offer.setStatus(validateOfferStatus(request.status()));

        JobOffer updatedOffer = jobOfferRepository.save(offer);
        return convertToResponse(updatedOffer);
    }

    @Transactional
    public void deleteOffer(Long id, String hrEmail) {
        User hrUser = userRepository.findByEmail(hrEmail)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur RH non trouvé: " + hrEmail));

        JobOffer offer = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offre non trouvée: " + id));

        // Vérification de sécurité : Seul le créateur de l'offre peut la supprimer
        if (!offer.getCreatedBy().equals(hrUser)) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à supprimer cette offre.");
        }

        jobOfferRepository.delete(offer);
    }

    @Transactional(readOnly = true)
    public List<JobOfferResponse> getOffersByRecruiter(String hrEmail) {
        User hrUser = userRepository.findByEmail(hrEmail)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur RH non trouvé: " + hrEmail));

        // Utilise la méthode existante du repository
        return jobOfferRepository.findByCreatedById(hrUser.getId()).stream()
                .map(this::convertToResponse) // Réutilise le mapper DTO existant
                .collect(Collectors.toList());
    }

    // --- Méthodes utilitaires ---

    private ContractType validateContractType(String type) {
        try {
            return ContractType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Type de contrat invalide: " + type);
        }
    }

    private OfferStatus validateOfferStatus(String status) {
        try {
            return OfferStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Statut d'offre invalide: " + status);
        }
    }

    // Mapper Entité vers DTO
    private JobOfferResponse convertToResponse(JobOffer offer) {
        String fullName = offer.getCreatedBy().getFirstName() + " " + offer.getCreatedBy().getLastName();
        return new JobOfferResponse(
                offer.getId(),
                offer.getTitle(),
                offer.getDescription(),
                offer.getLocation(),
                offer.getContractType().name(),
                offer.getStatus().name(),
                offer.getCreatedBy().getId(),
                fullName,
                offer.getCreatedAt(),
                offer.getUpdatedAt()
        );
    }
}