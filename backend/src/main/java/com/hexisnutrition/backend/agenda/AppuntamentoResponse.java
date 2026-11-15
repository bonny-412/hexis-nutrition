package com.hexisnutrition.backend.agenda;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hexisnutrition.backend.pazienti.Paziente;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppuntamentoResponse(
        UUID id,
        String tipo,
        String stato,
        LocalDate data,
        @JsonFormat(pattern = "HH:mm") LocalTime oraInizio,
        Integer durataMinuti,
        UUID pazienteId,
        String pazienteNome,
        String pazienteCognome,
        String testo,
        String linkVideo,
        boolean fatto,
        UUID visitaId
) {

    public static AppuntamentoResponse da(Appuntamento a, Paziente pazienteOrNull) {
        return new AppuntamentoResponse(
                a.getId(),
                a.getTipo().name(),
                a.getStato() == null ? null : a.getStato().name(),
                a.getData(),
                a.getOraInizio(),
                a.getDurataMinuti(),
                a.getPazienteId(),
                pazienteOrNull == null ? null : pazienteOrNull.getNome(),
                pazienteOrNull == null ? null : pazienteOrNull.getCognome(),
                a.getTesto(),
                a.getLinkVideo(),
                a.isFatto(),
                a.getVisitaId());
    }
}
