ALTER TABLE appuntamenti
    ADD COLUMN data_fine DATE,
    ADD COLUMN ora_fine TIME,
    ADD COLUMN giornata_intera BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE appuntamenti
SET data_fine = data,
    ora_fine = ora_inizio + make_interval(mins => durata_minuti),
    durata_minuti = NULL
WHERE tipo = 'BLOCCO';

CREATE INDEX idx_appuntamenti_professionista_data_fine ON appuntamenti(professionista_id, data_fine);
