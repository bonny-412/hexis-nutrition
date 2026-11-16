package com.hexisnutrition.backend.notifiche;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Invio delle Web Push (protocollo VAPID). Senza chiavi configurate le push sono disattivate:
 * nessun errore, il resto delle notifiche (campanello, puntini) continua a funzionare.
 */
@Service
public class WebPushService {

    private static final Logger log = LoggerFactory.getLogger(WebPushService.class);

    private static final long TIMEOUT_INVIO_SECONDI = 10;
    private static final int MAX_ISCRIZIONI_PER_PROFESSIONISTA = 10;

    private final PushSubscriptionRepository repository;
    private final ObjectMapper objectMapper;
    private final String chiavePubblica;
    private final PushService pushService;
    private final List<String> endpointAmmessi;

    public WebPushService(PushSubscriptionRepository repository,
                          ObjectMapper objectMapper,
                          @Value("${app.push.vapid-public-key:}") String chiavePubblica,
                          @Value("${app.push.vapid-private-key:}") String chiavePrivata,
                          @Value("${app.push.subject:}") String subject,
                          @Value("${app.push.endpoint-ammessi:fcm.googleapis.com,push.services.mozilla.com,notify.windows.com,push.apple.com}")
                          List<String> endpointAmmessi) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.endpointAmmessi = endpointAmmessi.stream().map(d -> d.trim().toLowerCase(Locale.ROOT)).toList();
        PushService servizio = null;
        if (!chiavePubblica.isBlank() && !chiavePrivata.isBlank()) {
            try {
                if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                    Security.addProvider(new BouncyCastleProvider());
                }
                servizio = new PushService(chiavePubblica, chiavePrivata, subject);
            } catch (Exception e) {
                log.warn("Chiavi VAPID non valide: le notifiche push sono disattivate ({})", e.getMessage());
            }
        }
        this.pushService = servizio;
        this.chiavePubblica = servizio == null ? null : chiavePubblica;
    }

    public Optional<String> chiavePubblica() {
        return Optional.ofNullable(chiavePubblica);
    }

    /**
     * L'endpoint è scelto dal client e il backend vi invia richieste: ammesso solo https sui servizi push dei browser
     * (allowlist configurabile), per non trasformare l'iscrizione in una richiesta verso indirizzi interni.
     */
    private void verificaEndpoint(String endpoint) {
        URI uri;
        try {
            uri = new URI(endpoint);
        } catch (URISyntaxException e) {
            throw new IscrizionePushNonValidaException("Endpoint push non valido");
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        boolean hostAmmesso = endpointAmmessi.stream().anyMatch(d -> host.equals(d) || host.endsWith("." + d));
        boolean portaOk = uri.getPort() == -1 || uri.getPort() == 443;
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getUserInfo() != null || !portaOk || !hostAmmesso) {
            throw new IscrizionePushNonValidaException("Endpoint push non valido");
        }
    }

    @Transactional
    public void iscrivi(UUID professionistaId, String endpoint, String p256dh, String auth, String userAgent) {
        verificaEndpoint(endpoint);
        if (repository.findByEndpoint(endpoint).isEmpty()
                && repository.findAllByProfessionistaId(professionistaId).size() >= MAX_ISCRIZIONI_PER_PROFESSIONISTA) {
            throw new IscrizionePushNonValidaException("Troppi dispositivi registrati");
        }
        String ua = userAgent == null ? null : userAgent.substring(0, Math.min(userAgent.length(), 300));
        repository.findByEndpoint(endpoint).ifPresentOrElse(
                s -> s.aggiorna(professionistaId, p256dh, auth, ua),
                () -> repository.save(new PushSubscription(professionistaId, endpoint, p256dh, auth, ua)));
    }

    @Transactional
    public void rimuovi(UUID professionistaId, String endpoint) {
        repository.deleteByEndpointAndProfessionistaId(endpoint, professionistaId);
    }

    /** Best-effort: non lancia mai. Le iscrizioni scadute (404/410) vengono eliminate. */
    public void invia(UUID professionistaId, String titolo, String corpo, String url) {
        if (pushService == null) {
            return;
        }
        byte[] payload;
        try {
            payload = objectMapper.writeValueAsString(Map.of("titolo", titolo, "corpo", corpo, "url", url))
                    .getBytes(StandardCharsets.UTF_8);
        } catch (JsonProcessingException e) {
            log.warn("Payload push non serializzabile: {}", e.getMessage());
            return;
        }
        for (PushSubscription s : repository.findAllByProfessionistaId(professionistaId)) {
            try {
                // Con timeout: un servizio push che non risponde non deve bloccare lo scheduler (thread condiviso).
                var invio = pushService.sendAsync(new Notification(s.getEndpoint(), s.getP256dh(), s.getAuth(), payload));
                try {
                    var risposta = invio.get(TIMEOUT_INVIO_SECONDI, TimeUnit.SECONDS);
                    int stato = risposta.getStatusLine().getStatusCode();
                    if (stato == 404 || stato == 410) {
                        repository.deleteByEndpoint(s.getEndpoint());
                    } else if (stato >= 400) {
                        log.warn("Push rifiutata dal servizio (stato {})", stato);
                    }
                } catch (TimeoutException e) {
                    invio.cancel(true);
                    log.warn("Invio push scaduto dopo {} s", TIMEOUT_INVIO_SECONDI);
                }
            } catch (Exception e) {
                log.warn("Invio push fallito: {}", e.getMessage());
            }
        }
    }
}
