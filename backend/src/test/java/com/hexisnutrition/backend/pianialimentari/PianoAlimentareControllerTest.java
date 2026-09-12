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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PianoAlimentareControllerTest extends AbstractIntegrationTest {

    @Autowired
    private PazienteRepository pazienteRepository;
    @Autowired
    private VisitaRepository visitaRepository;
    @Autowired
    private ProfessionistaRepository professionistaRepository;
    @Autowired
    private JwtService jwtService;

    private Professionista professionista;
    private String token;
    private Paziente paziente;

    // Stesso pattern di autenticazione già usato da AlimentoControllerTest/PazienteControllerTest:
    // un JWT generato direttamente da JwtService, nessun vero giro su /auth/login.
    private String tokenPer(Professionista professionista) {
        return jwtService.generateToken(professionista.getId(), Ruolo.PROFESSIONISTA);
    }

    @BeforeEach
    void creaProfessionistaEPaziente() {
        professionista = professionistaRepository.save(
                new Professionista("prof@test.it", "$2a$10$hash", "Anna", "Rossi"));
        token = tokenPer(professionista);
        paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Mario", "Bianchi",
                "BNCMRA80A01H501U", "mario@test.it", "333", LocalDate.of(1990, 1, 1), Sesso.M,
                null, StileDiVita.ATTIVO, null));
        visitaRepository.save(new Visita(paziente.getId(), LocalDate.now(), 178, BigDecimal.valueOf(78),
                null, null, null, null, null, null, null, null, null, null, null, null, null,
                ObiettivoVisita.IPERTROFIA));
    }

    @Test
    void creaBozzaPianoConPastiCreaSetteGiorniConICinquePastiTemplateENessunaRiga() throws Exception {
        String body = """
                {"pazienteId":"%s","nome":"Ipertrofia · fase 1","modalita":"PASTI"}
                """.formatted(paziente.getId());

        mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stato", is("BOZZA")))
                .andExpect(jsonPath("$.modalita", is("PASTI")))
                .andExpect(jsonPath("$.pasti.length()", is(35))) // 7 giorni * 5 pasti template
                .andExpect(jsonPath("$.pasti[0].righe.length()", is(0)))
                .andExpect(jsonPath("$.obiettivoKcalSuggerito").isNotEmpty());
    }

    @Test
    void aggiornaSostituisceLaStrutturaPastiEIRicalcoliDiTotaleRestanoLatoClient() throws Exception {
        String bodyCreazione = """
                {"pazienteId":"%s","nome":"Piano","modalita":"PASTI"}
                """.formatted(paziente.getId());
        String rispostaCreazione = mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(bodyCreazione))
                .andReturn().getResponse().getContentAsString();
        UUID pianoId = UUID.fromString(objectMapper.readTree(rispostaCreazione).get("id").asText());

        String bodyAggiornamento = """
                {
                  "nome": "Piano aggiornato",
                  "dataFine": "2026-10-10",
                  "obiettivoKcal": 2500,
                  "pasti": [
                    {
                      "giornoSettimana": "LUNEDI",
                      "nome": "Colazione",
                      "tipo": "COLAZIONE",
                      "nota": null,
                      "righe": [
                        {"alimentoId": null, "nome": "Avena in fiocchi", "kcal100g": 372, "proteine100g": 12.9,
                         "carboidrati100g": 65, "grassi100g": 6.5, "zuccheri100g": 1.1, "grammi": 60}
                      ]
                    }
                  ],
                  "giorniMacroTarget": null,
                  "esempi": null
                }
                """;

        mockMvc.perform(put("/piani-alimentari/" + pianoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(bodyAggiornamento))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", is("Piano aggiornato")))
                .andExpect(jsonPath("$.obiettivoKcal", is(2500)))
                // I 35 pasti template sono sostituiti da un solo pasto: sostituzione totale, non merge.
                .andExpect(jsonPath("$.pasti.length()", is(1)))
                .andExpect(jsonPath("$.pasti[0].righe.length()", is(1)))
                .andExpect(jsonPath("$.pasti[0].righe[0].nome", is("Avena in fiocchi")));
    }

    @Test
    void aggiornaPianoMacroConTargetRealiDueVolteSostituisceIValoriSenzaViolareIlVincoloUnique() throws Exception {
        String bodyCreazione = """
                {"pazienteId":"%s","nome":"Piano macro","modalita":"MACRO"}
                """.formatted(paziente.getId());
        String rispostaCreazione = mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(bodyCreazione))
                .andReturn().getResponse().getContentAsString();
        UUID pianoId = UUID.fromString(objectMapper.readTree(rispostaCreazione).get("id").asText());

        String bodyPrimoAggiornamento = """
                {
                  "nome": "Piano macro",
                  "dataFine": null,
                  "obiettivoKcal": null,
                  "pasti": null,
                  "giorniMacroTarget": [
                    {"giornoSettimana":"LUNEDI","kcalTarget":2200,"proteineTarget":180,"carboidratiTarget":220,"grassiTarget":70},
                    {"giornoSettimana":"MARTEDI","kcalTarget":2300,"proteineTarget":185,"carboidratiTarget":230,"grassiTarget":75},
                    {"giornoSettimana":"MERCOLEDI","kcalTarget":2100,"proteineTarget":170,"carboidratiTarget":210,"grassiTarget":65}
                  ],
                  "esempi": null
                }
                """;

        mockMvc.perform(put("/piani-alimentari/" + pianoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(bodyPrimoAggiornamento))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.giorniMacroTarget.length()", is(3)))
                .andExpect(jsonPath("$.giorniMacroTarget[?(@.giornoSettimana=='LUNEDI')].kcalTarget", hasItem(2200)))
                .andExpect(jsonPath("$.giorniMacroTarget[?(@.giornoSettimana=='LUNEDI')].proteineTarget", hasItem(180)))
                .andExpect(jsonPath("$.giorniMacroTarget[?(@.giornoSettimana=='LUNEDI')].carboidratiTarget", hasItem(220)))
                .andExpect(jsonPath("$.giorniMacroTarget[?(@.giornoSettimana=='LUNEDI')].grassiTarget", hasItem(70)))
                .andExpect(jsonPath("$.giorniMacroTarget[?(@.giornoSettimana=='MARTEDI')].kcalTarget", hasItem(2300)))
                .andExpect(jsonPath("$.giorniMacroTarget[?(@.giornoSettimana=='MERCOLEDI')].kcalTarget", hasItem(2100)));

        // Secondo salvataggio sugli stessi giorni con valori diversi: il delete-poi-insert di
        // sostituisciGiorniMacro (che ora fa flush esplicito, vedi PianoAlimentareService) deve
        // sostituire le righe appena persistite dal salvataggio precedente senza violare lo
        // UNIQUE(piano_id, giorno_settimana) di piano_giorno_macro_target.
        String bodySecondoAggiornamento = """
                {
                  "nome": "Piano macro",
                  "dataFine": null,
                  "obiettivoKcal": null,
                  "pasti": null,
                  "giorniMacroTarget": [
                    {"giornoSettimana":"LUNEDI","kcalTarget":1900,"proteineTarget":150,"carboidratiTarget":190,"grassiTarget":55},
                    {"giornoSettimana":"MARTEDI","kcalTarget":2000,"proteineTarget":160,"carboidratiTarget":200,"grassiTarget":60},
                    {"giornoSettimana":"MERCOLEDI","kcalTarget":1800,"proteineTarget":140,"carboidratiTarget":180,"grassiTarget":50}
                  ],
                  "esempi": null
                }
                """;

        mockMvc.perform(put("/piani-alimentari/" + pianoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(bodySecondoAggiornamento))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.giorniMacroTarget.length()", is(3)))
                .andExpect(jsonPath("$.giorniMacroTarget[?(@.giornoSettimana=='LUNEDI')].kcalTarget", hasItem(1900)))
                .andExpect(jsonPath("$.giorniMacroTarget[?(@.giornoSettimana=='LUNEDI')].proteineTarget", hasItem(150)))
                .andExpect(jsonPath("$.giorniMacroTarget[?(@.giornoSettimana=='LUNEDI')].carboidratiTarget", hasItem(190)))
                .andExpect(jsonPath("$.giorniMacroTarget[?(@.giornoSettimana=='LUNEDI')].grassiTarget", hasItem(55)))
                .andExpect(jsonPath("$.giorniMacroTarget[?(@.giornoSettimana=='MARTEDI')].kcalTarget", hasItem(2000)))
                .andExpect(jsonPath("$.giorniMacroTarget[?(@.giornoSettimana=='MERCOLEDI')].kcalTarget", hasItem(1800)));
    }

    @Test
    void aggiornaPianoEsempiConRigheRestituisceLeRigheSalvate() throws Exception {
        String bodyCreazione = """
                {"pazienteId":"%s","nome":"Piano esempi","modalita":"ESEMPI"}
                """.formatted(paziente.getId());
        String rispostaCreazione = mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(bodyCreazione))
                .andReturn().getResponse().getContentAsString();
        UUID pianoId = UUID.fromString(objectMapper.readTree(rispostaCreazione).get("id").asText());

        String bodyAggiornamento = """
                {
                  "nome": "Piano esempi",
                  "dataFine": null,
                  "obiettivoKcal": null,
                  "pasti": null,
                  "giorniMacroTarget": null,
                  "esempi": [
                    {
                      "tipoPasto": "PRANZO",
                      "nome": "Pranzo tipo",
                      "righe": [
                        {"alimentoId": null, "nome": "Petto di pollo", "kcal100g": 165, "proteine100g": 31,
                         "carboidrati100g": 0, "grassi100g": 3.6, "zuccheri100g": 0, "grammi": 150}
                      ]
                    }
                  ]
                }
                """;

        mockMvc.perform(put("/piani-alimentari/" + pianoId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(bodyAggiornamento))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.esempi.length()", is(1)))
                .andExpect(jsonPath("$.esempi[0].tipoPasto", is("PRANZO")))
                .andExpect(jsonPath("$.esempi[0].nome", is("Pranzo tipo")))
                .andExpect(jsonPath("$.esempi[0].righe.length()", is(1)))
                .andExpect(jsonPath("$.esempi[0].righe[0].nome", is("Petto di pollo")))
                .andExpect(jsonPath("$.esempi[0].righe[0].grammi", is(150)))
                .andExpect(jsonPath("$.esempi[0].righe[0].proteine100g", is(31)));
    }

    @Test
    void nonPuoAggiornareUnPianoDiUnAltroProfessionista() throws Exception {
        Professionista altroProfessionista = professionistaRepository.save(
                new Professionista("altro@test.it", "$2a$10$hash", "Luca", "Verdi"));
        String bodyCreazione = """
                {"pazienteId":"%s","nome":"Piano","modalita":"MACRO"}
                """.formatted(paziente.getId());
        String risposta = mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(bodyCreazione))
                .andReturn().getResponse().getContentAsString();
        UUID pianoId = UUID.fromString(objectMapper.readTree(risposta).get("id").asText());

        mockMvc.perform(put("/piani-alimentari/" + pianoId)
                        .header("Authorization", "Bearer " + tokenPer(altroProfessionista))
                        .contentType("application/json")
                        .content("""
                                {"nome":"x","dataFine":null,"obiettivoKcal":null,"pasti":null,
                                 "giorniMacroTarget":[],"esempi":null}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void attivaDisattivaIlPianoPrecedenteDelloStessoPazienteEDiventaTerminato() throws Exception {
        String bodyPrimoPiano = """
                {"pazienteId":"%s","nome":"Fase 1","modalita":"MACRO"}
                """.formatted(paziente.getId());
        String rispostaPrimoPiano = mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(bodyPrimoPiano))
                .andReturn().getResponse().getContentAsString();
        UUID primoPianoId = UUID.fromString(objectMapper.readTree(rispostaPrimoPiano).get("id").asText());
        mockMvc.perform(post("/piani-alimentari/" + primoPianoId + "/attiva")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        String bodySecondoPiano = """
                {"pazienteId":"%s","nome":"Fase 2","modalita":"MACRO"}
                """.formatted(paziente.getId());
        String rispostaSecondoPiano = mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token).contentType("application/json")
                        .content(bodySecondoPiano))
                .andReturn().getResponse().getContentAsString();
        UUID secondoPianoId = UUID.fromString(objectMapper.readTree(rispostaSecondoPiano).get("id").asText());
        mockMvc.perform(post("/piani-alimentari/" + secondoPianoId + "/attiva")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/piani-alimentari/" + primoPianoId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.stato", is("TERMINATO")));
        mockMvc.perform(get("/piani-alimentari/" + secondoPianoId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.stato", is("ATTIVO")));
    }

    @Test
    void eliminaUnaBozzaFunzionaMaUnPianoAttivoRestituisce409() throws Exception {
        String body = """
                {"pazienteId":"%s","nome":"Da eliminare","modalita":"MACRO"}
                """.formatted(paziente.getId());
        String risposta = mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token).contentType("application/json").content(body))
                .andReturn().getResponse().getContentAsString();
        UUID pianoId = UUID.fromString(objectMapper.readTree(risposta).get("id").asText());

        mockMvc.perform(post("/piani-alimentari/" + pianoId + "/attiva").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/piani-alimentari/" + pianoId).header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());

        String bodyBozza = """
                {"pazienteId":"%s","nome":"Bozza","modalita":"MACRO"}
                """.formatted(paziente.getId());
        String rispostaBozza = mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token).contentType("application/json").content(bodyBozza))
                .andReturn().getResponse().getContentAsString();
        UUID bozzaId = UUID.fromString(objectMapper.readTree(rispostaBozza).get("id").asText());
        mockMvc.perform(delete("/piani-alimentari/" + bozzaId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void ricercaFiltraPerStatoERestituiscePaginazione() throws Exception {
        String bodyBozza = """
                {"pazienteId":"%s","nome":"Piano bozza","modalita":"MACRO"}
                """.formatted(paziente.getId());
        mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(bodyBozza))
                .andExpect(status().isCreated());

        String bodyAttivo = """
                {"pazienteId":"%s","nome":"Piano attivo","modalita":"MACRO"}
                """.formatted(paziente.getId());
        String rispostaAttivo = mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(bodyAttivo))
                .andReturn().getResponse().getContentAsString();
        UUID pianoAttivoId = UUID.fromString(objectMapper.readTree(rispostaAttivo).get("id").asText());
        mockMvc.perform(post("/piani-alimentari/" + pianoAttivoId + "/attiva")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/piani-alimentari/ricerca")
                        .param("stato", "BOZZA")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()", is(1)))
                .andExpect(jsonPath("$.contenuto[0].nome", is("Piano bozza")))
                .andExpect(jsonPath("$.contenuto[0].pazienteNomeCompleto", is("Mario Bianchi")))
                .andExpect(jsonPath("$.contenuto[0].stato", is("BOZZA")))
                .andExpect(jsonPath("$.paginaCorrente", is(0)))
                .andExpect(jsonPath("$.dimensionePagina", is(20)))
                .andExpect(jsonPath("$.totaleElementi", is(1)))
                .andExpect(jsonPath("$.totalePagine", is(1)));
    }

    @Test
    void ricercaFiltraPerPazienteIdRestituendoSoloIPianiDiQuelPaziente() throws Exception {
        Paziente altroPaziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "VRDLCU85A01H501U", "luca@test.it", "334", LocalDate.of(1985, 1, 1), Sesso.M,
                null, StileDiVita.ATTIVO, null));

        String bodyPazienteOriginale = """
                {"pazienteId":"%s","nome":"Piano di Mario","modalita":"MACRO"}
                """.formatted(paziente.getId());
        mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(bodyPazienteOriginale))
                .andExpect(status().isCreated());

        String bodyAltroPaziente = """
                {"pazienteId":"%s","nome":"Piano di Luca","modalita":"MACRO"}
                """.formatted(altroPaziente.getId());
        mockMvc.perform(post("/piani-alimentari")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content(bodyAltroPaziente))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/piani-alimentari/ricerca")
                        .param("pazienteId", altroPaziente.getId().toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()", is(1)))
                .andExpect(jsonPath("$.contenuto[0].nome", is("Piano di Luca")))
                .andExpect(jsonPath("$.contenuto[0].pazienteNomeCompleto", is("Luca Verdi")));
    }
}
