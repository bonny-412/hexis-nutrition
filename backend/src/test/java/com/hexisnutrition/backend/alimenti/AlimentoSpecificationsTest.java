package com.hexisnutrition.backend.alimenti;

import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AlimentoSpecificationsTest extends AbstractIntegrationTest {

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

    private Alimento creaAlimento(UUID professionistaId, String nome, String categoria) {
        Alimento salvato = alimentoRepository.save(new Alimento(professionistaId, nome, categoria,
                new BigDecimal("100.00"), new BigDecimal("100.00"), new BigDecimal("10.00"), new BigDecimal("5.00"),
                new BigDecimal("15.00"), null, null, null, null, null, null));
        if (professionistaId == null) {
            bdaCreatiInQuestoTest.add(salvato.getId());
        }
        return salvato;
    }

    @Test
    void visibilePerRestituisceLeRigheBdaEQuelleDelProfessionista() {
        UUID professionistaA = UUID.randomUUID();
        UUID professionistaB = UUID.randomUUID();
        Alimento bda = creaAlimento(null, "Alimento BDA di test spec1", "Categoria test");
        Alimento diA = creaAlimento(professionistaA, "Alimento di A spec1", "Personalizzato");
        creaAlimento(professionistaB, "Alimento di B spec1", "Personalizzato");

        List<Alimento> risultato = alimentoRepository.findAll(AlimentoSpecifications.visibilePer(professionistaA));

        assertThat(risultato).extracting(Alimento::getId).contains(bda.getId(), diA.getId());
        assertThat(risultato).extracting(Alimento::getNome).doesNotContain("Alimento di B spec1");
    }

    @Test
    void conRicercaTrovaPerNomeOCategoria() {
        UUID professionistaId = UUID.randomUUID();
        creaAlimento(professionistaId, "Petto di pollo spec2", "Carni bianche spec2");
        creaAlimento(professionistaId, "Salmone spec2", "Pesce spec2");

        List<Alimento> perNome = alimentoRepository.findAll(Specification.allOf(
                AlimentoSpecifications.visibilePer(professionistaId),
                AlimentoSpecifications.conRicerca("pollo spec2")));
        List<Alimento> perCategoria = alimentoRepository.findAll(Specification.allOf(
                AlimentoSpecifications.visibilePer(professionistaId),
                AlimentoSpecifications.conRicerca("pesce spec2")));

        assertThat(perNome).extracting(Alimento::getNome).containsExactly("Petto di pollo spec2");
        assertThat(perCategoria).extracting(Alimento::getNome).containsExactly("Salmone spec2");
    }

    @Test
    void soloBdaESoloPersonalizzatiFiltranoPerFonte() {
        UUID professionistaId = UUID.randomUUID();
        Alimento bda = creaAlimento(null, "Alimento BDA spec3", "Categoria test spec3");
        Alimento custom = creaAlimento(professionistaId, "Alimento custom spec3", "Categoria test spec3");

        List<Alimento> soloBda = alimentoRepository.findAll(Specification.allOf(
                AlimentoSpecifications.visibilePer(professionistaId),
                AlimentoSpecifications.conRicerca("spec3"),
                AlimentoSpecifications.soloBda()));
        List<Alimento> soloPersonalizzati = alimentoRepository.findAll(Specification.allOf(
                AlimentoSpecifications.visibilePer(professionistaId),
                AlimentoSpecifications.conRicerca("spec3"),
                AlimentoSpecifications.soloPersonalizzati()));

        assertThat(soloBda).extracting(Alimento::getId).containsExactly(bda.getId());
        assertThat(soloPersonalizzati).extracting(Alimento::getId).containsExactly(custom.getId());
    }
}
