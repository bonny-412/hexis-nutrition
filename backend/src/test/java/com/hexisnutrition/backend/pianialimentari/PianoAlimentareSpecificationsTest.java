package com.hexisnutrition.backend.pianialimentari;

import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.Sesso;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PianoAlimentareSpecificationsTest extends AbstractIntegrationTest {

    @Autowired
    private PianoAlimentareRepository pianoAlimentareRepository;
    @Autowired
    private PazienteRepository pazienteRepository;
    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @Test
    void conStatoScadutoTrovaSoloAttiviConDataFinePassata() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof@test.it", "hash", "Anna", "Rossi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Mario", "Bianchi",
                "BNCMRA80A01H501U", "mario@test.it", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null));

        PianoAlimentare scaduto = new PianoAlimentare(paziente.getId(), professionista.getId(), null, "Scaduto",
                ModalitaPiano.MACRO);
        scaduto.setStato(StatoPiano.ATTIVO);
        scaduto.setDataFine(LocalDate.now().minusDays(1));
        pianoAlimentareRepository.save(scaduto);

        PianoAlimentare ancoraAttivo = new PianoAlimentare(paziente.getId(), professionista.getId(), null, "Attivo",
                ModalitaPiano.MACRO);
        ancoraAttivo.setStato(StatoPiano.ATTIVO);
        ancoraAttivo.setDataFine(LocalDate.now().plusDays(10));
        pianoAlimentareRepository.save(ancoraAttivo);

        var risultato = pianoAlimentareRepository.findAll(
                Specification.allOf(
                        PianoAlimentareSpecifications.delProfessionista(professionista.getId()),
                        PianoAlimentareSpecifications.conStato(StatoPianoVisualizzato.SCADUTO)),
                PageRequest.of(0, 10));

        assertThat(risultato.getContent()).extracting("nome").containsExactly("Scaduto");

        // Lato opposto, stessa coppia di fixture: conStato(ATTIVO) deve restituire solo il piano
        // ancora attivo ed escludere quello scaduto (che è ATTIVO in DB ma con data fine passata).
        var risultatoAttivo = pianoAlimentareRepository.findAll(
                Specification.allOf(
                        PianoAlimentareSpecifications.delProfessionista(professionista.getId()),
                        PianoAlimentareSpecifications.conStato(StatoPianoVisualizzato.ATTIVO)),
                PageRequest.of(0, 10));

        assertThat(risultatoAttivo.getContent()).extracting("nome").containsExactly("Attivo");
    }

    @Test
    void conRicercaTrovaAncheSulNomeDelPaziente() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof2@test.it", "hash", "Anna", "Rossi"));
        Paziente paziente = pazienteRepository.save(new Paziente(professionista.getId(), "Giulia", "Verdi",
                "VRDGLI80A01H501U", "giulia@test.it", null, LocalDate.of(1990, 1, 1), Sesso.F, null, null, null));
        pianoAlimentareRepository.save(new PianoAlimentare(paziente.getId(), professionista.getId(), null,
                "Piano X", ModalitaPiano.MACRO));

        var risultato = pianoAlimentareRepository.findAll(
                Specification.allOf(
                        PianoAlimentareSpecifications.delProfessionista(professionista.getId()),
                        PianoAlimentareSpecifications.conRicerca("giulia")),
                PageRequest.of(0, 10));

        assertThat(risultato.getContent()).hasSize(1);
    }
}
