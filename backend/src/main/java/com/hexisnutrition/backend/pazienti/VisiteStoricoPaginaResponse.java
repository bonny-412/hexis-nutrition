package com.hexisnutrition.backend.pazienti;

import java.util.List;

public record VisiteStoricoPaginaResponse(
        List<VisitaStoricoResponse> contenuto,
        int paginaCorrente,
        int dimensionePagina,
        long totaleElementi,
        int totalePagine
) {
}
