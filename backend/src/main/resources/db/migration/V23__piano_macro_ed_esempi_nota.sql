-- Stesso campo nota libera già disponibile per i pasti (vedi pasti.nota in V21),
-- ora anche per il target macro giornaliero e per gli esempi intercambiabili.
ALTER TABLE piano_giorno_macro_target ADD COLUMN nota TEXT;
ALTER TABLE piano_esempi ADD COLUMN nota TEXT;
