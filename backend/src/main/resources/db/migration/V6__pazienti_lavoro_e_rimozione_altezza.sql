ALTER TABLE pazienti ADD COLUMN lavoro VARCHAR(150);
ALTER TABLE pazienti ADD COLUMN tipo_lavoro VARCHAR(20);
ALTER TABLE pazienti DROP COLUMN altezza_cm;
