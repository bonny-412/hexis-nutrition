package com.hexisnutrition.backend.notifiche;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IscrizionePushNonValidaException extends RuntimeException {

    public IscrizionePushNonValidaException(String messaggio) {
        super(messaggio);
    }
}
