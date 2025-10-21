package com.smarthire.api.security;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;          // ← ajouté
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;
    private static final AntPathMatcher matcher = new AntPathMatcher(); // ← ajouté

    public JwtFilter(JwtProvider jwtProvider,
                     CustomUserDetailsService userDetailsService) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
    }

    /** Skip JWT logic on public endpoints */
    @Override                                           // ← ajouté
    protected boolean shouldNotFilter(HttpServletRequest req) {
        String path = req.getServletPath();
        return matcher.match("/api/device/**", path) || // toutes les routes 'device'
                matcher.match("/api/auth/**", path) ||  // login, register
                matcher.match("/api/measurements", path); // mesures IoT
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain)
            throws ServletException, IOException {

        String h = req.getHeader("Authorization");
        if (h != null && h.startsWith("Bearer ")) {
            String token = h.substring(7);
            if (jwtProvider.validate(token)) {
                String user = jwtProvider.getUsername(token);
                UserDetails ud = userDetailsService.loadUserByUsername(user);
                var auth = new UsernamePasswordAuthenticationToken(
                        ud, null, ud.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }
        chain.doFilter(req, res);
    }
}
