package com.hexisnutrition.backend.pianialimentari;

import java.math.BigDecimal;
import java.util.UUID;

public record RigaAlimentoResponse(
        UUID id,
        UUID alimentoId,
        String nome,
        BigDecimal kcal100g,
        BigDecimal proteine100g,
        BigDecimal carboidrati100g,
        BigDecimal grassi100g,
        BigDecimal zuccheri100g,
        BigDecimal fibre100g,
        BigDecimal ferro100mg,
        BigDecimal calcio100mg,
        BigDecimal acqua100g,
        BigDecimal grammi
) {
    public static RigaAlimentoResponse da(PianoAlimentoRiga riga) {
        return new RigaAlimentoResponse(riga.getId(), riga.getAlimentoId(), riga.getNome(), riga.getKcal100g(),
                riga.getProteine100g(), riga.getCarboidrati100g(), riga.getGrassi100g(), riga.getZuccheri100g(),
                riga.getFibre100g(), riga.getFerro100mg(), riga.getCalcio100mg(), riga.getAcqua100g(),
                riga.getGrammi());
    }
}
