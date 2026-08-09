package com.hexisnutrition.backend.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * Il database di test è locale e persistente, non un container usa e getta:
     * va svuotato prima di ogni test, altrimenti i dati lasciati da un'esecuzione
     * interrotta a metà fanno fallire quella successiva.
     */
    @BeforeEach
    void svuotaDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE token_azione, pazienti, professionisti RESTART IDENTITY CASCADE");
    }
}
