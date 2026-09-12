package com.hexisnutrition.backend.pianialimentari;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PianoAlimentareRigaListaResponse(
        UUID id,
        String pazienteNomeCompleto,
        String nome,
        StatoPianoVisualizzato stato,
        BigDecimal obiettivoKcal,
        LocalDate dataFine
) {
}
