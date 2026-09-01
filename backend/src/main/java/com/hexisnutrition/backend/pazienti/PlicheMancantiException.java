package com.hexisnutrition.backend.pazienti;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PlicheMancantiException extends RuntimeException {
    public PlicheMancantiException(String messaggio) {
        super(messaggio);
    }
}
