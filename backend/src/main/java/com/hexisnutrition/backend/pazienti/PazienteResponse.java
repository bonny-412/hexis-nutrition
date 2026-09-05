package com.hexisnutrition.backend.pazienti;

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
        LocalDate dataUltimaVisita
) {
    public static PazienteResponse da(Paziente paziente) {
        return da(paziente, null);
    }

    public static PazienteResponse da(Paziente paziente, Visita ultimaVisita) {
        return new PazienteResponse(paziente.getId(), paziente.getNome(), paziente.getCognome(),
                paziente.getCodiceFiscale(), paziente.getEmail(), paziente.getTelefono(), paziente.getDataNascita(),
                paziente.getSesso().name(), paziente.getLavoro(),
                paziente.getStileDiVita() != null ? paziente.getStileDiVita().name() : null,
                paziente.getNote(), paziente.getStatoAccount().name(), paziente.isArchiviato(),
                ultimaVisita != null ? ultimaVisita.getObiettivo().name() : null,
                ultimaVisita != null ? ultimaVisita.getDataVisita() : null);
    }
}
