package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PazienteRepositoryTest extends AbstractIntegrationTest {

    @Autowired
    private PazienteRepository pazienteRepository;

    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @AfterEach
    void pulisci() {
        pazienteRepository.deleteAll();
        professionistaRepository.deleteAll();
    }

    @Test
    void salvaERitrovaPerProfessionista() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof@example.com", "hash", "Anna", "Bianchi"));

        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca.verdi@example.com", "3331234567", LocalDate.of(1990, 5, 20), Sesso.M, "Impiegato", StileDiVita.ATTIVO, null);
        pazienteRepository.save(paziente);

        List<Paziente> pazienti = pazienteRepository.findAllByProfessionistaId(professionista.getId());

        assertThat(pazienti).hasSize(1);
        assertThat(pazienti.get(0).getEmail()).isEqualTo("luca.verdi@example.com");
        assertThat(pazienti.get(0).getStatoAccount()).isEqualTo(StatoAccountPaziente.MAI_INVITATO);
    }

    @Test
    void trovaSoloIlPazienteAttivoPerEmail() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof2@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "attivo@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null);
        paziente.setStatoAccount(StatoAccountPaziente.ATTIVO);
        paziente.setPasswordHash("hash-paziente");
        pazienteRepository.save(paziente);

        Optional<Paziente> trovato = pazienteRepository.findByEmailAndStatoAccount(
                "attivo@example.com", StatoAccountPaziente.ATTIVO);
        assertThat(trovato).isPresent();

        boolean esisteNonAttivo = pazienteRepository.existsByEmailAndStatoAccount(
                "attivo@example.com", StatoAccountPaziente.INVITATO);
        assertThat(esisteNonAttivo).isFalse();
    }

    @Test
    void unNuovoPazienteNonEArchiviatoPerDefault() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-archivio@example.com", "hash", "Anna", "Bianchi"));
        Paziente paziente = new Paziente(professionista.getId(), "Luca", "Verdi",
                "RSSMRA80A01H501U", "luca.archivio@example.com", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null);
        pazienteRepository.save(paziente);

        Paziente ritrovato = pazienteRepository.findById(paziente.getId()).orElseThrow();
        assertThat(ritrovato.isArchiviato()).isFalse();
    }
}
