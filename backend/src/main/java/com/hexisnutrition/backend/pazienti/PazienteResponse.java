package com.hexisnutrition.backend.pazienti;

import java.time.LocalDate;
import java.util.UUID;

public record PazienteResponse(
        UUID id,
        String nome,
        String cognome,
        String email,
        String telefono,
        LocalDate dataNascita,
        String sesso,
        String lavoro,
        String tipoLavoro,
        String statoAccount
) {
    public static PazienteResponse da(Paziente paziente) {
        return new PazienteResponse(paziente.getId(), paziente.getNome(), paziente.getCognome(),
                paziente.getEmail(), paziente.getTelefono(), paziente.getDataNascita(),
                paziente.getSesso().name(), paziente.getLavoro(),
                paziente.getTipoLavoro() != null ? paziente.getTipoLavoro().name() : null,
                paziente.getStatoAccount().name());
    }
}
