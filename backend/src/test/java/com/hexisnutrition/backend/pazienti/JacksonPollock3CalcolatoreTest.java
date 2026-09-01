package com.hexisnutrition.backend.pazienti;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonPollock3CalcolatoreTest {

    private final JacksonPollock3Calcolatore calcolatore = new JacksonPollock3Calcolatore();

    @Test
    void plicheRichiesteDipendonoDalSesso() {
        assertThat(calcolatore.plicheRichieste(Sesso.M))
                .containsExactlyInAnyOrder(CampoPlica.PETTORALE, CampoPlica.ADDOMINALE, CampoPlica.COSCIA);
        assertThat(calcolatore.plicheRichieste(Sesso.F))
                .containsExactlyInAnyOrder(CampoPlica.TRICIPITALE, CampoPlica.SOPRAILIACA, CampoPlica.COSCIA);
    }

    @Test
    void calcolaDensitaEPercentualeGrassoUomo() {
        PlicheInput pliche = new PlicheInput(new BigDecimal("10.00"), null, null, null, null, null,
                new BigDecimal("10.00"), new BigDecimal("10.00"), null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.M, 25, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("30.00");
        assertThat(risultato.densitaCorporea().doubleValue()).isCloseTo(1.0796, Offset.offset(0.001));
        assertThat(risultato.percentualeGrasso().doubleValue()).isCloseTo(8.51, Offset.offset(0.05));
        assertThat(risultato.formulaVersione()).isEqualTo("jackson-pollock-1978-3siti");
    }

    @Test
    void calcolaDensitaEPercentualeGrassoDonna() {
        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("15.00"), null, null,
                new BigDecimal("10.00"), null, new BigDecimal("15.00"), null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.F, 25, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("40.00");
        assertThat(risultato.densitaCorporea().doubleValue()).isCloseTo(1.0600, Offset.offset(0.001));
        assertThat(risultato.percentualeGrasso().doubleValue()).isCloseTo(16.99, Offset.offset(0.05));
    }
}
