package com.smarthire.api.repository;

import com.smarthire.api.model.Application;
import com.smarthire.api.model.enums.ApplicationStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    Optional<Application> findByApplicantIdAndJobOfferId(Long applicantId, Long jobOfferId);
    List<Application> findByApplicantId(Long applicantId);
    List<Application> findByJobOfferId(Long jobOfferId);

    // Stats Globales (Pour RH/Admin)
    long countByStatus(ApplicationStatus status);

    // --- AJOUTS POUR LE CANDIDAT ---
    // Compter MES candidatures
    long countByApplicantId(Long applicantId);

    // Compter MES candidatures par statut (ex: combien sont en attente ?)
    long countByApplicantIdAndStatus(Long applicantId, ApplicationStatus status);

    @Query("SELECT a FROM Application a WHERE a.jobOffer.id = :offerId ORDER BY a.cvScore DESC NULLS LAST")
    List<Application> findTopByOfferIdOrderByCvScoreDesc(Long offerId, PageRequest pageable);
}