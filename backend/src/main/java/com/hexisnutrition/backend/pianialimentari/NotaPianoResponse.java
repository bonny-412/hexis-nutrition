package com.hexisnutrition.backend.pianialimentari;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record NotaPianoResponse(
        UUID id,
        UUID pianoId,
        LocalDate data,
        String autore,
        String testo,
        Instant createdAt
) {
    static NotaPianoResponse da(PianoNota nota, String autore) {
        return new NotaPianoResponse(nota.getId(), nota.getPianoId(), nota.getData(), autore, nota.getTesto(),
                nota.getCreatoIl());
    }
}
