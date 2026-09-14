package com.hexisnutrition.backend.pianialimentari;

import java.math.BigDecimal;

public record GiornoMacroTargetResponse(
        GiornoSettimana giornoSettimana,
        BigDecimal kcalTarget,
        BigDecimal proteineTarget,
        BigDecimal carboidratiTarget,
        BigDecimal grassiTarget,
        String nota
) {
    public static GiornoMacroTargetResponse da(PianoGiornoMacroTarget target) {
        return new GiornoMacroTargetResponse(target.getGiornoSettimana(), target.getKcalTarget(),
                target.getProteineTarget(), target.getCarboidratiTarget(), target.getGrassiTarget(),
                target.getNota());
    }
}
