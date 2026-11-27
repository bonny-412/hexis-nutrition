package com.hexisnutrition.backend.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final VerificaSessione verificaSessione;

    public JwtAuthenticationFilter(JwtService jwtService, VerificaSessione verificaSessione) {
        this.jwtService = jwtService;
        this.verificaSessione = verificaSessione;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                var claims = jwtService.parseToken(token).getPayload();
                UUID userId = UUID.fromString(claims.getSubject());
                String ruolo = claims.get("ruolo", String.class);
                // Token emessi prima di questa versione non hanno il claim: valgono come versione 0.
                Integer versione = claims.get(JwtService.CLAIM_VERSIONE_SESSIONE, Integer.class);
                if (!verificaSessione.valida(userId, Ruolo.valueOf(ruolo), versione == null ? 0 : versione)) {
                    // Password reimpostata dopo l'emissione del token (o utente inesistente): non autenticato, 401.
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + ruolo));
                var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
