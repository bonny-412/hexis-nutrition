package com.hexisnutrition.backend.pazienti;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record PlicometriaRequest(
        ProtocolloPlicometrico protocollo,
        EtniaAtleta etniaAtleta,
        @Positive @Digits(integer = 3, fraction = 2) @DecimalMax("100") BigDecimal plicaPettoraleMm,
        @Positive @Digits(integer = 3, fraction = 2) @DecimalMax("100") BigDecimal plicaAscellareMm,
        @Positive @Digits(integer = 3, fraction = 2) @DecimalMax("100") BigDecimal plicaTricipitaleMm,
        @Positive @Digits(integer = 3, fraction = 2) @DecimalMax("100") BigDecimal plicaBicipitaleMm,
        @Positive @Digits(integer = 3, fraction = 2) @DecimalMax("100") BigDecimal plicaSottoscapolareMm,
        @Positive @Digits(integer = 3, fraction = 2) @DecimalMax("100") BigDecimal plicaSoprailiacaMm,
        @Positive @Digits(integer = 3, fraction = 2) @DecimalMax("100") BigDecimal plicaAddominaleMm,
        @Positive @Digits(integer = 3, fraction = 2) @DecimalMax("100") BigDecimal plicaCosciaMm,
        @Positive @Digits(integer = 3, fraction = 2) @DecimalMax("100") BigDecimal plicaPolpaccioMm
) {
}
