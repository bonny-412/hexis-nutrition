package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record PastoRequest(
        @NotNull GiornoSettimana giornoSettimana,
        @NotBlank String nome,
        @NotNull TipoPasto tipo,
        String nota,
        @Valid List<RigaAlimentoRequest> righe
) {
}
