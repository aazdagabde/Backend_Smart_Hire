package com.smarthire.api.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication; // IMPORT REQUIS
import org.springframework.security.core.GrantedAuthority; // IMPORT REQUIS
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

import java.security.Key;
import java.util.Date;
import java.util.List; // IMPORT REQUIS
import java.util.stream.Collectors; // IMPORT REQUIS

@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expirationMs}")
    private int expMs;

    // INJECTION DE LA PROPRIÉTÉ D'AUDIENCE
    @Value("${jwt.audience}")
    private String jwtAudience;

    private Key key;

    @PostConstruct
    public void init() {
        // La correction de l'erreur "IllegalArgumentException" de l'étape précédente
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    /**
     * CORRECTION DE LA SIGNATURE :
     * Accepte 'Authentication' au lieu de 'String'
     */
    public String generateToken(Authentication authentication) {
        String email = authentication.getName();
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expMs);

        // Récupérer les rôles
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .setSubject(email)
                .claim("roles", roles) // Ajout des rôles
                .setIssuedAt(now)
                .setExpiration(expiry)
                .setAudience(jwtAudience) // Ajout de l'audience
                .signWith(key, SignatureAlgorithm.HS512)
                .compact();
    }

    /**
     * CORRECTION : Ajout de la vérification de l'audience
     */
    public String getUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .requireAudience(jwtAudience) // VÉRIFICATION
                .build()
                .parseClaimsJws(token).getBody()
                .getSubject();
    }

    /**
     * CORRECTION : Ajout de la vérification de l'audience
     */
    public boolean validate(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .requireAudience(jwtAudience) // VÉRIFICATION
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            // Le token n'est pas valide
        }
        return false;
    }
}