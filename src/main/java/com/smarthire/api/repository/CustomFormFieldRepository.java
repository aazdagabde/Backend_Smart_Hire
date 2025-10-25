package com.smarthire.api.repository;

import com.smarthire.api.model.CustomFormField;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomFormFieldRepository extends JpaRepository<CustomFormField, Long> {

    // Pour récupérer tous les champs d'un formulaire pour une offre
    List<CustomFormField> findByJobOfferId(Long jobOfferId);
}