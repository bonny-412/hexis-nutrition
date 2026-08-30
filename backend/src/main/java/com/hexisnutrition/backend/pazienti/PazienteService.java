package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.email.EmailSender;
import com.hexisnutrition.backend.inviti.EmailGiaInUsoException;
import com.hexisnutrition.backend.inviti.TipoToken;
import com.hexisnutrition.backend.inviti.TokenAzione;
import com.hexisnutrition.backend.inviti.TokenAzioneRepository;
import com.hexisnutrition.backend.inviti.TokenNonValidoException;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class PazienteService {

    private final PazienteRepository pazienteRepository;
    private final ProfessionistaRepository professionistaRepository;
    private final TokenAzioneRepository tokenAzioneRepository;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;

    public PazienteService(PazienteRepository pazienteRepository,
                            ProfessionistaRepository professionistaRepository,
                            TokenAzioneRepository tokenAzioneRepository,
                            EmailSender emailSender,
                            PasswordEncoder passwordEncoder) {
        this.pazienteRepository = pazienteRepository;
        this.professionistaRepository = professionistaRepository;
        this.tokenAzioneRepository = tokenAzioneRepository;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
    }

    public Paziente crea(UUID professionistaId, CreaPazienteRequest request) {
        Paziente paziente = new Paziente(professionistaId, request.nome(), request.cognome(), request.email(),
                request.telefono(), request.dataNascita(), request.sesso(), request.altezzaCm());
        return pazienteRepository.save(paziente);
    }

    public List<Paziente> listaPerProfessionista(UUID professionistaId) {
        return pazienteRepository.findAllByProfessionistaId(professionistaId);
    }

    public Paziente dettaglio(UUID professionistaId, UUID pazienteId) {
        Paziente paziente = pazienteRepository.findById(pazienteId)
                .orElseThrow(PazienteNonTrovatoException::new);
        if (!paziente.getProfessionistaId().equals(professionistaId)) {
            throw new PazienteNonTrovatoException();
        }
        return paziente;
    }

    @Transactional
    public void invita(UUID professionistaId, UUID pazienteId) {
        Paziente paziente = dettaglio(professionistaId, pazienteId);
        if (paziente.getStatoAccount() == StatoAccountPaziente.ATTIVO) {
            throw new PazienteGiaAttivoException();
        }
        TokenAzione token = TokenAzione.perPaziente(TipoToken.INVITO, paziente.getId(), Duration.ofDays(7));
        tokenAzioneRepository.save(token);
        paziente.setStatoAccount(StatoAccountPaziente.INVITATO);
        pazienteRepository.save(paziente);
        emailSender.invia(paziente.getEmail(), "Sei stato invitato su Hexis Nutrition",
                "<p>Attiva il tuo account: <a href=\"https://app.hexisnutrition.example/attiva?token="
                        + token.getToken() + "\">Attiva account</a></p>");
    }

    @Transactional
    public void attiva(String token, String nuovaPassword) {
        TokenAzione tokenAzione = tokenAzioneRepository.findByTokenHash(TokenAzione.hash(token))
                .filter(TokenAzione::isValido)
                .filter(t -> t.getTipo() == TipoToken.INVITO)
                .orElseThrow(TokenNonValidoException::new);

        Paziente paziente = pazienteRepository.findById(tokenAzione.getPazienteId())
                .orElseThrow(PazienteNonTrovatoException::new);

        boolean emailUsataDaProfessionista = professionistaRepository.findByEmail(paziente.getEmail()).isPresent();
        boolean emailUsataDaAltroPazienteAttivo = pazienteRepository
                .existsByEmailAndStatoAccount(paziente.getEmail(), StatoAccountPaziente.ATTIVO);
        if (emailUsataDaProfessionista || emailUsataDaAltroPazienteAttivo) {
            throw new EmailGiaInUsoException();
        }

        paziente.setPasswordHash(passwordEncoder.encode(nuovaPassword));
        paziente.setStatoAccount(StatoAccountPaziente.ATTIVO);
        pazienteRepository.save(paziente);

        tokenAzioneRepository.delete(tokenAzione);
    }
}
