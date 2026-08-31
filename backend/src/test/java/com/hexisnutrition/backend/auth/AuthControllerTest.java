package com.hexisnutrition.backend.auth;

import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.StatoAccountPaziente;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import com.hexisnutrition.backend.support.TestEmailConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(TestEmailConfig.class)
class AuthControllerTest extends AbstractIntegrationTest {

    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @Autowired
    private PazienteRepository pazienteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.hexisnutrition.backend.inviti.TokenAzioneRepository tokenAzioneRepository;

    @Autowired
    private com.hexisnutrition.backend.email.FakeEmailSender fakeEmailSender;

    @Autowired
    private JwtService jwtService;

    @AfterEach
    void pulisci() {
        tokenAzioneRepository.deleteAll();
        pazienteRepository.deleteAll();
        professionistaRepository.deleteAll();
        fakeEmailSender.getInviate().clear();
    }

    @Test
    void loginProfessionistaConCredenzialiCorretteRestituisceToken() throws Exception {
        professionistaRepository.save(new Professionista(
                "mario@example.com", passwordEncoder.encode("password123"), "Mario", "Rossi"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mario@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruolo").value("PROFESSIONISTA"))
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void loginConPasswordErrataRestituisce401() throws Exception {
        professionistaRepository.save(new Professionista(
                "mario2@example.com", passwordEncoder.encode("password123"), "Mario", "Rossi"));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"mario2@example.com\",\"password\":\"sbagliata\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pazienteNonAttivoNonPuoFareLogin() throws Exception {
        Professionista professionista = professionistaRepository.save(new Professionista(
                "prof@example.com", passwordEncoder.encode("x"), "Anna", "Bianchi"));
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca@example.com", null, null, null, null, null);
        paziente.setPasswordHash(passwordEncoder.encode("password123"));
        paziente.setStatoAccount(StatoAccountPaziente.INVITATO);
        pazienteRepository.save(paziente);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"luca@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void richiestaResetPasswordInviaEmailSeProfessionistaEsiste() throws Exception {
        professionistaRepository.save(new Professionista(
                "reset@example.com", passwordEncoder.encode("x"), "Anna", "Bianchi"));

        mockMvc.perform(post("/auth/password-dimenticata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reset@example.com\"}"))
                .andExpect(status().isNoContent());

        assertThat(fakeEmailSender.getInviate()).hasSize(1);
        assertThat(fakeEmailSender.getInviate().get(0).destinatario()).isEqualTo("reset@example.com");
    }

    @Test
    void richiestaResetPasswordNonRivelaSeEmailNonEsiste() throws Exception {
        mockMvc.perform(post("/auth/password-dimenticata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"non-esiste@example.com\"}"))
                .andExpect(status().isNoContent());

        assertThat(fakeEmailSender.getInviate()).isEmpty();
    }

    @Test
    void resetPasswordConTokenValidoCambiaLaPassword() throws Exception {
        Professionista professionista = professionistaRepository.save(new Professionista(
                "reset2@example.com", passwordEncoder.encode("vecchia"), "Anna", "Bianchi"));
        var token = com.hexisnutrition.backend.inviti.TokenAzione.perProfessionista(
                com.hexisnutrition.backend.inviti.TipoToken.RESET_PASSWORD,
                professionista.getId(), java.time.Duration.ofHours(1));
        tokenAzioneRepository.save(token);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token.getToken() + "\",\"nuovaPassword\":\"nuovaPassword1\"}"))
                .andExpect(status().isNoContent());

        Professionista aggiornato = professionistaRepository.findById(professionista.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("nuovaPassword1", aggiornato.getPasswordHash())).isTrue();
    }

    @Test
    void resetPasswordConTokenScadutoRestituisceErrore() throws Exception {
        Professionista professionista = professionistaRepository.save(new Professionista(
                "reset3@example.com", passwordEncoder.encode("vecchia"), "Anna", "Bianchi"));
        var token = com.hexisnutrition.backend.inviti.TokenAzione.perProfessionista(
                com.hexisnutrition.backend.inviti.TipoToken.RESET_PASSWORD,
                professionista.getId(), java.time.Duration.ofSeconds(-1));
        tokenAzioneRepository.save(token);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token.getToken() + "\",\"nuovaPassword\":\"nuovaPassword1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPasswordConTokenDiTipoInvitoRestituisce400() throws Exception {
        Professionista professionista = professionistaRepository.save(new Professionista(
                "reset4@example.com", passwordEncoder.encode("vecchia"), "Anna", "Bianchi"));
        var token = com.hexisnutrition.backend.inviti.TokenAzione.perProfessionista(
                com.hexisnutrition.backend.inviti.TipoToken.INVITO,
                professionista.getId(), java.time.Duration.ofHours(1));
        tokenAzioneRepository.save(token);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token.getToken() + "\",\"nuovaPassword\":\"nuovaPassword1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetPasswordConTokenValidoCambiaLaPasswordDelPaziente() throws Exception {
        Professionista professionista = professionistaRepository.save(new Professionista(
                "reset5@example.com", passwordEncoder.encode("x"), "Anna", "Bianchi"));
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca-reset@example.com", null, null, null, null, null);
        paziente.setPasswordHash(passwordEncoder.encode("vecchia"));
        paziente.setStatoAccount(StatoAccountPaziente.ATTIVO);
        paziente = pazienteRepository.save(paziente);

        var token = com.hexisnutrition.backend.inviti.TokenAzione.perPaziente(
                com.hexisnutrition.backend.inviti.TipoToken.RESET_PASSWORD,
                paziente.getId(), java.time.Duration.ofHours(1));
        tokenAzioneRepository.save(token);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token.getToken() + "\",\"nuovaPassword\":\"nuovaPassword1\"}"))
                .andExpect(status().isNoContent());

        Paziente aggiornato = pazienteRepository.findById(paziente.getId()).orElseThrow();
        assertThat(passwordEncoder.matches("nuovaPassword1", aggiornato.getPasswordHash())).isTrue();
    }

    @Test
    void resetPasswordConTokenGiaUsatoRestituisceErrore() throws Exception {
        Professionista professionista = professionistaRepository.save(new Professionista(
                "reset6@example.com", passwordEncoder.encode("vecchia"), "Anna", "Bianchi"));
        var token = com.hexisnutrition.backend.inviti.TokenAzione.perProfessionista(
                com.hexisnutrition.backend.inviti.TipoToken.RESET_PASSWORD,
                professionista.getId(), java.time.Duration.ofHours(1));
        tokenAzioneRepository.save(token);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token.getToken() + "\",\"nuovaPassword\":\"nuovaPassword1\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token.getToken() + "\",\"nuovaPassword\":\"altraPassword1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unaNuovaRichiestaDiResetInvalidaLaPrecedente() throws Exception {
        Professionista professionista = professionistaRepository.save(new Professionista(
                "reset7@example.com", passwordEncoder.encode("vecchia"), "Anna", "Bianchi"));
        var primoToken = com.hexisnutrition.backend.inviti.TokenAzione.perProfessionista(
                com.hexisnutrition.backend.inviti.TipoToken.RESET_PASSWORD,
                professionista.getId(), java.time.Duration.ofHours(1));
        tokenAzioneRepository.save(primoToken);

        mockMvc.perform(post("/auth/password-dimenticata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"reset7@example.com\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + primoToken.getToken() + "\",\"nuovaPassword\":\"nuovaPassword1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unaNuovaRichiestaDiResetInvalidaLaPrecedenteDelPaziente() throws Exception {
        Professionista professionista = professionistaRepository.save(new Professionista(
                "prof10@example.com", passwordEncoder.encode("x"), "Anna", "Bianchi"));
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "luca-reset2@example.com", null, null, null, null, null);
        paziente.setPasswordHash(passwordEncoder.encode("vecchia"));
        paziente.setStatoAccount(StatoAccountPaziente.ATTIVO);
        paziente = pazienteRepository.save(paziente);
        var primoToken = com.hexisnutrition.backend.inviti.TokenAzione.perPaziente(
                com.hexisnutrition.backend.inviti.TipoToken.RESET_PASSWORD,
                paziente.getId(), java.time.Duration.ofHours(1));
        tokenAzioneRepository.save(primoToken);

        mockMvc.perform(post("/auth/password-dimenticata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"luca-reset2@example.com\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + primoToken.getToken() + "\",\"nuovaPassword\":\"nuovaPassword1\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void meRestituisceIDatiDelProfessionistaAutenticato() throws Exception {
        Professionista professionista = professionistaRepository.save(new Professionista(
                "me@example.com", passwordEncoder.encode("password123"), "Anna", "Bianchi"));
        String token = jwtService.generateToken(professionista.getId(), Ruolo.PROFESSIONISTA);

        mockMvc.perform(get("/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Anna"))
                .andExpect(jsonPath("$.cognome").value("Bianchi"))
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.ruolo").value("PROFESSIONISTA"));
    }

    @Test
    void meSenzaTokenRestituisce401() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emailDiResetPasswordContieneLUrlDelFrontendConfigurato() throws Exception {
        professionistaRepository.save(new Professionista(
                "resetlink@example.com", passwordEncoder.encode("x"), "Anna", "Bianchi"));

        mockMvc.perform(post("/auth/password-dimenticata")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"resetlink@example.com\"}"))
                .andExpect(status().isNoContent());

        String corpoEmail = fakeEmailSender.getInviate().get(0).corpoHtml();
        assertThat(corpoEmail).contains("http://localhost:5173/reset-password?token=");
    }
}
