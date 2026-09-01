package com.hexisnutrition.backend.pazienti;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class PlicometriaNonDisponibilePerSessoException extends RuntimeException {
    public PlicometriaNonDisponibilePerSessoException() {
        super("La plicometria non è disponibile per sesso ALTRO: le equazioni richiedono M o F.");
    }
}
