package com.hexisnutrition.backend.pianialimentari;

import com.hexisnutrition.backend.pazienti.ObiettivoVisita;
import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.Plicometria;
import com.hexisnutrition.backend.pazienti.Sesso;
import com.hexisnutrition.backend.pazienti.StileDiVita;
import com.hexisnutrition.backend.pazienti.Visita;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CalcolatoreTdeeTest {

    private final CalcolatoreTdee calcolatore = new CalcolatoreTdee();

    private Paziente paziente(LocalDate dataNascita, Sesso sesso, StileDiVita stileDiVita) {
        return new Paziente(UUID.randomUUID(), "Nome", "Cognome", "AAAAAA00A00A000A", "test@test.it", null,
                dataNascita, sesso, null, stileDiVita, null);
    }

    private Visita visita(LocalDate dataVisita, BigDecimal pesoKg, Integer altezzaCm, ObiettivoVisita obiettivo) {
        // 18 parametri posizionali: pazienteId, dataVisita, altezzaCm, pesoKg, le 11 circonferenze (null,
        // non servono per il calcolo TDEE), protocolloVita (null → default OMS), note (null), obiettivo.
        return new Visita(UUID.randomUUID(), dataVisita, altezzaCm, pesoKg,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, obiettivo);
    }

    @Test
    void esempioNumericoCompletoDellaSpecifica() {
        // Donna 35 anni, 68kg, 165cm, moderatamente attiva, Dimagrimento, nessuna plicometria.
        // NOTA: `specifica-tdee.docx` §7 riporta come risultato 1300.25/2015.4/1612.3 kcal, ma applicando
        // la sua stessa formula (§4.2/4.3) ai suoi stessi input si ottiene 1375.25/2131.64 — il documento
        // sorgente ha un errore aritmetico nel proprio esempio numerico (probabile refuso, i passaggi
        // successivi del documento sono comunque coerenti tra loro: 1300.25×1.55=2015.4 torna, ma
        // 10×68+6.25×165−5×35−161 non fa 1300.25). Qui si segue la formula (autorevole, non l'esempio),
        // scoperto e verificato in fase di implementazione — vedi ledger SDD Task 2.
        Paziente paziente = paziente(LocalDate.of(1991, 9, 5), Sesso.F, StileDiVita.ATTIVO);
        Visita visita = visita(LocalDate.of(2026, 9, 5), BigDecimal.valueOf(68), 165, ObiettivoVisita.DIMAGRIMENTO);

        Optional<SuggerimentoTdee> risultato = calcolatore.calcola(paziente, visita, null);

        assertThat(risultato).isPresent();
        assertThat(risultato.get().formulaUsata()).isEqualTo(FormulaBmr.MIFFLIN_ST_JEOR);
        // BMR = 10*68 + 6.25*165 - 5*35 - 161 = 680 + 1031.25 - 175 - 161 = 1375.25
        assertThat(risultato.get().bmr().doubleValue()).isCloseTo(1375.25, org.assertj.core.data.Offset.offset(0.01));
        // TDEE = 1375.25 * 1.55 = 2131.6375
        assertThat(risultato.get().tdee().doubleValue()).isCloseTo(2131.64, org.assertj.core.data.Offset.offset(0.5));
        // Aggiustamento sempre 0%: le calorie suggerite coincidono col TDEE puro (decisione di Andrea,
        // diversa dal -20% proposto nel documento originale per Dimagrimento).
        assertThat(risultato.get().calorieSuggerite()).isEqualByComparingTo(risultato.get().tdee());
        assertThat(risultato.get().sottoSogliaSicurezza()).isFalse();
    }

    @Test
    void usaKatchMcArdleQuandoLaMassaMagraEDisponibile() {
        Paziente paziente = paziente(LocalDate.of(1990, 1, 1), Sesso.M, StileDiVita.SEDENTARIO);
        Visita visita = visita(LocalDate.of(2026, 1, 1), BigDecimal.valueOf(80), 180, ObiettivoVisita.MANTENIMENTO);
        Plicometria plicometria = plicometriaConMassaMagra(BigDecimal.valueOf(65));

        Optional<SuggerimentoTdee> risultato = calcolatore.calcola(paziente, visita, plicometria);

        assertThat(risultato).isPresent();
        assertThat(risultato.get().formulaUsata()).isEqualTo(FormulaBmr.KATCH_MCARDLE);
        // BMR = 370 + 21.6 * 65 = 1774
        assertThat(risultato.get().bmr().doubleValue()).isCloseTo(1774.0, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void etaCalcolataSullaDataDellaVisitaNonSuOggi() {
        // Nato il 10 settembre 1990, visita il 1 settembre 2026 (9 giorni prima del compleanno, in cui
        // compirebbe 36 anni): ha ancora 35 anni.
        Paziente paziente = paziente(LocalDate.of(1990, 9, 10), Sesso.M, StileDiVita.SEDENTARIO);
        Visita visita = visita(LocalDate.of(2026, 9, 1), BigDecimal.valueOf(70), 175, ObiettivoVisita.MANTENIMENTO);

        Optional<SuggerimentoTdee> risultato = calcolatore.calcola(paziente, visita, null);

        // BMR Mifflin M = 10*70 + 6.25*175 - 5*35 + 5 = 700 + 1093.75 - 175 + 5 = 1623.75
        assertThat(risultato.get().bmr().doubleValue()).isCloseTo(1623.75, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void coefficienteSessoAltroEMediaTraMEF() {
        Paziente paziente = paziente(LocalDate.of(1990, 1, 1), Sesso.ALTRO, StileDiVita.SEDENTARIO);
        Visita visita = visita(LocalDate.of(2026, 1, 1), BigDecimal.valueOf(70), 175, ObiettivoVisita.MANTENIMENTO);

        Optional<SuggerimentoTdee> risultato = calcolatore.calcola(paziente, visita, null);

        // BMR = 10*70 + 6.25*175 - 5*36 - 78 = 700 + 1093.75 - 180 - 78 = 1535.75
        assertThat(risultato.get().bmr().doubleValue()).isCloseTo(1535.75, org.assertj.core.data.Offset.offset(0.01));
    }

    @Test
    void fattoreDiAttivitaDiDefaultQuandoStileDiVitaNonCompilato() {
        Paziente paziente = paziente(LocalDate.of(1990, 1, 1), Sesso.M, null);
        Visita visita = visita(LocalDate.of(2026, 1, 1), BigDecimal.valueOf(70), 175, ObiettivoVisita.MANTENIMENTO);

        Optional<SuggerimentoTdee> risultato = calcolatore.calcola(paziente, visita, null);

        BigDecimal bmrAtteso = BigDecimal.valueOf(10.0 * 70 + 6.25 * 175 - 5 * 36 + 5);
        BigDecimal tdeeAtteso = bmrAtteso.multiply(BigDecimal.valueOf(1.375));
        assertThat(risultato.get().tdee().doubleValue()).isCloseTo(tdeeAtteso.doubleValue(),
                org.assertj.core.data.Offset.offset(0.5));
    }

    @Test
    void nessunCalcoloPerPatologiaClinica() {
        Paziente paziente = paziente(LocalDate.of(1990, 1, 1), Sesso.M, StileDiVita.ATTIVO);
        Visita visita = visita(LocalDate.of(2026, 1, 1), BigDecimal.valueOf(70), 175, ObiettivoVisita.PATOLOGIA_CLINICA);

        assertThat(calcolatore.calcola(paziente, visita, null)).isEmpty();
    }

    @Test
    void nessunCalcoloPerGravidanzaAllattamento() {
        Paziente paziente = paziente(LocalDate.of(1990, 1, 1), Sesso.F, StileDiVita.ATTIVO);
        Visita visita = visita(LocalDate.of(2026, 1, 1), BigDecimal.valueOf(70), 165, ObiettivoVisita.GRAVIDANZA_ALLATTAMENTO);

        assertThat(calcolatore.calcola(paziente, visita, null)).isEmpty();
    }

    @Test
    void mostraIlTdeePuroPerObiettivoEducativo() {
        Paziente paziente = paziente(LocalDate.of(1990, 1, 1), Sesso.M, StileDiVita.ATTIVO);
        Visita visita = visita(LocalDate.of(2026, 1, 1), BigDecimal.valueOf(70), 175, ObiettivoVisita.EDUCATIVO);

        Optional<SuggerimentoTdee> risultato = calcolatore.calcola(paziente, visita, null);

        assertThat(risultato).isPresent();
        assertThat(risultato.get().calorieSuggerite()).isEqualByComparingTo(risultato.get().tdee());
    }

    @Test
    void segnalaSottoSogliaDiSicurezzaPerCalorieMoltoBasse() {
        // Donna molto minuta e sedentaria, 25 anni: BMR = 10*35 + 6.25*140 - 5*25 - 161 = 939,
        // TDEE = 939 * 1.2 = 1126.8, sotto la soglia di 1200 kcal (F).
        Paziente paziente = paziente(LocalDate.of(2001, 1, 1), Sesso.F, StileDiVita.SEDENTARIO);
        Visita visita = visita(LocalDate.of(2026, 1, 1), BigDecimal.valueOf(35), 140, ObiettivoVisita.DIMAGRIMENTO);

        Optional<SuggerimentoTdee> risultato = calcolatore.calcola(paziente, visita, null);

        assertThat(risultato.get().sottoSogliaSicurezza()).isTrue();
    }

    private Plicometria plicometriaConMassaMagra(BigDecimal massaMagraKg) {
        // 24 parametri posizionali: visitaId, protocollo, formulaVersione, etaAnni, coefficienteC,
        // coefficienteM, etniaAtleta, le 9 pliche (tutte null, non servono per questo test), sommaPlicheMm,
        // densitaCorporea, percentualeGrasso, massaGrassaKg, massaMagraKg, fmi, ffmi, limiteSicurezzaApplicato.
        return new Plicometria(UUID.randomUUID(), com.hexisnutrition.backend.pazienti.ProtocolloPlicometrico.JACKSON_POLLOCK_3,
                "v1", 30, null, null, null,
                null, null, null, null, null, null, null, null, null,
                BigDecimal.valueOf(10), BigDecimal.valueOf(1.06), BigDecimal.valueOf(15), BigDecimal.valueOf(10),
                massaMagraKg, BigDecimal.ONE, BigDecimal.ONE, false);
    }
}
