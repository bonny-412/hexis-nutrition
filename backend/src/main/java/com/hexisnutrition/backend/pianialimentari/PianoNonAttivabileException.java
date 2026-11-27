package com.hexisnutrition.backend.pianialimentari;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Si attivano solo le bozze (collaudo, bug 09) e solo da quando arriva la data di inizio:
 * un piano con inizio futuro resta in bozza e si attiva a mano quel giorno (deciso con Andrea il 27/11/2026).
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class PianoNonAttivabileException extends RuntimeException {
    public PianoNonAttivabileException(String messaggio) {
        super(messaggio);
    }
}
