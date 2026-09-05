package com.hexisnutrition.backend.alimenti;

import java.math.BigDecimal;
import java.util.UUID;

public record AlimentoResponse(
        UUID id,
        String nome,
        String categoria,
        BigDecimal kcal,
        BigDecimal proteineG,
        BigDecimal grassiG,
        BigDecimal carboidratiG,
        BigDecimal acquaG,
        BigDecimal fibreG,
        BigDecimal zuccheriG,
        BigDecimal ferroMg,
        BigDecimal calcioMg,
        BigDecimal sodioMg,
        boolean bda
) {
    public static AlimentoResponse da(Alimento alimento) {
        return new AlimentoResponse(alimento.getId(), alimento.getNome(), alimento.getCategoria(), alimento.getKcal(),
                alimento.getProteineG(), alimento.getGrassiG(), alimento.getCarboidratiG(), alimento.getAcquaG(),
                alimento.getFibreG(), alimento.getZuccheriG(), alimento.getFerroMg(), alimento.getCalcioMg(),
                alimento.getSodioMg(), alimento.isBda());
    }
}
