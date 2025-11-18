package com.smarthire.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smarthire.api.model.enums.ContractType;
import com.smarthire.api.model.enums.OfferStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job_offers")
public class JobOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Lob // Large Object, pour les descriptions longues
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractType contractType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OfferStatus status;

    // Relation: Quel utilisateur (RH) a créé l'offre
    // <<< MODIFICATION ICI : EAGER pour résoudre potentiel problème de chargement lors de la vérification >>>
    @ManyToOne(fetch = FetchType.EAGER) // Charger l'utilisateur immédiatement
    @JoinColumn(name = "user_id", nullable = false)
    private User createdBy;

    // RELATION EXISTANTE : Une offre peut avoir plusieurs candidatures
    @OneToMany(mappedBy = "jobOffer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // Important pour éviter les boucles JSON
    private Set<Application> applications = new HashSet<>();

    // (Assurez-vous que l'entité CustomFormField est bien créée)
    @OneToMany(mappedBy = "jobOffer", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<CustomFormField> customFormFields = new HashSet<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;
}