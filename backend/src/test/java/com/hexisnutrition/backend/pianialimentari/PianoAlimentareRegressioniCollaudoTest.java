package com.hexisnutrition.backend.pianialimentari;

import com.hexisnutrition.backend.auth.JwtService;
import com.hexisnutrition.backend.auth.Ruolo;
import com.hexisnutrition.backend.pazienti.ObiettivoVisita;
import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.Sesso;
import com.hexisnutrition.backend.pazienti.StileDiVita;
import com.hexisnutrition.backend.pazienti.Visita;
import com.hexisnutrition.backend.pazienti.VisitaRepository;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.hexisnutrition.backend.support.Concorrenza.inParallelo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Regressioni del collaudo del 27/11/2026 sui piani: 18, 21 (gruppo 1), 10, 07 (gruppo 4), ruolo sulle rotte (gruppo 2). */
class PianoAlimentareRegressioniCollaudoTest extends AbstractIntegrationTest {

    @Autowired
    private PazienteRepository pazienteRepository;
    @Autowired
    private VisitaRepository visitaRepository;
    @Autowired
    private ProfessionistaRepository professionistaRepository;
    @Autowired
    private PianoAlimentareRepository pianoAlimentareRepository;
    @Autowired
    private JwtService jwtService;

    private String token;
    private Paziente paziente;

    @BeforeEach
    void creaProfessionistaEPaziente() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-collaudo@test.it", "$2a$10$hash", "Anna", "Rossi"));
        token = jwtService.generateToken(professionista.getId(), Ruolo.PROFESSIONISTA);
        paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Mario", "Bianchi",
                "BNCMRA80A01H501U", "mario@test.it", "333", LocalDate.of(1990, 1, 1), Sesso.M,
                null, StileDiVita.ATTIVO, null));
        visitaRepository.save(new Visita(paziente.getId(), LocalDate.now(), 178, BigDecimal.valueOf(78),
                null, null, null, null, null, null, null, null, null,
                ObiettivoVisita.IPERTROFIA));
    }

    private UUID creaBozza(String nome) throws Exception {
        String risposta = mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pazienteId":"%s","nome":"%s","modalita":"PASTI"}
                                """.formatted(paziente.getId(), nome)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(risposta).get("id").asText());
    }

    /** Corpo minimo di un PUT in modalità PASTI, con un pasto e una riga dai grammi indicati. */
    private String corpoAggiornamento(String nome, String dataFine, String obiettivoKcal, String grammi, Long versione) {
        return """
                {
                  "nome": "%s",
                  "dataFine": %s,
                  "obiettivoKcal": %s,
                  "versione": %s,
                  "pasti": [
                    {"giornoSettimana": "LUNEDI", "nome": "Colazione", "tipo": "COLAZIONE", "righe": [
                      {"nome": "Avena", "kcal100g": 372, "proteine100g": 12.9, "carboidrati100g": 65,
                       "grassi100g": 6.5, "grammi": %s}
                    ]}
                  ]
                }
                """.formatted(nome, dataFine == null ? "null" : "\"" + dataFine + "\"", obiettivoKcal,
                versione == null ? "null" : versione, grammi);
    }

    // --- 18: due schede sulla stessa bozza ---

    @Test
    void secondoSalvataggioConVersioneVecchiaRicevePianoModificatoAltroveENonSovrascrive() throws Exception {
        UUID pianoId = creaBozza("Bozza");

        mockMvc.perform(put("/piani-alimentari/" + pianoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAggiornamento("Scheda A", null, "2000", "60", 0L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.versione", is(1)));

        // La scheda B aveva letto la versione 0: il suo salvataggio non deve passare.
        mockMvc.perform(put("/piani-alimentari/" + pianoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAggiornamento("Scheda B", null, "1800", "80", 0L)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codice", is("PIANO_MODIFICATO_ALTROVE")));

        mockMvc.perform(get("/piani-alimentari/" + pianoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", is("Scheda A")))
                .andExpect(jsonPath("$.obiettivoKcal").value(2000))
                .andExpect(jsonPath("$.versione", is(1)));
    }

    // --- 21: due attivazioni simultanee ---

    @RepeatedTest(5)
    void dueAttivazioniSimultaneeLascianoUnSoloPianoAttivo() throws Exception {
        UUID primo = creaBozza("Primo");
        UUID secondo = creaBozza("Secondo");

        List<Integer> esiti = inParallelo(
                () -> mockMvc.perform(post("/piani-alimentari/" + primo + "/attiva")
                        .header("Authorization", "Bearer " + token)).andReturn().getResponse().getStatus(),
                () -> mockMvc.perform(post("/piani-alimentari/" + secondo + "/attiva")
                        .header("Authorization", "Bearer " + token)).andReturn().getResponse().getStatus());

        // Serializzate: entrambe 204 (la seconda conclude la prima). In gara: una 204 e una 409. Mai 500.
        assertThat(esiti).allMatch(s -> s == 204 || s == 409);
        assertThat(esiti).contains(204);
        long attivi = pianoAlimentareRepository.findAll().stream()
                .filter(p -> p.getPazienteId().equals(paziente.getId()) && p.getStato() == StatoPiano.ATTIVO)
                .count();
        assertThat(attivi).isEqualTo(1);
    }

    // --- 10: intervallo invertito e obiettivo calorico non positivo ---

    @Test
    void dataFinePrimaDellInizioRisponde400() throws Exception {
        UUID pianoId = creaBozza("Bozza");

        mockMvc.perform(put("/piani-alimentari/" + pianoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAggiornamento("Bozza", LocalDate.now().minusDays(7).toString(), "2000", "60", null)))
                .andExpect(status().isBadRequest());

        assertThat(pianoAlimentareRepository.findById(pianoId).orElseThrow().getDataFine()).isNull();
    }

    @Test
    void dataFineUgualeAllInizioEAccettata() throws Exception {
        UUID pianoId = creaBozza("Bozza");

        mockMvc.perform(put("/piani-alimentari/" + pianoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAggiornamento("Bozza", LocalDate.now().toString(), "2000", "60", null)))
                .andExpect(status().isOk());
    }

    @Test
    void obiettivoCaloricoNegativoOZeroRisponde400() throws Exception {
        UUID pianoId = creaBozza("Bozza");

        for (String obiettivo : List.of("-5", "0")) {
            mockMvc.perform(put("/piani-alimentari/" + pianoId)
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoAggiornamento("Bozza", null, obiettivo, "60", null)))
                    .andExpect(status().isBadRequest());
        }
    }

    // --- 07: porzione zero ---

    @Test
    void rigaConPorzioneZeroRisponde400() throws Exception {
        UUID pianoId = creaBozza("Bozza");

        mockMvc.perform(put("/piani-alimentari/" + pianoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAggiornamento("Bozza", null, "2000", "0", null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nomeDelPianoOltre200CaratteriRisponde400() throws Exception {
        UUID pianoId = creaBozza("Bozza");

        mockMvc.perform(put("/piani-alimentari/" + pianoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoAggiornamento("x".repeat(201), null, "2000", "60", null)))
                .andExpect(status().isBadRequest());
    }

    // --- Ruolo sulle rotte dei piani ---

    @Test
    void unPazienteRiceve403SuiPiani() throws Exception {
        UUID pianoId = creaBozza("Bozza");
        String tokenPaziente = jwtService.generateToken(paziente.getId(), Ruolo.PAZIENTE);

        mockMvc.perform(get("/piani-alimentari/ricerca").header("Authorization", "Bearer " + tokenPaziente))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/piani-alimentari/" + pianoId).header("Authorization", "Bearer " + tokenPaziente))
                .andExpect(status().isForbidden());
    }
}
