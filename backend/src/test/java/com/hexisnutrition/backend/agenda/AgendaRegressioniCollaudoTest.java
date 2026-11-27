package com.hexisnutrition.backend.agenda;

import com.hexisnutrition.backend.auth.JwtService;
import com.hexisnutrition.backend.auth.Ruolo;
import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.Sesso;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static com.hexisnutrition.backend.support.Concorrenza.inParallelo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Regressioni del collaudo del 27/11/2026 sull'agenda: 22 (gruppo 1), 11, 14, 15 (gruppo 4). */
class AgendaRegressioniCollaudoTest extends AbstractIntegrationTest {

    @Autowired
    private ProfessionistaRepository professionistaRepository;
    @Autowired
    private PazienteRepository pazienteRepository;
    @Autowired
    private AppuntamentoRepository appuntamentoRepository;
    @Autowired
    private JwtService jwtService;

    private String token;
    private Paziente paziente;
    private final LocalDate giorno = LocalDate.now().plusDays(7);

    @BeforeEach
    void creaProfessionistaEPaziente() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-agenda@test.it", "$2a$10$hash", "Anna", "Rossi"));
        token = jwtService.generateToken(professionista.getId(), Ruolo.PROFESSIONISTA);
        paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Mario", "Bianchi",
                "BNCMRA80A01H501U", "mario@test.it", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));
    }

    private int creaVisita(String tipo, String ora, String durata, String linkVideo) throws Exception {
        String corpo = """
                {"tipo":"%s","pazienteId":"%s","data":"%s","oraInizio":"%s","durataMinuti":%s,"linkVideo":%s}
                """.formatted(tipo, paziente.getId(), giorno, ora, durata,
                linkVideo == null ? "null" : "\"" + linkVideo + "\"");
        return mockMvc.perform(post("/agenda")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andReturn().getResponse().getStatus();
    }

    // --- 22: due visite sovrapposte create insieme ---

    @RepeatedTest(5)
    void dueVisiteSovrapposteCreateInsiemeNonVengonoSalvateEntrambe() throws Exception {
        List<Integer> esiti = inParallelo(
                () -> creaVisita("VISITA_STUDIO", "10:00", "60", null),
                () -> creaVisita("VISITA_STUDIO", "10:30", "60", null));

        // Una passa, l'altra riceve la richiesta di conferma (409): mai due visite salvate senza conferma.
        assertThat(esiti).containsExactlyInAnyOrder(201, 409);
        assertThat(appuntamentoRepository.findAll()).hasSize(1);
    }

    // --- 11: "fatto" obbligatorio ---

    @Test
    void segnareFattoConCorpoVuotoRisponde400ELasciaIlPromemoriaInvariato() throws Exception {
        String risposta = mockMvc.perform(post("/agenda")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipo":"PROMEMORIA","data":"%s","testo":"Richiamare il laboratorio"}
                                """.formatted(giorno)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(risposta).get("id").asText());

        mockMvc.perform(put("/agenda/" + id + "/fatto")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fatto\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/agenda/" + id + "/fatto")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(appuntamentoRepository.findById(id).orElseThrow().isFatto()).isTrue();
    }

    // --- 14: link del videoconsulto senza host ---

    @Test
    void linkVideoSenzaHostRisponde400() throws Exception {
        for (String link : List.of("https://?", "https://", "https:///percorso", "https://:443", "ftp://meet.example.com")) {
            assertThat(creaVisita("VIDEOCONSULTO", "10:00", "30", link)).as(link).isEqualTo(400);
        }
        assertThat(appuntamentoRepository.findAll()).isEmpty();
    }

    @Test
    void linkVideoCompletoEAccettato() throws Exception {
        assertThat(creaVisita("VIDEOCONSULTO", "10:00", "30", "https://meet.example.com/abc-defg-hij?pwd=1")).isEqualTo(201);
        assertThat(creaVisita("VIDEOCONSULTO", "12:00", "30", "https://zoom.example.com:8443")).isEqualTo(201);
    }

    // --- 15: durata decimale ---

    @Test
    void durataDecimaleRisponde400InveceDiEssereTroncata() throws Exception {
        assertThat(creaVisita("VISITA_STUDIO", "10:00", "15.9", null)).isEqualTo(400);
        assertThat(creaVisita("VISITA_STUDIO", "10:00", "480.9", null)).isEqualTo(400);
        assertThat(appuntamentoRepository.findAll()).isEmpty();
    }
}
