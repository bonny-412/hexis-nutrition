package com.hexisnutrition.backend.pianialimentari;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Un piano si può modificare solo finché è in bozza: da attivo in poi è in sola lettura. */
@ResponseStatus(HttpStatus.CONFLICT)
public class PianoAlimentareNonModificabileException extends RuntimeException {
}
