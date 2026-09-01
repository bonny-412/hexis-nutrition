package com.hexisnutrition.backend.pazienti;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class JacksonPollock7CalcolatoreTest {

    private final JacksonPollock7Calcolatore calcolatore = new JacksonPollock7Calcolatore();

    @Test
    void plicheRichiesteSonoLeStesseSetteSediIndipendentementeDalSesso() {
        assertThat(calcolatore.plicheRichieste(Sesso.M)).containsExactlyInAnyOrder(
                CampoPlica.PETTORALE, CampoPlica.ASCELLARE, CampoPlica.TRICIPITALE, CampoPlica.SOTTOSCAPOLARE,
                CampoPlica.ADDOMINALE, CampoPlica.SOPRAILIACA, CampoPlica.COSCIA);
        assertThat(calcolatore.plicheRichieste(Sesso.F)).isEqualTo(calcolatore.plicheRichieste(Sesso.M));
    }

    @Test
    void calcolaDensitaEPercentualeGrassoUomo() {
        PlicheInput pliche = new PlicheInput(new BigDecimal("10.00"), new BigDecimal("12.00"),
                new BigDecimal("8.00"), null, new BigDecimal("14.00"), new BigDecimal("10.00"),
                new BigDecimal("16.00"), new BigDecimal("12.00"), null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.M, 30, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("82.00");
        assertThat(risultato.densitaCorporea().doubleValue()).isCloseTo(1.0714, Offset.offset(0.001));
        assertThat(risultato.percentualeGrasso().doubleValue()).isCloseTo(12.02, Offset.offset(0.05));
    }

    @Test
    void calcolaDensitaEPercentualeGrassoDonna() {
        PlicheInput pliche = new PlicheInput(new BigDecimal("8.00"), new BigDecimal("10.00"),
                new BigDecimal("15.00"), null, new BigDecimal("12.00"), new BigDecimal("14.00"),
                new BigDecimal("18.00"), new BigDecimal("16.00"), null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.F, 28, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("93.00");
        assertThat(risultato.densitaCorporea().doubleValue()).isCloseTo(1.0546, Offset.offset(0.001));
        assertThat(risultato.percentualeGrasso().doubleValue()).isCloseTo(19.39, Offset.offset(0.05));
    }
}
