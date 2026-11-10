package com.hexisnutrition.backend.pianialimentari;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Un piano si può modificare finché è in bozza o attivo: una volta concluso (scaduto o terminato) è in sola lettura. */
@ResponseStatus(HttpStatus.CONFLICT)
public class PianoAlimentareNonModificabileException extends RuntimeException {
}
