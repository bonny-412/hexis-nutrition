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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

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
    private final PlicometriaRepository plicometriaRepository;

    public PazienteService(PazienteRepository pazienteRepository,
                            VisitaRepository visitaRepository,
                            ProfessionistaRepository professionistaRepository,
                            TokenAzioneRepository tokenAzioneRepository,
                            EmailSender emailSender,
                            PasswordEncoder passwordEncoder,
                            PlicometriaService plicometriaService,
                            PlicometriaRepository plicometriaRepository) {
        this.pazienteRepository = pazienteRepository;
        this.visitaRepository = visitaRepository;
        this.professionistaRepository = professionistaRepository;
        this.tokenAzioneRepository = tokenAzioneRepository;
        this.emailSender = emailSender;
        this.passwordEncoder = passwordEncoder;
        this.plicometriaService = plicometriaService;
        this.plicometriaRepository = plicometriaRepository;
    }

    @Transactional
    public Paziente crea(UUID professionistaId, CreaPazienteRequest request) {
        Paziente paziente = new Paziente(professionistaId, request.nome(), request.cognome(), request.codiceFiscale(), request.email(),
                request.telefono(), request.dataNascita(), request.sesso(), request.lavoro(), request.stileDiVita(),
                request.note());
        pazienteRepository.save(paziente);

        VisitaRequest v = request.visita();
        Visita visita = new Visita(paziente.getId(), v.dataVisita(), v.altezzaCm(), v.pesoKg(),
                v.circonferenzaVitaCm(), v.circonferenzaFianchiCm(), v.circonferenzaAddomeCm(),
                v.circonferenzaBraccioRilassatoCm(), v.circonferenzaCosciaCm(), v.circonferenzaPolpaccioCm(),
                v.circonferenzaColloCm(), v.circonferenzaToraceCm(), v.circonferenzaBraccioContrattoCm(),
                v.circonferenzaAvambraccioCm(), v.circonferenzaCavigliaCm(), v.protocolloVita(),
                v.note(), v.obiettivo());
        VisitaCalcoli.applica(visita);
        visitaRepository.save(visita);
        plicometriaService.elabora(paziente, visita, v.plicometria());

        return paziente;
    }

    @Transactional
    public Paziente aggiorna(UUID professionistaId, UUID pazienteId, AggiornaPazienteRequest request) {
        Paziente paziente = dettaglio(professionistaId, pazienteId);
        paziente.setNome(request.nome());
        paziente.setCognome(request.cognome());
        paziente.setCodiceFiscale(request.codiceFiscale());
        paziente.setEmail(request.email());
        paziente.setTelefono(request.telefono());
        paziente.setDataNascita(request.dataNascita());
        paziente.setSesso(request.sesso());
        paziente.setLavoro(request.lavoro());
        paziente.setStileDiVita(request.stileDiVita());
        paziente.setNote(request.note());
        return pazienteRepository.save(paziente);
    }

    public List<Paziente> listaPerProfessionista(UUID professionistaId) {
        return pazienteRepository.findAllByProfessionistaId(professionistaId);
    }

    /** Ultima visita (per data) di ciascun paziente tra gli id passati, in un'unica query. */
    public Map<UUID, Visita> ultimeVisitePerPazienti(List<UUID> pazienteIds) {
        if (pazienteIds.isEmpty()) {
            return Map.of();
        }
        return visitaRepository.findAllByPazienteIdIn(pazienteIds).stream()
                .collect(Collectors.toMap(Visita::getPazienteId, v -> v,
                        (v1, v2) -> v1.getDataVisita().isAfter(v2.getDataVisita()) ? v1 : v2));
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
        if (criteri.obiettivo() != null) {
            specifiche.add(PazienteSpecifications.conObiettivoUltimaVisita(criteri.obiettivo()));
        }
        if (criteri.dataUltimaVisitaDa() != null || criteri.dataUltimaVisitaA() != null) {
            specifiche.add(PazienteSpecifications.conDataUltimaVisitaTra(criteri.dataUltimaVisitaDa(), criteri.dataUltimaVisitaA()));
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

    public List<VisitaResponse> visite(UUID professionistaId, UUID pazienteId) {
        dettaglio(professionistaId, pazienteId);
        return visitaRepository.findAllByPazienteIdOrderByDataVisitaAsc(pazienteId).stream()
                .map(v -> VisitaResponse.da(v, plicometriaRepository.findByVisitaId(v.getId()).orElse(null)))
                .toList();
    }

    private Visita visitaDelPaziente(UUID pazienteId, UUID visitaId) {
        Visita visita = visitaRepository.findById(visitaId).orElseThrow(VisitaNonTrovataException::new);
        if (!visita.getPazienteId().equals(pazienteId)) {
            throw new VisitaNonTrovataException();
        }
        return visita;
    }

    public VisitaResponse visitaSingola(UUID professionistaId, UUID pazienteId, UUID visitaId) {
        dettaglio(professionistaId, pazienteId);
        Visita visita = visitaDelPaziente(pazienteId, visitaId);
        return VisitaResponse.da(visita, plicometriaRepository.findByVisitaId(visita.getId()).orElse(null));
    }

    @Transactional
    public VisitaResponse creaVisita(UUID professionistaId, UUID pazienteId, VisitaRequest request) {
        Paziente paziente = dettaglio(professionistaId, pazienteId);

        Visita visita = new Visita(paziente.getId(), request.dataVisita(), request.altezzaCm(), request.pesoKg(),
                request.circonferenzaVitaCm(), request.circonferenzaFianchiCm(), request.circonferenzaAddomeCm(),
                request.circonferenzaBraccioRilassatoCm(), request.circonferenzaCosciaCm(), request.circonferenzaPolpaccioCm(),
                request.circonferenzaColloCm(), request.circonferenzaToraceCm(), request.circonferenzaBraccioContrattoCm(),
                request.circonferenzaAvambraccioCm(), request.circonferenzaCavigliaCm(), request.protocolloVita(),
                request.note(), request.obiettivo());
        VisitaCalcoli.applica(visita);
        visitaRepository.save(visita);
        plicometriaService.elabora(paziente, visita, request.plicometria());
        visitaRepository.save(visita);

        return VisitaResponse.da(visita, plicometriaRepository.findByVisitaId(visita.getId()).orElse(null));
    }

    @Transactional
    public VisitaResponse aggiornaVisita(UUID professionistaId, UUID pazienteId, UUID visitaId, VisitaRequest request) {
        Paziente paziente = dettaglio(professionistaId, pazienteId);
        Visita visita = visitaDelPaziente(pazienteId, visitaId);

        visita.setDataVisita(request.dataVisita());
        visita.setAltezzaCm(request.altezzaCm());
        visita.setPesoKg(request.pesoKg());
        visita.setCirconferenzaVitaCm(request.circonferenzaVitaCm());
        visita.setCirconferenzaFianchiCm(request.circonferenzaFianchiCm());
        visita.setCirconferenzaAddomeCm(request.circonferenzaAddomeCm());
        visita.setCirconferenzaBraccioRilassatoCm(request.circonferenzaBraccioRilassatoCm());
        visita.setCirconferenzaCosciaCm(request.circonferenzaCosciaCm());
        visita.setCirconferenzaPolpaccioCm(request.circonferenzaPolpaccioCm());
        visita.setCirconferenzaColloCm(request.circonferenzaColloCm());
        visita.setCirconferenzaToraceCm(request.circonferenzaToraceCm());
        visita.setCirconferenzaBraccioContrattoCm(request.circonferenzaBraccioContrattoCm());
        visita.setCirconferenzaAvambraccioCm(request.circonferenzaAvambraccioCm());
        visita.setCirconferenzaCavigliaCm(request.circonferenzaCavigliaCm());
        visita.setProtocolloVita(request.protocolloVita());
        visita.setNote(request.note());
        visita.setObiettivo(request.obiettivo());

        visita.setWhr(null);
        visita.setWhtr(null);
        visita.setMamcCm(null);
        VisitaCalcoli.applica(visita);

        plicometriaRepository.deleteByVisitaId(visita.getId());
        plicometriaRepository.flush();
        visitaRepository.save(visita);
        plicometriaService.elabora(paziente, visita, request.plicometria());
        visitaRepository.save(visita);

        return VisitaResponse.da(visita, plicometriaRepository.findByVisitaId(visita.getId()).orElse(null));
    }

    @Transactional
    public void eliminaVisita(UUID professionistaId, UUID pazienteId, UUID visitaId) {
        dettaglio(professionistaId, pazienteId);
        Visita visita = visitaDelPaziente(pazienteId, visitaId);
        plicometriaRepository.deleteByVisitaId(visita.getId());
        visitaRepository.delete(visita);
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
