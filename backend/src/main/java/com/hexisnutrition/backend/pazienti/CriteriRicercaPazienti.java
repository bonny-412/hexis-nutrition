package com.hexisnutrition.backend.pazienti;

import java.time.LocalDate;

public record CriteriRicercaPazienti(
        String ricerca,
        StatoAccountPaziente statoAccount,
        Sesso sesso,
        LocalDate dataNascitaDa,
        LocalDate dataNascitaA,
        boolean archiviato
) {
}
