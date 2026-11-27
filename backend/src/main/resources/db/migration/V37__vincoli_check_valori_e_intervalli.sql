-- Bug 17 del collaudo (27/11/2026): le invarianti già controllate dall'API valgono anche per le scritture dirette.
-- Prima di questa migrazione i dati di hexis sono stati controllati: nessuna riga viola questi vincoli.
-- La somma dei macronutrienti entro la quantità (bug 02) resta solo nell'API: alcuni alimenti BDA la superano.

ALTER TABLE alimenti
    ADD CONSTRAINT chk_alimenti_quantita_positiva CHECK (quantita_g > 0),
    ADD CONSTRAINT chk_alimenti_valori_non_negativi CHECK (
        kcal >= 0 AND proteine_g >= 0 AND grassi_g >= 0 AND carboidrati_g >= 0
        AND (acqua_g IS NULL OR acqua_g >= 0)
        AND (fibre_g IS NULL OR fibre_g >= 0)
        AND (zuccheri_g IS NULL OR zuccheri_g >= 0)
        AND (ferro_mg IS NULL OR ferro_mg >= 0)
        AND (calcio_mg IS NULL OR calcio_mg >= 0)
        AND (sodio_mg IS NULL OR sodio_mg >= 0)
    );

ALTER TABLE visite
    ADD CONSTRAINT chk_visite_peso_positivo CHECK (peso_kg > 0),
    ADD CONSTRAINT chk_visite_altezza_positiva CHECK (altezza_cm > 0);

ALTER TABLE piani_alimentari
    ADD CONSTRAINT chk_piani_alimentari_intervallo CHECK (data_fine IS NULL OR data_fine >= data_inizio),
    ADD CONSTRAINT chk_piani_alimentari_obiettivo_positivo CHECK (obiettivo_kcal IS NULL OR obiettivo_kcal > 0);

ALTER TABLE piano_alimento_righe
    ADD CONSTRAINT chk_piano_alimento_righe_grammi_positivi CHECK (grammi > 0),
    ADD CONSTRAINT chk_piano_alimento_righe_valori_non_negativi CHECK (
        kcal_100g >= 0 AND proteine_100g >= 0 AND carboidrati_100g >= 0 AND grassi_100g >= 0
        AND (zuccheri_100g IS NULL OR zuccheri_100g >= 0)
        AND (fibre_100g IS NULL OR fibre_100g >= 0)
        AND (ferro_100mg IS NULL OR ferro_100mg >= 0)
        AND (calcio_100mg IS NULL OR calcio_100mg >= 0)
        AND (acqua_100g IS NULL OR acqua_100g >= 0)
    );

ALTER TABLE piano_giorno_macro_target
    ADD CONSTRAINT chk_piano_giorno_macro_target_non_negativi CHECK (
        (kcal_target IS NULL OR kcal_target >= 0)
        AND (proteine_target IS NULL OR proteine_target >= 0)
        AND (carboidrati_target IS NULL OR carboidrati_target >= 0)
        AND (grassi_target IS NULL OR grassi_target >= 0)
    );

-- Un blocco ha sempre una fine; se non è a giornate intere ha anche gli orari e la fine segue l'inizio.
ALTER TABLE appuntamenti
    ADD CONSTRAINT chk_appuntamenti_durata CHECK (durata_minuti IS NULL OR durata_minuti BETWEEN 5 AND 480),
    ADD CONSTRAINT chk_appuntamenti_intervallo_blocco CHECK (
        tipo <> 'BLOCCO' OR (
            data_fine IS NOT NULL AND data_fine >= data
            AND (giornata_intera OR (
                ora_inizio IS NOT NULL AND ora_fine IS NOT NULL
                AND data_fine + ora_fine > data + ora_inizio
            ))
        )
    );
