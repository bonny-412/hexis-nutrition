package com.hexisnutrition.backend.pazienti;

import java.math.BigDecimal;

public record RisultatoDensita(
        BigDecimal sommaPlicheMm,
        BigDecimal densitaCorporea,
        BigDecimal percentualeGrasso,
        BigDecimal coefficienteC,
        BigDecimal coefficienteM,
        String formulaVersione
) {
}
