package com.hexisnutrition.backend.pazienti;

import java.math.BigDecimal;

public record PlicheInput(
        BigDecimal pettoraleMm,
        BigDecimal ascellareMm,
        BigDecimal tricipitaleMm,
        BigDecimal bicipitaleMm,
        BigDecimal sottoscapolareMm,
        BigDecimal soprailiacaMm,
        BigDecimal addominaleMm,
        BigDecimal cosciaMm,
        BigDecimal polpaccioMm
) {
}
