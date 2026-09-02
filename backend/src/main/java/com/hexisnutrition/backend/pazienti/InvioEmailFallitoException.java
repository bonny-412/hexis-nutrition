package com.hexisnutrition.backend.pazienti;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class InvioEmailFallitoException extends RuntimeException {

    public InvioEmailFallitoException(Throwable causa) {
        super("Impossibile inviare l'email di invito. Riprova più tardi.", causa);
    }
}
