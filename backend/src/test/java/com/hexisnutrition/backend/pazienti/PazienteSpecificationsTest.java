package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PazienteSpecificationsTest extends AbstractIntegrationTest {

    @Autowired
    private PazienteRepository pazienteRepository;

    @Autowired
    private VisitaRepository visitaRepository;

    @Autowired
    private ProfessionistaRepository professionistaRepository;

    @AfterEach
    void pulisci() {
        visitaRepository.deleteAll();
        pazienteRepository.deleteAll();
        professionistaRepository.deleteAll();
    }

    private Visita creaVisita(UUID pazienteId, LocalDate dataVisita, ObiettivoVisita obiettivo) {
        Visita visita = new Visita(pazienteId, dataVisita, 178, new java.math.BigDecimal("75.0"),
                null, null, null, null, null, null, null, null, null, null, null,
                ProtocolloVita.OMS, null, obiettivo);
        return visitaRepository.save(visita);
    }

    private Paziente creaPaziente(UUID professionistaId, String nome, String cognome, String email,
                                   String codiceFiscale, LocalDate dataNascita, Sesso sesso) {
        Paziente paziente = new Paziente(professionistaId, nome, cognome, codiceFiscale, email,
                null, dataNascita, sesso, null, null, null);
        return pazienteRepository.save(paziente);
    }

    @Test
    void conRicercaTrovaPerNomeCognomeEmailOCodiceFiscale() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-spec1@example.com", "hash", "Anna", "Bianchi"));
        creaPaziente(professionista.getId(), "Marco", "Rossi", "marco.rossi@example.com",
                "RSSMRC80A01H501U", LocalDate.of(1990, 1, 1), Sesso.M);
        creaPaziente(professionista.getId(), "Giulia", "Verdi", "giulia.verdi@example.com",
                "VRDGLI85A41H501U", LocalDate.of(1985, 3, 10), Sesso.F);

        Specification<Paziente> spec = Specification.allOf(
                PazienteSpecifications.delProfessionista(professionista.getId()),
                PazienteSpecifications.conArchiviato(false),
                PazienteSpecifications.conRicerca("giulia"));

        List<Paziente> risultato = pazienteRepository.findAll(spec);

        assertThat(risultato).hasSize(1);
        assertThat(risultato.get(0).getNome()).isEqualTo("Giulia");
    }

    @Test
    void conStatoAccountFiltraPerStato() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-spec2@example.com", "hash", "Anna", "Bianchi"));
        Paziente attivo = creaPaziente(professionista.getId(), "Marco", "Rossi", "marco-attivo@example.com",
                "RSSMRC80A01H501U", LocalDate.of(1990, 1, 1), Sesso.M);
        attivo.setStatoAccount(StatoAccountPaziente.ATTIVO);
        pazienteRepository.save(attivo);
        creaPaziente(professionista.getId(), "Giulia", "Verdi", "giulia-non-invitata@example.com",
                "VRDGLI85A41H501U", LocalDate.of(1985, 3, 10), Sesso.F);

        Specification<Paziente> spec = Specification.allOf(
                PazienteSpecifications.delProfessionista(professionista.getId()),
                PazienteSpecifications.conArchiviato(false),
                PazienteSpecifications.conStatoAccount(StatoAccountPaziente.ATTIVO));

        List<Paziente> risultato = pazienteRepository.findAll(spec);

        assertThat(risultato).hasSize(1);
        assertThat(risultato.get(0).getEmail()).isEqualTo("marco-attivo@example.com");
    }

    @Test
    void conObiettivoUltimaVisitaFiltraPerObiettivoDellaVisitaPiuRecente() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-spec3@example.com", "hash", "Anna", "Bianchi"));
        Paziente ipertrofia = creaPaziente(professionista.getId(), "Marco", "Rossi", "marco-m@example.com",
                "RSSMRC80A01H501U", LocalDate.of(1990, 1, 1), Sesso.M);
        // Prima visita "Dimagrimento", poi passato a "Ipertrofia": deve contare solo l'ultima.
        creaVisita(ipertrofia.getId(), LocalDate.of(2026, 1, 1), ObiettivoVisita.DIMAGRIMENTO);
        creaVisita(ipertrofia.getId(), LocalDate.of(2026, 6, 1), ObiettivoVisita.IPERTROFIA);
        Paziente dimagrimento = creaPaziente(professionista.getId(), "Giulia", "Verdi", "giulia-f@example.com",
                "VRDGLI85A41H501U", LocalDate.of(1985, 3, 10), Sesso.F);
        creaVisita(dimagrimento.getId(), LocalDate.of(2026, 1, 1), ObiettivoVisita.DIMAGRIMENTO);
        // Nessuna visita: non deve mai comparire in nessun filtro per obiettivo.
        creaPaziente(professionista.getId(), "Anna", "Neri", "anna-senza-visite@example.com",
                "NRIANN90A01H501U", LocalDate.of(1990, 1, 1), Sesso.F);

        Specification<Paziente> spec = Specification.allOf(
                PazienteSpecifications.delProfessionista(professionista.getId()),
                PazienteSpecifications.conArchiviato(false),
                PazienteSpecifications.conObiettivoUltimaVisita(ObiettivoVisita.IPERTROFIA));

        List<Paziente> risultato = pazienteRepository.findAll(spec);

        assertThat(risultato).extracting(Paziente::getEmail).containsExactly("marco-m@example.com");
    }

    @Test
    void conDataUltimaVisitaTraFiltraPerIntervallo() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-spec4@example.com", "hash", "Anna", "Bianchi"));
        Paziente vecchia = creaPaziente(professionista.getId(), "Marco", "Rossi", "marco-1970@example.com",
                "RSSMRC70A01H501U", LocalDate.of(1970, 1, 1), Sesso.M);
        creaVisita(vecchia.getId(), LocalDate.of(1970, 1, 1), ObiettivoVisita.MANTENIMENTO);
        Paziente recente = creaPaziente(professionista.getId(), "Giulia", "Verdi", "giulia-1995@example.com",
                "VRDGLI95A41H501U", LocalDate.of(1995, 3, 10), Sesso.F);
        creaVisita(recente.getId(), LocalDate.of(1995, 3, 10), ObiettivoVisita.MANTENIMENTO);

        Specification<Paziente> spec = Specification.allOf(
                PazienteSpecifications.delProfessionista(professionista.getId()),
                PazienteSpecifications.conArchiviato(false),
                PazienteSpecifications.conDataUltimaVisitaTra(LocalDate.of(1990, 1, 1), LocalDate.of(2000, 1, 1)));

        List<Paziente> risultato = pazienteRepository.findAll(spec);

        assertThat(risultato).hasSize(1);
        assertThat(risultato.get(0).getEmail()).isEqualTo("giulia-1995@example.com");
    }

    @Test
    void conDataUltimaVisitaTraSoloMinoroFiltraPerDataDa() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-spec4b@example.com", "hash", "Anna", "Bianchi"));
        Paziente vecchia = creaPaziente(professionista.getId(), "Marco", "Rossi", "marco-1970@example.com",
                "RSSMRC70A01H501U", LocalDate.of(1970, 1, 1), Sesso.M);
        creaVisita(vecchia.getId(), LocalDate.of(1970, 1, 1), ObiettivoVisita.MANTENIMENTO);
        Paziente recente = creaPaziente(professionista.getId(), "Giulia", "Verdi", "giulia-1995@example.com",
                "VRDGLI95A41H501U", LocalDate.of(1995, 3, 10), Sesso.F);
        creaVisita(recente.getId(), LocalDate.of(1995, 3, 10), ObiettivoVisita.MANTENIMENTO);

        Specification<Paziente> spec = Specification.allOf(
                PazienteSpecifications.delProfessionista(professionista.getId()),
                PazienteSpecifications.conArchiviato(false),
                PazienteSpecifications.conDataUltimaVisitaTra(LocalDate.of(1990, 1, 1), null));

        List<Paziente> risultato = pazienteRepository.findAll(spec);

        assertThat(risultato).hasSize(1);
        assertThat(risultato.get(0).getEmail()).isEqualTo("giulia-1995@example.com");
    }

    @Test
    void conDataUltimaVisitaTraSoloMassimoFiltraPerDataA() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-spec4c@example.com", "hash", "Anna", "Bianchi"));
        Paziente vecchia = creaPaziente(professionista.getId(), "Marco", "Rossi", "marco-1970@example.com",
                "RSSMRC70A01H501U", LocalDate.of(1970, 1, 1), Sesso.M);
        creaVisita(vecchia.getId(), LocalDate.of(1970, 1, 1), ObiettivoVisita.MANTENIMENTO);
        Paziente recente = creaPaziente(professionista.getId(), "Giulia", "Verdi", "giulia-1995@example.com",
                "VRDGLI95A41H501U", LocalDate.of(1995, 3, 10), Sesso.F);
        creaVisita(recente.getId(), LocalDate.of(1995, 3, 10), ObiettivoVisita.MANTENIMENTO);

        Specification<Paziente> spec = Specification.allOf(
                PazienteSpecifications.delProfessionista(professionista.getId()),
                PazienteSpecifications.conArchiviato(false),
                PazienteSpecifications.conDataUltimaVisitaTra(null, LocalDate.of(1980, 1, 1)));

        List<Paziente> risultato = pazienteRepository.findAll(spec);

        assertThat(risultato).hasSize(1);
        assertThat(risultato.get(0).getEmail()).isEqualTo("marco-1970@example.com");
    }

    @Test
    void conArchiviatoEscludeGliArchiviatiPerDefaultEIsolaGliArchiviati() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-spec5@example.com", "hash", "Anna", "Bianchi"));
        Paziente archiviato = creaPaziente(professionista.getId(), "Marco", "Rossi", "marco-archiviato@example.com",
                "RSSMRC80A01H501U", LocalDate.of(1990, 1, 1), Sesso.M);
        archiviato.setArchiviato(true);
        pazienteRepository.save(archiviato);
        creaPaziente(professionista.getId(), "Giulia", "Verdi", "giulia-attiva@example.com",
                "VRDGLI85A41H501U", LocalDate.of(1985, 3, 10), Sesso.F);

        List<Paziente> attivi = pazienteRepository.findAll(Specification.allOf(
                PazienteSpecifications.delProfessionista(professionista.getId()),
                PazienteSpecifications.conArchiviato(false)));
        List<Paziente> archiviati = pazienteRepository.findAll(Specification.allOf(
                PazienteSpecifications.delProfessionista(professionista.getId()),
                PazienteSpecifications.conArchiviato(true)));

        assertThat(attivi).extracting(Paziente::getEmail).containsExactly("giulia-attiva@example.com");
        assertThat(archiviati).extracting(Paziente::getEmail).containsExactly("marco-archiviato@example.com");
    }

    @Test
    void combinaPiuFiltriConAnd() {
        Professionista professionista = professionistaRepository.save(
                new Professionista("prof-spec6@example.com", "hash", "Anna", "Bianchi"));
        Paziente match = creaPaziente(professionista.getId(), "Marco", "Rossi", "marco-combo@example.com",
                "RSSMRC90A01H501U", LocalDate.of(1990, 1, 1), Sesso.M);
        match.setStatoAccount(StatoAccountPaziente.ATTIVO);
        pazienteRepository.save(match);
        creaPaziente(professionista.getId(), "Marco", "Bianchi", "marco-non-attivo@example.com",
                "BNCMRC90A01H501U", LocalDate.of(1990, 1, 1), Sesso.M);

        Specification<Paziente> spec = Specification.allOf(
                PazienteSpecifications.delProfessionista(professionista.getId()),
                PazienteSpecifications.conArchiviato(false),
                PazienteSpecifications.conRicerca("marco"),
                PazienteSpecifications.conStatoAccount(StatoAccountPaziente.ATTIVO));

        List<Paziente> risultato = pazienteRepository.findAll(spec);

        assertThat(risultato).hasSize(1);
        assertThat(risultato.get(0).getEmail()).isEqualTo("marco-combo@example.com");
    }

    @Test
    void delProfessionistaIsolaIPazientiPerProfessionista() {
        Professionista professionistaA = professionistaRepository.save(
                new Professionista("prof-spec7a@example.com", "hash", "A", "A"));
        Professionista professionistaB = professionistaRepository.save(
                new Professionista("prof-spec7b@example.com", "hash", "B", "B"));
        creaPaziente(professionistaA.getId(), "Marco", "Rossi", "marco-a@example.com",
                "RSSMRC90A01H501U", LocalDate.of(1990, 1, 1), Sesso.M);
        creaPaziente(professionistaB.getId(), "Giulia", "Verdi", "giulia-b@example.com",
                "VRDGLI85A41H501U", LocalDate.of(1985, 3, 10), Sesso.F);

        List<Paziente> risultato = pazienteRepository.findAll(Specification.allOf(
                PazienteSpecifications.delProfessionista(professionistaA.getId()),
                PazienteSpecifications.conArchiviato(false)));

        assertThat(risultato).extracting(Paziente::getEmail).containsExactly("marco-a@example.com");
    }
}
