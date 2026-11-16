package com.hexisnutrition.backend.notifiche;

import com.hexisnutrition.backend.agenda.AgendaTempo;
import com.hexisnutrition.backend.agenda.Appuntamento;
import com.hexisnutrition.backend.agenda.AppuntamentoRepository;
import com.hexisnutrition.backend.agenda.StatoAppuntamento;
import com.hexisnutrition.backend.agenda.TipoAppuntamento;
import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificheService {

    /** Notifica appena creata: serve allo scheduler per inviare la push dopo il commit. */
    public record NotificaNuova(UUID professionistaId, TipoAppuntamento tipo, int anticipoMinuti) {
    }

    private final NotificaRepository notificaRepository;
    private final AppuntamentoRepository appuntamentoRepository;
    private final PazienteRepository pazienteRepository;

    public NotificheService(NotificaRepository notificaRepository,
                            AppuntamentoRepository appuntamentoRepository,
                            PazienteRepository pazienteRepository) {
        this.notificaRepository = notificaRepository;
        this.appuntamentoRepository = appuntamentoRepository;
        this.pazienteRepository = pazienteRepository;
    }

    /** Visita programmata o promemoria non fatto, con orario e notifica attiva. */
    private static boolean notificabile(Appuntamento a) {
        if (!a.isNotifica() || a.getOraInizio() == null) {
            return false;
        }
        return switch (a.getTipo()) {
            case VISITA_STUDIO, VIDEOCONSULTO -> a.getStato() == StatoAppuntamento.PROGRAMMATA;
            case PROMEMORIA -> !a.isFatto();
            case BLOCCO -> false;
        };
    }

    private static LocalDateTime inizio(Appuntamento a) {
        return LocalDateTime.of(a.getData(), a.getOraInizio());
    }

    private static Optional<Instant> scatto(Appuntamento a) {
        if (!notificabile(a)) {
            return Optional.empty();
        }
        return Optional.of(inizio(a).atZone(AgendaTempo.FUSO).minusMinutes(a.getAnticipoNotificaMinuti()).toInstant());
    }

    /** Crea le notifiche scadute (una sola volta per evento e orario di scatto) e restituisce quelle nuove. */
    @Transactional
    public List<NotificaNuova> generaScadute() {
        Instant ora = Instant.now();
        LocalDate oggi = LocalDate.now(AgendaTempo.FUSO);
        List<NotificaNuova> nuove = new java.util.ArrayList<>();
        for (Appuntamento a : appuntamentoRepository.findAllByDataBetweenAndNotificaTrue(oggi, oggi.plusDays(1))) {
            Optional<Instant> scatto = scatto(a);
            if (scatto.isEmpty() || scatto.get().isAfter(ora)) {
                continue;
            }
            // Già iniziato: nessuna notifica (anche se creato tardi).
            if (!inizio(a).atZone(AgendaTempo.FUSO).toInstant().isAfter(ora)) {
                continue;
            }
            int inserite = notificaRepository.inserisciSeAssente(UUID.randomUUID(), a.getProfessionistaId(), a.getId(), scatto.get());
            if (inserite == 1) {
                nuove.add(new NotificaNuova(a.getProfessionistaId(), a.getTipo(), a.getAnticipoNotificaMinuti()));
            }
        }
        return nuove;
    }

    /** Dopo una modifica: elimina le notifiche non più valide, senza toccare quelle con lo stesso orario di scatto. */
    @Transactional
    public void riallinea(Appuntamento a) {
        Optional<Instant> scatto = scatto(a);
        if (scatto.isEmpty()) {
            notificaRepository.deleteByAppuntamentoId(a.getId());
        } else {
            notificaRepository.deleteByAppuntamentoIdAndScattaIlNot(a.getId(), scatto.get());
        }
    }

    @Transactional(readOnly = true)
    public NotificheResponse attive(UUID professionistaId) {
        List<Notifica> notifiche = notificaRepository.findAllByProfessionistaId(professionistaId);
        Set<UUID> appuntamentoIds = notifiche.stream().map(Notifica::getAppuntamentoId).collect(Collectors.toSet());
        Map<UUID, Appuntamento> appuntamenti = new HashMap<>();
        appuntamentoRepository.findAllById(appuntamentoIds).forEach(a -> appuntamenti.put(a.getId(), a));

        LocalDateTime adesso = AgendaTempo.adesso();
        List<Notifica> valide = notifiche.stream()
                .filter(n -> ancoraValida(appuntamenti.get(n.getAppuntamentoId()), adesso))
                .sorted(Comparator.comparing(Notifica::getScattaIl).reversed())
                .toList();

        Set<UUID> pazienteIds = valide.stream()
                .map(n -> appuntamenti.get(n.getAppuntamentoId()).getPazienteId())
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<UUID, Paziente> pazienti = new HashMap<>();
        if (!pazienteIds.isEmpty()) {
            pazienteRepository.findAllById(pazienteIds).forEach(p -> pazienti.put(p.getId(), p));
        }

        List<NotificaResponse> risposte = valide.stream().map(n -> {
            Appuntamento a = appuntamenti.get(n.getAppuntamentoId());
            Paziente p = a.getPazienteId() == null ? null : pazienti.get(a.getPazienteId());
            return new NotificaResponse(n.getId(), a.getId(), a.getTipo(), a.getData(), a.getOraInizio(),
                    a.getAnticipoNotificaMinuti(), a.getTesto(),
                    p == null ? null : p.getNome(), p == null ? null : p.getCognome(),
                    n.getScattaIl(), n.getLettaIl() != null, n.getAgendaVistaIl() != null);
        }).toList();

        return new NotificheResponse(risposte,
                risposte.stream().filter(r -> !r.letta()).count(),
                risposte.stream().filter(r -> !r.agendaVista()).count());
    }

    /** Visita: finché è programmata e non finita. Promemoria: finché non è fatto e solo nel giorno stesso. */
    private static boolean ancoraValida(Appuntamento a, LocalDateTime adesso) {
        if (a == null || a.getOraInizio() == null) {
            return false;
        }
        if (a.getTipo().isVisita()) {
            int durata = a.getDurataMinuti() == null ? 0 : a.getDurataMinuti();
            return a.getStato() == StatoAppuntamento.PROGRAMMATA && adesso.isBefore(inizio(a).plusMinutes(durata));
        }
        return a.getTipo() == TipoAppuntamento.PROMEMORIA && !a.isFatto() && a.getData().equals(adesso.toLocalDate());
    }

    /** Segna come lette solo le notifiche del professionista nell'elenco ricevuto. */
    @Transactional
    public void segnaLette(UUID professionistaId, Collection<UUID> ids) {
        Instant ora = Instant.now();
        for (Notifica n : notificaRepository.findAllByProfessionistaId(professionistaId)) {
            if (n.getLettaIl() == null && ids.contains(n.getId())) {
                n.setLettaIl(ora);
            }
        }
    }

    @Transactional
    public void segnaAgendaViste(UUID professionistaId) {
        Instant ora = Instant.now();
        for (Notifica n : notificaRepository.findAllByProfessionistaId(professionistaId)) {
            if (n.getAgendaVistaIl() == null) {
                n.setAgendaVistaIl(ora);
            }
        }
    }

    /** Pulizia notturna: nessuna notifica sopravvive al giorno in cui è scattata. */
    @Transactional
    public void eliminaScattatePrimaDiOggi() {
        Instant inizioOggi = LocalDate.now(AgendaTempo.FUSO).atStartOfDay(AgendaTempo.FUSO).toInstant();
        notificaRepository.deleteByScattaIlBefore(inizioOggi);
    }
}
