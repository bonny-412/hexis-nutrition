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
        @Min(5) @Max(480) Integer durataMinuti,
        @Size(max = 500) String testo,
        @Size(max = 500) @Pattern(regexp = "^https?://\\S+$") String linkVideo
) {
}
