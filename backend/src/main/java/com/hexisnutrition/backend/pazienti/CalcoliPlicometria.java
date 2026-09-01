package com.hexisnutrition.backend.pazienti;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class CalcoliPlicometria {

    private CalcoliPlicometria() {
    }

    static double somma(BigDecimal... valori) {
        double totale = 0;
        for (BigDecimal valore : valori) {
            totale += valore.doubleValue();
        }
        return totale;
    }

    static BigDecimal arrotonda(double valore, int scala) {
        return BigDecimal.valueOf(valore).setScale(scala, RoundingMode.HALF_UP);
    }

    static double percentualeGrassoSiri(double densitaCorporea) {
        return 495 / densitaCorporea - 450;
    }

    static double applicaLimiteSicurezza(double percentualeGrasso, Sesso sesso) {
        double soglia = sesso == Sesso.M ? 3.0 : 10.0;
        return Math.max(soglia, percentualeGrasso);
    }

    static boolean limiteSicurezzaApplicato(double percentualeGrasso, Sesso sesso) {
        double soglia = sesso == Sesso.M ? 3.0 : 10.0;
        return percentualeGrasso < soglia;
    }
}
