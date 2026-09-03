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
        String tipoLavoro,
        String note,
        String statoAccount,
        boolean archiviato
) {
    public static PazienteResponse da(Paziente paziente) {
        return new PazienteResponse(paziente.getId(), paziente.getNome(), paziente.getCognome(),
                paziente.getCodiceFiscale(), paziente.getEmail(), paziente.getTelefono(), paziente.getDataNascita(),
                paziente.getSesso().name(), paziente.getLavoro(),
                paziente.getTipoLavoro() != null ? paziente.getTipoLavoro().name() : null,
                paziente.getNote(), paziente.getStatoAccount().name(), paziente.isArchiviato());
    }
}
