package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record GiornoMacroTargetRequest(
        @NotNull GiornoSettimana giornoSettimana,
        BigDecimal kcalTarget,
        BigDecimal proteineTarget,
        BigDecimal carboidratiTarget,
        BigDecimal grassiTarget
) {
}
