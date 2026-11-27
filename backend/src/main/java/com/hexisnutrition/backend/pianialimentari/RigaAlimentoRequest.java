package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/** I massimi seguono le colonne di piano_alimento_righe: NUMERIC(7,2) per kcal e grammi, NUMERIC(6,2) per il resto. */
public record RigaAlimentoRequest(
        UUID alimentoId,
        @NotBlank @Size(max = 200) String nome,
        @NotNull @PositiveOrZero @DecimalMax("99999.99") BigDecimal kcal100g,
        @NotNull @PositiveOrZero @DecimalMax("9999.99") BigDecimal proteine100g,
        @NotNull @PositiveOrZero @DecimalMax("9999.99") BigDecimal carboidrati100g,
        @NotNull @PositiveOrZero @DecimalMax("9999.99") BigDecimal grassi100g,
        @PositiveOrZero @DecimalMax("9999.99") BigDecimal zuccheri100g,
        @PositiveOrZero @DecimalMax("9999.99") BigDecimal fibre100g,
        @PositiveOrZero @DecimalMax("9999.99") BigDecimal ferro100mg,
        @PositiveOrZero @DecimalMax("9999.99") BigDecimal calcio100mg,
        @PositiveOrZero @DecimalMax("9999.99") BigDecimal acqua100g,
        /** Porzione della riga: deve essere positiva, una riga da 0 g non ha senso nel piano (bug 07 del collaudo). */
        @NotNull @Positive @DecimalMax("99999.99") BigDecimal grammi
) {
}
