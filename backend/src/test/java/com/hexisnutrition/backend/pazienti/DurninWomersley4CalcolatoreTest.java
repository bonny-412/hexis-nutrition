package com.hexisnutrition.backend.pazienti;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DurninWomersley4CalcolatoreTest {

    @Test
    void plicheRichiesteSonoLeQuattroStandard() {
        DurninWomersley4Calcolatore calcolatore = new DurninWomersley4Calcolatore(mock(DurninWomersleyCoefficienteRepository.class));

        assertThat(calcolatore.plicheRichieste(Sesso.M)).containsExactlyInAnyOrder(
                CampoPlica.BICIPITALE, CampoPlica.TRICIPITALE, CampoPlica.SOTTOSCAPOLARE, CampoPlica.SOPRAILIACA);
    }

    @Test
    void calcolaDensitaEPercentualeGrassoConCoefficientiTrovati() {
        DurninWomersleyCoefficienteRepository repository = mock(DurninWomersleyCoefficienteRepository.class);
        DurninWomersleyCoefficiente coefficiente = new DurninWomersleyCoefficiente(
                Sesso.M, 20, 29, new BigDecimal("1.1631"), new BigDecimal("0.0632"));
        when(repository.trovaCandidati(Sesso.M, 25)).thenReturn(List.of(coefficiente));
        DurninWomersley4Calcolatore calcolatore = new DurninWomersley4Calcolatore(repository);

        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("15.00"), new BigDecimal("8.00"),
                new BigDecimal("14.00"), new BigDecimal("12.00"), null, null, null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.M, 25, EtniaAtleta.CAUCASICO);

        RisultatoDensita risultato = calcolatore.calcola(pliche, contesto);

        assertThat(risultato.sommaPlicheMm()).isEqualByComparingTo("49.00");
        assertThat(risultato.densitaCorporea().doubleValue()).isCloseTo(1.0563, Offset.offset(0.001));
        assertThat(risultato.percentualeGrasso().doubleValue()).isCloseTo(18.63, Offset.offset(0.05));
        assertThat(risultato.coefficienteC()).isEqualByComparingTo("1.1631");
        assertThat(risultato.coefficienteM()).isEqualByComparingTo("0.0632");
    }

    @Test
    void lanciaEccezioneSeNessunCoefficienteApplicabile() {
        DurninWomersleyCoefficienteRepository repository = mock(DurninWomersleyCoefficienteRepository.class);
        when(repository.trovaCandidati(any(), anyInt())).thenReturn(List.of());
        DurninWomersley4Calcolatore calcolatore = new DurninWomersley4Calcolatore(repository);

        PlicheInput pliche = new PlicheInput(null, null, new BigDecimal("15.00"), new BigDecimal("8.00"),
                new BigDecimal("14.00"), new BigDecimal("12.00"), null, null, null);
        ContestoPlicometria contesto = new ContestoPlicometria(Sesso.M, 10, EtniaAtleta.CAUCASICO);

        assertThatThrownBy(() -> calcolatore.calcola(pliche, contesto))
                .isInstanceOf(CoefficientiDurninMancantiException.class);
    }
}
