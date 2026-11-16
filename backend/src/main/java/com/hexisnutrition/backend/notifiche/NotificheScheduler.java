package com.hexisnutrition.backend.notifiche;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificheScheduler {

    private final NotificheService notificheService;
    private final WebPushService webPushService;

    public NotificheScheduler(NotificheService notificheService, WebPushService webPushService) {
        this.notificheService = notificheService;
        this.webPushService = webPushService;
    }

    /** Le notifiche si creano in una transazione; le push partono dopo il commit, solo per quelle nuove. */
    @Scheduled(fixedDelay = 60_000)
    public void generaScadute() {
        for (NotificheService.NotificaNuova nuova : notificheService.generaScadute()) {
            String cosa = nuova.tipo().isVisita() ? "una visita" : "un promemoria";
            // Testo generico: nessun nome, titolo o dato sanitario (può comparire a schermo bloccato).
            webPushService.invia(nuova.professionistaId(), "Hexis",
                    "Hai " + cosa + " tra " + nuova.anticipoMinuti() + " minuti", "/agenda");
        }
    }

    @Scheduled(cron = "0 0 3 * * *", zone = "Europe/Rome")
    public void pulisci() {
        notificheService.eliminaScattatePrimaDiOggi();
    }
}
