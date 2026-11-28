-- Verifica 3 prima del rilascio (28/11/2026), bug S4: se la risposta di una creazione va persa e il frontend
-- riprova, il backend deve restituire la risposta della prima richiesta invece di creare un doppione.
-- Il frontend manda l'header Idempotency-Key; qui si tiene, per utente e chiave, la risposta già data.
-- stato_http NULL = richiesta ancora in corso. Le righe si cancellano dopo 24 ore (PuliziaRichiesteIdempotenti).
CREATE TABLE richieste_idempotenti (
    utente_id    UUID         NOT NULL,
    chiave       VARCHAR(100) NOT NULL,
    metodo       VARCHAR(10)  NOT NULL,
    percorso     VARCHAR(500) NOT NULL,
    stato_http   INTEGER,
    content_type VARCHAR(100),
    corpo        TEXT,
    creata_il    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    PRIMARY KEY (utente_id, chiave)
);

CREATE INDEX idx_richieste_idempotenti_creata_il ON richieste_idempotenti (creata_il);
