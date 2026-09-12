package com.hexisnutrition.backend.pianialimentari;

/** Stato esposto in API: include SCADUTO, calcolato da ATTIVO + dataFine passata, mai persistito. */
public enum StatoPianoVisualizzato {
    BOZZA, ATTIVO, SCADUTO, TERMINATO
}
