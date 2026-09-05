-- Rende esplicita la quantità di riferimento (in grammi) a cui si riferiscono i valori
-- nutrizionali di ogni alimento: sempre 100 per i dati BDA-IEO (il DEFAULT valorizza
-- retroattivamente le 1109 righe già seminate), modificabile solo per gli alimenti
-- personalizzati.
ALTER TABLE alimenti ADD COLUMN quantita_g NUMERIC(6,2) NOT NULL DEFAULT 100;
