package com.smarthire.api.service;

import com.smarthire.api.dto.CustomFormFieldRequest;
import com.smarthire.api.dto.CustomFormFieldResponse;
import com.smarthire.api.model.CustomFormField;
import com.smarthire.api.model.JobOffer;
import com.smarthire.api.model.User;
import com.smarthire.api.model.enums.FormFieldType;
import com.smarthire.api.repository.CustomFormFieldRepository;
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
public class CustomFormService {

    private final CustomFormFieldRepository customFormFieldRepository;
    private final JobOfferRepository jobOfferRepository;
    private final UserRepository userRepository;

    @Transactional
    public CustomFormFieldResponse createFormField(Long offerId, CustomFormFieldRequest request, String rhEmail) {

        JobOffer jobOffer = jobOfferRepository.findById(offerId)
                .orElseThrow(() -> new EntityNotFoundException("Offre non trouvée."));

        User rhUser = userRepository.findByEmail(rhEmail)
                .orElseThrow(() -> new EntityNotFoundException("Recruteur non trouvé."));

        // Sécurité : Vérifier que le RH est bien le propriétaire de l'offre
        if (!jobOffer.getCreatedBy().equals(rhUser)) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à modifier cette offre.");
        }

        CustomFormField field = CustomFormField.builder()
                .jobOffer(jobOffer)
                .label(request.label())
                .fieldType(FormFieldType.valueOf(request.fieldType().toUpperCase())) // Convertit String en Enum
                .options(request.options())
                .isRequired(request.isRequired())
                .build();

        CustomFormField savedField = customFormFieldRepository.save(field);
        return CustomFormFieldResponse.fromEntity(savedField);
    }

    @Transactional(readOnly = true)
    public List<CustomFormFieldResponse> getFormFieldsForOffer(Long offerId) {
        // Pas besoin de sécurité ici, les candidats doivent voir les champs pour postuler
        if (!jobOfferRepository.existsById(offerId)) {
            throw new EntityNotFoundException("Offre non trouvée.");
        }

        return customFormFieldRepository.findByJobOfferId(offerId).stream()
                .map(CustomFormFieldResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteFormField(Long fieldId, String rhEmail) {

        User rhUser = userRepository.findByEmail(rhEmail)
                .orElseThrow(() -> new EntityNotFoundException("Recruteur non trouvé."));

        CustomFormField field = customFormFieldRepository.findById(fieldId)
                .orElseThrow(() -> new EntityNotFoundException("Champ de formulaire non trouvé."));

        // Sécurité : Vérifier que le RH est bien le propriétaire de l'offre associée à ce champ
        if (!field.getJobOffer().getCreatedBy().equals(rhUser)) {
            throw new AccessDeniedException("Vous n'êtes pas autorisé à supprimer ce champ.");
        }

        customFormFieldRepository.delete(field);
    }
}