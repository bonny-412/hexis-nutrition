package com.hexisnutrition.backend.pazienti;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record VisitaResponse(
        UUID id,
        LocalDate dataVisita,
        Integer altezzaCm,
        BigDecimal pesoKg,
        BigDecimal bmi,
        BigDecimal whr,
        BigDecimal whtr,
        BigDecimal mamcCm,
        Circonferenze circonferenze,
        String note,
        String obiettivo,
        PlicometriaResponse plicometria
) {
    public static VisitaResponse da(Visita visita, Plicometria plicometria) {
        return new VisitaResponse(
                visita.getId(),
                visita.getDataVisita(),
                visita.getAltezzaCm(),
                visita.getPesoKg(),
                visita.getBmi(),
                visita.getWhr(),
                visita.getWhtr(),
                visita.getMamcCm(),
                Circonferenze.da(visita),
                visita.getNote(),
                visita.getObiettivo().name(),
                plicometria != null ? PlicometriaResponse.da(plicometria) : null);
    }

    public record Circonferenze(
            BigDecimal vitaCm,
            BigDecimal fianchiCm,
            BigDecimal addomeCm,
            BigDecimal braccioRilassatoCm,
            BigDecimal cosciaCm,
            BigDecimal polpaccioCm,
            BigDecimal colloCm,
            BigDecimal toraceCm,
            BigDecimal braccioContrattoCm,
            BigDecimal avambraccioCm,
            BigDecimal cavigliaCm
    ) {
        public static Circonferenze da(Visita visita) {
            return new Circonferenze(
                    visita.getCirconferenzaVitaCm(),
                    visita.getCirconferenzaFianchiCm(),
                    visita.getCirconferenzaAddomeCm(),
                    visita.getCirconferenzaBraccioRilassatoCm(),
                    visita.getCirconferenzaCosciaCm(),
                    visita.getCirconferenzaPolpaccioCm(),
                    visita.getCirconferenzaColloCm(),
                    visita.getCirconferenzaToraceCm(),
                    visita.getCirconferenzaBraccioContrattoCm(),
                    visita.getCirconferenzaAvambraccioCm(),
                    visita.getCirconferenzaCavigliaCm());
        }
    }

    public record PlicometriaResponse(
            BigDecimal percentualeGrassoCorporeo,
            BigDecimal massaGrassaKg,
            BigDecimal massaMagraKg,
            BigDecimal fmi,
            BigDecimal ffmi
    ) {
        public static PlicometriaResponse da(Plicometria plicometria) {
            return new PlicometriaResponse(
                    plicometria.getPercentualeGrasso(),
                    plicometria.getMassaGrassaKg(),
                    plicometria.getMassaMagraKg(),
                    plicometria.getFmi(),
                    plicometria.getFfmi());
        }
    }
}
