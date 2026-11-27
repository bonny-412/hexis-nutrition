package com.hexisnutrition.backend.auth;

import com.hexisnutrition.backend.inviti.TipoToken;
import com.hexisnutrition.backend.inviti.TokenAzione;
import com.hexisnutrition.backend.inviti.TokenAzioneRepository;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Regressione del bug 19 del collaudo del 27/11/2026: il reset password deve invalidare i JWT già emessi. */
class SessioneDopoResetCollaudoTest extends AbstractIntegrationTest {

    @Autowired
    private ProfessionistaRepository professionistaRepository;
    @Autowired
    private TokenAzioneRepository tokenAzioneRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private String login(String email, String password) throws Exception {
        String risposta = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(risposta).get("token").asText();
    }

    @Test
    void dopoIlResetIlVecchioTokenRiceve401EIlNuovoLoginFunziona() throws Exception {
        Professionista professionista = professionistaRepository.save(new Professionista(
                "sessione@example.com", passwordEncoder.encode("vecchiaPassword1"), "Anna", "Bianchi"));
        String tokenVecchio = login("sessione@example.com", "vecchiaPassword1");

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + tokenVecchio))
                .andExpect(status().isOk());

        TokenAzione reset = TokenAzione.perProfessionista(TipoToken.RESET_PASSWORD, professionista.getId(),
                Duration.ofHours(1));
        tokenAzioneRepository.save(reset);
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"%s\",\"nuovaPassword\":\"nuovaPassword1\"}".formatted(reset.getToken())))
                .andExpect(status().isNoContent());

        // Il token emesso prima del reset non vale più, su qualunque rotta protetta.
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + tokenVecchio))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/pazienti").header("Authorization", "Bearer " + tokenVecchio))
                .andExpect(status().isUnauthorized());

        // La vecchia password non entra, la nuova sì e il suo token funziona.
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"sessione@example.com\",\"password\":\"vecchiaPassword1\"}"))
                .andExpect(status().isUnauthorized());
        String tokenNuovo = login("sessione@example.com", "nuovaPassword1");
        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + tokenNuovo))
                .andExpect(status().isOk());
    }
}
