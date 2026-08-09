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
    private TokenAzioneRepository tokenAzioneRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private FakeEmailSender fakeEmailSender;

    @AfterEach
    void pulisci() {
        tokenAzioneRepository.deleteAll();
        pazienteRepository.deleteAll();
        professionistaRepository.deleteAll();
        fakeEmailSender.getInviate().clear();
    }

    private String tokenPer(Professionista professionista) {
        return jwtService.generateToken(professionista.getId(), Ruolo.PROFESSIONISTA);
    }

    @Test
    void creaPazienteRestituisce201() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof@example.com", "hash", "Anna", "Bianchi"));

        mockMvc.perform(post("/pazienti")
                        .header("Authorization", "Bearer " + tokenPer(professionista))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Luca","cognome":"Verdi","email":"luca@example.com",
                                 "telefono":"333123456","dataNascita":"1990-05-20","sesso":"M","altezzaCm":178}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("luca@example.com"))
                .andExpect(jsonPath("$.statoAccount").value("MAI_INVITATO"));
    }

    @Test
    void listaRestituisceSoloIPazientiDelProfessionistaAutenticato() throws Exception {
        Professionista professionistaA = professionistaRepository.save(
                new Professionista("a@example.com", "hash", "A", "A"));
        Professionista professionistaB = professionistaRepository.save(
                new Professionista("b@example.com", "hash", "B", "B"));
        pazienteRepository.save(new Paziente(professionistaA.getId(), "Paziente", "DiA",
                "diA@example.com", null, null, null, null));
        pazienteRepository.save(new Paziente(professionistaB.getId(), "Paziente", "DiB",
                "diB@example.com", null, null, null, null));

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
                "diB2@example.com", null, null, null, null));

        mockMvc.perform(get("/pazienti/" + pazienteDiB.getId())
                        .header("Authorization", "Bearer " + tokenPer(professionistaA)))
                .andExpect(status().isNotFound());
    }

    @Test
    void unPazienteAutenticatoNonPuoCreareAltriPazienti() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof3@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca3@example.com", null, null, null, null));
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
                "luca4@example.com", null, null, null, null));

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
                "luca5@example.com", null, null, null, null);
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
                "luca6@example.com", null, null, null, null));
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
                "luca7@example.com", null, null, null, null));
        TokenAzione token = TokenAzione.perPaziente(TipoToken.INVITO, paziente.getId(), Duration.ofDays(7));
        token.segnaUsato();
        tokenAzioneRepository.save(token);

        mockMvc.perform(post("/inviti/" + token.getToken() + "/attiva")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nuovaPassword\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void attivaConEmailGiaInUsoDaUnProfessionistaRestituisce409() throws Exception {
        Professionista professionista = professionistaRepository.save(
                new Professionista("collisione@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "collisione@example.com", null, null, null, null));
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
                "luca8@example.com", null, null, null, null));
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
                "luca9@example.com", null, null, null, null));

        mockMvc.perform(post("/pazienti/" + paziente.getId() + "/invito")
                        .header("Authorization", "Bearer " + tokenPer(professionista)))
                .andExpect(status().isNoContent());

        TokenAzione token = tokenAzioneRepository.findAll().stream()
                .filter(t -> paziente.getId().equals(t.getPazienteId()) && t.getTipo() == TipoToken.INVITO)
                .findFirst()
                .orElseThrow();

        mockMvc.perform(post("/inviti/" + token.getToken() + "/attiva")
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
}
