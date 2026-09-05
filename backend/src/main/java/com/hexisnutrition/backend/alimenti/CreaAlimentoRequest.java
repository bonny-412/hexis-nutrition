package com.hexisnutrition.backend.alimenti;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record CreaAlimentoRequest(
        @NotBlank String nome,
        @NotBlank String categoria,
        @NotNull @Positive BigDecimal quantitaG,
        @NotNull @PositiveOrZero BigDecimal kcal,
        @NotNull @PositiveOrZero BigDecimal proteineG,
        @NotNull @PositiveOrZero BigDecimal grassiG,
        @NotNull @PositiveOrZero BigDecimal carboidratiG,
        @PositiveOrZero BigDecimal acquaG,
        @PositiveOrZero BigDecimal fibreG,
        @PositiveOrZero BigDecimal zuccheriG,
        @PositiveOrZero BigDecimal ferroMg,
        @PositiveOrZero BigDecimal calcioMg,
        @PositiveOrZero BigDecimal sodioMg
) {
}
