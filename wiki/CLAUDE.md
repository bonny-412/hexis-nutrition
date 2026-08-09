# hexis-nutrition — schema della wiki

Questa è la wiki di progetto per hexis-nutrition (pattern LLM Wiki — vedi [CLAUDE.md della radice del workspace](../../../CLAUDE.md) per le convenzioni generali: frontmatter, stato pagina, formato log, operazioni ingest/query/lint/handoff).

Se stai lavorando in una sessione aperta dentro `backend/`, `frontend-professionisti/` o `frontend-cliente/` e i comandi `/ingest` `/lint` `/handoff` non sono disponibili in quella sessione, esegui comunque i workflow descritti qui sotto quando richiesto in linguaggio naturale — sono la stessa cosa.

## Mappa delle pagine

- `index.md` — catalogo di tutte le pagine di questa wiki.
- `log.md` — log append-only di ingest/query/lint/handoff.
- `stato.md` — sintesi corrente: cosa si sta facendo, cosa è deciso, cosa resta aperto. **Prima pagina da leggere a inizio sessione.**
- `architettura.md` — come sono organizzate le tre applicazioni (backend + 2 frontend) e come comunicano.
- `modello-dati.md` — entità principali e relazioni (PostgreSQL).
- `api-contracts.md` — endpoint esposti dal backend e come li consumano i due frontend.
- `moduli/` — una pagina per modulo/servizio rilevante, creata quando il modulo esiste davvero, non prima.
- `decisioni/` — ADR numerate, una per scelta architetturale importante.
- `glossario.md` — terminologia di dominio (nutrizionista, paziente, piano alimentare, ecc.).
- `domande-aperte.md` — cose non ancora decise o chiarite.
- `sorgenti/` — materiale grezzo immutabile che ha originato o aggiornato pagine di questa wiki (mai modificarlo, solo leggerlo).
