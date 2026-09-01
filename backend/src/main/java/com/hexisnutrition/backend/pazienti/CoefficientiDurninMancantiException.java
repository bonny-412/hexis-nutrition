package com.hexisnutrition.backend.pazienti;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CoefficientiDurninMancantiException extends RuntimeException {
    public CoefficientiDurninMancantiException(String messaggio) {
        super(messaggio);
    }
}
