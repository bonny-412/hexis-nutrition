package com.hexisnutrition.backend.pianialimentari;

import java.math.BigDecimal;

public record SuggerimentoTdee(
        BigDecimal bmr,
        BigDecimal tdee,
        FormulaBmr formulaUsata,
        BigDecimal calorieSuggerite,
        boolean sottoSogliaSicurezza
) {
}
