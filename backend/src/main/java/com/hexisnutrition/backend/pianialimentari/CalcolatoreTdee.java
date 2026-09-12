package com.hexisnutrition.backend.pianialimentari;

import com.hexisnutrition.backend.pazienti.ObiettivoVisita;
import com.hexisnutrition.backend.pazienti.Paziente;
import com.hexisnutrition.backend.pazienti.Plicometria;
import com.hexisnutrition.backend.pazienti.Sesso;
import com.hexisnutrition.backend.pazienti.StileDiVita;
import com.hexisnutrition.backend.pazienti.Visita;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Period;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Component
public class CalcolatoreTdee {

    private static final Set<ObiettivoVisita> OBIETTIVI_ESCLUSI =
            Set.of(ObiettivoVisita.PATOLOGIA_CLINICA, ObiettivoVisita.GRAVIDANZA_ALLATTAMENTO);

    private static final Map<StileDiVita, BigDecimal> FATTORI_ATTIVITA = new EnumMap<>(StileDiVita.class);
    static {
        FATTORI_ATTIVITA.put(StileDiVita.SEDENTARIO, BigDecimal.valueOf(1.2));
        FATTORI_ATTIVITA.put(StileDiVita.POCO_ATTIVO, BigDecimal.valueOf(1.375));
        FATTORI_ATTIVITA.put(StileDiVita.ATTIVO, BigDecimal.valueOf(1.55));
        FATTORI_ATTIVITA.put(StileDiVita.MOLTO_ATTIVO, BigDecimal.valueOf(1.725));
        FATTORI_ATTIVITA.put(StileDiVita.ESTREMAMENTE_ATTIVO, BigDecimal.valueOf(1.9));
    }
    private static final BigDecimal FATTORE_ATTIVITA_DEFAULT = BigDecimal.valueOf(1.375);

    private static final BigDecimal SOGLIA_MINIMA_F = BigDecimal.valueOf(1200);
    private static final BigDecimal SOGLIA_MINIMA_ALTRI = BigDecimal.valueOf(1500);

    public Optional<SuggerimentoTdee> calcola(Paziente paziente, Visita visita, Plicometria plicometriaOpzionale) {
        if (OBIETTIVI_ESCLUSI.contains(visita.getObiettivo())) {
            return Optional.empty();
        }

        int eta = Period.between(paziente.getDataNascita(), visita.getDataVisita()).getYears();

        BigDecimal bmr;
        FormulaBmr formulaUsata;
        if (plicometriaOpzionale != null && plicometriaOpzionale.getMassaMagraKg() != null) {
            // Katch-McArdle: BMR = 370 + 21.6 * massa magra (kg)
            bmr = BigDecimal.valueOf(370).add(
                    BigDecimal.valueOf(21.6).multiply(plicometriaOpzionale.getMassaMagraKg()));
            formulaUsata = FormulaBmr.KATCH_MCARDLE;
        } else {
            // Mifflin-St Jeor: BMR = 10*peso + 6.25*altezza - 5*eta + s
            BigDecimal base = BigDecimal.valueOf(10).multiply(visita.getPesoKg())
                    .add(BigDecimal.valueOf(6.25).multiply(BigDecimal.valueOf(visita.getAltezzaCm())))
                    .subtract(BigDecimal.valueOf(5L * eta));
            BigDecimal s = switch (paziente.getSesso()) {
                case M -> BigDecimal.valueOf(5);
                case F -> BigDecimal.valueOf(-161);
                case ALTRO -> BigDecimal.valueOf(-78);
            };
            bmr = base.add(s);
            formulaUsata = FormulaBmr.MIFFLIN_ST_JEOR;
        }

        BigDecimal fattoreAttivita = paziente.getStileDiVita() != null
                ? FATTORI_ATTIVITA.get(paziente.getStileDiVita())
                : FATTORE_ATTIVITA_DEFAULT;
        BigDecimal tdee = bmr.multiply(fattoreAttivita);

        // Aggiustamento sempre 0% per decisione di prodotto (vedi spec): le calorie suggerite
        // coincidono sempre col TDEE puro, per ogni obiettivo non escluso.
        BigDecimal calorieSuggerite = tdee.setScale(2, RoundingMode.HALF_UP);

        BigDecimal sogliaMinima = paziente.getSesso() == Sesso.F ? SOGLIA_MINIMA_F : SOGLIA_MINIMA_ALTRI;
        boolean sottoSoglia = calorieSuggerite.compareTo(sogliaMinima) < 0;

        return Optional.of(new SuggerimentoTdee(bmr.setScale(2, RoundingMode.HALF_UP),
                tdee.setScale(2, RoundingMode.HALF_UP), formulaUsata, calorieSuggerite, sottoSoglia));
    }
}
