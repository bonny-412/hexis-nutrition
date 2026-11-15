package com.hexisnutrition.backend.agenda;

import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteArchiviatoException;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.PazienteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
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

    public AgendaService(AppuntamentoRepository appuntamentoRepository,
                         PazienteRepository pazienteRepository,
                         PazienteService pazienteService) {
        this.appuntamentoRepository = appuntamentoRepository;
        this.pazienteRepository = pazienteRepository;
        this.pazienteService = pazienteService;
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
                .findAllByProfessionistaIdAndDataBetweenOrderByDataAscOraInizioAsc(professionistaId, da, a);
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
     * Se l'appuntamento è una visita, cerca visite programmate/concluse e blocchi dello stesso giorno che si
     * sovrappongono (intervalli semiaperti [inizio, fine)). Va chiamato prima di salvare: l'eccezione fa
     * rollback della transazione, quindi nulla viene persistito.
     */
    private void verificaSovrapposizione(UUID professionistaId, Appuntamento app, boolean confermaSovrapposizione) {
        if (confermaSovrapposizione || !app.getTipo().isVisita()) {
            return;
        }
        LocalTime inizio = app.getOraInizio();
        LocalTime fine = inizio.plusMinutes(app.getDurataMinuti());
        List<Appuntamento> conflitti = appuntamentoRepository
                .findAllByProfessionistaIdAndData(professionistaId, app.getData()).stream()
                .filter(a -> !a.getId().equals(app.getId()))
                .filter(a -> a.getTipo() == TipoAppuntamento.BLOCCO
                        || (a.getTipo().isVisita()
                            && (a.getStato() == StatoAppuntamento.PROGRAMMATA
                                || a.getStato() == StatoAppuntamento.CONCLUSA)))
                .filter(a -> a.getOraInizio() != null && a.getDurataMinuti() != null)
                .filter(a -> inizio.isBefore(a.getOraInizio().plusMinutes(a.getDurataMinuti()))
                        && a.getOraInizio().isBefore(fine))
                .sorted(java.util.Comparator.comparing(Appuntamento::getOraInizio))
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
        return AppuntamentoResponse.da(appuntamentoRepository.save(app), paziente);
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
        app.setTipo(r.tipo());
        app.setData(r.data());
        Paziente paziente = applica(professionistaId, app, r);
        // L'entità è già modificata ma l'eccezione fa rollback: nulla viene persistito.
        verificaSovrapposizione(professionistaId, app, confermaSovrapposizione);
        return AppuntamentoResponse.da(appuntamentoRepository.save(app), paziente);
    }

    @Transactional
    public void annulla(UUID professionistaId, UUID id) {
        Appuntamento app = trova(professionistaId, id);
        if (!app.getTipo().isVisita() || app.getStato() != StatoAppuntamento.PROGRAMMATA) {
            throw new AppuntamentoNonValidoException("Si possono annullare solo le visite programmate");
        }
        app.annulla();
        appuntamentoRepository.save(app);
    }

    @Transactional
    public AppuntamentoResponse impostaFatto(UUID professionistaId, UUID id, boolean fatto) {
        Appuntamento app = trova(professionistaId, id);
        if (app.getTipo() != TipoAppuntamento.PROMEMORIA) {
            throw new AppuntamentoNonValidoException("Solo i promemoria si possono segnare come fatti");
        }
        app.setFatto(fatto);
        appuntamentoRepository.save(app);
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
                app.setPazienteId(paziente.getId());
                app.setOraInizio(r.oraInizio());
                app.setDurataMinuti(r.durataMinuti());
                app.setLinkVideo(tipo == TipoAppuntamento.VIDEOCONSULTO ? r.linkVideo() : null);
                app.setTesto(r.testo());
            }
            case BLOCCO -> {
                if (r.oraInizio() == null || r.durataMinuti() == null) {
                    throw new AppuntamentoNonValidoException("Ora di inizio e durata sono obbligatorie");
                }
                verificaFascia(r.oraInizio(), r.durataMinuti());
                app.setStato(null);
                app.setPazienteId(null);
                app.setOraInizio(r.oraInizio());
                app.setDurataMinuti(r.durataMinuti());
                app.setLinkVideo(null);
                app.setTesto(r.testo());
            }
            case PROMEMORIA -> {
                if (r.testo() == null || r.testo().isBlank()) {
                    throw new AppuntamentoNonValidoException("Il testo del promemoria è obbligatorio");
                }
                LocalTime ora = r.oraInizio();
                if (ora != null && (ora.isBefore(AgendaTempo.INIZIO_GRIGLIA) || !ora.isBefore(AgendaTempo.FINE_GRIGLIA))) {
                    throw new AppuntamentoNonValidoException("L'orario deve essere compreso tra le 08:00 e le 20:00");
                }
                if (r.pazienteId() != null) {
                    paziente = pazienteAttivo(professionistaId, r.pazienteId());
                }
                app.setStato(null);
                app.setPazienteId(paziente == null ? null : paziente.getId());
                app.setOraInizio(ora);
                app.setDurataMinuti(null);
                app.setLinkVideo(null);
                app.setTesto(r.testo());
            }
        }
        return paziente;
    }

    private Paziente pazienteAttivo(UUID professionistaId, UUID pazienteId) {
        Paziente paziente = pazienteService.dettaglio(professionistaId, pazienteId);
        if (paziente.isArchiviato()) {
            throw new PazienteArchiviatoException();
        }
        return paziente;
    }

    private static void verificaFascia(LocalTime inizio, int durataMinuti) {
        int inizioMin = inizio.getHour() * 60 + inizio.getMinute();
        int fineMin = inizioMin + durataMinuti;
        int limiteInizio = AgendaTempo.INIZIO_GRIGLIA.getHour() * 60 + AgendaTempo.INIZIO_GRIGLIA.getMinute();
        int limiteFine = AgendaTempo.FINE_GRIGLIA.getHour() * 60 + AgendaTempo.FINE_GRIGLIA.getMinute();
        if (inizioMin < limiteInizio || fineMin > limiteFine) {
            throw new AppuntamentoNonValidoException("L'orario deve essere compreso tra le 08:00 e le 20:00");
        }
    }
}
