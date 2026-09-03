package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.auth.JwtService;
import com.hexisnutrition.backend.auth.Ruolo;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PazienteVisiteControllerTest extends AbstractIntegrationTest {

    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @Autowired
    private PazienteRepository pazienteRepository;

    @Autowired
    private VisitaRepository visitaRepository;

    @Autowired
    private PlicometriaRepository plicometriaRepository;

    @Autowired
    private JwtService jwtService;

    @AfterEach
    void pulisci() {
        plicometriaRepository.deleteAll();
        visitaRepository.deleteAll();
        pazienteRepository.deleteAll();
        professionistaRepository.deleteAll();
    }

    private String tokenPer(Professionista professionista) {
        return jwtService.generateToken(professionista.getId(), Ruolo.PROFESSIONISTA);
    }

    private Professionista creaProfessionista(String email) {
        return professionistaRepository.save(new Professionista(email, "hash", "Anna", "Bianchi"));
    }

    private Paziente creaPaziente(Professionista professionista, String email) {
        return pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", email, null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));
    }

    @Test
    void creaVisitaPerPazienteEsistenteRestituisce201EPersiste() throws Exception {
        Professionista professionista = creaProfessionista("prof-crea-visita@example.com");
        Paziente paziente = creaPaziente(professionista, "paz-crea-visita@example.com");

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/visite")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dataVisita":"2026-03-01","altezzaCm":180,"pesoKg":85.0,
                                 "circonferenzaVitaCm":90.0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.altezzaCm").value(180))
                .andExpect(jsonPath("$.pesoKg").value(85.0))
                .andExpect(jsonPath("$.dataVisita").value("2026-03-01"));

        List<Visita> visite = visitaRepository.findAllByPazienteIdOrderByDataVisitaAsc(paziente.getId());
        assertThat(visite).hasSize(1);
        assertThat(visite.get(0).getPazienteId()).isEqualTo(paziente.getId());
    }

    @Test
    void creaVisitaDiPazienteDiAltroProfessionistaRestituisce404() throws Exception {
        Professionista professionistaA = creaProfessionista("prof-crea-visita-a@example.com");
        Professionista professionistaB = creaProfessionista("prof-crea-visita-b@example.com");
        Paziente pazienteDiB = creaPaziente(professionistaB, "paz-crea-visita-b@example.com");

        mockMvc.perform(post("/pazienti/" + pazienteDiB.getId() + "/visite")
                        .header("Authorization", "Bearer " + tokenPer(professionistaA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"altezzaCm\":180,\"pesoKg\":85.0}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void creaVisitaConAltezzaMancanteRestituisce400() throws Exception {
        Professionista professionista = creaProfessionista("prof-crea-visita-400@example.com");
        Paziente paziente = creaPaziente(professionista, "paz-crea-visita-400@example.com");

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/visite")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pesoKg\":85.0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dettaglioVisitaRestituisceIDatiCompletiInclusaPlicometriaGrezza() throws Exception {
        Professionista professionista = creaProfessionista("prof-dettaglio-visita@example.com");
        Paziente paziente = creaPaziente(professionista, "paz-dettaglio-visita@example.com");

        String risposta = mockMvc.perform(post("/pazienti/" + paziente.getId() + "/visite")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dataVisita":"2026-01-15","altezzaCm":180,"pesoKg":80.00,
                                 "plicometria":{"protocollo":"JACKSON_POLLOCK_3",
                                 "plicaPettoraleMm":10.00,"plicaAddominaleMm":10.00,"plicaCosciaMm":10.00}}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String visitaId = com.jayway.jsonpath.JsonPath.read(risposta, "$.id");

        mockMvc.perform(get("/pazienti/" + paziente.getId() + "/visite/" + visitaId)
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.protocolloVita").value("OMS"))
                .andExpect(jsonPath("$.plicometria.protocollo").value("JACKSON_POLLOCK_3"))
                .andExpect(jsonPath("$.plicometria.plicaPettoraleMm").value(10.00))
                .andExpect(jsonPath("$.plicometria.plicaAddominaleMm").value(10.00))
                .andExpect(jsonPath("$.plicometria.plicaCosciaMm").value(10.00));
    }

    @Test
    void dettaglioVisitaInesistenteRestituisce404() throws Exception {
        Professionista professionista = creaProfessionista("prof-dettaglio-404@example.com");
        Paziente paziente = creaPaziente(professionista, "paz-dettaglio-404@example.com");

        mockMvc.perform(get("/pazienti/" + paziente.getId() + "/visite/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isNotFound());
    }

    @Test
    void dettaglioVisitaDiUnAltroPazienteRestituisce404() throws Exception {
        Professionista professionista = creaProfessionista("prof-dettaglio-altro-paz@example.com");
        Paziente pazienteA = creaPaziente(professionista, "paz-dettaglio-altro-a@example.com");
        Paziente pazienteB = creaPaziente(professionista, "paz-dettaglio-altro-b@example.com");
        Visita visitaDiB = new Visita(pazienteB.getId(), LocalDate.of(2026, 1, 1), 170, new java.math.BigDecimal("70.0"),
                null, null, null, null, null, null, null, null, null, null, null, ProtocolloVita.OMS, null, null);
        visitaRepository.save(visitaDiB);

        mockMvc.perform(get("/pazienti/" + pazienteA.getId() + "/visite/" + visitaDiB.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isNotFound());
    }

    @Test
    void aggiornaVisitaAggiornaICampiERicalcolaBmi() throws Exception {
        Professionista professionista = creaProfessionista("prof-aggiorna-visita@example.com");
        Paziente paziente = creaPaziente(professionista, "paz-aggiorna-visita@example.com");
        Visita visita = new Visita(paziente.getId(), LocalDate.of(2026, 1, 1), 178, new java.math.BigDecimal("80.0"),
                null, null, null, null, null, null, null, null, null, null, null, ProtocolloVita.OMS, null, null);
        VisitaCalcoli.applica(visita);
        visitaRepository.save(visita);

        mockMvc.perform(put("/pazienti/" + paziente.getId() + "/visite/" + visita.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dataVisita":"2026-02-01","altezzaCm":180,"pesoKg":90.00}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.altezzaCm").value(180))
                .andExpect(jsonPath("$.pesoKg").value(90.0))
                .andExpect(jsonPath("$.bmi").value(27.78));

        Visita aggiornata = visitaRepository.findById(visita.getId()).orElseThrow();
        assertThat(aggiornata.getAltezzaCm()).isEqualTo(180);
        assertThat(aggiornata.getPesoKg()).isEqualByComparingTo("90.00");
        assertThat(aggiornata.getDataVisita()).isEqualTo(LocalDate.of(2026, 2, 1));
    }

    @Test
    void aggiornaVisitaRimuovendoVitaEFianchiAzzeraWhrEWhtr() throws Exception {
        Professionista professionista = creaProfessionista("prof-aggiorna-whr@example.com");
        Paziente paziente = creaPaziente(professionista, "paz-aggiorna-whr@example.com");
        Visita visita = new Visita(paziente.getId(), LocalDate.of(2026, 1, 1), 180, new java.math.BigDecimal("80.0"),
                new java.math.BigDecimal("95.0"), new java.math.BigDecimal("100.0"),
                null, null, null, null, null, null, null, null, null, ProtocolloVita.OMS, null, null);
        VisitaCalcoli.applica(visita);
        visitaRepository.save(visita);
        assertThat(visita.getWhr()).isNotNull();
        assertThat(visita.getWhtr()).isNotNull();

        mockMvc.perform(put("/pazienti/" + paziente.getId() + "/visite/" + visita.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"altezzaCm\":180,\"pesoKg\":80.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.whr").doesNotExist())
                .andExpect(jsonPath("$.whtr").doesNotExist());

        Visita aggiornata = visitaRepository.findById(visita.getId()).orElseThrow();
        assertThat(aggiornata.getWhr()).isNull();
        assertThat(aggiornata.getWhtr()).isNull();
    }

    @Test
    void aggiornaVisitaSostituisceLaPlicometriaPrecedente() throws Exception {
        Professionista professionista = creaProfessionista("prof-aggiorna-plico@example.com");
        Paziente paziente = creaPaziente(professionista, "paz-aggiorna-plico@example.com");

        String risposta = mockMvc.perform(post("/pazienti/" + paziente.getId() + "/visite")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dataVisita":"2026-01-15","altezzaCm":180,"pesoKg":80.00,
                                 "plicometria":{"protocollo":"JACKSON_POLLOCK_3",
                                 "plicaPettoraleMm":10.00,"plicaAddominaleMm":10.00,"plicaCosciaMm":10.00}}
                                """))
                .andReturn().getResponse().getContentAsString();
        String visitaId = com.jayway.jsonpath.JsonPath.read(risposta, "$.id");

        mockMvc.perform(put("/pazienti/" + paziente.getId() + "/visite/" + visitaId)
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dataVisita":"2026-01-15","altezzaCm":180,"pesoKg":80.00,
                                 "plicometria":{"protocollo":"JACKSON_POLLOCK_3",
                                 "plicaPettoraleMm":20.00,"plicaAddominaleMm":20.00,"plicaCosciaMm":20.00}}
                                """))
                .andExpect(status().isOk());

        List<Plicometria> plicometrie = plicometriaRepository.findAll();
        assertThat(plicometrie).hasSize(1);
        assertThat(plicometrie.get(0).getPlicaPettoraleMm()).isEqualByComparingTo("20.00");
    }

    @Test
    void aggiornaVisitaSenzaPlicometriaEliminaQuellaPrecedente() throws Exception {
        Professionista professionista = creaProfessionista("prof-aggiorna-plico-rimossa@example.com");
        Paziente paziente = creaPaziente(professionista, "paz-aggiorna-plico-rimossa@example.com");

        String risposta = mockMvc.perform(post("/pazienti/" + paziente.getId() + "/visite")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dataVisita":"2026-01-15","altezzaCm":180,"pesoKg":80.00,
                                 "plicometria":{"protocollo":"JACKSON_POLLOCK_3",
                                 "plicaPettoraleMm":10.00,"plicaAddominaleMm":10.00,"plicaCosciaMm":10.00}}
                                """))
                .andReturn().getResponse().getContentAsString();
        String visitaId = com.jayway.jsonpath.JsonPath.read(risposta, "$.id");

        mockMvc.perform(put("/pazienti/" + paziente.getId() + "/visite/" + visitaId)
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataVisita\":\"2026-01-15\",\"altezzaCm\":180,\"pesoKg\":80.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plicometria").doesNotExist());

        assertThat(plicometriaRepository.findAll()).isEmpty();
    }

    @Test
    void aggiornaVisitaDiPazienteDiAltroProfessionistaRestituisce404() throws Exception {
        Professionista professionistaA = creaProfessionista("prof-aggiorna-visita-a@example.com");
        Professionista professionistaB = creaProfessionista("prof-aggiorna-visita-b@example.com");
        Paziente pazienteDiB = creaPaziente(professionistaB, "paz-aggiorna-visita-b@example.com");
        Visita visitaDiB = new Visita(pazienteDiB.getId(), LocalDate.of(2026, 1, 1), 170, new java.math.BigDecimal("70.0"),
                null, null, null, null, null, null, null, null, null, null, null, ProtocolloVita.OMS, null, null);
        visitaRepository.save(visitaDiB);

        mockMvc.perform(put("/pazienti/" + pazienteDiB.getId() + "/visite/" + visitaDiB.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionistaA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"altezzaCm\":170,\"pesoKg\":70.0}"))
                .andExpect(status().isNotFound());
    }
}
