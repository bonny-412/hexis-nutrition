package com.hexisnutrition.backend.agenda;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

/** La parte di un blocco (anche multi-giorno) che cade in un singolo giorno, come intervallo [inizio, fine). */
public record TrattoBlocco(LocalTime inizio, LocalTime fine) {

    public static Optional<TrattoBlocco> nelGiorno(Appuntamento blocco, LocalDate giorno) {
        if (blocco.getData() == null || blocco.getDataFine() == null
                || giorno.isBefore(blocco.getData()) || giorno.isAfter(blocco.getDataFine())) {
            return Optional.empty();
        }
        LocalTime inizio = AgendaTempo.INIZIO_GRIGLIA;
        LocalTime fine = AgendaTempo.FINE_GRIGLIA;
        if (!blocco.isGiornataIntera()) {
            if (giorno.equals(blocco.getData()) && blocco.getOraInizio() != null) {
                inizio = blocco.getOraInizio();
            }
            if (giorno.equals(blocco.getDataFine()) && blocco.getOraFine() != null) {
                fine = blocco.getOraFine();
            }
        }
        if (!inizio.isBefore(fine)) {
            return Optional.empty();
        }
        return Optional.of(new TrattoBlocco(inizio, fine));
    }

    public boolean siSovrappone(LocalTime da, LocalTime a) {
        return inizio.isBefore(a) && da.isBefore(fine);
    }
}
