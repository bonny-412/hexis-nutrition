package com.hexisnutrition.backend.pazienti;

import java.time.LocalDate;

public record CriteriRicercaPazienti(
        String ricerca,
        StatoAccountPaziente statoAccount,
        ObiettivoVisita obiettivo,
        LocalDate dataUltimaVisitaDa,
        LocalDate dataUltimaVisitaA,
        boolean archiviato
) {
}
