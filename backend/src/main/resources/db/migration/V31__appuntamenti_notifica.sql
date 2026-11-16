ALTER TABLE appuntamenti
    ADD COLUMN notifica BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN anticipo_notifica_minuti INTEGER NOT NULL DEFAULT 45,
    ADD CONSTRAINT chk_appuntamenti_anticipo_notifica CHECK (anticipo_notifica_minuti IN (15, 30, 45, 60, 120));

-- Gli eventi esistenti non devono generare notifiche alla prima installazione.
UPDATE appuntamenti SET notifica = FALSE;
