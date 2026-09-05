package com.hexisnutrition.backend.alimenti;

import org.springframework.data.domain.Page;

import java.util.List;

public record AlimentoListaPaginataResponse(
        List<AlimentoResponse> contenuto,
        int paginaCorrente,
        int dimensionePagina,
        long totaleElementi,
        int totalePagine
) {
    public static AlimentoListaPaginataResponse da(Page<Alimento> pagina) {
        return new AlimentoListaPaginataResponse(
                pagina.getContent().stream().map(AlimentoResponse::da).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
