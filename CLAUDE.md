# hexis-nutrition

SaaS di gestione dello studio per dietisti/nutrizionisti: area professionista per la gestione di pazienti e piani alimentari, area cliente dedicata ai pazienti.

## Stack

- **Backend**: Java Spring Boot, autenticazione JWT, migrazioni con Flyway, test con Testcontainers.
- **Frontend**: due applicazioni Vue.js 3 separate — `frontend-professionisti/` e `frontend-cliente/` — entrambe con shadcn-vue + Tailwind.
- **Database**: PostgreSQL.

Dettagli e motivazioni: [wiki/decisioni/0001-stack-tecnologico.md](wiki/decisioni/0001-stack-tecnologico.md).

## Struttura

Tutto il progetto sta in un **unico repo git**, con la radice in questa cartella:

- [`backend/`](backend/) — API Spring Boot.
- [`frontend-professionisti/`](frontend-professionisti/) — app Vue per i professionisti.
- [`frontend-cliente/`](frontend-cliente/) — app Vue per i pazienti.
- [`wiki/`](wiki/) — base di conoscenza del progetto. Leggi [`wiki/CLAUDE.md`](wiki/CLAUDE.md) prima di lavorare qui dentro.

## Stato attuale

Vedi [wiki/stato.md](wiki/stato.md).
