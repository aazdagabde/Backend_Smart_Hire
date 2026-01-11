package com.smarthire.api.service;

import com.smarthire.api.dto.DashboardStatsDTO;
import com.smarthire.api.model.User;
import com.smarthire.api.model.enums.ApplicationStatus;
import com.smarthire.api.model.enums.OfferStatus;
import com.smarthire.api.repository.ApplicationRepository;
import com.smarthire.api.repository.JobOfferRepository;
import com.smarthire.api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final JobOfferRepository jobOfferRepository;
    private final ApplicationRepository applicationRepository;

    public DashboardStatsDTO getStatsForUser(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));

        // Vérifier le rôle
        boolean isCandidate = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals("ROLE_CANDIDAT"));

        if (isCandidate) {
            // --- STATS POUR LE CANDIDAT ---
            return DashboardStatsDTO.builder()
                    // Pour un candidat, "Total Users" n'a pas de sens => on affiche ses Entretiens/Acceptés
                    .totalUsers(applicationRepository.countByApplicantIdAndStatus(user.getId(), ApplicationStatus.ACCEPTED))
                    // Offres actives sur le marché (reste global)
                    .activeOffers(jobOfferRepository.countByStatus(OfferStatus.PUBLISHED))
                    // SES candidatures
                    .totalApplications(applicationRepository.countByApplicantId(user.getId()))
                    // SES candidatures en attente
                    .pendingApplications(applicationRepository.countByApplicantIdAndStatus(user.getId(), ApplicationStatus.PENDING))
                    .build();
        } else {
            // --- STATS POUR LE RH / ADMIN (Globales) ---
            return DashboardStatsDTO.builder()
                    .totalUsers(userRepository.count())
                    .activeOffers(jobOfferRepository.countByStatus(OfferStatus.PUBLISHED))
                    .totalApplications(applicationRepository.count())
                    .pendingApplications(applicationRepository.countByStatus(ApplicationStatus.PENDING))
                    .build();
        }
    }
}