package com.hexisnutrition.backend.agenda;

public enum TipoAppuntamento {
    VISITA_STUDIO,
    VIDEOCONSULTO,
    PROMEMORIA,
    BLOCCO;

    public boolean isVisita() {
        return this == VISITA_STUDIO || this == VIDEOCONSULTO;
    }
}
