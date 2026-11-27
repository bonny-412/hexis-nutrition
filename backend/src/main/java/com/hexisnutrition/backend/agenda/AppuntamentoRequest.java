package com.hexisnutrition.backend.agenda;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record AppuntamentoRequest(
        @NotNull TipoAppuntamento tipo,
        UUID pazienteId,
        @NotNull LocalDate data,
        @JsonFormat(pattern = "HH:mm") LocalTime oraInizio,
        /** Un numero decimale (15.9) è rifiutato con 400 invece di essere troncato: vedi spring.jackson in application.yml (bug 15). */
        @Min(5) @Max(480) Integer durataMinuti,
        LocalDate dataFine,
        @JsonFormat(pattern = "HH:mm") LocalTime oraFine,
        Boolean giornataIntera,
        @Size(max = 500) String testo,
        /** http o https con un host vero: "https://?" è rifiutato (bug 14). Niente credenziali (@) nel link. */
        @Size(max = 500) @Pattern(regexp = "^https?://[^\\s/?#@:]+(:\\d{1,5})?([/?#]\\S*)?$",
                message = "Il link deve essere un indirizzo http o https completo") String linkVideo,
        Boolean notifica,
        Integer anticipoNotificaMinuti
) {
}
