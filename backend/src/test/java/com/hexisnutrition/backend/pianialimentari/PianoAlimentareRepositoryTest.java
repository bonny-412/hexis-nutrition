package com.hexisnutrition.backend.pianialimentari;

import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.Sesso;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PianoAlimentareRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private PianoAlimentareRepository pianoAlimentareRepository;
    @Autowired
    private PastoRepository pastoRepository;
    @Autowired
    private PianoAlimentoRigaRepository pianoAlimentoRigaRepository;
    @Autowired
    private PazienteRepository pazienteRepository;
    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @Test
    void salvaPianoConPastoERigaEIndiciziaPerPazienteEStato() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof@test.it", "hash", "Anna", "Rossi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Mario", "Bianchi",
                "BNCMRA80A01H501U", "mario@test.it", "333", LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));

        PianoAlimentare piano = pianoAlimentareRepository.save(
                new PianoAlimentare(paziente.getId(), professionista.getId(), null, "Ipertrofia · fase 1",
                        ModalitaPiano.PASTI));

        Pasto pasto = pastoRepository.save(
                new Pasto(piano.getId(), GiornoSettimana.LUNEDI, "Colazione", TipoPasto.COLAZIONE, null, 0));
        pianoAlimentoRigaRepository.save(PianoAlimentoRiga.perPasto(pasto.getId(), null, "Avena",
                java.math.BigDecimal.valueOf(372), java.math.BigDecimal.valueOf(12.9), java.math.BigDecimal.valueOf(65),
                java.math.BigDecimal.valueOf(6.5), java.math.BigDecimal.valueOf(1.1), null, null, null, null,
                java.math.BigDecimal.valueOf(60), 0));

        assertThat(pianoAlimentareRepository.findAllByPazienteIdAndStato(paziente.getId(), StatoPiano.BOZZA))
                .hasSize(1);
        assertThat(pastoRepository.findAllByPianoIdOrderByGiornoSettimanaAscOrdineAsc(piano.getId())).hasSize(1);
        assertThat(pianoAlimentoRigaRepository.findAllByPastoIdInOrderByOrdineAsc(java.util.List.of(pasto.getId())))
                .hasSize(1);
    }
}
