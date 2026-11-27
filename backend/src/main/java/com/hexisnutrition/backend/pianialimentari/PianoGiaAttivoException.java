package com.hexisnutrition.backend.pianialimentari;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Un'altra attivazione per lo stesso paziente è arrivata nello stesso momento: l'indice unico (V35) l'ha respinta. */
@ResponseStatus(HttpStatus.CONFLICT)
public class PianoGiaAttivoException extends RuntimeException {
    public PianoGiaAttivoException() {
        super("Nel frattempo è stato attivato un altro piano per questo paziente. Ricarica la pagina e riprova.");
    }
}
