package com.hexisnutrition.backend.pazienti;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VisitaRequest(
        LocalDate dataVisita,
        @NotNull @Min(50) @Max(300) Integer altezzaCm,
        @NotNull @Positive @Digits(integer = 4, fraction = 2) BigDecimal pesoKg,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaVitaCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaFianchiCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaAddomeCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaBraccioRilassatoCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaCosciaCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaPolpaccioCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaColloCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaToraceCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaBraccioContrattoCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaAvambraccioCm,
        @Positive @Digits(integer = 4, fraction = 2) BigDecimal circonferenzaCavigliaCm,
        ProtocolloVita protocolloVita,
        String note,
        ObiettivoVisita obiettivo,
        @Valid PlicometriaRequest plicometria
) {
}
