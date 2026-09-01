package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.auth.JwtService;
import com.hexisnutrition.backend.auth.Ruolo;
import com.hexisnutrition.backend.inviti.TipoToken;
import com.hexisnutrition.backend.inviti.TokenAzione;
import com.hexisnutrition.backend.inviti.TokenAzioneRepository;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import com.hexisnutrition.backend.support.TestEmailConfig;
import com.hexisnutrition.backend.email.FakeEmailSender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestEmailConfig.class)
class PazienteControllerTest extends AbstractIntegrationTest {

    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @Autowired
    private PazienteRepository pazienteRepository;

    @Autowired
    private VisitaRepository visitaRepository;

    @Autowired
    private PlicometriaRepository plicometriaRepository;

    @Autowired
    private TokenAzioneRepository tokenAzioneRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender fakeEmailSender;

    @AfterEach
    void pulisci() {
        plicometriaRepository.deleteAll();
        visitaRepository.deleteAll();
        tokenAzioneRepository.deleteAll();
        pazienteRepository.deleteAll();
        professionistaRepository.deleteAll();
        fakeEmailSender.getInviate().clear();
    }

    private String tokenPer(Professionista professionista) {
        return jwtService.generateToken(professionista.getId(), Ruolo.PROFESSIONISTA);
    }

    @Test
    void creaPazienteRestituisce201EPersisteLaPrimaVisita() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca@example.com",
                                 "telefono":"333123456","dataNascita":"1990-05-20","sesso":"M",
                                 "lavoro":"Impiegato","tipoLavoro":"ATTIVO",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5,"circonferenzaVitaCm":95.0}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("luca@example.com"))
                .andExpect(jsonPath("$.lavoro").value("Impiegato"))
                .andExpect(jsonPath("$.tipoLavoro").value("ATTIVO"))
                .andExpect(jsonPath("$.statoAccount").value("MAI_INVITATO"));

        List<Visita> visite = visitaRepository.findAll();
        assertThat(visite).hasSize(1);
        assertThat(visite.get(0).getAltezzaCm()).isEqualTo(178);
        assertThat(visite.get(0).getPesoKg()).isEqualByComparingTo("82.5");
        assertThat(visite.get(0).getCirconferenzaVitaCm()).isEqualByComparingTo("95.0");
    }

    @Test
    void creaPazienteConTutteLeCirconferenzeLePersisteNeiCampiCorretti() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-11-misure@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-11-misure@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5,
                                 "circonferenzaVitaCm":90.1,"circonferenzaFianchiCm":91.2,
                                 "circonferenzaAddomeCm":92.3,"circonferenzaBraccioRilassatoCm":93.4,
                                 "circonferenzaCosciaCm":94.5,"circonferenzaPolpaccioCm":95.6,
                                 "circonferenzaColloCm":96.7,"circonferenzaToraceCm":97.8,
                                 "circonferenzaBraccioContrattoCm":98.9,"circonferenzaAvambraccioCm":99.0,
                                 "circonferenzaCavigliaCm":100.1,"protocolloVita":"OMBELICALE"}}
                                """))
                .andExpect(status().isCreated());

        List<Visita> visite = visitaRepository.findAll();
        assertThat(visite).hasSize(1);
        Visita visita = visite.get(0);
        assertThat(visita.getAltezzaCm()).isEqualTo(178);
        assertThat(visita.getPesoKg()).isEqualByComparingTo("82.5");
        assertThat(visita.getCirconferenzaVitaCm()).isEqualByComparingTo("90.1");
        assertThat(visita.getCirconferenzaFianchiCm()).isEqualByComparingTo("91.2");
        assertThat(visita.getCirconferenzaAddomeCm()).isEqualByComparingTo("92.3");
        assertThat(visita.getCirconferenzaBraccioRilassatoCm()).isEqualByComparingTo("93.4");
        assertThat(visita.getCirconferenzaCosciaCm()).isEqualByComparingTo("94.5");
        assertThat(visita.getCirconferenzaPolpaccioCm()).isEqualByComparingTo("95.6");
        assertThat(visita.getCirconferenzaColloCm()).isEqualByComparingTo("96.7");
        assertThat(visita.getCirconferenzaToraceCm()).isEqualByComparingTo("97.8");
        assertThat(visita.getCirconferenzaBraccioContrattoCm()).isEqualByComparingTo("98.9");
        assertThat(visita.getCirconferenzaAvambraccioCm()).isEqualByComparingTo("99.0");
        assertThat(visita.getCirconferenzaCavigliaCm()).isEqualByComparingTo("100.1");
        assertThat(visita.getProtocolloVita()).isEqualTo(ProtocolloVita.OMBELICALE);
    }

    @Test
    void creaPazienteSenzaProtocolloVitaUsaOmsPerDefault() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-protocollo-default@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-protocollo-default@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5}}
                                """))
                .andExpect(status().isCreated());

        List<Visita> visite = visitaRepository.findAll();
        assertThat(visite.get(0).getProtocolloVita()).isEqualTo(ProtocolloVita.OMS);
    }

    @Test
    void creaPazienteConDataVisitaLaPersisteEsattamente() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-data-visita@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-data-visita@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M",
                                 "visita":{"dataVisita":"2026-08-15","altezzaCm":178,"pesoKg":82.5}}
                                """))
                .andExpect(status().isCreated());

        List<Visita> visite = visitaRepository.findAll();
        assertThat(visite).hasSize(1);
        assertThat(visite.get(0).getDataVisita()).isEqualTo(LocalDate.of(2026, 8, 15));
    }

    @Test
    void creaPazienteSenzaDataVisitaUsaLaDataOdierna() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-senza-data-visita@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-senza-data-visita@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5}}
                                """))
                .andExpect(status().isCreated());

        List<Visita> visite = visitaRepository.findAll();
        assertThat(visite).hasSize(1);
        assertThat(visite.get(0).getDataVisita()).isEqualTo(LocalDate.now());
    }

    @Test
    void creaPazienteConPesoECirconferenzeADueDecimaliLiPersisteEsattamente() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-due-decimali@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-due-decimali@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M",
                                 "visita":{"altezzaCm":178,"pesoKg":82.55,"circonferenzaVitaCm":95.25}}
                                """))
                .andExpect(status().isCreated());

        List<Visita> visite = visitaRepository.findAll();
        assertThat(visite).hasSize(1);
        assertThat(visite.get(0).getPesoKg()).isEqualByComparingTo("82.55");
        assertThat(visite.get(0).getCirconferenzaVitaCm()).isEqualByComparingTo("95.25");
    }

    @Test
    void creaPazienteConVitaEFianchiCalcolaBmiWhrWhtr() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-bmi-whr@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-bmi-whr@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M",
                                 "visita":{"altezzaCm":180,"pesoKg":82.50,
                                 "circonferenzaVitaCm":95.00,"circonferenzaFianchiCm":100.00}}
                                """))
                .andExpect(status().isCreated());

        Visita visita = visitaRepository.findAll().get(0);
        assertThat(visita.getBmi()).isEqualByComparingTo("25.46");
        assertThat(visita.getWhr()).isEqualByComparingTo("0.95");
        assertThat(visita.getWhtr()).isEqualByComparingTo("0.53");
    }

    @Test
    void creaPazienteSenzaVisitaRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-senza-visita@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Luca\",\"cognome\":\"Verdi\",\"email\":\"luca-senza-visita@example.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creaPazienteSenzaDataNascitaRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-senza-data-nascita@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-senza-data-nascita@example.com",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creaPazienteConVisitaSenzaAltezzaRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-visita-incompleta@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-visita-incompleta@example.com",
                                 "visita":{"pesoKg":82.5}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creaPazienteConVisitaSenzaPesoRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-visita-senza-peso@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-visita-senza-peso@example.com",
                                 "visita":{"altezzaCm":178}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creaPazienteConAltezzaZeroRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-altezza-zero@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-altezza-zero@example.com",
                                 "visita":{"altezzaCm":0,"pesoKg":82.5}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creaPazienteConPesoNegativoRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-peso-negativo@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-peso-negativo@example.com",
                                 "visita":{"altezzaCm":178,"pesoKg":-80}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creaPazienteConLavoroETipoLavoroLiRestituisceNellaRisposta() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-lavoro@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-lavoro@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M",
                                 "lavoro":"Impiegato","tipoLavoro":"ATTIVO",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lavoro").value("Impiegato"))
                .andExpect(jsonPath("$.tipoLavoro").value("ATTIVO"));
    }

    @Test
    void listaRestituisceSoloIPazientiDelProfessionistaAutenticato() throws Exception {
        Professionista professionistaA = professionistaRepository.save(
                new Professionista("a@example.com", "hash", "A", "A"));
        Professionista professionistaB = professionistaRepository.save(
                new Professionista("b@example.com", "hash", "B", "B"));
        pazienteRepository.save(new Paziente(professionistaA.getId(), "Paziente", "DiA",
                "diA@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));
        pazienteRepository.save(new Paziente(professionistaB.getId(), "Paziente", "DiB",
                "diB@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));

        mockMvc.perform(get("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionistaA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].email").value("diA@example.com"));
    }

    @Test
    void dettaglioDiPazienteDiAltroProfessionistaRestituisce404() throws Exception {
        Professionista professionistaA = professionistaRepository.save(
                new Professionista("a2@example.com", "hash", "A", "A"));
        Professionista professionistaB = professionistaRepository.save(
                new Professionista("b2@example.com", "hash", "B", "B"));
        Paziente pazienteDiB = pazienteRepository.save(new Paziente(professionistaB.getId(), "Paziente", "DiB",
                "diB2@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));

        mockMvc.perform(get("/pazienti/" + pazienteDiB.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionistaA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unPazienteAutenticatoNonPuoCreareAltriPazienti() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof3@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca3@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));
        String tokenPaziente = jwtService.generateToken(paziente.getId(), Ruolo.PAZIENTE);

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPaziente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"cognome\":\"Y\",\"email\":\"x@example.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invitoGeneraTokenCambiaStatoEInviaEmail() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof4@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca4@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/invito")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isNoContent());

        Paziente aggiornato = pazienteRepository.findById(paziente.getId()).orElseThrow();
        assertThat(aggiornato.getStatoAccount()).isEqualTo(StatoAccountPaziente.INVITATO);
        assertThat(fakeEmailSender.getInviate()).hasSize(1);
        assertThat(fakeEmailSender.getInviate().get(0).destinatario()).isEqualTo("luca4@example.com");
    }

    @Test
    void invitoAPazienteGiaAttivoRestituisce409() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof5@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca5@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null);
        paziente.setStatoAccount(StatoAccountPaziente.ATTIVO);
        paziente.setPasswordHash("hash");
        pazienteRepository.save(paziente);

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/invito")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isConflict());
    }

    @Test
    void attivaConTokenValidoImpostaPasswordEAttivaAccount() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof6@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca6@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));
        TokenAzione token = TokenAzione.perPaziente(TipoToken.INVITO, paziente.getId(), Duration.ofDays(7));
        tokenAzioneRepository.save(token);

        mockMvc.perform(post("/inviti/" + token.getToken() + "/attiva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuovaPassword\":\"password123\"}"))
                .andExpect(status().isNoContent());

        Paziente aggiornato = pazienteRepository.findById(paziente.getId()).orElseThrow();
        assertThat(aggiornato.getStatoAccount()).isEqualTo(StatoAccountPaziente.ATTIVO);
        assertThat(passwordEncoder.matches("password123", aggiornato.getPasswordHash())).isTrue();
    }

    @Test
    void attivaConTokenGiaUsatoRestituisceErrore() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof7@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca7@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));
        TokenAzione token = TokenAzione.perPaziente(TipoToken.INVITO, paziente.getId(), Duration.ofDays(7));
        tokenAzioneRepository.save(token);

        mockMvc.perform(post("/inviti/" + token.getToken() + "/attiva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuovaPassword\":\"password123\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/inviti/" + token.getToken() + "/attiva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuovaPassword\":\"altraPassword1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attivaConEmailGiaInUsoDaUnProfessionistaRestituisce409() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("collisione@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "collisione@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));
        TokenAzione token = TokenAzione.perPaziente(TipoToken.INVITO, paziente.getId(), Duration.ofDays(7));
        tokenAzioneRepository.save(token);

        mockMvc.perform(post("/inviti/" + token.getToken() + "/attiva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuovaPassword\":\"password123\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void attivaConTokenDiTipoResetPasswordRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof8@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca8@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));
        TokenAzione token = TokenAzione.perPaziente(TipoToken.RESET_PASSWORD, paziente.getId(), Duration.ofDays(7));
        tokenAzioneRepository.save(token);

        mockMvc.perform(post("/inviti/" + token.getToken() + "/attiva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuovaPassword\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void flussoCompletoInvitoAttivazioneELoginFunziona() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof9@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca9@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/invito")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isNoContent());

        String corpoEmail = fakeEmailSender.getInviate().get(0).corpoHtml();
        String token = corpoEmail.replaceAll(".*token=([\\w-]+).*", "$1");

        mockMvc.perform(post("/inviti/" + token + "/attiva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuovaPassword\":\"nuovaPassword1\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"luca9@example.com\",\"password\":\"nuovaPassword1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruolo").value("PAZIENTE"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void creaPazienteConPlicometriaJackson3CalcolaEPersisteIRisultati() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-plico-jp3@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-plico-jp3@example.com",
                                 "dataNascita":"2001-01-01","sesso":"M",
                                 "visita":{"dataVisita":"2026-01-15","altezzaCm":180,"pesoKg":80.00,
                                 "plicometria":{"protocollo":"JACKSON_POLLOCK_3",
                                 "plicaPettoraleMm":10.00,"plicaAddominaleMm":10.00,"plicaCosciaMm":10.00}}}
                                """))
                .andExpect(status().isCreated());

        List<Plicometria> plicometrie = plicometriaRepository.findAll();
        assertThat(plicometrie).hasSize(1);
        Plicometria plicometria = plicometrie.get(0);
        assertThat(plicometria.getEtaAnni()).isEqualTo(25);
        assertThat(plicometria.getPercentualeGrasso().doubleValue()).isCloseTo(8.51, org.assertj.core.data.Offset.offset(0.1));
        assertThat(plicometria.getMassaGrassaKg().doubleValue()).isCloseTo(6.81, org.assertj.core.data.Offset.offset(0.1));
        assertThat(plicometria.getMassaMagraKg().doubleValue()).isCloseTo(73.19, org.assertj.core.data.Offset.offset(0.1));
        assertThat(plicometria.isLimiteSicurezzaApplicato()).isFalse();
    }

    @Test
    void creaPazienteConPlicheVicineAZeroApplicaIlLimiteDiSicurezzaESegnalaIlFlag() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-plico-limite@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-plico-limite@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M",
                                 "visita":{"altezzaCm":180,"pesoKg":80.00,
                                 "plicometria":{"protocollo":"EVANS_ATLETI",
                                 "plicaTricipitaleMm":0.30,"plicaAddominaleMm":0.30,"plicaCosciaMm":0.30}}}
                                """))
                .andExpect(status().isCreated());

        Plicometria plicometria = plicometriaRepository.findAll().get(0);
        assertThat(plicometria.isLimiteSicurezzaApplicato()).isTrue();
        assertThat(plicometria.getPercentualeGrasso()).isEqualByComparingTo("3.00");
    }

    @Test
    void creaPazienteConPlicometriaESessoAltroRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-plico-altro@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-plico-altro@example.com",
                                 "dataNascita":"1990-05-20","sesso":"ALTRO",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5,
                                 "plicometria":{"protocollo":"FAULKNER_4",
                                 "plicaTricipitaleMm":10.00,"plicaSottoscapolareMm":10.00,
                                 "plicaSoprailiacaMm":10.00,"plicaAddominaleMm":10.00}}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creaPazienteConProtocolloJackson3EPlicaMancanteRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-plico-mancante@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-plico-mancante@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5,
                                 "plicometria":{"protocollo":"JACKSON_POLLOCK_3","plicaPettoraleMm":10.00}}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creaPazienteConPlicaTricipitaleEBraccioRilassatoCalcolaMamc() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-mamc@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-mamc@example.com",
                                 "dataNascita":"2001-01-01","sesso":"F",
                                 "visita":{"dataVisita":"2026-01-15","altezzaCm":165,"pesoKg":65.00,
                                 "circonferenzaBraccioRilassatoCm":32.00,
                                 "plicometria":{"protocollo":"JACKSON_POLLOCK_3",
                                 "plicaTricipitaleMm":16.00,"plicaSoprailiacaMm":10.00,"plicaCosciaMm":15.00}}}
                                """))
                .andExpect(status().isCreated());

        Visita visita = visitaRepository.findAll().get(0);
        assertThat(visita.getMamcCm()).isEqualByComparingTo("26.97");
    }

    @Test
    void creaPazienteSenzaBraccioRilassatoNonCalcolaMamcAncheConPlicaTricipitale() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-mamc-senza-braccio@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-mamc-senza-braccio@example.com",
                                 "dataNascita":"2001-01-01","sesso":"F",
                                 "visita":{"dataVisita":"2026-01-15","altezzaCm":165,"pesoKg":65.00,
                                 "plicometria":{"protocollo":"JACKSON_POLLOCK_3",
                                 "plicaTricipitaleMm":16.00,"plicaSoprailiacaMm":10.00,"plicaCosciaMm":15.00}}}
                                """))
                .andExpect(status().isCreated());

        Visita visita = visitaRepository.findAll().get(0);
        assertThat(visita.getMamcCm()).isNull();
    }

    @Test
    void creaPazienteConDurninWomersleyEEtaSottoLaFasciaMinimaRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-plico-durnin-eta@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-plico-durnin-eta@example.com",
                                 "dataNascita":"2020-01-01","sesso":"M",
                                 "visita":{"dataVisita":"2026-01-15","altezzaCm":120,"pesoKg":25.0,
                                 "plicometria":{"protocollo":"DURNIN_WOMERSLEY_4",
                                 "plicaBicipitaleMm":8.00,"plicaTricipitaleMm":15.00,
                                 "plicaSottoscapolareMm":14.00,"plicaSoprailiacaMm":12.00}}}
                                """))
                .andExpect(status().isBadRequest());
    }
}
