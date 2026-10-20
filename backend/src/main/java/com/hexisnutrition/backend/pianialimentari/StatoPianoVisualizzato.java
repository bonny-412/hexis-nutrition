package com.hexisnutrition.backend.pianialimentari;

/**
 * Stato esposto in API, mai persistito. CONCLUSO raccoglie i piani terminati a mano (stato TERMINATO) e quelli
 * attivi con data di fine passata: per il professionista sono la stessa cosa, un piano chiuso e consultabile.
 */
public enum StatoPianoVisualizzato {
    BOZZA, ATTIVO, CONCLUSO
}
