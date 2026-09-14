package com.hexisnutrition.backend.pazienti;

import com.hexisnutrition.backend.pianialimentari.PianoAlimentare;

import java.time.LocalDate;
import java.util.UUID;

public record PazienteResponse(
        UUID id,
        String nome,
        String cognome,
        String codiceFiscale,
        String email,
        String telefono,
        LocalDate dataNascita,
        String sesso,
        String lavoro,
        String stileDiVita,
        String note,
        String statoAccount,
        boolean archiviato,
        String obiettivoUltimaVisita,
        LocalDate dataUltimaVisita,
        String pianoNome,
        String pianoStato,
        LocalDate pianoDataFine,
        LocalDate dataInizioObiettivo
) {
    public static PazienteResponse da(Paziente paziente) {
        return da(paziente, null, null);
    }

    public static PazienteResponse da(Paziente paziente, Visita ultimaVisita) {
        return da(paziente, ultimaVisita, null);
    }

    // ultimoPiano = piano più di recente creazione del paziente, qualunque sia il suo stato
    // (bozza/attivo/scaduto/terminato) — vedi PazienteService.ultimiPianiPerPazienti.
    public static PazienteResponse da(Paziente paziente, Visita ultimaVisita, PianoAlimentare ultimoPiano) {
        return da(paziente, ultimaVisita, ultimoPiano, null);
    }

    // dataInizioObiettivo = data della prima visita (risalendo dall'ultima) con lo stesso
    // obiettivo dell'ultima visita — vedi PazienteService.dataInizioObiettivoPerPazienti.
    public static PazienteResponse da(Paziente paziente, Visita ultimaVisita, PianoAlimentare ultimoPiano,
            LocalDate dataInizioObiettivo) {
        return new PazienteResponse(paziente.getId(), paziente.getNome(), paziente.getCognome(),
                paziente.getCodiceFiscale(), paziente.getEmail(), paziente.getTelefono(), paziente.getDataNascita(),
                paziente.getSesso().name(), paziente.getLavoro(),
                paziente.getStileDiVita() != null ? paziente.getStileDiVita().name() : null,
                paziente.getNote(), paziente.getStatoAccount().name(), paziente.isArchiviato(),
                ultimaVisita != null ? ultimaVisita.getObiettivo().name() : null,
                ultimaVisita != null ? ultimaVisita.getDataVisita() : null,
                ultimoPiano != null ? ultimoPiano.getNome() : null,
                ultimoPiano != null ? ultimoPiano.statoEffettivo().name() : null,
                ultimoPiano != null ? ultimoPiano.getDataFine() : null,
                dataInizioObiettivo);
    }
}
