-- Aggiunge a piano_alimento_righe gli stessi micronutrienti opzionali già presenti su
-- alimenti (fibre_g/ferro_mg/calcio_mg/acqua_g), finora non copiati nello snapshot della riga
-- di piano: servono per le card di riepilogo e le colonne extra della tabella pasto/esempio.
ALTER TABLE piano_alimento_righe
    ADD COLUMN fibre_100g NUMERIC(6,2),
    ADD COLUMN ferro_100mg NUMERIC(6,2),
    ADD COLUMN calcio_100mg NUMERIC(6,2),
    ADD COLUMN acqua_100g NUMERIC(6,2);
