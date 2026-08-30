package com.hexisnutrition.backend.auth;

import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityConfigTest extends AbstractIntegrationTest {

    @Test
    void richiestaSenzaTokenSuEndpointProtettoRestituisce401() throws Exception {
        mockMvc.perform(get("/pazienti"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void richiestaSuEndpointPubblicoNonVieneBloccataDallaSicurezza() throws Exception {
        var risultato = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();

        assertThat(risultato.getResponse().getStatus()).isNotEqualTo(401);
    }

    @Test
    void richiestaPreflightDaOrigineFrontendRicevaAccessControlAllowOrigin() throws Exception {
        mockMvc.perform(options("/auth/login")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));
    }
}
