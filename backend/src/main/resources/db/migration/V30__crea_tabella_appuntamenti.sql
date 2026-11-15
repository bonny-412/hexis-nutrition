CREATE TABLE appuntamenti (
    id UUID PRIMARY KEY,
    professionista_id UUID NOT NULL REFERENCES professionisti(id),
    paziente_id UUID REFERENCES pazienti(id),
    visita_id UUID REFERENCES visite(id) ON DELETE SET NULL,
    tipo VARCHAR(20) NOT NULL,
    stato VARCHAR(20),
    data DATE NOT NULL,
    ora_inizio TIME,
    durata_minuti INTEGER,
    testo VARCHAR(500),
    link_video VARCHAR(500),
    fatto BOOLEAN NOT NULL DEFAULT FALSE,
    creato_il TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_appuntamenti_professionista_data ON appuntamenti(professionista_id, data);
CREATE INDEX idx_appuntamenti_paziente_id ON appuntamenti(paziente_id);
