package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.email.EmailSender;
import com.hexisnutrition.backend.inviti.EmailGiaInUsoException;
import com.hexisnutrition.backend.inviti.TipoToken;
import com.hexisnutrition.backend.inviti.TokenAzione;
import com.hexisnutrition.backend.inviti.TokenAzioneRepository;
import com.hexisnutrition.backend.inviti.TokenNonValidoException;
import com.hexisnutrition.backend.pianialimentari.PianoAlimentare;
import com.hexisnutrition.backend.pianialimentari.PianoAlimentareRepository;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PazienteService {

    private static final Logger log = LoggerFactory.getLogger(PazienteService.class);

    // Campi di ordinamento non presenti come colonna su Paziente (derivati da tabelle collegate,
    // popolati in batch solo per la pagina corrente — vedi ultimeVisitePerPazienti/
    // ultimiPianiPerPazienti): non ordinabili a livello di query con Specification+Pageable,
    // gestiti a parte in cerca() caricando l'intero risultato e ordinandolo in Java.
    private static final Set<CampoOrdinamentoPazienti> ORDINAMENTI_DERIVATI =
            Set.of(CampoOrdinamentoPazienti.dataUltimaVisita, CampoOrdinamentoPazienti.piano);

    private final PazienteRepository pazienteRepository;
    private final VisitaRepository visitaRepository;
    private final PianoAlimentareRepository pianoAlimentareRepository;
    private final ProfessionistaRepository professionistaRepository;
    private final TokenAzioneRepository tokenAzioneRepository;
    private final EmailSender emailSender;
    private final PasswordEncoder passwordEncoder;
    private final PlicometriaService plicometriaService;
    private final PlicometriaRepository plicometriaRepository;

    public PazienteService(PazienteRepository pazienteRepository,
                            VisitaRepository visitaRepository,
                            PianoAlimentareRepository pianoAlimentareRepository,
                            ProfessionistaRepository professionistaRepository,
                            TokenAzioneRepository tokenAzioneRepository,
                            EmailSender emailSender,
                            PasswordEncoder passwordEncoder,
                            PlicometriaService plicometriaService,
                            PlicometriaRepository plicometriaRepository) {
        this.pazienteRepository = pazienteRepository;
        this.visitaRepository = visitaRepository;
        this.pianoAlimentareRepository = pianoAlimentareRepository;
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

    /**
     * Data della prima visita (in ordine cronologico, partendo dall'ultima e risalendo) con lo
     * stesso obiettivo dell'ultima visita di ciascun paziente — cioè da quando è impostato
     * l'obiettivo attuale, non semplicemente la data dell'ultima visita. Se l'obiettivo è
     * cambiato all'ultima visita rispetto alla precedente, coincide con la data dell'ultima
     * visita stessa. Una sola query per tutti i pazienti passati (nessuna per paziente).
     */
    public Map<UUID, LocalDate> dataInizioObiettivoPerPazienti(List<UUID> pazienteIds) {
        if (pazienteIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, List<Visita>> visitePerPaziente = visitaRepository.findAllByPazienteIdIn(pazienteIds).stream()
                .collect(Collectors.groupingBy(Visita::getPazienteId));

        Map<UUID, LocalDate> risultato = new HashMap<>();
        for (Map.Entry<UUID, List<Visita>> voce : visitePerPaziente.entrySet()) {
            List<Visita> visiteOrdinate = voce.getValue().stream()
                    .sorted(Comparator.comparing(Visita::getDataVisita))
                    .toList();
            Visita ultima = visiteOrdinate.get(visiteOrdinate.size() - 1);
            LocalDate dataInizio = ultima.getDataVisita();
            for (int i = visiteOrdinate.size() - 2; i >= 0; i--) {
                if (visiteOrdinate.get(i).getObiettivo() != ultima.getObiettivo()) {
                    break;
                }
                dataInizio = visiteOrdinate.get(i).getDataVisita();
            }
            risultato.put(voce.getKey(), dataInizio);
        }
        return risultato;
    }

    /**
     * Piano più di recente creazione di ciascun paziente tra gli id passati, in un'unica query
     * — qualunque sia il suo stato (bozza/attivo/scaduto/terminato), non solo quello attivo.
     */
    public Map<UUID, PianoAlimentare> ultimiPianiPerPazienti(List<UUID> pazienteIds) {
        if (pazienteIds.isEmpty()) {
            return Map.of();
        }
        return pianoAlimentareRepository.findAllByPazienteIdIn(pazienteIds).stream()
                .collect(Collectors.toMap(PianoAlimentare::getPazienteId, p -> p,
                        (p1, p2) -> p1.getCreatoIl().isAfter(p2.getCreatoIl()) ? p1 : p2));
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
        Specification<Paziente> specifica = Specification.allOf(specifiche);

        Sort.Order ordinePrincipale = pageable.getSort().stream().findFirst().orElse(null);
        if (ordinePrincipale != null && ORDINAMENTI_DERIVATI.contains(CampoOrdinamentoPazienti.valueOf(ordinePrincipale.getProperty()))) {
            return cercaConOrdinamentoDerivato(specifica, pageable, ordinePrincipale);
        }
        return pazienteRepository.findAll(specifica, pageable);
    }

    /**
     * Ramo per dataUltimaVisita/piano (vedi ORDINAMENTI_DERIVATI): non essendo colonne di
     * Paziente non sono esprimibili in un Sort passato a Specification+Pageable, quindi si
     * carica l'intero risultato filtrato, si ordina in Java e si pagina manualmente. Accettabile
     * per il volume di pazienti per professionista di questo dominio (uno studio, non pensato
     * per migliaia di pazienti): evita una query nativa con subquery correlate solo per due
     * colonne derivate usate raramente come criterio di ordinamento.
     */
    private Page<Paziente> cercaConOrdinamentoDerivato(Specification<Paziente> specifica, Pageable pageable,
            Sort.Order ordine) {
        List<Paziente> tutti = pazienteRepository.findAll(specifica);
        List<UUID> tuttiGliId = tutti.stream().map(Paziente::getId).toList();
        boolean desc = ordine.getDirection() == Sort.Direction.DESC;

        Comparator<Paziente> comparatore;
        if (ordine.getProperty().equals(CampoOrdinamentoPazienti.dataUltimaVisita.name())) {
            Map<UUID, Visita> ultimeVisite = ultimeVisitePerPazienti(tuttiGliId);
            comparatore = comparatoreConNullUltimo(
                    p -> Optional.ofNullable(ultimeVisite.get(p.getId())).map(Visita::getDataVisita).orElse(null), desc);
        } else {
            Map<UUID, PianoAlimentare> ultimiPiani = ultimiPianiPerPazienti(tuttiGliId);
            comparatore = comparatoreConNullUltimo(
                    p -> Optional.ofNullable(ultimiPiani.get(p.getId())).map(PianoAlimentare::getNome).orElse(null), desc);
        }
        List<Paziente> ordinati = tutti.stream().sorted(comparatore).toList();

        int totale = ordinati.size();
        int daIndice = Math.min((int) pageable.getOffset(), totale);
        int aIndice = Math.min(daIndice + pageable.getPageSize(), totale);
        return new PageImpl<>(ordinati.subList(daIndice, aIndice), pageable, totale);
    }

    /** I pazienti senza il valore (nessuna visita / nessun piano) vanno sempre in fondo, a prescindere dalla direzione. */
    private <T extends Comparable<T>> Comparator<Paziente> comparatoreConNullUltimo(Function<Paziente, T> chiave, boolean desc) {
        Comparator<T> base = desc ? Comparator.<T>naturalOrder().reversed() : Comparator.naturalOrder();
        return Comparator.comparing(chiave, Comparator.nullsLast(base));
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
