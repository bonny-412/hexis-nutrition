package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AggiornaPianoAlimentareRequest(
        @NotBlank String nome,
        LocalDate dataFine,
        BigDecimal obiettivoKcal,
        @Valid List<PastoRequest> pasti,
        @Valid List<GiornoMacroTargetRequest> giorniMacroTarget,
        @Valid List<EsempioRequest> esempi
) {
}
