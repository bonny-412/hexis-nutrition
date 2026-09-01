package com.hexisnutrition.backend.pazienti;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class Faulkner4CalcolatoreTest {

    private final Faulkner4Calcolatore calcolatore = new Faulkner4Calcolatore();

    @Test
    void plicheRichiesteSonoLeQuattroStandard() {
        assertThat(calcolatore.plicheRichieste(Sesso.M)).containsExactlyInAnyOrder(
                CampoPlica.TRICIPITALE, CampoPlica.SOTTOSCAPOLARE, CampoPlica.SOPRAILIACA, CampoPlica.ADDOMINALE);
    }

    @Test
    void calcolaPercentualeGrassoDirettaSenzaDensita() {
        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("10.00"), null,
                new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("10.00"), null, null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.M, 30, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("40.00");
        assertThat(risultato.densitaCorporea()).isNull();
        assertThat(risultato.percentualeGrasso()).isEqualByComparingTo("11.90");
    }
}
