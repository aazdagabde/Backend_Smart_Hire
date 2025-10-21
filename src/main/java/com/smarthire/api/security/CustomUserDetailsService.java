package com.smarthire.api.security;

import com.smarthire.api.model.User;
import com.smarthire.api.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository users;

    public CustomUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {
        User user = users.findByEmail(username)
                .orElseThrow(() ->
                        new UsernameNotFoundException("Utilisateur introuvable"));
        // Retourne spring UserDetails, avec login/password/roles
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(user.getRoles().stream()
                        .map(r -> r.getName())
                        .toArray(String[]::new))
                .build();
    }
}
