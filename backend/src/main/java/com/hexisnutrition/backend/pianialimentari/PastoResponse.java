package com.hexisnutrition.backend.pianialimentari;

import java.util.List;
import java.util.UUID;

public record PastoResponse(
        UUID id,
        GiornoSettimana giornoSettimana,
        String nome,
        TipoPasto tipo,
        String nota,
        int ordine,
        List<RigaAlimentoResponse> righe
) {
    public static PastoResponse da(Pasto pasto, List<PianoAlimentoRiga> righe) {
        return new PastoResponse(pasto.getId(), pasto.getGiornoSettimana(), pasto.getNome(), pasto.getTipo(),
                pasto.getNota(), pasto.getOrdine(), righe.stream().map(RigaAlimentoResponse::da).toList());
    }
}
