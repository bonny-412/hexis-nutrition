package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.email.EmailSender;
import com.hexisnutrition.backend.inviti.EmailGiaInUsoException;
import com.hexisnutrition.backend.inviti.TipoToken;
import com.hexisnutrition.backend.inviti.TokenAzione;
import com.hexisnutrition.backend.inviti.TokenAzioneRepository;
import com.hexisnutrition.backend.inviti.TokenNonValidoException;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class PazienteService {

    private static final Logger log = LoggerFactory.getLogger(PazienteService.class);

    private final PazienteRepository pazienteRepository;
    private final VisitaRepository visitaRepository;
    private final ProfessionistaRepository professionistaRepository;
    private final TokenAzioneRepository tokenAzioneRepository;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final PlicometriaService plicometriaService;

    public PazienteService(PazienteRepository pazienteRepository,
                            VisitaRepository visitaRepository,
                            ProfessionistaRepository professionistaRepository,
                            TokenAzioneRepository tokenAzioneRepository,
                            EmailSender emailSender,
                            PasswordEncoder passwordEncoder,
                            PlicometriaService plicometriaService) {
        this.pazienteRepository = pazienteRepository;
        this.visitaRepository = visitaRepository;
        this.professionistaRepository = professionistaRepository;
        this.tokenAzioneRepository = tokenAzioneRepository;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.plicometriaService = plicometriaService;
    }

    @Transactional
    public Paziente crea(UUID professionistaId, CreaPazienteRequest request) {
        Paziente paziente = new Paziente(professionistaId, request.nome(), request.cognome(), request.codiceFiscale(), request.email(),
                request.telefono(), request.dataNascita(), request.sesso(), request.lavoro(), request.tipoLavoro());
        pazienteRepository.save(paziente);

        VisitaRequest v = request.visita();
        Visita visita = new Visita(paziente.getId(), v.dataVisita(), v.altezzaCm(), v.pesoKg(),
                v.circonferenzaVitaCm(), v.circonferenzaFianchiCm(), v.circonferenzaAddomeCm(),
                v.circonferenzaBraccioRilassatoCm(), v.circonferenzaCosciaCm(), v.circonferenzaPolpaccioCm(),
                v.circonferenzaColloCm(), v.circonferenzaToraceCm(), v.circonferenzaBraccioContrattoCm(),
                v.circonferenzaAvambraccioCm(), v.circonferenzaCavigliaCm(), v.protocolloVita());
        VisitaCalcoli.applica(visita);
        visitaRepository.save(visita);
        plicometriaService.elabora(paziente, visita, v.plicometria());

        return paziente;
    }

    public List<Paziente> listaPerProfessionista(UUID professionistaId) {
        return pazienteRepository.findAllByProfessionistaId(professionistaId);
    }

    public Page<Paziente> cerca(UUID professionistaId, CriteriRicercaPazienti criteri, Pageable pageable) {
        List<Specification<Paziente>> specifiche = new ArrayList<>();
        specifiche.add(PazienteSpecifications.delProfessionista(professionistaId));
        specifiche.add(PazienteSpecifications.conArchiviato(criteri.archiviato()));
        if (criteri.ricerca() != null && !criteri.ricerca().isBlank()) {
            specifiche.add(PazienteSpecifications.conRicerca(criteri.ricerca()));
        }
        if (criteri.statoAccount() != null) {
            specifiche.add(PazienteSpecifications.conStatoAccount(criteri.statoAccount()));
        }
        if (criteri.sesso() != null) {
            specifiche.add(PazienteSpecifications.conSesso(criteri.sesso()));
        }
        if (criteri.dataNascitaDa() != null || criteri.dataNascitaA() != null) {
            specifiche.add(PazienteSpecifications.conDataNascitaTra(criteri.dataNascitaDa(), criteri.dataNascitaA()));
        }
        return pazienteRepository.findAll(Specification.allOf(specifiche), pageable);
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
        if (paziente.isArchiviato()) {
            throw new PazienteArchiviatoException();
        }
        if (paziente.getStatoAccount() == StatoAccountPaziente.ATTIVO) {
            throw new PazienteGiaAttivoException();
        }
        TokenAzione token = TokenAzione.perPaziente(TipoToken.INVITO, paziente.getId(), Duration.ofDays(7));
        tokenAzioneRepository.save(token);
        paziente.setStatoAccount(StatoAccountPaziente.INVITATO);
        pazienteRepository.save(paziente);
        try {
            emailSender.invia(paziente.getEmail(), "Sei stato invitato su Hexis Nutrition",
                    "<p>Attiva il tuo account: <a href=\"https://app.hexisnutrition.example/attiva?token="
                            + token.getToken() + "\">Attiva account</a></p>");
        } catch (RestClientException e) {
            log.warn("Invio email di invito fallito per il paziente {}", paziente.getId(), e);
            throw new InvioEmailFallitoException(e);
        }
    }

    @Transactional
    public void archivia(UUID professionistaId, UUID pazienteId) {
        Paziente paziente = dettaglio(professionistaId, pazienteId);
        paziente.setArchiviato(true);
        pazienteRepository.save(paziente);
    }

    @Transactional
    public void deArchivia(UUID professionistaId, UUID pazienteId) {
        Paziente paziente = dettaglio(professionistaId, pazienteId);
        paziente.setArchiviato(false);
        pazienteRepository.save(paziente);
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
