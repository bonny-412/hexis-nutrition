package com.hexisnutrition.backend.pianialimentari;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PianoAlimentareResponse(
        UUID id,
        UUID pazienteId,
        String pazienteNomeCompleto,
        String nome,
        ModalitaPiano modalita,
        StatoPianoVisualizzato stato,
        LocalDate dataInizio,
        LocalDate dataFine,
        BigDecimal obiettivoKcal,
        BigDecimal obiettivoKcalSuggerito,
        BigDecimal bmrCalcolato,
        BigDecimal tdeeCalcolato,
        FormulaBmr formulaBmrUsata,
        boolean sottoSogliaSicurezza,
        List<PastoResponse> pasti,
        List<GiornoMacroTargetResponse> giorniMacroTarget,
        List<EsempioResponse> esempi
) {
}
