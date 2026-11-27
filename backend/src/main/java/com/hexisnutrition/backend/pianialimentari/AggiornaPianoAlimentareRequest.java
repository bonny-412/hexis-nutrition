package com.hexisnutrition.backend.pianialimentari;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record AggiornaPianoAlimentareRequest(
        @NotBlank @Size(max = 200) String nome,
        /** Non può precedere la data di inizio del piano: controllato nel servizio, che la conosce. */
        LocalDate dataFine,
        /** Facoltativo, ma se presente deve essere positivo (bug 10 del collaudo). Colonna NUMERIC(7,2). */
        @Positive @DecimalMax("99999.99") BigDecimal obiettivoKcal,
        @Valid List<PastoRequest> pasti,
        @Valid List<GiornoMacroTargetRequest> giorniMacroTarget,
        @Valid List<EsempioRequest> esempi,
        /** Opzionale: se presente e diversa da quella attuale cambia la modalità, ammesso solo per piani in bozza. */
        ModalitaPiano modalita,
        /** Versione del piano letta dal client: se è cambiata nel frattempo (un'altra scheda ha salvato) il PUT risponde 409. Assente = nessun controllo. */
        Long versione
) {
}
