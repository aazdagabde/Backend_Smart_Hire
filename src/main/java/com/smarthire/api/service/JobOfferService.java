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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException; // Correction: java.io et non io.jsonwebtoken
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobOfferService {

    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;

    // --- Méthodes publiques (pour les candidats) ---

    @Transactional(readOnly = true)
    public List<JobOfferResponse> getAllPublicOffers(String searchTerm) {
        List<JobOffer> offers;
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String processedSearchTerm = "%" + searchTerm.trim().toLowerCase() + "%";
            offers = jobOfferRepository.findPublishedOffersBySearchTerm(
                    OfferStatus.PUBLISHED,
                    processedSearchTerm
            );
        } else {
            offers = jobOfferRepository.findByStatus(OfferStatus.PUBLISHED);
        }
        return offers.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public JobOfferResponse getPublicOfferById(Long id) {
        JobOffer offer = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offre non trouvée: " + id));

        // Vérifie si l'offre est publiée avant de la retourner publiquement
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
                .deadline(request.deadline())
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

        // Vérification de sécurité
        if (offer.getCreatedBy() == null || !Objects.equals(offer.getCreatedBy().getId(), hrUser.getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à modifier cette offre.");
        }

        offer.setTitle(request.title());
        offer.setDescription(request.description());
        offer.setLocation(request.location());
        offer.setDeadline(request.deadline());
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

        // Vérification de sécurité
        if (offer.getCreatedBy() == null || !Objects.equals(offer.getCreatedBy().getId(), hrUser.getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à supprimer cette offre.");
        }
        jobOfferRepository.delete(offer);
    }

    @Transactional(readOnly = true)
    public List<JobOfferResponse> getOffersByRecruiter(String hrEmail) {
        User hrUser = userRepository.findByEmail(hrEmail)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur RH non trouvé: " + hrEmail));

        return jobOfferRepository.findByCreatedById(hrUser.getId()).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public JobOfferResponse getOfferDetailsForOwner(Long id, String hrEmail) {
        User hrUser = userRepository.findByEmail(hrEmail)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur RH non trouvé: " + hrEmail));

        JobOffer offer = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offre non trouvée: " + id));

        if (offer.getCreatedBy() == null || !Objects.equals(offer.getCreatedBy().getId(), hrUser.getId())) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à accéder aux détails de cette offre.");
        }

        return convertToResponse(offer);
    }

    // --- GESTION DES IMAGES ---

    @Transactional
    public void uploadOfferImage(Long id, MultipartFile file) throws IOException {
        // Utilisation directe du repository pour éviter la confusion avec getPublicOfferById
        JobOffer offer = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offre non trouvée: " + id));

        offer.setImage(file.getBytes());
        offer.setImageContentType(file.getContentType());
        jobOfferRepository.save(offer);
    }

    @Transactional(readOnly = true)
    public byte[] getOfferImage(Long id) {
        JobOffer offer = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offre non trouvée: " + id));
        return offer.getImage();
    }

    @Transactional(readOnly = true)
    public String getOfferImageContentType(Long id) {
        JobOffer offer = jobOfferRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offre non trouvée: " + id));
        return offer.getImageContentType();
    }

    // --- Méthodes utilitaires ---

    private ContractType validateContractType(String type) {
        try {
            return ContractType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Type de contrat invalide: " + type +
                    ". Valeurs possibles: CDI, CDD, STAGE, ALTERNANCE, FREELANCE");
        }
    }

    private OfferStatus validateOfferStatus(String status) {
        try {
            return OfferStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Statut d'offre invalide: " + status +
                    ". Valeurs possibles: PUBLISHED, DRAFT, ARCHIVED");
        }
    }

    private JobOfferResponse convertToResponse(JobOffer offer) {
        String fullName = (offer.getCreatedBy() != null)
                ? offer.getCreatedBy().getFirstName() + " " + offer.getCreatedBy().getLastName()
                : "Inconnu";
        Long createdById = (offer.getCreatedBy() != null) ? offer.getCreatedBy().getId() : null;

        return new JobOfferResponse(
                offer.getId(),
                offer.getTitle(),
                offer.getDescription(),
                offer.getLocation(),
                offer.getDeadline(),
                offer.getContractType().name(),
                offer.getStatus().name(),
                createdById,
                fullName,
                offer.getCreatedAt(),
                offer.getUpdatedAt(),
                offer.getImage() != null // hasImage
        );
    }
}