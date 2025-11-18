package com.smarthire.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// NOUVEL IMPORT NÉCESSAIRE POUR FetchType.LAZY
import jakarta.persistence.FetchType;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;

    @Column(unique = true)
    private String email;

    private String password;

    @Column(nullable = true)
    private String phoneNumber;

    // --- SECTION PHOTO DE PROFIL (MODIFIÉE ET CORRIGÉE) ---

    /**
     * Les données binaires de l'image.
     * @Lob - Indique que c'est un Large Object.
     * @Basic(fetch = FetchType.LAZY) - TRÈS IMPORTANT: Pour la performance.
     * Ne charge pas l'image de la BDD sauf si on appelle user.getProfilePicture().
     * @Column(...) - Identique à votre configuration pour les CV.
     * Doit être 'nullable' car un utilisateur peut ne pas avoir de photo.
     */
    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(nullable = true, columnDefinition = "LONGBLOB")
    private byte[] profilePicture; // Nom corrigé

    /**
     * Le type MIME de l'image (ex: "image/jpeg" ou "image/png").
     * Nécessaire pour que le navigateur sache comment afficher l'image.
     */
    @Column(nullable = true)
    private String profilePictureType; // Nom corrigé

    // --- FIN SECTION PHOTO DE PROFIL ---


    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    @OneToMany(
            mappedBy = "createdBy",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JsonIgnore
    private Set<JobOffer> jobOffers = new HashSet<>();

    @OneToMany(mappedBy = "applicant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore
    private Set<Application> applications = new HashSet<>();


    // --- Méthodes de UserDetails ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}