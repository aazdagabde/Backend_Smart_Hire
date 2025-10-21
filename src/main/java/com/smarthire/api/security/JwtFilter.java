package com.smarthire.api.security;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;
    private static final AntPathMatcher matcher = new AntPathMatcher();

    public JwtFilter(JwtProvider jwtProvider,
                     CustomUserDetailsService userDetailsService) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
    }

    /**
     * N'exécute pas le filtre JWT pour les routes publiques (inscription et connexion).
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String path = req.getServletPath();
        // Ne filtre pas les routes commençant par /api/auth/
        return matcher.match("/api/auth/**", path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 1. Récupérer le header "Authorization"
        String h = req.getHeader("Authorization");

        // 2. Vérifier s'il est présent et s'il commence par "Bearer "
        if (h != null && h.startsWith("Bearer ")) {
            String token = h.substring(7); // Extrait le token

            // 3. Valider le token
            if (jwtProvider.validate(token)) {

                // 4. Extraire le username (qui est l'email) du token
                String userEmail = jwtProvider.getUsername(token);

                // 5. Charger les détails de l'utilisateur depuis la BDD
                UserDetails ud = userDetailsService.loadUserByUsername(userEmail);

                // 6. Créer l'objet d'authentification
                var auth = new UsernamePasswordAuthenticationToken(
                        ud, null, ud.getAuthorities());

                // 7. Placer l'authentification dans le contexte de sécurité de Spring
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // 8. Passer la requête au filtre suivant
        chain.doFilter(req, res);
    }
}