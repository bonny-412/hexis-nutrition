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
        String protocolloVita,
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
                visita.getProtocolloVita().name(),
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
            ProtocolloPlicometrico protocollo,
            EtniaAtleta etniaAtleta,
            BigDecimal plicaPettoraleMm,
            BigDecimal plicaAscellareMm,
            BigDecimal plicaTricipitaleMm,
            BigDecimal plicaBicipitaleMm,
            BigDecimal plicaSottoscapolareMm,
            BigDecimal plicaSoprailiacaMm,
            BigDecimal plicaAddominaleMm,
            BigDecimal plicaCosciaMm,
            BigDecimal plicaPolpaccioMm,
            BigDecimal percentualeGrassoCorporeo,
            BigDecimal massaGrassaKg,
            BigDecimal massaMagraKg,
            BigDecimal fmi,
            BigDecimal ffmi
    ) {
        public static PlicometriaResponse da(Plicometria plicometria) {
            return new PlicometriaResponse(
                    plicometria.getProtocollo(),
                    plicometria.getEtniaAtleta(),
                    plicometria.getPlicaPettoraleMm(),
                    plicometria.getPlicaAscellareMm(),
                    plicometria.getPlicaTricipitaleMm(),
                    plicometria.getPlicaBicipitaleMm(),
                    plicometria.getPlicaSottoscapolareMm(),
                    plicometria.getPlicaSoprailiacaMm(),
                    plicometria.getPlicaAddominaleMm(),
                    plicometria.getPlicaCosciaMm(),
                    plicometria.getPlicaPolpaccioMm(),
                    plicometria.getPercentualeGrasso(),
                    plicometria.getMassaGrassaKg(),
                    plicometria.getMassaMagraKg(),
                    plicometria.getFmi(),
                    plicometria.getFfmi());
        }
    }
}
