package com.hexisnutrition.backend.agenda;

import com.hexisnutrition.backend.notifiche.NotificheService;
import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteArchiviatoException;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.PazienteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AgendaService {

    private static final long GIORNI_MASSIMI = 62;

    private final AppuntamentoRepository appuntamentoRepository;
    private final PazienteRepository pazienteRepository;
    private final PazienteService pazienteService;

    private final NotificheService notificheService;

    public AgendaService(AppuntamentoRepository appuntamentoRepository,
                         PazienteRepository pazienteRepository,
                         PazienteService pazienteService,
                         NotificheService notificheService) {
        this.appuntamentoRepository = appuntamentoRepository;
        this.pazienteRepository = pazienteRepository;
        this.pazienteService = pazienteService;
        this.notificheService = notificheService;
    }

    @Transactional(readOnly = true)
    public List<AppuntamentoResponse> elenco(UUID professionistaId, LocalDate da, LocalDate a) {
        if (da.isAfter(a)) {
            throw new AppuntamentoNonValidoException("Intervallo non valido: la data iniziale segue quella finale");
        }
        if (ChronoUnit.DAYS.between(da, a) > GIORNI_MASSIMI) {
            throw new AppuntamentoNonValidoException("Intervallo troppo ampio (massimo 62 giorni)");
        }
        List<Appuntamento> appuntamenti = appuntamentoRepository
                .elencoNelPeriodo(professionistaId, da, a);
        return rispondi(appuntamenti);
    }

    /** Costruisce le risposte caricando i nomi dei pazienti con una sola query. */
    private List<AppuntamentoResponse> rispondi(List<Appuntamento> appuntamenti) {
        Set<UUID> pazienteIds = appuntamenti.stream()
                .map(Appuntamento::getPazienteId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<UUID, Paziente> pazienti = new HashMap<>();
        if (!pazienteIds.isEmpty()) {
            // Solo lettura dei nomi: gli id arrivano da appuntamenti del professionista.
            pazienteRepository.findAllById(pazienteIds).forEach(p -> pazienti.put(p.getId(), p));
        }
        return appuntamenti.stream()
                .map(app -> AppuntamentoResponse.da(app, pazienti.get(app.getPazienteId())))
                .toList();
    }

    /**
     * Se l'appuntamento è un blocco, cerca le visite programmate che copre (solo se in modifica cambia
     * l'intervallo); se è una visita, cerca visite programmate/concluse e blocchi (anche su più giorni, per il
     * tratto di quel giorno) che si sovrappongono (intervalli semiaperti [inizio, fine)). Va chiamato prima di salvare: l'eccezione fa
     * rollback della transazione, quindi nulla viene persistito.
     */
    private void verificaSovrapposizione(UUID professionistaId, Appuntamento app, boolean confermaSovrapposizione) {
        if (confermaSovrapposizione) {
            return;
        }
        if (app.getTipo() == TipoAppuntamento.BLOCCO) {
            verificaBloccoControVisite(professionistaId, app);
            return;
        }
        if (!app.getTipo().isVisita()) {
            return;
        }
        LocalTime inizio = app.getOraInizio();
        LocalTime fine = inizio.plusMinutes(app.getDurataMinuti());
        List<Appuntamento> conflitti = new ArrayList<>(appuntamentoRepository
                .findAllByProfessionistaIdAndData(professionistaId, app.getData()).stream()
                .filter(a -> !a.getId().equals(app.getId()))
                .filter(a -> a.getTipo().isVisita()
                        && (a.getStato() == StatoAppuntamento.PROGRAMMATA
                            || a.getStato() == StatoAppuntamento.CONCLUSA))
                .filter(a -> a.getOraInizio() != null && a.getDurataMinuti() != null)
                .filter(a -> inizio.isBefore(a.getOraInizio().plusMinutes(a.getDurataMinuti()))
                        && a.getOraInizio().isBefore(fine))
                .toList());
        appuntamentoRepository.blocchiCheCopronoIlGiorno(professionistaId, app.getData()).stream()
                .filter(b -> !b.getId().equals(app.getId()))
                .filter(b -> TrattoBlocco.nelGiorno(b, app.getData())
                        .map(t -> t.siSovrappone(inizio, fine)).orElse(false))
                .forEach(conflitti::add);
        conflitti.sort(Comparator.comparing(Appuntamento::getData)
                .thenComparing(a -> a.getOraInizio() == null ? AgendaTempo.INIZIO_GRIGLIA : a.getOraInizio()));
        if (!conflitti.isEmpty()) {
            throw new SovrapposizioneException(rispondi(conflitti));
        }
    }

    /** Un blocco che copre visite programmate: le restituisce come conflitti, ordinate per data e ora. */
    private void verificaBloccoControVisite(UUID professionistaId, Appuntamento blocco) {
        List<Appuntamento> conflitti = appuntamentoRepository
                .elencoNelPeriodo(professionistaId, blocco.getData(), blocco.getDataFine()).stream()
                .filter(a -> !a.getId().equals(blocco.getId()))
                .filter(a -> a.getTipo().isVisita() && a.getStato() == StatoAppuntamento.PROGRAMMATA)
                .filter(a -> a.getOraInizio() != null && a.getDurataMinuti() != null)
                .filter(a -> TrattoBlocco.nelGiorno(blocco, a.getData())
                        .map(t -> t.siSovrappone(a.getOraInizio(), a.getOraInizio().plusMinutes(a.getDurataMinuti())))
                        .orElse(false))
                .sorted(Comparator.comparing(Appuntamento::getData).thenComparing(Appuntamento::getOraInizio))
                .toList();
        if (!conflitti.isEmpty()) {
            throw new SovrapposizioneException(rispondi(conflitti));
        }
    }

    @Transactional
    public AppuntamentoResponse crea(UUID professionistaId, AppuntamentoRequest r, boolean confermaSovrapposizione) {
        Appuntamento app = new Appuntamento(professionistaId, r.tipo(), r.data());
        Paziente paziente = applica(professionistaId, app, r);
        verificaSovrapposizione(professionistaId, app, confermaSovrapposizione);
        Appuntamento salvato = appuntamentoRepository.save(app);
        notificheService.riallinea(salvato);
        return AppuntamentoResponse.da(salvato, paziente);
    }

    @Transactional
    public AppuntamentoResponse aggiorna(UUID professionistaId, UUID id, AppuntamentoRequest r,
                                         boolean confermaSovrapposizione) {
        Appuntamento app = trova(professionistaId, id);
        if (famiglia(app.getTipo()) != famiglia(r.tipo())) {
            throw new AppuntamentoNonValidoException("Il tipo di appuntamento non può cambiare famiglia");
        }
        if (app.getTipo().isVisita() && app.getStato() != StatoAppuntamento.PROGRAMMATA) {
            throw new AppuntamentoNonValidoException("Solo le visite programmate sono modificabili");
        }
        boolean eraBlocco = app.getTipo() == TipoAppuntamento.BLOCCO;
        LocalDate vecchiaData = app.getData();
        LocalTime vecchiaOraInizio = app.getOraInizio();
        LocalDate vecchiaDataFine = app.getDataFine();
        LocalTime vecchiaOraFine = app.getOraFine();
        boolean vecchiaGiornataIntera = app.isGiornataIntera();
        app.setTipo(r.tipo());
        app.setData(r.data());
        Paziente paziente = applica(professionistaId, app, r);
        boolean intervalloInvariato = eraBlocco
                && Objects.equals(vecchiaData, app.getData())
                && Objects.equals(vecchiaOraInizio, app.getOraInizio())
                && Objects.equals(vecchiaDataFine, app.getDataFine())
                && Objects.equals(vecchiaOraFine, app.getOraFine())
                && vecchiaGiornataIntera == app.isGiornataIntera();
        // L'entità è già modificata ma l'eccezione fa rollback: nulla viene persistito.
        verificaSovrapposizione(professionistaId, app, confermaSovrapposizione || intervalloInvariato);
        Appuntamento salvato = appuntamentoRepository.save(app);
        notificheService.riallinea(salvato);
        return AppuntamentoResponse.da(salvato, paziente);
    }

    @Transactional
    public void annulla(UUID professionistaId, UUID id) {
        Appuntamento app = trova(professionistaId, id);
        if (!app.getTipo().isVisita() || app.getStato() != StatoAppuntamento.PROGRAMMATA) {
            throw new AppuntamentoNonValidoException("Si possono annullare solo le visite programmate");
        }
        app.annulla();
        appuntamentoRepository.save(app);
        notificheService.riallinea(app);
    }

    @Transactional
    public AppuntamentoResponse impostaFatto(UUID professionistaId, UUID id, boolean fatto) {
        Appuntamento app = trova(professionistaId, id);
        if (app.getTipo() != TipoAppuntamento.PROMEMORIA) {
            throw new AppuntamentoNonValidoException("Solo i promemoria si possono segnare come fatti");
        }
        app.setFatto(fatto);
        appuntamentoRepository.save(app);
        notificheService.riallinea(app);
        return AppuntamentoResponse.da(app, pazienteCollegato(app));
    }

    @Transactional
    public void elimina(UUID professionistaId, UUID id) {
        Appuntamento app = trova(professionistaId, id);
        if (app.getTipo().isVisita()) {
            throw new AppuntamentoNonValidoException("Le visite si annullano, non si eliminano");
        }
        appuntamentoRepository.delete(app);
    }

    private Appuntamento trova(UUID professionistaId, UUID id) {
        return appuntamentoRepository.findById(id)
                .filter(a -> a.getProfessionistaId().equals(professionistaId))
                .orElseThrow(AppuntamentoNonTrovatoException::new);
    }

    private Paziente pazienteCollegato(Appuntamento app) {
        return app.getPazienteId() == null ? null : pazienteRepository.findById(app.getPazienteId()).orElse(null);
    }

    private static int famiglia(TipoAppuntamento tipo) {
        return tipo.isVisita() ? 0 : tipo == TipoAppuntamento.PROMEMORIA ? 1 : 2;
    }

    /** Valida la richiesta secondo il tipo e ne copia i campi sull'appuntamento. Restituisce il paziente collegato, se c'è. */
    private Paziente applica(UUID professionistaId, Appuntamento app, AppuntamentoRequest r) {
        TipoAppuntamento tipo = r.tipo();
        Paziente paziente = null;
        switch (tipo) {
            case VISITA_STUDIO, VIDEOCONSULTO -> {
                if (r.pazienteId() == null || r.oraInizio() == null || r.durataMinuti() == null) {
                    throw new AppuntamentoNonValidoException("Paziente, ora di inizio e durata sono obbligatori");
                }
                paziente = pazienteAttivo(professionistaId, r.pazienteId());
                verificaFascia(r.oraInizio(), r.durataMinuti());
                app.setStato(StatoAppuntamento.PROGRAMMATA);
                azzeraIntervallo(app);
                app.setPazienteId(paziente.getId());
                app.setOraInizio(r.oraInizio());
                app.setDurataMinuti(r.durataMinuti());
                app.setLinkVideo(tipo == TipoAppuntamento.VIDEOCONSULTO ? r.linkVideo() : null);
                app.setTesto(r.testo());
            }
            case BLOCCO -> {
                if (r.dataFine() == null) {
                    throw new AppuntamentoNonValidoException("La data di fine è obbligatoria");
                }
                if (r.dataFine().isBefore(r.data())) {
                    throw new AppuntamentoNonValidoException("La fine deve seguire l'inizio");
                }
                if (ChronoUnit.DAYS.between(r.data(), r.dataFine()) > GIORNI_MASSIMI) {
                    throw new AppuntamentoNonValidoException("Un blocco può durare al massimo 62 giorni");
                }
                boolean giornataIntera = Boolean.TRUE.equals(r.giornataIntera());
                if (!giornataIntera) {
                    if (r.oraInizio() == null || r.oraFine() == null) {
                        throw new AppuntamentoNonValidoException("Ora di inizio e di fine sono obbligatorie");
                    }
                    if (r.oraInizio().isBefore(AgendaTempo.INIZIO_GRIGLIA)
                            || !r.oraInizio().isBefore(AgendaTempo.FINE_GRIGLIA)
                            || !r.oraFine().isAfter(AgendaTempo.INIZIO_GRIGLIA)
                            || r.oraFine().isAfter(AgendaTempo.FINE_GRIGLIA)) {
                        throw new AppuntamentoNonValidoException("L'orario deve essere compreso tra le 07:00 e le 20:00");
                    }
                    if (!LocalDateTime.of(r.data(), r.oraInizio())
                            .isBefore(LocalDateTime.of(r.dataFine(), r.oraFine()))) {
                        throw new AppuntamentoNonValidoException("La fine deve seguire l'inizio");
                    }
                }
                app.setStato(null);
                app.setPazienteId(null);
                app.setDataFine(r.dataFine());
                app.setGiornataIntera(giornataIntera);
                app.setOraInizio(giornataIntera ? null : r.oraInizio());
                app.setOraFine(giornataIntera ? null : r.oraFine());
                app.setDurataMinuti(null);
                app.setLinkVideo(null);
                app.setTesto(r.testo());
            }
            case PROMEMORIA -> {
                if (r.testo() == null || r.testo().isBlank()) {
                    throw new AppuntamentoNonValidoException("Il testo del promemoria è obbligatorio");
                }
                LocalTime ora = r.oraInizio();
                if (ora != null && (ora.isBefore(AgendaTempo.INIZIO_GRIGLIA) || !ora.isBefore(AgendaTempo.FINE_GRIGLIA))) {
                    throw new AppuntamentoNonValidoException("L'orario deve essere compreso tra le 07:00 e le 20:00");
                }
                if (r.pazienteId() != null) {
                    paziente = pazienteAttivo(professionistaId, r.pazienteId());
                }
                app.setStato(null);
                azzeraIntervallo(app);
                app.setPazienteId(paziente == null ? null : paziente.getId());
                app.setOraInizio(ora);
                app.setDurataMinuti(null);
                app.setLinkVideo(null);
                app.setTesto(r.testo());
            }
        }
        applicaNotifica(app, r);
        return paziente;
    }

    /** Le notifiche valgono solo per visite e promemoria con orario; per gli altri eventi il flag è sempre spento. */
    private static void applicaNotifica(Appuntamento app, AppuntamentoRequest r) {
        boolean notificabile = app.getTipo() != TipoAppuntamento.BLOCCO && app.getOraInizio() != null;
        int anticipo = r.anticipoNotificaMinuti() == null ? AgendaTempo.ANTICIPO_NOTIFICA_DEFAULT : r.anticipoNotificaMinuti();
        if (notificabile && !AgendaTempo.ANTICIPI_NOTIFICA_MINUTI.contains(anticipo)) {
            throw new AppuntamentoNonValidoException("Anticipo di notifica non valido");
        }
        app.setNotifica(notificabile && !Boolean.FALSE.equals(r.notifica()));
        app.setAnticipoNotificaMinuti(notificabile ? anticipo : AgendaTempo.ANTICIPO_NOTIFICA_DEFAULT);
    }

    private Paziente pazienteAttivo(UUID professionistaId, UUID pazienteId) {
        Paziente paziente = pazienteService.dettaglio(professionistaId, pazienteId);
        if (paziente.isArchiviato()) {
            throw new PazienteArchiviatoException();
        }
        return paziente;
    }

    private static void azzeraIntervallo(Appuntamento app) {
        app.setDataFine(null);
        app.setOraFine(null);
        app.setGiornataIntera(false);
    }

    private static void verificaFascia(LocalTime inizio, int durataMinuti) {
        int inizioMin = inizio.getHour() * 60 + inizio.getMinute();
        int fineMin = inizioMin + durataMinuti;
        int limiteInizio = AgendaTempo.INIZIO_GRIGLIA.getHour() * 60 + AgendaTempo.INIZIO_GRIGLIA.getMinute();
        int limiteFine = AgendaTempo.FINE_GRIGLIA.getHour() * 60 + AgendaTempo.FINE_GRIGLIA.getMinute();
        if (inizioMin < limiteInizio || fineMin > limiteFine) {
            throw new AppuntamentoNonValidoException("L'orario deve essere compreso tra le 07:00 e le 20:00");
        }
    }
}
