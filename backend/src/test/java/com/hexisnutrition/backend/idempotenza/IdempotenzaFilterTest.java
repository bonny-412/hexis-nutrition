package com.hexisnutrition.backend.idempotenza;

import com.hexisnutrition.backend.auth.JwtService;
import com.hexisnutrition.backend.auth.Ruolo;
import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.Sesso;
import com.hexisnutrition.backend.pazienti.StileDiVita;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifica 3 prima del rilascio (28/11/2026), bug S4: un POST ripetuto con la stessa Idempotency-Key (risposta
 * persa, nuovo tentativo) non deve creare un doppione. Si usa la creazione di una visita, il caso riprodotto.
 */
class IdempotenzaFilterTest extends AbstractIntegrationTest {

    private static final String VISITA = """
            {"dataVisita":"%s","altezzaCm":178,"pesoKg":79.5,"obiettivo":"MANTENIMENTO"}
            """.formatted(LocalDate.now());

    @Autowired
    private PazienteRepository pazienteRepository;
    @Autowired
    private ProfessionistaRepository professionistaRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private RegistroIdempotenza registro;

    private UUID professionistaId;
    private String token;
    private Paziente paziente;

    @BeforeEach
    void creaProfessionistaEPaziente() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-idempotenza@test.it", "$2a$10$hash", "Anna", "Rossi"));
        professionistaId = professionista.getId();
        token = jwtService.generateToken(professionistaId, Ruolo.PROFESSIONISTA);
        paziente = pazienteRepository.save(new Paziente(professionistaId, "Mario", "Bianchi",
                "BNCMRA80A01H501U", "mario@test.it", "333", LocalDate.of(1990, 1, 1), Sesso.M,
                null, StileDiVita.ATTIVO, null));
    }

    private ResultActions creaVisita(String chiave, String corpo) throws Exception {
        var richiesta = post("/pazienti/" + paziente.getId() + "/visite")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo);
        if (chiave != null) richiesta.header(IdempotenzaFilter.HEADER, chiave);
        return mockMvc.perform(richiesta);
    }

    private int visiteDelPaziente() {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM visite WHERE paziente_id = ?", Integer.class, paziente.getId());
        return n == null ? 0 : n;
    }

    @Test
    void stessaChiaveDueVolteCreaUnaVisitaSolaERestituisceLaStessaRisposta() throws Exception {
        String prima = creaVisita("chiave-1", VISITA).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String seconda = creaVisita("chiave-1", VISITA).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat(seconda).isEqualTo(prima);
        assertThat(visiteDelPaziente()).isEqualTo(1);
    }

    @Test
    void chiaviDiverseCreanoDueVisite() throws Exception {
        creaVisita("chiave-a", VISITA).andExpect(status().isCreated());
        creaVisita("chiave-b", VISITA).andExpect(status().isCreated());

        assertThat(visiteDelPaziente()).isEqualTo(2);
    }

    @Test
    void senzaHeaderOgniRichiestaCreaUnaVisita() throws Exception {
        creaVisita(null, VISITA).andExpect(status().isCreated());
        creaVisita(null, VISITA).andExpect(status().isCreated());

        assertThat(visiteDelPaziente()).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM richieste_idempotenti", Integer.class)).isZero();
    }

    @Test
    void dopoUnErroreLaChiaveSiLiberaEIlNuovoTentativoVieneEseguito() throws Exception {
        creaVisita("chiave-errore", "{}").andExpect(status().isBadRequest());

        creaVisita("chiave-errore", VISITA).andExpect(status().isCreated());

        assertThat(visiteDelPaziente()).isEqualTo(1);
    }

    @Test
    void laStessaChiaveSuUnAltroPercorsoERifiutata() throws Exception {
        creaVisita("chiave-condivisa", VISITA).andExpect(status().isCreated());

        mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token)
                        .header(IdempotenzaFilter.HEADER, "chiave-condivisa")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"pazienteId":"%s","nome":"Piano","modalita":"MACRO"}
                                """.formatted(paziente.getId())))
                .andExpect(status().isUnprocessableEntity());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM piani_alimentari", Integer.class)).isZero();
    }

    @Test
    void conLaPrimaRichiestaAncoraInCorsoRisponde409SenzaEseguire() throws Exception {
        registro.prenota(professionistaId, "chiave-in-corso", "POST", "/pazienti/" + paziente.getId() + "/visite");

        creaVisita("chiave-in-corso", VISITA)
                .andExpect(status().isConflict())
                .andExpect(content().string("RICHIESTA_IN_CORSO"));
        assertThat(visiteDelPaziente()).isZero();
    }

    @Test
    void unaPrenotazioneAbbandonataNonBloccaIlNuovoTentativo() throws Exception {
        registro.prenota(professionistaId, "chiave-abbandonata", "POST", "/pazienti/" + paziente.getId() + "/visite");
        jdbc.update("UPDATE richieste_idempotenti SET creata_il = now() - interval '10 minutes' WHERE chiave = ?",
                "chiave-abbandonata");

        creaVisita("chiave-abbandonata", VISITA).andExpect(status().isCreated());

        assertThat(visiteDelPaziente()).isEqualTo(1);
    }

    @Test
    void laChiaveValePerUtente() throws Exception {
        creaVisita("chiave-di-anna", VISITA).andExpect(status().isCreated());
        Professionista altro = professionistaRepository.save(
                new Professionista("altro-idempotenza@test.it", "$2a$10$hash", "Luca", "Verdi"));
        String tokenAltro = jwtService.generateToken(altro.getId(), Ruolo.PROFESSIONISTA);

        // Stessa chiave da un altro professionista: non riceve la risposta di Anna (la visita non è sua: 404).
        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/visite")
                        .header("Authorization", "Bearer " + tokenAltro)
                        .header(IdempotenzaFilter.HEADER, "chiave-di-anna")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VISITA))
                .andExpect(status().isNotFound());
        assertThat(visiteDelPaziente()).isEqualTo(1);
    }

    @Test
    void chiaveTroppoLungaRifiutata() throws Exception {
        creaVisita("x".repeat(IdempotenzaFilter.LUNGHEZZA_MASSIMA + 1), VISITA).andExpect(status().isBadRequest());

        assertThat(visiteDelPaziente()).isZero();
    }

    @Test
    void laPuliziaCancellaLeRichiestePiuVecchieDi24Ore() {
        registro.prenota(professionistaId, "vecchia", "POST", "/x");
        registro.prenota(professionistaId, "recente", "POST", "/x");
        jdbc.update("UPDATE richieste_idempotenti SET creata_il = now() - interval '25 hours' WHERE chiave = 'vecchia'");

        registro.cancellaPiuVecchieDi(Duration.ofHours(24));

        assertThat(jdbc.queryForList("SELECT chiave FROM richieste_idempotenti", String.class)).containsExactly("recente");
    }
}
