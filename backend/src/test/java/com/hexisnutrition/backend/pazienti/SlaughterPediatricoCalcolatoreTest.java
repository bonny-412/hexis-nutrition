package com.hexisnutrition.backend.pazienti;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class SlaughterPediatricoCalcolatoreTest {

    private final SlaughterPediatricoCalcolatore calcolatore = new SlaughterPediatricoCalcolatore();

    @Test
    void plicheRichiesteSonoTricipitaleEPolpaccio() {
        assertThat(calcolatore.plicheRichieste(Sesso.M))
                .containsExactlyInAnyOrder(CampoPlica.TRICIPITALE, CampoPlica.POLPACCIO);
    }

    @Test
    void maschioSottoI35MmUsaLaFormulaLineare() {
        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("10.00"), null, null, null, null, null,
                new BigDecimal("10.00"));
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.M, 12, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("20.00");
        assertThat(risultato.densitaCorporea()).isNull();
        assertThat(risultato.percentualeGrasso()).isEqualByComparingTo("15.70");
    }

    @Test
    void femminaSopraI35MmUsaLaFormulaQuadratica() {
        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("20.00"), null, null, null, null, null,
                new BigDecimal("20.00"));
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.F, 14, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("40.00");
        assertThat(risultato.percentualeGrasso()).isEqualByComparingTo("32.20");
    }

    @Test
    void maschioEsattamenteA35MmUsaLaFormulaQuadratica() {
        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("17.50"), null, null, null, null, null,
                new BigDecimal("17.50"));
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.M, 15, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("35.00");
        assertThat(risultato.percentualeGrasso()).isEqualByComparingTo("30.85");
    }
}
