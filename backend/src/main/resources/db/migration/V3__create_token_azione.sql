CREATE TABLE token_azione (
    id UUID PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    tipo VARCHAR(20) NOT NULL,
    professionista_id UUID REFERENCES professionisti(id),
    paziente_id UUID REFERENCES pazienti(id),
    scadenza TIMESTAMPTZ NOT NULL,
    usato BOOLEAN NOT NULL DEFAULT false,
    creato_il TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_token_azione_target CHECK (
        (professionista_id IS NOT NULL AND paziente_id IS NULL) OR
        (professionista_id IS NULL AND paziente_id IS NOT NULL)
    )
);
