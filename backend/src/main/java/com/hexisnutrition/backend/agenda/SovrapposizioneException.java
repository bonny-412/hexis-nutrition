package com.hexisnutrition.backend.agenda;

import java.util.List;

/** La visita che si sta salvando si sovrappone ad altri appuntamenti; serve la conferma esplicita. */
public class SovrapposizioneException extends RuntimeException {

    private final List<AppuntamentoResponse> conflitti;

    public SovrapposizioneException(List<AppuntamentoResponse> conflitti) {
        super("La visita si sovrappone ad altri appuntamenti");
        this.conflitti = List.copyOf(conflitti);
    }

    public List<AppuntamentoResponse> getConflitti() {
        return conflitti;
    }
}
