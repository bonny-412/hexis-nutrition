package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.UUID;

public record RigaAlimentoRequest(
        UUID alimentoId,
        @NotBlank String nome,
        @NotNull @PositiveOrZero BigDecimal kcal100g,
        @NotNull @PositiveOrZero BigDecimal proteine100g,
        @NotNull @PositiveOrZero BigDecimal carboidrati100g,
        @NotNull @PositiveOrZero BigDecimal grassi100g,
        @PositiveOrZero BigDecimal zuccheri100g,
        @NotNull @PositiveOrZero BigDecimal grammi
) {
}
