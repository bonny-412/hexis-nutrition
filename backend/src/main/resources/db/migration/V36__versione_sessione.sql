-- Collaudo 27/11/2026, bug 19: il reset della password deve invalidare i JWT già emessi.
-- Ogni token porta la versione di sessione dell'utente al momento del login (claim "sv");
-- il reset la incrementa e il filtro JWT rifiuta i token con una versione diversa.
ALTER TABLE professionisti
    ADD COLUMN versione_sessione INTEGER NOT NULL DEFAULT 0;

ALTER TABLE pazienti
    ADD COLUMN versione_sessione INTEGER NOT NULL DEFAULT 0;
