package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record EsempioRequest(
        @NotNull TipoPasto tipoPasto,
        @NotBlank @Size(max = 100) String nome,
        @Size(max = 5000) String nota,
        @Valid List<RigaAlimentoRequest> righe
) {
}
