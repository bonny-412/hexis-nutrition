ALTER TABLE pazienti ALTER COLUMN sesso SET NOT NULL;
ALTER TABLE pazienti ADD CONSTRAINT chk_pazienti_sesso CHECK (sesso IN ('M', 'F', 'ALTRO'));
