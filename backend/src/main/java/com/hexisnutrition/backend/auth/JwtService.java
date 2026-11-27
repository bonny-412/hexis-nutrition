package com.hexisnutrition.backend.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

public class JwtService {

    private final SecretKey key;
    private final long expirationMinutes;

    public JwtService(String secret, long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;
    }

    /** Claim con la versione di sessione dell'utente: un token con versione diversa da quella nel DB non vale più. */
    public static final String CLAIM_VERSIONE_SESSIONE = "sv";

    /** Token con versione di sessione 0, quella di un utente che non ha mai reimpostato la password. */
    public String generateToken(UUID userId, Ruolo ruolo) {
        return generateToken(userId, ruolo, 0);
    }

    public String generateToken(UUID userId, Ruolo ruolo, int versioneSessione) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("ruolo", ruolo.name())
                .claim(CLAIM_VERSIONE_SESSIONE, versioneSessione)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expirationMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> parseToken(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }
}
