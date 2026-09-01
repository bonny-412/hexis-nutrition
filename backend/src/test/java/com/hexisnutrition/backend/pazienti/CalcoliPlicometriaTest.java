package com.hexisnutrition.backend.pazienti;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class CalcoliPlicometriaTest {

    @Test
    void sommaValoriBigDecimal() {
        double somma = CalcoliPlicometria.somma(new BigDecimal("10.00"), new BigDecimal("10.00"), new BigDecimal("10.00"));

        assertThat(somma).isEqualTo(30.0);
    }

    @Test
    void arrotondaConHalfUp() {
        assertThat(CalcoliPlicometria.arrotonda(8.5097, 2)).isEqualByComparingTo("8.51");
        assertThat(CalcoliPlicometria.arrotonda(11.903, 2)).isEqualByComparingTo("11.90");
    }

    @Test
    void limiteSicurezzaNonAlteraValoriSopraSoglia() {
        assertThat(CalcoliPlicometria.applicaLimiteSicurezza(5.0, Sesso.M)).isEqualTo(5.0);
        assertThat(CalcoliPlicometria.applicaLimiteSicurezza(15.0, Sesso.F)).isEqualTo(15.0);
    }

    @Test
    void limiteSicurezzaAlzaValoriSottoSoglia() {
        assertThat(CalcoliPlicometria.applicaLimiteSicurezza(2.9, Sesso.M)).isEqualTo(3.0);
        assertThat(CalcoliPlicometria.applicaLimiteSicurezza(8.0, Sesso.F)).isEqualTo(10.0);
    }

    @Test
    void limiteSicurezzaApplicatoFalseSopraOSullaSoglia() {
        assertThat(CalcoliPlicometria.limiteSicurezzaApplicato(5.0, Sesso.M)).isFalse();
        assertThat(CalcoliPlicometria.limiteSicurezzaApplicato(3.0, Sesso.M)).isFalse();
        assertThat(CalcoliPlicometria.limiteSicurezzaApplicato(15.0, Sesso.F)).isFalse();
        assertThat(CalcoliPlicometria.limiteSicurezzaApplicato(10.0, Sesso.F)).isFalse();
    }

    @Test
    void limiteSicurezzaApplicatoTrueSottoSoglia() {
        assertThat(CalcoliPlicometria.limiteSicurezzaApplicato(2.9, Sesso.M)).isTrue();
        assertThat(CalcoliPlicometria.limiteSicurezzaApplicato(8.0, Sesso.F)).isTrue();
    }
}
