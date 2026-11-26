package com.hexisnutrition.backend.pianialimentari;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PianoAlimentareRigaListaResponse(
        UUID id,
        String pazienteNomeCompleto,
        // Separati per le iniziali dell'avatar: dal nome completo non si distingue un nome doppio da un cognome composto.
        String pazienteNome,
        String pazienteCognome,
        // Sotto il nome nella lista piani, come nella lista pazienti. null se il paziente non esiste più.
        String pazienteEmail,
        String nome,
        StatoPianoVisualizzato stato,
        BigDecimal obiettivoKcal,
        ModalitaPiano modalita,
        LocalDate dataInizio,
        LocalDate dataFine
) {
}
