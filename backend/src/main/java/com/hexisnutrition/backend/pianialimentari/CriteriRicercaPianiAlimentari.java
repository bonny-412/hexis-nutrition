package com.hexisnutrition.backend.pianialimentari;

import java.util.UUID;

public record CriteriRicercaPianiAlimentari(
        String ricerca,
        StatoPianoVisualizzato stato,
        UUID pazienteId
) {
}
