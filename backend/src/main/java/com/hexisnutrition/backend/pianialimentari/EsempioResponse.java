package com.hexisnutrition.backend.pianialimentari;

import java.util.List;
import java.util.UUID;

public record EsempioResponse(
        UUID id,
        TipoPasto tipoPasto,
        String nome,
        int ordine,
        List<RigaAlimentoResponse> righe
) {
    public static EsempioResponse da(PianoEsempio esempio, List<PianoAlimentoRiga> righe) {
        return new EsempioResponse(esempio.getId(), esempio.getTipoPasto(), esempio.getNome(), esempio.getOrdine(),
                righe.stream().map(RigaAlimentoResponse::da).toList());
    }
}
