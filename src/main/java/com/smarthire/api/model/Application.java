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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applicant_user_id", nullable = false)
    private User applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_offer_id", nullable = false)
    private JobOffer jobOffer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGBLOB")
    private byte[] cvData;

    @Column(nullable = false)
    private String cvFileName;

    @Column(nullable = false)
    private String cvFileType;

    @CreationTimestamp
    private Instant appliedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<ApplicationCustomData> customData = new HashSet<>();

    // NOUVEAUX CHAMPS
    @Column(nullable = true)
    private Integer cvScore; // Note sur 10, 100, etc. ou null si non noté

    @Column(nullable = true, length = 255) // Message optionnel pour le candidat
    private String candidateMessage;

    @Lob
    @Column(nullable = true, columnDefinition = "TEXT")
    private String internalNotes; // Notes privées pour le RH
}