package com.hexisnutrition.backend.alimenti;

import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AlimentoRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private AlimentoRepository alimentoRepository;

    // Righe BDA-shaped (professionista_id nullo) create da un test: pulite in @AfterEach
    // anche se un'asserzione del test fallisce, per non lasciare righe fasulle permanenti
    // nel database di test condiviso (alimenti è escluso dal TRUNCATE globale).
    private final List<UUID> bdaCreatiInQuestoTest = new ArrayList<>();

    @AfterEach
    void pulisci() {
        alimentoRepository.findAll().stream()
                .filter(a -> a.getProfessionistaId() != null)
                .forEach(alimentoRepository::delete);
        bdaCreatiInQuestoTest.forEach(id -> alimentoRepository.findById(id).ifPresent(alimentoRepository::delete));
        bdaCreatiInQuestoTest.clear();
    }

    @Test
    void unAlimentoCustomHaProfessionistaIdValorizzatoENonEBda() {
        UUID professionistaId = UUID.randomUUID();
        Alimento alimento = new Alimento(professionistaId, "Petto di pollo fatto in casa", "Personalizzato",
                new BigDecimal("165.00"), new BigDecimal("31.00"), new BigDecimal("3.60"), new BigDecimal("0.00"),
                null, null, null, null, null, null);

        Alimento salvato = alimentoRepository.save(alimento);
        Alimento ritrovato = alimentoRepository.findById(salvato.getId()).orElseThrow();

        assertThat(ritrovato.getProfessionistaId()).isEqualTo(professionistaId);
        assertThat(ritrovato.isBda()).isFalse();
        assertThat(ritrovato.getNome()).isEqualTo("Petto di pollo fatto in casa");
        assertThat(ritrovato.getKcal()).isEqualByComparingTo("165.00");
        assertThat(ritrovato.getAcquaG()).isNull();
    }

    @Test
    void unAlimentoSenzaProfessionistaIdEBda() {
        Alimento alimento = new Alimento(null, "Alimento di test BDA", "Categoria test",
                new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("5.00"), new BigDecimal("15.00"),
                new BigDecimal("60.00"), new BigDecimal("2.00"), new BigDecimal("3.00"),
                new BigDecimal("1.50"), new BigDecimal("20.00"), new BigDecimal("50.00"));

        Alimento salvato = alimentoRepository.save(alimento);
        bdaCreatiInQuestoTest.add(salvato.getId());
        Alimento ritrovato = alimentoRepository.findById(salvato.getId()).orElseThrow();

        assertThat(ritrovato.getProfessionistaId()).isNull();
        assertThat(ritrovato.isBda()).isTrue();
        assertThat(ritrovato.getFerroMg()).isEqualByComparingTo("1.50");
    }

    @Test
    void laMigrazioneDiSeedPopolaMilleCentoNoveAlimentiBda() {
        long conteggioBda = alimentoRepository.findAll().stream().filter(Alimento::isBda).count();
        assertThat(conteggioBda).isEqualTo(1109);
    }

    @Test
    void lAlimentoAcquaESeminatoConICampiCorretti() {
        Alimento acqua = alimentoRepository.findAll().stream()
                .filter(a -> a.getCodiceBda() != null && a.getCodiceBda() == 999999)
                .findFirst()
                .orElseThrow();

        assertThat(acqua.getNome()).isEqualTo("ACQUA");
        assertThat(acqua.getCategoria()).isEqualTo("acqua");
        assertThat(acqua.getKcal()).isEqualByComparingTo("0.00");
        assertThat(acqua.getAcquaG()).isEqualByComparingTo("99.90");
        assertThat(acqua.getCalcioMg()).isEqualByComparingTo("10.00");
        assertThat(acqua.getSodioMg()).isEqualByComparingTo("2.00");
        assertThat(acqua.isBda()).isTrue();
    }
}
