package com.hexisnutrition.backend.auth;

import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String CHIAVE_UNO = "0123456789abcdef0123456789abcdef0123456789abcdef";
    private static final String CHIAVE_DUE = "fedcba9876543210fedcba9876543210fedcba9876543210";

    @Test
    void generaEValidaUnTokenConRuolo() {
        JwtService jwtService = new JwtService(CHIAVE_UNO, 60);
        UUID userId = UUID.randomUUID();

        String token = jwtService.generateToken(userId, Ruolo.PROFESSIONISTA);
        var claims = jwtService.parseToken(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo(userId.toString());
        assertThat(claims.get("ruolo", String.class)).isEqualTo("PROFESSIONISTA");
    }

    @Test
    void rifiutaUnTokenFirmatoConUnaChiaveDiversa() {
        JwtService emittente = new JwtService(CHIAVE_UNO, 60);
        JwtService verificatore = new JwtService(CHIAVE_DUE, 60);

        String token = emittente.generateToken(UUID.randomUUID(), Ruolo.PAZIENTE);

        assertThatThrownBy(() -> verificatore.parseToken(token))
                .isInstanceOf(SignatureException.class);
    }
}
