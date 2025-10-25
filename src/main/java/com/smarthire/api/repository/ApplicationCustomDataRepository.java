package com.smarthire.api.repository;

import com.smarthire.api.model.ApplicationCustomData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ApplicationCustomDataRepository extends JpaRepository<ApplicationCustomData, Long> {

    // Pour récupérer toutes les réponses personnalisées d'une candidature
    List<ApplicationCustomData> findByApplicationId(Long applicationId);
}