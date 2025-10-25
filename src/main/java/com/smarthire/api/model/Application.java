package com.smarthire.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smarthire.api.model.enums.ApplicationStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Le candidat qui postule
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_user_id", nullable = false)
    private User applicant;

    // L'offre à laquelle il postule
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_offer_id", nullable = false)
    private JobOffer jobOffer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    // Stockage du fichier CV directement en BDD
    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] cvData;

    @Column(nullable = false)
    private String cvFileName;

    @Column(nullable = false)
    private String cvFileType; // ex: "application/pdf"

    @CreationTimestamp
    private Instant appliedAt;

    // NOUVELLE MODIFICATION : Une candidature peut avoir plusieurs données personnalisées
    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<ApplicationCustomData> customData = new HashSet<>();
}