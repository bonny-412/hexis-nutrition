package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record CreaPianoAlimentareRequest(
        @NotNull UUID pazienteId,
        @NotBlank String nome,
        @NotNull ModalitaPiano modalita,
        LocalDate dataInizio
) {
}
