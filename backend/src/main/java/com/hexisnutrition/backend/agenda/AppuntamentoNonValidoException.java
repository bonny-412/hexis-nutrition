package com.hexisnutrition.backend.agenda;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AppuntamentoNonValidoException extends RuntimeException {

    public AppuntamentoNonValidoException(String messaggio) {
        super(messaggio);
    }
}
