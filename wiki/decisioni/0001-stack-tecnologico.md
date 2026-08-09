---
title: 0001 - Stack tecnologico
tags: [adr, stack]
stato: stabile
creato: 2026-08-08
aggiornato: 2026-08-09
fonti: [sorgenti/2026-08-08-scope-e-stack-iniziali.md, sorgenti/2026-08-09-migrazione-a-repo-unico.md]
---

# ADR 0001 — Stack tecnologico

## Decisione

- **Backend**: Java Spring Boot, autenticazione JWT, migrazioni con Flyway, ~~test con Testcontainers~~ — corretto il 2026-08-09: i test di integrazione girano su un PostgreSQL locale, vedi [decisioni/0004](0004-test-su-postgres-locale.md).
- **Frontend**: due applicazioni separate in Vue.js 3 — "Area Professionisti" e "Area Cliente" — entrambe con shadcn-vue e Tailwind CSS.
- **Database**: PostgreSQL.

## Contesto

hexis-nutrition è una piattaforma SaaS di gestione studio per dietisti/nutrizionisti, con due pubblici distinti: il professionista (gestione pazienti/piani alimentari) e il paziente (area cliente dedicata).

## Alternative considerate

Non discusse esplicitamente al momento della decisione.

## Motivazione

Non raccolta in dettaglio. La separazione in due frontend riflette la separazione tra le due aree del prodotto (professionista/cliente) descritta nello scopo del progetto. Le ragioni specifiche delle altre scelte (Spring Boot, JWT, Flyway, Testcontainers, shadcn-vue+Tailwind) non sono state discusse — vedi [domande-aperte](../domande-aperte.md).

## Conseguenze

- Tre codebase da mantenere (`backend/`, `frontend-professionisti/`, `frontend-cliente/`), oltre alla wiki. ~~Tre repository di codice indipendenti~~ — corretto il 2026-08-09: le tre codebase e la wiki vivono in **un unico repo git**, vedi [decisioni/0003](0003-repo-unico-per-progetto.md).
- I contratti tra backend e ciascun frontend vanno documentati e tenuti allineati in [api-contracts](../api-contracts.md).
