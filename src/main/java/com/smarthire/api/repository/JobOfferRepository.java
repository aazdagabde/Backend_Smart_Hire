package com.smarthire.api.repository;

import com.smarthire.api.model.JobOffer;
import com.smarthire.api.model.enums.OfferStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JobOfferRepository extends JpaRepository<JobOffer, Long> {

    // Trouver toutes les offres par statut (pour la vue publique)
    List<JobOffer> findByStatus(OfferStatus status);

    // Trouver toutes les offres créées par un utilisateur spécifique (pour le tableau de bord RH)
    List<JobOffer> findByCreatedById(Long userId);
}