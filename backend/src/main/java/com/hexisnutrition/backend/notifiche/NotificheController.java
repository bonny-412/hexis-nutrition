package com.hexisnutrition.backend.notifiche;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/notifiche")
public class NotificheController {

    public record IdsRequest(List<UUID> ids) {
    }

    public record ChiaveResponse(String chiave) {
    }

    public record IscrizioneRequest(@NotBlank @Size(max = 1000) String endpoint,
                                    @NotBlank @Size(max = 200) String p256dh,
                                    @NotBlank @Size(max = 200) String auth) {
    }

    public record EndpointRequest(@NotBlank @Size(max = 1000) String endpoint) {
    }

    private final NotificheService notificheService;
    private final WebPushService webPushService;

    public NotificheController(NotificheService notificheService, WebPushService webPushService) {
        this.notificheService = notificheService;
        this.webPushService = webPushService;
    }

    /** Chiave pubblica VAPID; `null` se le push non sono configurate. */
    @GetMapping("/push/chiave")
    public ChiaveResponse chiave() {
        return new ChiaveResponse(webPushService.chiavePubblica().orElse(null));
    }

    @PostMapping("/push/iscrizioni")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void iscrivi(@AuthenticationPrincipal UUID professionistaId,
                        @Valid @RequestBody IscrizioneRequest richiesta,
                        @RequestHeader(value = "User-Agent", required = false) String userAgent) {
        webPushService.iscrivi(professionistaId, richiesta.endpoint(), richiesta.p256dh(), richiesta.auth(), userAgent);
    }

    @DeleteMapping("/push/iscrizioni")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void rimuovi(@AuthenticationPrincipal UUID professionistaId, @Valid @RequestBody EndpointRequest richiesta) {
        webPushService.rimuovi(professionistaId, richiesta.endpoint());
    }

    @GetMapping
    public NotificheResponse elenco(@AuthenticationPrincipal UUID professionistaId) {
        return notificheService.attive(professionistaId);
    }

    @PostMapping("/lette")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void lette(@AuthenticationPrincipal UUID professionistaId, @RequestBody IdsRequest richiesta) {
        notificheService.segnaLette(professionistaId, richiesta.ids() == null ? List.of() : richiesta.ids());
    }

    @PostMapping("/agenda-viste")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void agendaViste(@AuthenticationPrincipal UUID professionistaId) {
        notificheService.segnaAgendaViste(professionistaId);
    }
}
