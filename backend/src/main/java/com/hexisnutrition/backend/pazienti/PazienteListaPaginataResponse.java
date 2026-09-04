package com.hexisnutrition.backend.pazienti;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record PazienteListaPaginataResponse(
        List<PazienteResponse> contenuto,
        int paginaCorrente,
        int dimensionePagina,
        long totaleElementi,
        int totalePagine
) {
    public static PazienteListaPaginataResponse da(Page<Paziente> pagina, Map<UUID, Visita> ultimeVisitePerPaziente) {
        return new PazienteListaPaginataResponse(
                pagina.getContent().stream()
                        .map(paziente -> PazienteResponse.da(paziente, ultimeVisitePerPaziente.get(paziente.getId())))
                        .toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
