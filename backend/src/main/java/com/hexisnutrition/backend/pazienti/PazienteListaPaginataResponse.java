package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.pianialimentari.PianoAlimentare;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
    public static PazienteListaPaginataResponse da(Page<Paziente> pagina, Map<UUID, Visita> ultimeVisitePerPaziente,
            Map<UUID, PianoAlimentare> ultimiPianiPerPaziente, Map<UUID, LocalDate> dataInizioObiettivoPerPaziente,
            Map<UUID, LocalDateTime> prossimaVisitaPerPaziente) {
        return new PazienteListaPaginataResponse(
                pagina.getContent().stream()
                        .map(paziente -> PazienteResponse.da(paziente, ultimeVisitePerPaziente.get(paziente.getId()),
                                ultimiPianiPerPaziente.get(paziente.getId()),
                                dataInizioObiettivoPerPaziente.get(paziente.getId()),
                                prossimaVisitaPerPaziente.get(paziente.getId())))
                        .toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
