-- Le colonne circonferenza_*_cm esistenti di braccio rilassato/contratto, coscia e polpaccio
-- diventano, per significato, le misure della parte NON dominante; le nuove colonne *_dominante_cm
-- sono le stesse misure sulla parte dominante (opzionali).
ALTER TABLE visite ADD COLUMN parte_dominante VARCHAR(10);
ALTER TABLE visite ADD COLUMN circonferenza_braccio_rilassato_dominante_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_braccio_contratto_dominante_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_coscia_dominante_cm NUMERIC(6,2);
ALTER TABLE visite ADD COLUMN circonferenza_polpaccio_dominante_cm NUMERIC(6,2);
