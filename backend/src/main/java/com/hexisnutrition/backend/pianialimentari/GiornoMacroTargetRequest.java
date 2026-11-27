package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** I massimi seguono le colonne di piano_giorno_macro_target: NUMERIC(7,2) per le kcal, NUMERIC(6,2) per i grammi. */
public record GiornoMacroTargetRequest(
        @NotNull GiornoSettimana giornoSettimana,
        @PositiveOrZero @DecimalMax("99999.99") BigDecimal kcalTarget,
        @PositiveOrZero @DecimalMax("9999.99") BigDecimal proteineTarget,
        @PositiveOrZero @DecimalMax("9999.99") BigDecimal carboidratiTarget,
        @PositiveOrZero @DecimalMax("9999.99") BigDecimal grassiTarget,
        @Size(max = 5000) String nota
) {
}
