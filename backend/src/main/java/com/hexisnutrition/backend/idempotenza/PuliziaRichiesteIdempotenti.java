package com.hexisnutrition.backend.idempotenza;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/** Un nuovo tentativo arriva entro pochi minuti: dopo 24 ore la risposta salvata non serve più. */
@Component
public class PuliziaRichiesteIdempotenti {

    static final Duration DURATA = Duration.ofHours(24);

    private final RegistroIdempotenza registro;

    public PuliziaRichiesteIdempotenti(RegistroIdempotenza registro) {
        this.registro = registro;
    }

    @Scheduled(cron = "0 15 * * * *")
    public void pulisci() {
        registro.cancellaPiuVecchieDi(DURATA);
    }
}
