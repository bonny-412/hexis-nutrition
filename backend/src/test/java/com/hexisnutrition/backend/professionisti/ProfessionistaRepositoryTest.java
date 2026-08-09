package com.hexisnutrition.backend.professionisti;

import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProfessionistaRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @AfterEach
    void pulisci() {
        professionistaRepository.deleteAll();
    }

    @Test
    void salvaERitrovaPerEmail() {
        Professionista professionista = new Professionista(
                "mario.rossi@example.com", "hash-fittizio", "Mario", "Rossi");

        professionistaRepository.save(professionista);

        Optional<Professionista> trovato = professionistaRepository.findByEmail("mario.rossi@example.com");

        assertThat(trovato).isPresent();
        assertThat(trovato.get().getNome()).isEqualTo("Mario");
        assertThat(trovato.get().getCognome()).isEqualTo("Rossi");
    }

    @Test
    void nonTrovaUnaEmailInesistente() {
        Optional<Professionista> trovato = professionistaRepository.findByEmail("non-esiste@example.com");

        assertThat(trovato).isEmpty();
    }
}
