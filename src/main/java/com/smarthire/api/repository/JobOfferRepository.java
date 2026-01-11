package com.smarthire.api.repository;

import com.smarthire.api.model.JobOffer;
import com.smarthire.api.model.enums.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {

    // Trouver toutes les offres par statut
    List<JobOffer> findByStatus(OfferStatus status);

    // Trouver toutes les offres créées par un utilisateur
    List<JobOffer> findByCreatedById(Long userId);

    // Recherche avancée
    @Query("SELECT o FROM JobOffer o WHERE o.status = :status AND " +
            "(LOWER(o.title) LIKE :processedSearchTerm OR " +
            "o.description LIKE :processedSearchTerm OR " +
            "LOWER(o.location) LIKE :processedSearchTerm)")
    List<JobOffer> findPublishedOffersBySearchTerm(
            @Param("status") OfferStatus status,
            @Param("processedSearchTerm") String processedSearchTerm
    );

    // --- POUR LE DASHBOARD ---
    long countByStatus(OfferStatus status);
}