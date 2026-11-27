package com.hexisnutrition.backend.alimenti;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/** I massimi seguono le colonne di alimenti: NUMERIC(6,2) per quantità e grammi, NUMERIC(7,2) per le kcal, NUMERIC(8,2) per i milligrammi. */
public record CreaAlimentoRequest(
        @NotBlank @Size(max = 200) String nome,
        @NotBlank @Size(max = 200) String categoria,
        @NotNull @Positive @DecimalMax("9999.99") BigDecimal quantitaG,
        @NotNull @PositiveOrZero @DecimalMax("99999.99") BigDecimal kcal,
        @NotNull @PositiveOrZero @DecimalMax("9999.99") BigDecimal proteineG,
        @NotNull @PositiveOrZero @DecimalMax("9999.99") BigDecimal grassiG,
        @NotNull @PositiveOrZero @DecimalMax("9999.99") BigDecimal carboidratiG,
        @PositiveOrZero @DecimalMax("9999.99") BigDecimal acquaG,
        @PositiveOrZero @DecimalMax("9999.99") BigDecimal fibreG,
        @PositiveOrZero @DecimalMax("9999.99") BigDecimal zuccheriG,
        @PositiveOrZero @DecimalMax("999999.99") BigDecimal ferroMg,
        @PositiveOrZero @DecimalMax("999999.99") BigDecimal calcioMg,
        @PositiveOrZero @DecimalMax("999999.99") BigDecimal sodioMg
) {

    /**
     * Proteine, grassi e carboidrati insieme non possono pesare più della quantità di riferimento (bug 02 del collaudo).
     * Vale solo per gli alimenti personalizzati: alcuni alimenti BDA (zucchero, meringa) la superano di poco perché la
     * BDA esprime i carboidrati in monosaccaridi, per questo il vincolo non è anche un CHECK nel database.
     */
    @JsonIgnore
    @AssertTrue(message = "La somma di proteine, grassi e carboidrati non può superare la quantità di riferimento")
    public boolean isMacronutrientiEntroQuantita() {
        if (quantitaG == null || proteineG == null || grassiG == null || carboidratiG == null) {
            return true;
        }
        return proteineG.add(grassiG).add(carboidratiG).compareTo(quantitaG) <= 0;
    }
}
