package com.hexisnutrition.backend.pianialimentari;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Le note di un piano si aggiungono e si eliminano solo finché il piano è attivo (non scaduto né terminato). */
@ResponseStatus(HttpStatus.CONFLICT)
public class PianoNoteNonModificabiliException extends RuntimeException {
}
