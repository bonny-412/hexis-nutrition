package com.hexisnutrition.backend.notifiche;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.hexisnutrition.backend.agenda.TipoAppuntamento;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record NotificaResponse(
        UUID id,
        UUID appuntamentoId,
        TipoAppuntamento tipo,
        LocalDate data,
        @JsonFormat(pattern = "HH:mm") LocalTime oraInizio,
        int anticipoMinuti,
        String testo,
        String pazienteNome,
        String pazienteCognome,
        Instant scattaIl,
        boolean letta,
        boolean agendaVista
) {
}
