package com.hexisnutrition.backend.pazienti;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VisitaRequest(
        LocalDate dataVisita,
        @NotNull @Positive @Max(300) Integer altezzaCm,
        @NotNull @Positive @Digits(integer = 4, fraction = 2) BigDecimal pesoKg,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaVitaCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaOmbelicoCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaFianchiCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaPettoCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaCosciaDxCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaCosciaSxCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaPolpaccioDxCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaPolpaccioSxCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal larghezzaSpalleCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaSpalleCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaBicipiteDxCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaBicipiteSxCm
) {
}
