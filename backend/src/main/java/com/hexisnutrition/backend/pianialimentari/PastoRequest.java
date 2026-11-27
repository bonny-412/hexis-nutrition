package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PastoRequest(
        @NotNull GiornoSettimana giornoSettimana,
        @NotBlank @Size(max = 100) String nome,
        @NotNull TipoPasto tipo,
        @Size(max = 5000) String nota,
        @Valid List<RigaAlimentoRequest> righe
) {
}
