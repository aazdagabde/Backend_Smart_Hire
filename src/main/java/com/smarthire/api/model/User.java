package com.smarthire.api.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data; // Ou @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Data // Génère getters, setters, toString, equals, hashCode
@Builder // Un pattern de design utile pour créer des objets
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users") // "user" est souvent un mot-clé réservé en SQL
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String firstName;
    private String lastName;

    @Column(unique = true)
    private String email;

    private String password;

    @ManyToMany(fetch = FetchType.EAGER) // Charger les rôles immédiatement
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    // --- AJOUTER CETTE SECTION ---
    // Relation: Un utilisateur (RH) peut créer plusieurs offres
    @OneToMany(
            mappedBy = "createdBy", // "createdBy" est le nom du champ dans JobOffer.java
            cascade = CascadeType.ALL, // Si on supprime un User, ses offres sont supprimées
            orphanRemoval = true
    )
    @com.fasterxml.jackson.annotation.JsonIgnore // Évite les boucles infinies lors de la sérialisation
    private Set<JobOffer> jobOffers = new java.util.HashSet<>();
    // NOUVELLE RELATION : Un utilisateur (Candidat) peut avoir plusieurs candidatures
    @OneToMany(mappedBy = "applicant", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonIgnore // Important pour éviter les boucles JSON
    private Set<Application> applications = new HashSet<>();
    // --- FIN DE L'AJOUT ---

    // --- Méthodes de UserDetails ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Transforme notre Set<Role> en une collection de SimpleGrantedAuthority
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
        // Notre "username" est l'email
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true; // Par défaut, le compte n'expire pas
    }

    @Override
    public boolean isAccountNonLocked() {
        return true; // Par défaut, le compte n'est pas verrouillé
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true; // Par défaut, les identifiants n'expirent pas
    }

    @Override
    public boolean isEnabled() {
        return true; // Par défaut, le compte est activé
    }
}