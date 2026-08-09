package com.hexisnutrition.backend.auth;

import com.hexisnutrition.backend.email.EmailSender;
import com.hexisnutrition.backend.inviti.TipoToken;
import com.hexisnutrition.backend.inviti.TokenAzione;
import com.hexisnutrition.backend.inviti.TokenAzioneRepository;
import com.hexisnutrition.backend.inviti.TokenNonValidoException;
import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.StatoAccountPaziente;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final ProfessionistaRepository professionistaRepository;
    private final PazienteRepository pazienteRepository;
    private final TokenAzioneRepository tokenAzioneRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailSender emailSender;

    public AuthService(ProfessionistaRepository professionistaRepository,
                        PazienteRepository pazienteRepository,
                        TokenAzioneRepository tokenAzioneRepository,
                        PasswordEncoder passwordEncoder,
                        JwtService jwtService,
                        EmailSender emailSender) {
        this.professionistaRepository = professionistaRepository;
        this.pazienteRepository = pazienteRepository;
        this.tokenAzioneRepository = tokenAzioneRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailSender = emailSender;
    }

    public LoginResponse login(String email, String password) {
        var professionista = professionistaRepository.findByEmail(email);
        if (professionista.isPresent()
                && passwordEncoder.matches(password, professionista.get().getPasswordHash())) {
            String token = jwtService.generateToken(professionista.get().getId(), Ruolo.PROFESSIONISTA);
            return new LoginResponse(token, Ruolo.PROFESSIONISTA.name());
        }

        var paziente = pazienteRepository.findByEmailAndStatoAccount(email, StatoAccountPaziente.ATTIVO);
        if (paziente.isPresent() && paziente.get().getPasswordHash() != null
                && passwordEncoder.matches(password, paziente.get().getPasswordHash())) {
            String token = jwtService.generateToken(paziente.get().getId(), Ruolo.PAZIENTE);
            return new LoginResponse(token, Ruolo.PAZIENTE.name());
        }

        throw new CredenzialiNonValideException();
    }

    public void richiediResetPassword(String email) {
        professionistaRepository.findByEmail(email).ifPresent(professionista -> {
            TokenAzione token = TokenAzione.perProfessionista(
                    TipoToken.RESET_PASSWORD, professionista.getId(), Duration.ofHours(1));
            tokenAzioneRepository.save(token);
            try {
                emailSender.invia(professionista.getEmail(), "Reimposta la tua password",
                        corpoResetPassword(token.getToken()));
            } catch (Exception e) {
                log.warn("Invio email di reset password fallito per il professionista {}",
                        professionista.getId(), e);
            }
        });

        pazienteRepository.findByEmailAndStatoAccount(email, StatoAccountPaziente.ATTIVO).ifPresent(paziente -> {
            TokenAzione token = TokenAzione.perPaziente(
                    TipoToken.RESET_PASSWORD, paziente.getId(), Duration.ofHours(1));
            tokenAzioneRepository.save(token);
            try {
                emailSender.invia(paziente.getEmail(), "Reimposta la tua password",
                        corpoResetPassword(token.getToken()));
            } catch (Exception e) {
                log.warn("Invio email di reset password fallito per il paziente {}", paziente.getId(), e);
            }
        });
        // Nessuna eccezione se l'email non esiste: evita di rivelare quali indirizzi sono registrati.
    }

    @Transactional
    public void resetPassword(String token, String nuovaPassword) {
        TokenAzione tokenAzione = tokenAzioneRepository.findByToken(token)
                .filter(TokenAzione::isValido)
                .filter(t -> t.getTipo() == TipoToken.RESET_PASSWORD)
                .orElseThrow(TokenNonValidoException::new);

        String hash = passwordEncoder.encode(nuovaPassword);
        if (tokenAzione.getProfessionistaId() != null) {
            Professionista professionista = professionistaRepository.findById(tokenAzione.getProfessionistaId())
                    .orElseThrow(TokenNonValidoException::new);
            professionista.setPasswordHash(hash);
            professionistaRepository.save(professionista);
        } else {
            Paziente paziente = pazienteRepository.findById(tokenAzione.getPazienteId())
                    .orElseThrow(TokenNonValidoException::new);
            paziente.setPasswordHash(hash);
            pazienteRepository.save(paziente);
        }

        tokenAzione.segnaUsato();
        tokenAzioneRepository.save(tokenAzione);
    }

    private String corpoResetPassword(String token) {
        return "<p>Reimposta la password: <a href=\"https://app.hexisnutrition.example/reset-password?token="
                + token + "\">Reimposta password</a></p>";
    }
}
