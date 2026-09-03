package com.hexisnutrition.backend.pazienti;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VisitaCalcoliTest {

    private Visita visitaConMisure(BigDecimal vita, BigDecimal fianchi) {
        return new Visita(UUID.randomUUID(), null, 180, new BigDecimal("82.50"),
                vita, fianchi, null, null, null, null, null, null, null, null, null,
                ProtocolloVita.OMS, null, null);
    }

    @Test
    void calcolaBmiSempre() {
        Visita visita = visitaConMisure(null, null);

        VisitaCalcoli.applica(visita);

        assertThat(visita.getBmi()).isEqualByComparingTo("25.46");
    }

    @Test
    void calcolaWhrSoloSeVitaEFianchiPresenti() {
        Visita conEntrambe = visitaConMisure(new BigDecimal("95.00"), new BigDecimal("100.00"));
        Visita senzaFianchi = visitaConMisure(new BigDecimal("95.00"), null);

        VisitaCalcoli.applica(conEntrambe);
        VisitaCalcoli.applica(senzaFianchi);

        assertThat(conEntrambe.getWhr()).isEqualByComparingTo("0.95");
        assertThat(senzaFianchi.getWhr()).isNull();
    }

    @Test
    void calcolaWhtrSoloSeVitaPresente() {
        Visita conVita = visitaConMisure(new BigDecimal("95.00"), null);
        Visita senzaVita = visitaConMisure(null, new BigDecimal("100.00"));

        VisitaCalcoli.applica(conVita);
        VisitaCalcoli.applica(senzaVita);

        assertThat(conVita.getWhtr()).isEqualByComparingTo("0.53");
        assertThat(senzaVita.getWhtr()).isNull();
    }

    @Test
    void applicaMamcSoloSeBraccioRilassatoPresente() {
        Visita conBraccio = new Visita(UUID.randomUUID(), null, 180, new BigDecimal("82.50"),
                null, null, null, new BigDecimal("32.00"), null, null, null, null, null, null, null,
                ProtocolloVita.OMS, null, null);
        Visita senzaBraccio = visitaConMisure(null, null);

        VisitaCalcoli.applicaMamc(conBraccio, new BigDecimal("16.00"));
        VisitaCalcoli.applicaMamc(senzaBraccio, new BigDecimal("16.00"));

        assertThat(conBraccio.getMamcCm()).isEqualByComparingTo("26.97");
        assertThat(senzaBraccio.getMamcCm()).isNull();
    }
}
