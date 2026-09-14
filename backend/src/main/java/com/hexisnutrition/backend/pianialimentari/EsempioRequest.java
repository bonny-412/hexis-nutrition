package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record EsempioRequest(
        @NotNull TipoPasto tipoPasto,
        @NotBlank String nome,
        String nota,
        @Valid List<RigaAlimentoRequest> righe
) {
}
