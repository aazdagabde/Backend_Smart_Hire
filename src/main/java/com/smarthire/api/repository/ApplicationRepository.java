package com.smarthire.api.repository;

import com.smarthire.api.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<Application, Long> {

    // Pour vérifier si un candidat a déjà postulé à une offre
    Optional<Application> findByApplicantIdAndJobOfferId(Long applicantId, Long jobOfferId);

    // Pour la vue "Mes candidatures" du candidat
    List<Application> findByApplicantId(Long applicantId);

    // Pour la vue RH "Voir les candidats" d'une offre spécifique
    List<Application> findByJobOfferId(Long jobOfferId);
}