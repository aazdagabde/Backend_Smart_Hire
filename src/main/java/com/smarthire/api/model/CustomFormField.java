package com.smarthire.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smarthire.api.model.enums.FormFieldType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "custom_form_fields")
public class CustomFormField {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_offer_id", nullable = false)
    @JsonIgnore // Important pour éviter les boucles
    private JobOffer jobOffer;

    @Column(nullable = false)
    private String label; // La question, ex: "Avez-vous le permis B ?"

    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false)
    private FormFieldType fieldType;

    @Column(nullable = true) // Null si TEXT ou TEXTAREA
    private String options; // Options pour RADIO/CHECKBOX, séparées par ";"

    @Column(name = "is_required", nullable = false)
    private boolean isRequired;
}