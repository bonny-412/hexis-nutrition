package com.hexisnutrition.backend.pazienti;

import org.springframework.data.domain.Page;

import java.util.List;

public record PazienteListaPaginataResponse(
        List<PazienteResponse> contenuto,
        int paginaCorrente,
        int dimensionePagina,
        long totaleElementi,
        int totalePagine
) {
    public static PazienteListaPaginataResponse da(Page<Paziente> pagina) {
        return new PazienteListaPaginataResponse(
                pagina.getContent().stream().map(PazienteResponse::da).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
