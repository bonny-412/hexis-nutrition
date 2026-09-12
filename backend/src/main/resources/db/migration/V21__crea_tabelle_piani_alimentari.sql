-- Modello dati del sotto-progetto "Piano alimentare" (editor + lista), vedi
-- docs/superpowers/specs/2026-09-12-piano-alimentare-design.md.
CREATE TABLE piani_alimentari (
    id UUID PRIMARY KEY,
    paziente_id UUID NOT NULL REFERENCES pazienti(id),
    professionista_id UUID NOT NULL REFERENCES professionisti(id),
    visita_id UUID REFERENCES visite(id),
    nome VARCHAR(200) NOT NULL,
    modalita VARCHAR(10) NOT NULL,
    stato VARCHAR(10) NOT NULL DEFAULT 'BOZZA',
    data_inizio DATE NOT NULL,
    data_fine DATE,
    obiettivo_kcal NUMERIC(7,2),
    obiettivo_kcal_suggerito NUMERIC(7,2),
    bmr_calcolato NUMERIC(7,2),
    tdee_calcolato NUMERIC(7,2),
    formula_bmr_usata VARCHAR(20),
    sotto_soglia_sicurezza BOOLEAN NOT NULL DEFAULT false,
    creato_il TIMESTAMPTZ NOT NULL DEFAULT now(),
    aggiornato_il TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_piani_alimentari_paziente_id ON piani_alimentari(paziente_id);
CREATE INDEX idx_piani_alimentari_professionista_id ON piani_alimentari(professionista_id);

CREATE TABLE pasti (
    id UUID PRIMARY KEY,
    piano_id UUID NOT NULL REFERENCES piani_alimentari(id) ON DELETE CASCADE,
    giorno_settimana VARCHAR(10) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    nota TEXT,
    ordine INTEGER NOT NULL
);
CREATE INDEX idx_pasti_piano_id ON pasti(piano_id);

CREATE TABLE piano_giorno_macro_target (
    id UUID PRIMARY KEY,
    piano_id UUID NOT NULL REFERENCES piani_alimentari(id) ON DELETE CASCADE,
    giorno_settimana VARCHAR(10) NOT NULL,
    kcal_target NUMERIC(7,2),
    proteine_target NUMERIC(6,2),
    carboidrati_target NUMERIC(6,2),
    grassi_target NUMERIC(6,2),
    UNIQUE (piano_id, giorno_settimana)
);

CREATE TABLE piano_esempi (
    id UUID PRIMARY KEY,
    piano_id UUID NOT NULL REFERENCES piani_alimentari(id) ON DELETE CASCADE,
    tipo_pasto VARCHAR(20) NOT NULL,
    nome VARCHAR(100) NOT NULL,
    ordine INTEGER NOT NULL
);
CREATE INDEX idx_piano_esempi_piano_id ON piano_esempi(piano_id);

-- Righe di alimenti condivise tra pasti ed esempi (esattamente uno dei due FK
-- valorizzato), stesso pattern di esclusività già usato da token_azione per
-- professionista_id/paziente_id.
CREATE TABLE piano_alimento_righe (
    id UUID PRIMARY KEY,
    pasto_id UUID REFERENCES pasti(id) ON DELETE CASCADE,
    esempio_id UUID REFERENCES piano_esempi(id) ON DELETE CASCADE,
    alimento_id UUID,
    nome VARCHAR(200) NOT NULL,
    kcal_100g NUMERIC(7,2) NOT NULL,
    proteine_100g NUMERIC(6,2) NOT NULL,
    carboidrati_100g NUMERIC(6,2) NOT NULL,
    grassi_100g NUMERIC(6,2) NOT NULL,
    zuccheri_100g NUMERIC(6,2),
    grammi NUMERIC(7,2) NOT NULL,
    ordine INTEGER NOT NULL,
    CONSTRAINT chk_piano_alimento_riga_padre CHECK (
        (pasto_id IS NOT NULL AND esempio_id IS NULL) OR
        (pasto_id IS NULL AND esempio_id IS NOT NULL)
    )
);
CREATE INDEX idx_piano_alimento_righe_pasto_id ON piano_alimento_righe(pasto_id);
CREATE INDEX idx_piano_alimento_righe_esempio_id ON piano_alimento_righe(esempio_id);
