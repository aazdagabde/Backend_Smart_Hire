package com.smarthire.api.model;
import jakarta.persistence.*;
import lombok.Data; // Ou @Getter, @Setter, @NoArgsConstructor, @AllArgsConstructor
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

@Data // Génère getters, setters, toString, equals, hashCode
@Builder // Un pattern de design utile pour créer des objets
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class Role {
    String Name ;
    public String getName() {
        return Name;
    }
}
