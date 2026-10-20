package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreaNotaPianoRequest(
        @NotNull LocalDate data,
        @NotBlank @Size(max = 5000) String testo
) {
}
