package com.hexisnutrition.backend;

import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.PazienteRepository;
import com.hexisnutrition.backend.pazienti.Sesso;
import com.hexisnutrition.backend.professionisti.Professionista;
import com.hexisnutrition.backend.professionisti.ProfessionistaRepository;
import com.hexisnutrition.backend.support.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regressione del bug 17 del collaudo del 27/11/2026: i vincoli CHECK di V37 respingono le scritture dirette che
 * l'API già rifiuta. Le righe scritte qui passano da JdbcTemplate, senza entità né validazione.
 */
class VincoliDatabaseCollaudoTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ProfessionistaRepository professionistaRepository;
    @Autowired
    private PazienteRepository pazienteRepository;

    private UUID professionistaId;
    private UUID pazienteId;
    private final LocalDate oggi = LocalDate.now();

    @BeforeEach
    void creaProfessionistaEPaziente() {
        professionistaId = professionistaRepository.save(
                new Professionista("prof-vincoli@test.it", "$2a$10$hash", "Anna", "Rossi")).getId();
        pazienteId = pazienteRepository.save(new Paziente(professionistaId, "Mario", "Bianchi",
                "BNCMRA80A01H501U", "mario@test.it", null, LocalDate.of(1990, 1, 1), Sesso.M, null, null, null)).getId();
    }

    private void violaVincolo(String nomeVincolo, String sql, Object... parametri) {
        assertThatThrownBy(() -> jdbc.update(sql, parametri))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining(nomeVincolo);
    }

    private UUID inserisciPiano(LocalDate inizio, LocalDate fine) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO piani_alimentari (id, paziente_id, professionista_id, nome, modalita, data_inizio, data_fine) "
                + "VALUES (?, ?, ?, 'Piano', 'PASTI', ?, ?)", id, pazienteId, professionistaId, inizio, fine);
        return id;
    }

    @Test
    void quantitaDiRiferimentoDegliAlimentiDeveEsserePositiva() {
        for (String quantita : new String[]{"0", "-100"}) {
            violaVincolo("chk_alimenti_quantita_positiva",
                    "INSERT INTO alimenti (id, professionista_id, nome, categoria, quantita_g, kcal, proteine_g, grassi_g, carboidrati_g) "
                            + "VALUES (?, ?, 'Prova vincoli', 'Test', " + quantita + ", 0, 0, 0, 0)",
                    UUID.randomUUID(), professionistaId);
        }
    }

    @Test
    void pesoDellaVisitaDeveEsserePositivo() {
        violaVincolo("chk_visite_peso_positivo",
                "INSERT INTO visite (id, paziente_id, data_visita, altezza_cm, peso_kg) VALUES (?, ?, ?, 178, -10)",
                UUID.randomUUID(), pazienteId, oggi);
    }

    @Test
    void laFineDelPianoNonPuoPrecedereLInizioEObiettivoPositivo() {
        violaVincolo("chk_piani_alimentari_intervallo",
                "INSERT INTO piani_alimentari (id, paziente_id, professionista_id, nome, modalita, data_inizio, data_fine) "
                        + "VALUES (?, ?, ?, 'Piano', 'PASTI', ?, ?)",
                UUID.randomUUID(), pazienteId, professionistaId, oggi, oggi.minusDays(1));

        UUID piano = inserisciPiano(oggi, oggi);
        violaVincolo("chk_piani_alimentari_obiettivo_positivo",
                "UPDATE piani_alimentari SET obiettivo_kcal = -5 WHERE id = ?", piano);
    }

    @Test
    void laGrammaturaDiUnaRigaDelPianoDeveEsserePositiva() {
        UUID piano = inserisciPiano(oggi, null);
        UUID pasto = UUID.randomUUID();
        jdbc.update("INSERT INTO pasti (id, piano_id, giorno_settimana, nome, tipo, ordine) "
                + "VALUES (?, ?, 'LUNEDI', 'Colazione', 'COLAZIONE', 0)", pasto, piano);
        String riga = "INSERT INTO piano_alimento_righe (id, pasto_id, nome, kcal_100g, proteine_100g, carboidrati_100g, "
                + "grassi_100g, grammi, ordine) VALUES (?, ?, 'Avena', 372, 12.9, 65, 6.5, ?, 0)";

        violaVincolo("chk_piano_alimento_righe_grammi_positivi", riga, UUID.randomUUID(), pasto, -30);
        violaVincolo("chk_piano_alimento_righe_grammi_positivi", riga, UUID.randomUUID(), pasto, 0);

        // Caso valido di controllo: una riga da 60 g passa.
        jdbc.update(riga, UUID.randomUUID(), pasto, 60);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM piano_alimento_righe", Integer.class)).isEqualTo(1);
    }

    @Test
    void unBloccoNonPuoFinirePrimaDiIniziare() {
        String blocco = "INSERT INTO appuntamenti (id, professionista_id, tipo, data, data_fine, ora_inizio, ora_fine, giornata_intera) "
                + "VALUES (?, ?, 'BLOCCO', ?, ?, ?::time, ?::time, ?)";

        // Giornate intere con la fine prima dell'inizio.
        violaVincolo("chk_appuntamenti_intervallo_blocco", blocco,
                UUID.randomUUID(), professionistaId, oggi, oggi.minusDays(1), null, null, true);
        // Stesso giorno, orario invertito.
        violaVincolo("chk_appuntamenti_intervallo_blocco", blocco,
                UUID.randomUUID(), professionistaId, oggi, oggi, "12:00", "11:00", false);
        // Senza data di fine.
        violaVincolo("chk_appuntamenti_intervallo_blocco", blocco,
                UUID.randomUUID(), professionistaId, oggi, null, null, null, true);

        // Casi validi: su più giorni con la fine a un'ora più bassa dell'inizio, e una giornata intera.
        jdbc.update(blocco, UUID.randomUUID(), professionistaId, oggi, oggi.plusDays(1), "18:00", "09:00", false);
        jdbc.update(blocco, UUID.randomUUID(), professionistaId, oggi, oggi, null, null, true);
    }

    @Test
    void laDurataDiUnaVisitaStaTra5E480Minuti() {
        violaVincolo("chk_appuntamenti_durata",
                "INSERT INTO appuntamenti (id, professionista_id, paziente_id, tipo, stato, data, ora_inizio, durata_minuti) "
                        + "VALUES (?, ?, ?, 'VISITA_STUDIO', 'PROGRAMMATA', ?, '10:00', 481)",
                UUID.randomUUID(), professionistaId, pazienteId, oggi);
    }
}
