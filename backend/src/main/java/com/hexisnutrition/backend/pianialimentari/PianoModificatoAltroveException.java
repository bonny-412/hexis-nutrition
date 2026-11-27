package com.hexisnutrition.backend.pianialimentari;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Il piano è stato salvato da un'altra parte (un'altra scheda) dopo che il client l'aveva letto: niente sovrascrittura. */
@ResponseStatus(HttpStatus.CONFLICT)
public class PianoModificatoAltroveException extends RuntimeException {
    public PianoModificatoAltroveException() {
        super("Il piano è stato modificato in un'altra scheda. Ricarica la pagina per vedere la versione aggiornata.");
    }
}
