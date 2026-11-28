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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regole di attivazione decise da Andrea il 28/11/2026: si attiva solo un piano con la data di fine, un piano
 * attivo non può perderla, e il piano attivo precedente si conclude con la data di fine di oggi.
 */
class PianoAlimentareAttivazioneTest extends AbstractIntegrationTest {

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
                new Professionista("prof-attivazione@test.it", "$2a$10$hash", "Anna", "Rossi"));
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
                                {"pazienteId":"%s","nome":"%s","modalita":"MACRO"}
                                """.formatted(paziente.getId(), nome)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(objectMapper.readTree(risposta).get("id").asText());
    }

    /** Porta date e stato del piano dove serve al caso, direttamente sul repository. */
    private void imposta(UUID pianoId, LocalDate inizio, LocalDate fine, StatoPiano stato) {
        PianoAlimentare piano = pianoAlimentareRepository.findById(pianoId).orElseThrow();
        piano.setDataInizio(inizio);
        piano.setDataFine(fine);
        piano.setStato(stato);
        pianoAlimentareRepository.saveAndFlush(piano);
    }

    private void attiva(UUID pianoId, int statoAtteso) throws Exception {
        mockMvc.perform(post("/piani-alimentari/" + pianoId + "/attiva").header("Authorization", "Bearer " + token))
                .andExpect(status().is(statoAtteso));
    }

    private PianoAlimentare ricarica(UUID pianoId) {
        return pianoAlimentareRepository.findById(pianoId).orElseThrow();
    }

    @Test
    void unaBozzaSenzaDataDiFineNonSiAttivaERestaInBozza() throws Exception {
        UUID pianoId = creaBozza("Senza fine");

        mockMvc.perform(post("/piani-alimentari/" + pianoId + "/attiva").header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codice", is("PIANO_NON_ATTIVABILE")))
                .andExpect(jsonPath("$.message", is("Inserisci la data di fine per attivare il piano.")));

        assertThat(ricarica(pianoId).getStato()).isEqualTo(StatoPiano.BOZZA);
    }

    @Test
    void ilPianoAttivoPrecedenteSenzaFineSiConcludeConLaFineDiOggi() throws Exception {
        LocalDate oggi = LocalDate.now();
        UUID precedente = creaBozza("Precedente");
        imposta(precedente, oggi.minusDays(20), null, StatoPiano.ATTIVO);
        UUID nuovo = creaBozza("Nuovo");
        imposta(nuovo, oggi, oggi.plusDays(30), StatoPiano.BOZZA);

        attiva(nuovo, 204);

        assertThat(ricarica(precedente).getStato()).isEqualTo(StatoPiano.TERMINATO);
        assertThat(ricarica(precedente).getDataFine()).isEqualTo(oggi);
        assertThat(ricarica(nuovo).getStato()).isEqualTo(StatoPiano.ATTIVO);
    }

    @Test
    void unaFineFuturaDelPrecedenteVieneAnticipataAOggi() throws Exception {
        LocalDate oggi = LocalDate.now();
        UUID precedente = creaBozza("Precedente");
        imposta(precedente, oggi.minusDays(20), oggi.plusDays(40), StatoPiano.ATTIVO);
        UUID nuovo = creaBozza("Nuovo");
        imposta(nuovo, oggi, oggi.plusDays(30), StatoPiano.BOZZA);

        attiva(nuovo, 204);

        assertThat(ricarica(precedente).getDataFine()).isEqualTo(oggi);
    }

    @Test
    void unaFineGiaPassataDelPrecedenteNonVieneSpostata() throws Exception {
        LocalDate oggi = LocalDate.now();
        UUID precedente = creaBozza("Precedente scaduto");
        imposta(precedente, oggi.minusDays(40), oggi.minusDays(10), StatoPiano.ATTIVO);
        UUID nuovo = creaBozza("Nuovo");
        imposta(nuovo, oggi, oggi.plusDays(30), StatoPiano.BOZZA);

        attiva(nuovo, 204);

        assertThat(ricarica(precedente).getStato()).isEqualTo(StatoPiano.TERMINATO);
        assertThat(ricarica(precedente).getDataFine()).isEqualTo(oggi.minusDays(10));
    }

    @Test
    void unPrecedenteCheIniziaNelFuturoFinisceAlSuoInizioEMaiPrima() throws Exception {
        // Dato anomalo (attivo con inizio futuro, possibile solo prima delle regole del 27/11): la fine non
        // può precedere l'inizio, vincolo V37.
        LocalDate oggi = LocalDate.now();
        UUID precedente = creaBozza("Precedente futuro");
        imposta(precedente, oggi.plusDays(5), null, StatoPiano.ATTIVO);
        UUID nuovo = creaBozza("Nuovo");
        imposta(nuovo, oggi, oggi.plusDays(30), StatoPiano.BOZZA);

        attiva(nuovo, 204);

        assertThat(ricarica(precedente).getDataFine()).isEqualTo(oggi.plusDays(5));
    }

    @Test
    void unPianoAttivoNonPuoPerdereLaDataDiFine() throws Exception {
        LocalDate oggi = LocalDate.now();
        UUID pianoId = creaBozza("Attivo");
        imposta(pianoId, oggi, oggi.plusDays(30), StatoPiano.ATTIVO);

        mockMvc.perform(put("/piani-alimentari/" + pianoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Attivo","dataFine":null,"obiettivoKcal":null,
                                 "pasti":null,"giorniMacroTarget":[],"esempi":null}
                                """))
                .andExpect(status().isBadRequest());

        assertThat(ricarica(pianoId).getDataFine()).isEqualTo(oggi.plusDays(30));
    }

    @Test
    void ilDettaglioRiportaLUltimoSalvataggioCheAvanzaASalvataggioEAttivazione() throws Exception {
        UUID pianoId = creaBozza("Con ultimo salvataggio");
        java.time.Instant creato = ricarica(pianoId).getAggiornatoIl();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/piani-alimentari/" + pianoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.aggiornatoIl").isNotEmpty());

        imposta(pianoId, LocalDate.now(), LocalDate.now().plusDays(30), StatoPiano.BOZZA);
        java.time.Instant salvato = ricarica(pianoId).getAggiornatoIl();
        attiva(pianoId, 204);

        assertThat(salvato).isAfterOrEqualTo(creato);
        assertThat(ricarica(pianoId).getAggiornatoIl()).isAfterOrEqualTo(salvato);
    }

    @Test
    void unaBozzaPuoRestareSenzaDataDiFine() throws Exception {
        UUID pianoId = creaBozza("Bozza");

        mockMvc.perform(put("/piani-alimentari/" + pianoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Bozza","dataFine":null,"obiettivoKcal":null,
                                 "pasti":null,"giorniMacroTarget":[],"esempi":null}
                                """))
                .andExpect(status().isOk());
    }
}
