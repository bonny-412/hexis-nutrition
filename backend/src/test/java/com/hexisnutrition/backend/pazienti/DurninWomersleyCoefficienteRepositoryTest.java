package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DurninWomersleyCoefficienteRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private DurninWomersleyCoefficienteRepository repository;

    @Test
    void trovaLaFasciaCorrettaPerEtaIntermedia() {
        List<DurninWomersleyCoefficiente> candidati = repository.trovaCandidati(Sesso.M, 25);

        assertThat(candidati).hasSize(1);
        assertThat(candidati.get(0).getC()).isEqualByComparingTo("1.1631");
        assertThat(candidati.get(0).getM()).isEqualByComparingTo("0.0632");
    }

    @Test
    void usaSempreLUltimaFasciaOltreI50Anni() {
        List<DurninWomersleyCoefficiente> candidati = repository.trovaCandidati(Sesso.F, 70);

        assertThat(candidati).hasSize(1);
        assertThat(candidati.get(0).getC()).isEqualByComparingTo("1.1339");
        assertThat(candidati.get(0).getM()).isEqualByComparingTo("0.0645");
    }

    @Test
    void nessunaRigaSottoLaFasciaMinima() {
        List<DurninWomersleyCoefficiente> candidati = repository.trovaCandidati(Sesso.M, 10);

        assertThat(candidati).isEmpty();
    }
}
