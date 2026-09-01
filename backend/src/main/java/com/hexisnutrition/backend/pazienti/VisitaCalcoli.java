package com.hexisnutrition.backend.pazienti;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class VisitaCalcoli {

    private VisitaCalcoli() {
    }

    public static void applica(Visita visita) {
        BigDecimal altezzaCm = BigDecimal.valueOf(visita.getAltezzaCm());
        BigDecimal altezzaM = altezzaCm.divide(BigDecimal.valueOf(100));
        BigDecimal altezzaM2 = altezzaM.multiply(altezzaM);

        visita.setBmi(visita.getPesoKg().divide(altezzaM2, 2, RoundingMode.HALF_UP));

        BigDecimal vita = visita.getCirconferenzaVitaCm();
        BigDecimal fianchi = visita.getCirconferenzaFianchiCm();

        if (vita != null && fianchi != null) {
            visita.setWhr(vita.divide(fianchi, 2, RoundingMode.HALF_UP));
        }
        if (vita != null) {
            visita.setWhtr(vita.divide(altezzaCm, 2, RoundingMode.HALF_UP));
        }
    }

    public static void applicaMamc(Visita visita, BigDecimal plicaTricipitaleMm) {
        BigDecimal braccio = visita.getCirconferenzaBraccioRilassatoCm();
        if (braccio == null || plicaTricipitaleMm == null) {
            return;
        }
        BigDecimal termineCm = BigDecimal.valueOf(Math.PI)
                .multiply(plicaTricipitaleMm)
                .divide(BigDecimal.TEN, 4, RoundingMode.HALF_UP);
        visita.setMamcCm(braccio.subtract(termineCm).setScale(2, RoundingMode.HALF_UP));
    }
}
