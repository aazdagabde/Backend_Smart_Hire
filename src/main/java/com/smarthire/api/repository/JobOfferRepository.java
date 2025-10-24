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

    // Trouver toutes les offres par statut (pour la vue publique)
    List<JobOffer> findByStatus(OfferStatus status);

    // Trouver toutes les offres créées par un utilisateur spécifique (pour le tableau de bord RH)
    List<JobOffer> findByCreatedById(Long userId);

    // NOUVELLE MÉTHODE (CORRIGÉE)
    // Nous allons passer un terme de recherche déjà en minuscules et avec des wildcards
    // La recherche sur 'description' (TEXT/CLOB) n'utilise pas LOWER()
    // La plupart des collations MySQL par défaut sont insensibles à la casse de toute façon.
    @Query("SELECT o FROM JobOffer o WHERE o.status = :status AND " +
            "(LOWER(o.title) LIKE :processedSearchTerm OR " +
            "o.description LIKE :processedSearchTerm OR " +
            "LOWER(o.location) LIKE :processedSearchTerm)")
    List<JobOffer> findPublishedOffersBySearchTerm(
            @Param("status") OfferStatus status,
            @Param("processedSearchTerm") String processedSearchTerm // Le paramètre est renommé pour plus de clarté
    );
}