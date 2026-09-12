package com.hexisnutrition.backend.pianialimentari;

import org.springframework.data.domain.Page;

import java.util.List;

public record PianoAlimentareListaPaginataResponse(
        List<PianoAlimentareRigaListaResponse> contenuto,
        int paginaCorrente,
        int dimensionePagina,
        long totaleElementi,
        int totalePagine
) {
}
