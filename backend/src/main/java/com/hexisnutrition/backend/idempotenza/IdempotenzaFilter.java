package com.hexisnutrition.backend.idempotenza;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Verifica 3 prima del rilascio (28/11/2026), bug S4: un POST con l'header {@code Idempotency-Key} viene eseguito
 * una volta sola per utente e chiave. Se la stessa chiave torna (risposta persa, nuovo tentativo), si restituisce
 * la risposta salvata della prima richiesta, senza eseguirla di nuovo.
 *
 * <ul>
 *   <li>Si salvano solo le risposte 2xx: dopo un errore nulla è stato creato, la chiave si libera e si può riprovare.</li>
 *   <li>Se la prima richiesta è ancora in corso: 409 {@code RICHIESTA_IN_CORSO}. Dopo {@link #PRENOTAZIONE_SCADUTA}
 *       una prenotazione senza risposta (server fermato a metà) si considera abbandonata.</li>
 *   <li>Stessa chiave su un'altra richiesta (metodo o percorso diversi): 422, è un errore del client.</li>
 * </ul>
 *
 * Sta nella catena di Spring Security dopo l'autorizzazione: l'utente è già noto e le richieste rifiutate
 * non arrivano qui. Non è un {@code @Component}, altrimenti Spring Boot lo registrerebbe una seconda volta
 * fuori dalla catena.
 */
public class IdempotenzaFilter extends OncePerRequestFilter {

    public static final String HEADER = "Idempotency-Key";
    static final int LUNGHEZZA_MASSIMA = 100;
    static final Duration PRENOTAZIONE_SCADUTA = Duration.ofMinutes(5);

    private final RegistroIdempotenza registro;

    public IdempotenzaFilter(RegistroIdempotenza registro) {
        this.registro = registro;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"POST".equals(request.getMethod()) || request.getHeader(HEADER) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        var autenticazione = SecurityContextHolder.getContext().getAuthentication();
        if (autenticazione == null || !(autenticazione.getPrincipal() instanceof UUID utenteId)) {
            chain.doFilter(request, response);
            return;
        }
        String chiave = request.getHeader(HEADER).trim();
        if (chiave.isEmpty() || chiave.length() > LUNGHEZZA_MASSIMA) {
            scriviErrore(response, HttpStatus.BAD_REQUEST, "Idempotency-Key non valida.");
            return;
        }
        String metodo = request.getMethod();
        String percorso = request.getRequestURI() + (request.getQueryString() == null ? "" : "?" + request.getQueryString());

        if (!registro.prenota(utenteId, chiave, metodo, percorso)) {
            var registrata = registro.trova(utenteId, chiave).orElse(null);
            if (registrata == null || abbandonata(registrata)) {
                // Liberata nel frattempo, o prenotazione rimasta a metà: si riprova a prenotare una volta.
                if (registrata != null) registro.rilascia(utenteId, chiave);
                if (!registro.prenota(utenteId, chiave, metodo, percorso)) {
                    scriviErrore(response, HttpStatus.CONFLICT, "RICHIESTA_IN_CORSO");
                    return;
                }
            } else if (!registrata.metodo().equals(metodo) || !registrata.percorso().equals(percorso)) {
                scriviErrore(response, HttpStatus.UNPROCESSABLE_ENTITY, "Idempotency-Key già usata per un'altra richiesta.");
                return;
            } else if (registrata.statoHttp() == null) {
                scriviErrore(response, HttpStatus.CONFLICT, "RICHIESTA_IN_CORSO");
                return;
            } else {
                ripeti(response, registrata);
                return;
            }
        }

        var risposta = new ContentCachingResponseWrapper(response);
        boolean salvata = false;
        try {
            chain.doFilter(request, risposta);
            int stato = risposta.getStatus();
            if (stato >= 200 && stato < 300) {
                registro.completa(utenteId, chiave, stato, risposta.getContentType(),
                        new String(risposta.getContentAsByteArray(), StandardCharsets.UTF_8));
                salvata = true;
            }
        } finally {
            if (!salvata) registro.rilascia(utenteId, chiave);
            risposta.copyBodyToResponse();
        }
    }

    private static boolean abbandonata(RegistroIdempotenza.RichiestaRegistrata registrata) {
        return registrata.statoHttp() == null
                && registrata.creataIl().isBefore(Instant.now().minus(PRENOTAZIONE_SCADUTA));
    }

    private static void ripeti(HttpServletResponse response, RegistroIdempotenza.RichiestaRegistrata registrata)
            throws IOException {
        response.setStatus(registrata.statoHttp());
        if (registrata.contentType() != null) response.setContentType(registrata.contentType());
        if (registrata.corpo() != null && !registrata.corpo().isEmpty()) {
            byte[] corpo = registrata.corpo().getBytes(StandardCharsets.UTF_8);
            response.setContentLength(corpo.length);
            response.getOutputStream().write(corpo);
        }
    }

    private static void scriviErrore(HttpServletResponse response, HttpStatus stato, String messaggio) throws IOException {
        response.setStatus(stato.value());
        response.setContentType(MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8");
        response.getWriter().write(messaggio);
    }
}
