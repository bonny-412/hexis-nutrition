package com.hexisnutrition.backend.pazienti;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class EvansAtletiCalcolatoreTest {

    private final EvansAtletiCalcolatore calcolatore = new EvansAtletiCalcolatore();

    @Test
    void plicheRichiesteSonoTricipitaleAddominaleCoscia() {
        assertThat(calcolatore.plicheRichieste(Sesso.M))
                .containsExactlyInAnyOrder(CampoPlica.TRICIPITALE, CampoPlica.ADDOMINALE, CampoPlica.COSCIA);
    }

    @Test
    void uomoCaucasico() {
        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("10.00"), null, null, null,
                new BigDecimal("10.00"), new BigDecimal("10.00"), null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.M, 24, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("30.00");
        assertThat(risultato.densitaCorporea()).isNull();
        assertThat(risultato.percentualeGrasso()).isEqualByComparingTo("10.05");
    }

    @Test
    void donnaAfroamericana() {
        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("10.00"), null, null, null,
                new BigDecimal("10.00"), new BigDecimal("10.00"), null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.F, 24, EtniaAtleta.AFROAMERICANO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.percentualeGrasso()).isEqualByComparingTo("14.40");
    }
}
