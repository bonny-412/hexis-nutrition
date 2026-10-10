package com.hexisnutrition.backend.pianialimentari;

import java.time.LocalDate;
import java.util.UUID;

public record CriteriRicercaPianiAlimentari(
        String ricerca,
        StatoPianoVisualizzato stato,
        UUID pazienteId,
        boolean escludiAttivo,
        boolean escludiBozze,
        LocalDate dataInizioDa,
        LocalDate dataInizioA,
        LocalDate dataFineDa,
        LocalDate dataFineA
) {
}
