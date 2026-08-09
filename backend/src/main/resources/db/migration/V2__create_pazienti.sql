CREATE TABLE pazienti (
    id UUID PRIMARY KEY,
    professionista_id UUID NOT NULL REFERENCES professionisti(id),
    nome VARCHAR(100) NOT NULL,
    cognome VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    telefono VARCHAR(30),
    data_nascita DATE,
    sesso VARCHAR(10),
    altezza_cm INTEGER,
    password_hash VARCHAR(255),
    stato_account VARCHAR(20) NOT NULL DEFAULT 'MAI_INVITATO',
    creato_il TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_pazienti_professionista_id ON pazienti(professionista_id);

-- Un'email può comparire più volte come anagrafica (es. presso professionisti diversi),
-- ma può appartenere a un solo account ATTIVO per volta: vedi ADR 0002.
CREATE UNIQUE INDEX uq_pazienti_email_attivo ON pazienti(email) WHERE stato_account = 'ATTIVO';
