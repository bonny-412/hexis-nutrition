package com.hexisnutrition.backend.pazienti;

import java.math.BigDecimal;

/**
 * Una visita dello storico con i dati che dipendono dalla visita precedente (e quindi non
 * ricostruibili da una sola pagina): posizione dalla più recente e variazioni rispetto alla visita prima.
 */
public record VisitaStoricoResponse(
        VisitaResponse visita,
        int posizione,
        BigDecimal deltaPesoKg,
        BigDecimal deltaPercentualeGrasso
) {
}
