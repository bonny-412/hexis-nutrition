-- Niente FK da professionista_id verso professionisti: un vincolo farebbe scattare
-- TRUNCATE ... CASCADE su professionisti nei test, cancellando anche le righe BDA
-- seminate (professionista_id IS NULL). Vedi wiki/decisioni/0005.
CREATE TABLE alimenti (
    id UUID PRIMARY KEY,
    professionista_id UUID,
    codice_bda INTEGER UNIQUE,
    nome VARCHAR(200) NOT NULL,
    categoria VARCHAR(200) NOT NULL,
    kcal NUMERIC(7,2) NOT NULL,
    proteine_g NUMERIC(6,2) NOT NULL,
    grassi_g NUMERIC(6,2) NOT NULL,
    carboidrati_g NUMERIC(6,2) NOT NULL,
    acqua_g NUMERIC(6,2),
    fibre_g NUMERIC(6,2),
    zuccheri_g NUMERIC(6,2),
    ferro_mg NUMERIC(8,2),
    calcio_mg NUMERIC(8,2),
    sodio_mg NUMERIC(8,2),
    creato_il TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_alimenti_professionista_id ON alimenti(professionista_id);
