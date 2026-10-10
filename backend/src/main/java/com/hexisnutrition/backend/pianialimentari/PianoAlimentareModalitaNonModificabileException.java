package com.hexisnutrition.backend.pianialimentari;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** La modalità di un piano si può cambiare solo finché è in bozza. */
@ResponseStatus(HttpStatus.CONFLICT)
public class PianoAlimentareModalitaNonModificabileException extends RuntimeException {
}
