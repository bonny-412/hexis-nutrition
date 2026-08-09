---
title: Architettura
tags: [architettura]
stato: in-discussione
creato: 2026-08-08
aggiornato: 2026-08-09
fonti: [sorgenti/2026-08-08-scope-e-stack-iniziali.md, sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md, sorgenti/2026-08-09-docker-solo-in-produzione.md]
---

# Architettura — hexis-nutrition

## Componenti

- **backend** (Spring Boot): API consumata da entrambi i frontend, autenticazione JWT unica con ruoli, PostgreSQL con migrazioni Flyway, integrazione email (Resend) dietro un'interfaccia `EmailSender`.
- **frontend-professionisti** (Vue 3 + shadcn-vue + Tailwind): interfaccia per il professionista — gestione pazienti, piani alimentari, monitoraggio.
- **frontend-cliente** (Vue 3 + shadcn-vue + Tailwind): interfaccia per il paziente — consultazione piani, chat, auto-registrazione misurazioni.

## Perché due frontend separati invece di uno con ruoli

Risolto (vedi [decisioni/0002](decisioni/0002-autenticazione-e-onboarding.md) e [glossario](glossario.md) per la distinzione cliente/paziente): le due aree servono pubblici con cicli di vita dell'account molto diversi — il professionista è creato manualmente come cliente pagante del SaaS e gestisce dati di più pazienti, mentre il paziente è sempre dipendente da un professionista, ha un account opzionale attivato solo via invito, e vede solo i propri dati. La superficie funzionale e il modello di accesso sono sufficientemente diversi da giustificare due codebase separate pur condividendo lo stesso backend.

## Come comunicano i due frontend con il backend

Un solo backend, un solo sistema di autenticazione JWT: il token porta un claim di ruolo (`PROFESSIONISTA` / `PAZIENTE`) che il backend usa per autorizzare gli endpoint. Non ci sono superfici API distinte per i due frontend, solo permessi diversi in base al ruolo.

## Roadmap del prodotto

Lo scope funzionale è stato diviso in 4 sotto-progetti sequenziali (vedi [stato](stato.md) per lo stato di avanzamento):

1. Fondamenta — autenticazione, anagrafica paziente, invito/onboarding.
2. Piano alimentare — database alimenti, costruzione/assegnazione piani.
3. Monitoraggio — misurazioni e appuntamenti.
4. Chat — messaggistica professionista-paziente.

## Ambienti

- **Sviluppo e test**: tutto sulla macchina di Andrea, con un PostgreSQL 13 installato in locale e i database creati a mano (`hexis`, `hexis_test`) — vedi [decisioni/0004](decisioni/0004-test-su-postgres-locale.md). Niente Docker.
- **Produzione**: previsto Docker. Nient'altro deciso: immagini, orchestrazione, provider e versione di PostgreSQL restano da definire.

## Da definire

- Deploy: stessa infrastruttura o separata per le due aree? (ancora aperto, vedi [domande-aperte](domande-aperte.md))
