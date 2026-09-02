package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class VisitaRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private VisitaRepository visitaRepository;

    @Autowired
    private PazienteRepository pazienteRepository;

    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @AfterEach
    void pulisci() {
        visitaRepository.deleteAll();
        pazienteRepository.deleteAll();
        professionistaRepository.deleteAll();
    }

    @Test
    void salvaERitrovaPerPaziente() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("visite-prof@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "visite-luca@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));

        Visita visita = new Visita(paziente.getId(), null, 178, new BigDecimal("82.5"),
                new BigDecimal("95.0"), new BigDecimal("100.0"), null, new BigDecimal("32.0"),
                new BigDecimal("58.0"), new BigDecimal("38.0"), null, null, null, null, null,
                ProtocolloVita.OMS);
        visitaRepository.save(visita);

        List<Visita> visite = visitaRepository.findAllByPazienteId(paziente.getId());

        assertThat(visite).hasSize(1);
        assertThat(visite.get(0).getAltezzaCm()).isEqualTo(178);
        assertThat(visite.get(0).getPesoKg()).isEqualByComparingTo("82.5");
        assertThat(visite.get(0).getCirconferenzaFianchiCm()).isEqualByComparingTo("100.0");
        assertThat(visite.get(0).getCirconferenzaAddomeCm()).isNull();
        assertThat(visite.get(0).getCirconferenzaColloCm()).isNull();
        assertThat(visite.get(0).getProtocolloVita()).isEqualTo(ProtocolloVita.OMS);
        assertThat(visite.get(0).getDataVisita()).isNotNull();
    }

    @Test
    void findAllByPazienteIdOrderByDataVisitaAscRestituisceInOrdineCronologico() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("visite-ordine-prof@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "visite-ordine-luca@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null));

        Visita piuRecente = new Visita(paziente.getId(), LocalDate.of(2026, 8, 1), 178, new BigDecimal("77.5"),
                null, null, null, null, null, null, null, null, null, null, null, ProtocolloVita.OMS);
        Visita piuVecchia = new Visita(paziente.getId(), LocalDate.of(2026, 6, 1), 178, new BigDecimal("80.0"),
                null, null, null, null, null, null, null, null, null, null, null, ProtocolloVita.OMS);
        // Salvate in ordine inverso rispetto alla data, per verificare che sia la query a ordinare, non l'inserimento.
        visitaRepository.save(piuRecente);
        visitaRepository.save(piuVecchia);

        List<Visita> visite = visitaRepository.findAllByPazienteIdOrderByDataVisitaAsc(paziente.getId());

        assertThat(visite).hasSize(2);
        assertThat(visite.get(0).getDataVisita()).isEqualTo(LocalDate.of(2026, 6, 1));
        assertThat(visite.get(1).getDataVisita()).isEqualTo(LocalDate.of(2026, 8, 1));
    }
}
