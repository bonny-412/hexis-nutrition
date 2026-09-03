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
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
        fakeEmailSender.reset();
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca@example.com",
                                 "telefono":"333123456","dataNascita":"1990-05-20","sesso":"M",
                                 "lavoro":"Impiegato","tipoLavoro":"ATTIVO",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5,"circonferenzaVitaCm":95.0}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("luca@example.com"))
                .andExpect(jsonPath("$.codiceFiscale").value("RSSMRA80A01H501U"))
                .andExpect(jsonPath("$.lavoro").value("Impiegato"))
                .andExpect(jsonPath("$.tipoLavoro").value("ATTIVO"))
                .andExpect(jsonPath("$.statoAccount").value("MAI_INVITATO"))
                .andExpect(jsonPath("$.archiviato").value(false));

        List<Visita> visite = visitaRepository.findAll();
        assertThat(visite).hasSize(1);
        assertThat(visite.get(0).getAltezzaCm()).isEqualTo(178);
        assertThat(visite.get(0).getPesoKg()).isEqualByComparingTo("82.5");
        assertThat(visite.get(0).getCirconferenzaVitaCm()).isEqualByComparingTo("95.0");
    }

    @Test
    void creaPazienteConNoteEObiettivoLiPersisteERestituisce() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-note-obiettivo@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-note-obiettivo@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M","note":"Allergico ai crostacei",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5,"note":"Prima seduta, molto motivato","obiettivo":"IPERTROFIA"}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.note").value("Allergico ai crostacei"));

        List<Visita> visite = visitaRepository.findAll();
        assertThat(visite).hasSize(1);
        assertThat(visite.get(0).getNote()).isEqualTo("Prima seduta, molto motivato");
        assertThat(visite.get(0).getObiettivo()).isEqualTo(ObiettivoVisita.IPERTROFIA);
    }

    @Test
    void creaPazienteSenzaObiettivoVisitaUsaMantenimentoComeDefault() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-obiettivo-default@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-obiettivo-default@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5}}
                                """))
                .andExpect(status().isCreated());

        List<Visita> visite = visitaRepository.findAll();
        assertThat(visite).hasSize(1);
        assertThat(visite.get(0).getObiettivo()).isEqualTo(ObiettivoVisita.MANTENIMENTO);
        assertThat(visite.get(0).getNote()).isNull();
    }

    @Test
    void creaPazienteConTutteLeCirconferenzeLePersisteNeiCampiCorretti() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-11-misure@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-11-misure@example.com",
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-protocollo-default@example.com",
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-data-visita@example.com",
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-senza-data-visita@example.com",
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-due-decimali@example.com",
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-bmi-whr@example.com",
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
                        .content("{\"nome\":\"Luca\",\"cognome\":\"Verdi\",\"codiceFiscale\":\"RSSMRA80A01H501U\",\"email\":\"luca-senza-visita@example.com\"}"))
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-senza-data-nascita@example.com",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creaPazienteSenzaCodiceFiscaleRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-senza-cf@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca-senza-cf@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creaPazienteConCodiceFiscaleNonValidoRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-cf-non-valido@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"NONVALIDO",
                                 "email":"luca-cf-non-valido@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M",
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-visita-incompleta@example.com",
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-visita-senza-peso@example.com",
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-altezza-zero@example.com",
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-peso-negativo@example.com",
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-lavoro@example.com",
                                 "dataNascita":"1990-05-20","sesso":"M",
                                 "lavoro":"Impiegato","tipoLavoro":"ATTIVO",
                                 "visita":{"altezzaCm":178,"pesoKg":82.5}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.lavoro").value("Impiegato"))
                .andExpect(jsonPath("$.tipoLavoro").value("ATTIVO"));
    }

    @Test
    void aggiornaPazienteAggiornaICampiAnagraficiERestituisce200() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-aggiorna@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-aggiorna@example.com", "333123456", LocalDate.of(1990, 1, 1), Sesso.M,
                "Impiegato", TipoLavoro.SEDENTARIO, "Nota iniziale"));

        mockMvc.perform(put("/pazienti/" + paziente.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Rossi","codiceFiscale":"RSSMRA80A01H501U",
                                 "email":"luca-aggiornato@example.com","telefono":"333999888",
                                 "dataNascita":"1990-01-01","sesso":"M","lavoro":"Libero professionista",
                                 "tipoLavoro":"ATTIVO","note":"Nota aggiornata"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cognome").value("Rossi"))
                .andExpect(jsonPath("$.email").value("luca-aggiornato@example.com"))
                .andExpect(jsonPath("$.telefono").value("333999888"))
                .andExpect(jsonPath("$.lavoro").value("Libero professionista"))
                .andExpect(jsonPath("$.tipoLavoro").value("ATTIVO"))
                .andExpect(jsonPath("$.note").value("Nota aggiornata"));

        Paziente aggiornato = pazienteRepository.findById(paziente.getId()).orElseThrow();
        assertThat(aggiornato.getCognome()).isEqualTo("Rossi");
        assertThat(aggiornato.getEmail()).isEqualTo("luca-aggiornato@example.com");
    }

    @Test
    void aggiornaPazienteNonModificaLeVisiteEsistenti() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-aggiorna-visita@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-aggiorna-visita@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M,
                null, null, null));
        Visita visita = new Visita(paziente.getId(), LocalDate.of(2026, 1, 1), 178, new BigDecimal("80.0"),
                null, null, null, null, null, null, null, null, null, null, null, ProtocolloVita.OMS, null, null);
        visitaRepository.save(visita);

        mockMvc.perform(put("/pazienti/" + paziente.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Rossi","codiceFiscale":"RSSMRA80A01H501U",
                                 "email":"luca-aggiorna-visita@example.com",
                                 "dataNascita":"1990-01-01","sesso":"M"}
                                """))
                .andExpect(status().isOk());

        List<Visita> visite = visitaRepository.findAll();
        assertThat(visite).hasSize(1);
        assertThat(visite.get(0).getAltezzaCm()).isEqualTo(178);
    }

    @Test
    void aggiornaPazienteDiAltroProfessionistaRestituisce404() throws Exception {
        Professionista professionistaA = professionistaRepository.save(
                new Professionista("prof-aggiorna-a@example.com", "hash", "A", "A"));
        Professionista professionistaB = professionistaRepository.save(
                new Professionista("prof-aggiorna-b@example.com", "hash", "B", "B"));
        Paziente pazienteDiB = pazienteRepository.save(new Paziente(professionistaB.getId(), "Paziente", "DiB",
                "RSSMRA80A01H501U", "diB-aggiorna@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));

        mockMvc.perform(put("/pazienti/" + pazienteDiB.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionistaA))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Paziente","cognome":"DiB","codiceFiscale":"RSSMRA80A01H501U",
                                 "email":"diB-aggiorna@example.com","dataNascita":"1990-01-01","sesso":"M"}
                                """))
                .andExpect(status().isNotFound());
    }

    @Test
    void aggiornaPazienteConEmailNonValidaRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-aggiorna-email@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-aggiorna-email@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M,
                null, null, null));

        mockMvc.perform(put("/pazienti/" + paziente.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U",
                                 "email":"non-una-email","dataNascita":"1990-01-01","sesso":"M"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaRestituisceSoloIPazientiDelProfessionistaAutenticato() throws Exception {
        Professionista professionistaA = professionistaRepository.save(
                new Professionista("a@example.com", "hash", "A", "A"));
        Professionista professionistaB = professionistaRepository.save(
                new Professionista("b@example.com", "hash", "B", "B"));
        pazienteRepository.save(new Paziente(professionistaA.getId(), "Paziente", "DiA",
                "RSSMRA80A01H501U", "diA@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));
        pazienteRepository.save(new Paziente(professionistaB.getId(), "Paziente", "DiB",
                "RSSMRA80A01H501U", "diB@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));

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
                "RSSMRA80A01H501U", "diB2@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));

        mockMvc.perform(get("/pazienti/" + pazienteDiB.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionistaA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unPazienteAutenticatoNonPuoCreareAltriPazienti() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof3@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca3@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));
        String tokenPaziente = jwtService.generateToken(paziente.getId(), Ruolo.PAZIENTE);

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPaziente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"cognome\":\"Y\",\"codiceFiscale\":\"RSSMRA80A01H501U\",\"email\":\"x@example.com\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void invitoGeneraTokenCambiaStatoEInviaEmail() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof4@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca4@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));

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
                "RSSMRA80A01H501U", "luca5@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null);
        paziente.setStatoAccount(StatoAccountPaziente.ATTIVO);
        paziente.setPasswordHash("hash");
        pazienteRepository.save(paziente);

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/invito")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isConflict());
    }

    @Test
    void invitoAPazienteArchiviatoRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-arch-invito@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-arch-invito@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null);
        paziente.setArchiviato(true);
        pazienteRepository.save(paziente);

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/invito")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invitoConEmailNonInviataRestituisce502ENonModificaLoStato() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-email-fallita@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-email-fallita@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));
        fakeEmailSender.simulaFallimento();

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/invito")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isBadGateway());

        Paziente nonModificato = pazienteRepository.findById(paziente.getId()).orElseThrow();
        assertThat(nonModificato.getStatoAccount()).isEqualTo(StatoAccountPaziente.MAI_INVITATO);
        assertThat(tokenAzioneRepository.count()).isZero();
    }

    @Test
    void archiviaImpostaIlFlagArchiviato() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-archivia@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-archivia@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/archivia")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isNoContent());

        Paziente aggiornato = pazienteRepository.findById(paziente.getId()).orElseThrow();
        assertThat(aggiornato.isArchiviato()).isTrue();
    }

    @Test
    void deArchiviaRimuoveIlFlagArchiviato() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-dearchivia@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-dearchivia@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null);
        paziente.setArchiviato(true);
        pazienteRepository.save(paziente);

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/de-archivia")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isNoContent());

        Paziente aggiornato = pazienteRepository.findById(paziente.getId()).orElseThrow();
        assertThat(aggiornato.isArchiviato()).isFalse();
    }

    @Test
    void nonSiPuoArchiviareUnPazienteDiUnAltroProfessionista() throws Exception {
        Professionista professionistaA = professionistaRepository.save(
                new Professionista("prof-a-arch@example.com", "hash", "A", "A"));
        Professionista professionistaB = professionistaRepository.save(
                new Professionista("prof-b-arch@example.com", "hash", "B", "B"));
        Paziente pazienteDiB = pazienteRepository.save(new Paziente(professionistaB.getId(), "Paziente", "DiB",
                "RSSMRA80A01H501U", "diB-arch@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));

        mockMvc.perform(post("/pazienti/" + pazienteDiB.getId() + "/archivia")
                        .header("Authorization", "Bearer " + tokenPer(professionistaA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void ricercaSenzaParametriRestituiscePaginaDefaultEscludendoArchiviati() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-ricerca1@example.com", "hash", "Anna", "Bianchi"));
        pazienteRepository.save(new Paziente(professionista.getId(), "Marco", "Rossi",
                "RSSMRC90A01H501U", "marco-ricerca1@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));
        Paziente archiviato = new Paziente(professionista.getId(), "Giulia", "Verdi",
                "VRDGLI85A41H501U", "giulia-ricerca1@example.com", null, LocalDate.of(1985, 3, 10), Sesso.F, null, null, null);
        archiviato.setArchiviato(true);
        pazienteRepository.save(archiviato);

        mockMvc.perform(get("/pazienti/ricerca")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()").value(1))
                .andExpect(jsonPath("$.contenuto[0].email").value("marco-ricerca1@example.com"))
                .andExpect(jsonPath("$.paginaCorrente").value(0))
                .andExpect(jsonPath("$.dimensionePagina").value(20))
                .andExpect(jsonPath("$.totaleElementi").value(1))
                .andExpect(jsonPath("$.totalePagine").value(1));
    }

    @Test
    void ricercaConArchiviatoTrueRestituisceSoloGliArchiviati() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-ricerca2@example.com", "hash", "Anna", "Bianchi"));
        Paziente archiviato = new Paziente(professionista.getId(), "Giulia", "Verdi",
                "VRDGLI85A41H501U", "giulia-ricerca2@example.com", null, LocalDate.of(1985, 3, 10), Sesso.F, null, null, null);
        archiviato.setArchiviato(true);
        pazienteRepository.save(archiviato);
        pazienteRepository.save(new Paziente(professionista.getId(), "Marco", "Rossi",
                "RSSMRC90A01H501U", "marco-ricerca2@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));

        mockMvc.perform(get("/pazienti/ricerca?archiviato=true")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()").value(1))
                .andExpect(jsonPath("$.contenuto[0].email").value("giulia-ricerca2@example.com"));
    }

    @Test
    void ricercaConTestoLibero() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-ricerca3@example.com", "hash", "Anna", "Bianchi"));
        pazienteRepository.save(new Paziente(professionista.getId(), "Marco", "Rossi",
                "RSSMRC90A01H501U", "marco-ricerca3@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));
        pazienteRepository.save(new Paziente(professionista.getId(), "Giulia", "Verdi",
                "VRDGLI85A41H501U", "giulia-ricerca3@example.com", null, LocalDate.of(1985, 3, 10), Sesso.F, null, null, null));

        mockMvc.perform(get("/pazienti/ricerca?ricerca=giulia")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()").value(1))
                .andExpect(jsonPath("$.contenuto[0].nome").value("Giulia"));
    }

    @Test
    void ricercaConStatoAccountSessoEIntervalloDataNascita() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-ricerca4@example.com", "hash", "Anna", "Bianchi"));
        Paziente match = new Paziente(professionista.getId(), "Marco", "Rossi",
                "RSSMRC90A01H501U", "marco-ricerca4@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null);
        match.setStatoAccount(StatoAccountPaziente.ATTIVO);
        pazienteRepository.save(match);
        pazienteRepository.save(new Paziente(professionista.getId(), "Marco", "Bianchi",
                "BNCMRC70A01H501U", "marco-vecchio-ricerca4@example.com", null, LocalDate.of(1970, 1, 1), Sesso.M, null, null, null));

        mockMvc.perform(get("/pazienti/ricerca")
                        .param("statoAccount", "ATTIVO")
                        .param("sesso", "M")
                        .param("dataNascitaDa", "1985-01-01")
                        .param("dataNascitaA", "1995-01-01")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()").value(1))
                .andExpect(jsonPath("$.contenuto[0].email").value("marco-ricerca4@example.com"));
    }

    @Test
    void ricercaOrdinaEPaginaIRisultati() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-ricerca5@example.com", "hash", "Anna", "Bianchi"));
        pazienteRepository.save(new Paziente(professionista.getId(), "Carlo", "Neri",
                "NRICRL90A01H501U", "carlo-ricerca5@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));
        pazienteRepository.save(new Paziente(professionista.getId(), "Anna", "Bruni",
                "BRNANN90A01H501U", "anna-ricerca5@example.com", null, LocalDate.of(1990, 1, 1), Sesso.F, null, null, null));
        pazienteRepository.save(new Paziente(professionista.getId(), "Bruno", "Villa",
                "VLLBRN90A01H501U", "bruno-ricerca5@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));

        mockMvc.perform(get("/pazienti/ricerca")
                        .param("dimensione", "2")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()").value(2))
                .andExpect(jsonPath("$.contenuto[0].nome").value("Anna"))
                .andExpect(jsonPath("$.contenuto[1].nome").value("Bruno"))
                .andExpect(jsonPath("$.totaleElementi").value(3))
                .andExpect(jsonPath("$.totalePagine").value(2));

        mockMvc.perform(get("/pazienti/ricerca")
                        .param("dimensione", "2")
                        .param("pagina", "1")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto.length()").value(1))
                .andExpect(jsonPath("$.contenuto[0].nome").value("Carlo"))
                .andExpect(jsonPath("$.paginaCorrente").value(1));

        mockMvc.perform(get("/pazienti/ricerca")
                        .param("ordinaPer", "nome")
                        .param("direzione", "desc")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenuto[0].nome").value("Carlo"))
                .andExpect(jsonPath("$.contenuto[2].nome").value("Anna"));
    }

    @Test
    void ricercaSenzaAutenticazioneRestituisce401() throws Exception {
        mockMvc.perform(get("/pazienti/ricerca"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void archiviareUnPazienteGiaArchiviatoNonProduceErrori() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-arch-idem@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-arch-idem@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null);
        paziente.setArchiviato(true);
        pazienteRepository.save(paziente);

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/archivia")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isNoContent());

        assertThat(pazienteRepository.findById(paziente.getId()).orElseThrow().isArchiviato()).isTrue();
    }

    @Test
    void deArchiviareUnPazienteNonArchiviatoNonProduceErrori() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-dearch-idem@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-dearch-idem@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/de-archivia")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isNoContent());

        assertThat(pazienteRepository.findById(paziente.getId()).orElseThrow().isArchiviato()).isFalse();
    }

    @Test
    void attivaConTokenValidoImpostaPasswordEAttivaAccount() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof6@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca6@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));
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
                "RSSMRA80A01H501U", "luca7@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));
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
                "RSSMRA80A01H501U", "collisione@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));
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
                "RSSMRA80A01H501U", "luca8@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));
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
                "RSSMRA80A01H501U", "luca9@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));

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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-plico-jp3@example.com",
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-plico-limite@example.com",
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-plico-altro@example.com",
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-plico-mancante@example.com",
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-mamc@example.com",
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-mamc-senza-braccio@example.com",
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
                                {"nome":"Luca","cognome":"Verdi","codiceFiscale":"RSSMRA80A01H501U","email":"luca-plico-durnin-eta@example.com",
                                 "dataNascita":"2020-01-01","sesso":"M",
                                 "visita":{"dataVisita":"2026-01-15","altezzaCm":120,"pesoKg":25.0,
                                 "plicometria":{"protocollo":"DURNIN_WOMERSLEY_4",
                                 "plicaBicipitaleMm":8.00,"plicaTricipitaleMm":15.00,
                                 "plicaSottoscapolareMm":14.00,"plicaSoprailiacaMm":12.00}}}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void visiteRestituisceListaVuotaSeIlPazienteNonHaVisite() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-visite-vuote@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-visite-vuote@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));

        mockMvc.perform(get("/pazienti/" + paziente.getId() + "/visite")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void visiteRestituisceInOrdineCronologicoConPlicometriaSoloDoveApplicata() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-visite-ordine@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca-visite-ordine@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));

        Visita piuRecente = new Visita(paziente.getId(), LocalDate.of(2026, 8, 1), 178, new BigDecimal("77.5"),
                null, null, null, null, null, null, null, null, null, null, null, ProtocolloVita.OMS, null, null);
        piuRecente.setBmi(new BigDecimal("24.4"));
        Visita piuVecchia = new Visita(paziente.getId(), LocalDate.of(2026, 6, 1), 178, new BigDecimal("80.0"),
                null, null, null, null, null, null, null, null, null, null, null, ProtocolloVita.OMS, null, null);
        piuVecchia.setBmi(new BigDecimal("25.2"));
        visitaRepository.save(piuRecente);
        visitaRepository.save(piuVecchia);

        plicometriaRepository.save(new Plicometria(piuRecente.getId(), ProtocolloPlicometrico.JACKSON_POLLOCK_3, "v1", 36,
                null, null, null,
                null, null, new BigDecimal("12.5"),
                null, null, null,
                new BigDecimal("15.0"), new BigDecimal("14.0"), null,
                new BigDecimal("41.5"), new BigDecimal("1.06"), new BigDecimal("18.2"),
                new BigDecimal("14.1"), new BigDecimal("63.4"), new BigDecimal("4.4"), new BigDecimal("20.1"),
                false));

        mockMvc.perform(get("/pazienti/" + paziente.getId() + "/visite")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].dataVisita").value("2026-06-01"))
                .andExpect(jsonPath("$[0].bmi").value(25.2))
                .andExpect(jsonPath("$[0].plicometria").value(Matchers.nullValue()))
                .andExpect(jsonPath("$[1].dataVisita").value("2026-08-01"))
                .andExpect(jsonPath("$[1].bmi").value(24.4))
                .andExpect(jsonPath("$[1].plicometria.percentualeGrassoCorporeo").value(18.2));
    }

    @Test
    void visiteDiPazienteDiAltroProfessionistaRestituisce404() throws Exception {
        Professionista professionistaA = professionistaRepository.save(
                new Professionista("prof-visite-a@example.com", "hash", "A", "A"));
        Professionista professionistaB = professionistaRepository.save(
                new Professionista("prof-visite-b@example.com", "hash", "B", "B"));
        Paziente pazienteDiB = pazienteRepository.save(new Paziente(professionistaB.getId(), "Paziente", "DiB",
                "RSSMRA80A01H501U", "diB-visite@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));

        mockMvc.perform(get("/pazienti/" + pazienteDiB.getId() + "/visite")
                        .header("Authorization", "Bearer " + tokenPer(professionistaA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void visiteSenzaAutenticazioneRestituisce401() throws Exception {
        mockMvc.perform(get("/pazienti/" + UUID.randomUUID() + "/visite"))
                .andExpect(status().isUnauthorized());
    }
}
