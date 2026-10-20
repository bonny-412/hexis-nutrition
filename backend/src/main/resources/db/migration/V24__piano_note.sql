-- Note di aggiornamento di un piano alimentare: inserite dal professionista sul piano attivo, consultabili
-- su tutti i piani non in bozza. Non si modificano; si eliminano solo finché il piano è attivo.
CREATE TABLE piano_note (
    id UUID PRIMARY KEY,
    piano_id UUID NOT NULL REFERENCES piani_alimentari(id) ON DELETE CASCADE,
    autore_id UUID NOT NULL REFERENCES professionisti(id),
    data DATE NOT NULL,
    testo TEXT NOT NULL,
    creato_il TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_piano_note_piano_id ON piano_note(piano_id);
