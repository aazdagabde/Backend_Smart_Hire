package com.smarthire.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthire.api.dto.ApplicationCustomDataResponse;
import com.smarthire.api.dto.ApplicationRequestData;
import com.smarthire.api.dto.ApplicationResponse;
import com.smarthire.api.model.Application;
import com.smarthire.api.model.ApplicationCustomData;
import com.smarthire.api.model.CustomFormField;
import com.smarthire.api.model.JobOffer;
import com.smarthire.api.model.User;
import com.smarthire.api.model.enums.ApplicationStatus;
import com.smarthire.api.model.enums.OfferStatus;
import com.smarthire.api.repository.ApplicationCustomDataRepository;
import com.smarthire.api.repository.ApplicationRepository;
import com.smarthire.api.repository.CustomFormFieldRepository;
import com.smarthire.api.repository.JobOfferRepository;
import com.smarthire.api.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final JobOfferRepository jobOfferRepository;

    // NOUVELLES INJECTIONS
    private final ApplicationCustomDataRepository applicationCustomDataRepository;
    private final CustomFormFieldRepository customFormFieldRepository;
    private final ObjectMapper objectMapper; // Pour parser le JSON

    private final long MAX_CV_SIZE = 5 * 1024 * 1024; // 5 MB

    // 1. POSTULER À UNE OFFRE (MODIFIÉ)
    @Transactional
    public ApplicationResponse applyToOffer(Long offerId, MultipartFile cvFile, String customDataJson, String candidateEmail) throws IOException, JsonProcessingException {

        // --- Validation du fichier ---
        if (cvFile.isEmpty()) {
            throw new IllegalArgumentException("Le fichier CV ne peut pas être vide.");
        }
        if (!Objects.equals(cvFile.getContentType(), "application/pdf")) {
            throw new IllegalArgumentException("Le CV doit être au format PDF.");
        }
        if (cvFile.getSize() > MAX_CV_SIZE) {
            throw new IllegalArgumentException("Le fichier CV ne doit pas dépasser 5MB.");
        }

        // --- Récupération des entités ---
        User candidate = userRepository.findByEmail(candidateEmail)
                .orElseThrow(() -> new EntityNotFoundException("Candidat non trouvé."));

        JobOffer jobOffer = jobOfferRepository.findById(offerId)
                .orElseThrow(() -> new EntityNotFoundException("Offre non trouvée."));

        // --- Vérifications métier ---
        if (jobOffer.getStatus() != OfferStatus.PUBLISHED) {
            throw new AccessDeniedException("Vous ne pouvez pas postuler à une offre non publiée.");
        }

        applicationRepository.findByApplicantIdAndJobOfferId(candidate.getId(), jobOffer.getId())
                .ifPresent(app -> {
                    throw new IllegalArgumentException("Vous avez déjà postulé à cette offre.");
                });

        // --- Création de l'entité Application (Etape 1) ---
        String fileName = StringUtils.cleanPath(Objects.requireNonNull(cvFile.getOriginalFilename()));

        Application application = Application.builder()
                .applicant(candidate)
                .jobOffer(jobOffer)
                .status(ApplicationStatus.PENDING)
                .cvData(cvFile.getBytes())
                .cvFileName(fileName)
                .cvFileType(cvFile.getContentType())
                .build();

        Application savedApplication = applicationRepository.save(application);

        // --- NOUVELLE PARTIE : Sauvegarde des données personnalisées (Etape 2) ---
        if (customDataJson != null && !customDataJson.isEmpty()) {
            // Désérialiser la chaîne JSON en une liste de réponses
            List<ApplicationRequestData> customDataList = objectMapper.readValue(customDataJson, new TypeReference<List<ApplicationRequestData>>() {});

            for (ApplicationRequestData data : customDataList) {
                // Récupérer le champ de formulaire correspondant
                CustomFormField field = customFormFieldRepository.findById(data.getFieldId())
                        .orElseThrow(() -> new EntityNotFoundException("Champ de formulaire non trouvé : " + data.getFieldId()));

                // (Optionnel) Vérifier que le champ appartient bien à l'offre
                if (!field.getJobOffer().equals(jobOffer)) {
                    throw new AccessDeniedException("Tentative de soumission de données pour un champ n'appartenant pas à cette offre.");
                }

                // Créer et sauvegarder la réponse
                ApplicationCustomData dataEntry = ApplicationCustomData.builder()
                        .application(savedApplication)
                        .customFormField(field)
                        .value(data.getValue())
                        .build();

                applicationCustomDataRepository.save(dataEntry);
            }
        }
        // --- FIN DE LA NOUVELLE PARTIE ---

        return ApplicationResponse.fromEntity(savedApplication);
    }

    // 2. VOIR LES CANDIDATURES POUR UN CANDIDAT (Tableau de bord Candidat)
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsForCandidate(String candidateEmail) {
        User candidate = userRepository.findByEmail(candidateEmail)
                .orElseThrow(() -> new EntityNotFoundException("Candidat non trouvé."));

        return applicationRepository.findByApplicantId(candidate.getId()).stream()
                .map(ApplicationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // 3. VOIR LES CANDIDATURES POUR UNE OFFRE (Tableau de bord RH)
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsForOffer(Long offerId, String rhEmail) {
        User rhUser = userRepository.findByEmail(rhEmail)
                .orElseThrow(() -> new EntityNotFoundException("Recruteur non trouvé."));

        JobOffer jobOffer = jobOfferRepository.findById(offerId)
                .orElseThrow(() -> new EntityNotFoundException("Offre non trouvée."));

        // Sécurité : Vérifier que le RH est bien le propriétaire de l'offre
        if (!jobOffer.getCreatedBy().equals(rhUser)) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à voir les candidatures de cette offre.");
        }

        return applicationRepository.findByJobOfferId(jobOffer.getId()).stream()
                .map(ApplicationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // 4. RÉCUPÉRER UN CV SPÉCIFIQUE (Pour téléchargement)
    @Transactional(readOnly = true)
    public Application getApplicationCv(Long applicationId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new EntityNotFoundException("Utilisateur non trouvé."));

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Candidature non trouvée."));

        // Sécurité : L'utilisateur doit être soit le candidat, soit le RH propriétaire de l'offre
        boolean isApplicant = application.getApplicant().equals(user);
        boolean isRecruiter = application.getJobOffer().getCreatedBy().equals(user);

        if (!isApplicant && !isRecruiter) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à accéder à ce CV.");
        }

        return application;
    }

    // 5. METTRE À JOUR LE CV D'UNE CANDIDATURE
    @Transactional
    public ApplicationResponse updateApplicationCv(Long applicationId, MultipartFile cvFile, String candidateEmail) throws IOException {

        if (cvFile.isEmpty()) {
            throw new IllegalArgumentException("Le fichier CV ne peut pas être vide.");
        }
        if (!Objects.equals(cvFile.getContentType(), "application/pdf")) {
            throw new IllegalArgumentException("Le CV doit être au format PDF.");
        }
        if (cvFile.getSize() > MAX_CV_SIZE) {
            throw new IllegalArgumentException("Le fichier CV ne doit pas dépasser 5MB.");
        }

        User candidate = userRepository.findByEmail(candidateEmail)
                .orElseThrow(() -> new EntityNotFoundException("Candidat non trouvé."));

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new EntityNotFoundException("Candidature non trouvée."));

        if (!application.getApplicant().equals(candidate)) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à modifier cette candidature.");
        }

        String fileName = StringUtils.cleanPath(Objects.requireNonNull(cvFile.getOriginalFilename()));

        application.setCvData(cvFile.getBytes());
        application.setCvFileName(fileName);
        application.setCvFileType(cvFile.getContentType());

        Application savedApplication = applicationRepository.save(application);
        return ApplicationResponse.fromEntity(savedApplication);
    }

    // NOUVELLE MÉTHODE
    // 6. RÉCUPÉRER LES RÉPONSES PERSONNALISÉES D'UNE CANDIDATURE
    @Transactional(readOnly = true)
    public List<ApplicationCustomDataResponse> getApplicationCustomData(Long applicationId, String userEmail) {

        // On réutilise la logique de sécurité de 'getApplicationCv'
        // Si l'utilisateur n'est ni le candidat ni le RH de l'offre, cela lèvera une exception
        getApplicationCv(applicationId, userEmail);

        // Si la sécurité est passée, on récupère les données
        return applicationCustomDataRepository.findByApplicationId(applicationId).stream()
                .map(ApplicationCustomDataResponse::fromEntity)
                .collect(Collectors.toList());
    }
}