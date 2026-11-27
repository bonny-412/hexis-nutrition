package com.hexisnutrition.backend.pianialimentari;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Dati del piano incoerenti tra loro, che la validazione del DTO da sola non può vedere (es. fine prima dell'inizio). */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PianoAlimentareNonValidoException extends RuntimeException {

    public PianoAlimentareNonValidoException(String messaggio) {
        super(messaggio);
    }
}
