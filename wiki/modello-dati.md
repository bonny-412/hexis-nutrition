---
title: Modello dati
tags: [dati]
stato: in-discussione
creato: 2026-08-08
aggiornato: 2026-08-08
fonti: [sorgenti/2026-08-08-scope-e-stack-iniziali.md, sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md]
---

# Modello dati — hexis-nutrition

Database: PostgreSQL, migrazioni gestite con Flyway (in `backend/`).

## Entità implementate (sotto-progetto "Fondamenta", vedi [decisioni/0002](decisioni/0002-autenticazione-e-onboarding.md))

Migrazioni Flyway V1-V3 scritte e in staging in `backend/` (non ancora committate né applicate a un database reale — vedi [stato](stato.md)):

- **Professionista** (V1): email (univoca), password (hash), nome, cognome. Un account SaaS = un professionista (no team/studio condiviso).
- **Paziente** (V2): nome, cognome, email, telefono, data di nascita, sesso, altezza; collegato a un `Professionista`; stato account (`MAI_INVITATO` / `INVITATO` / `ATTIVO`). Non richiede un account attivo per esistere. Email univoca solo tra account `ATTIVO` (indice parziale), non a livello di anagrafica.
- **TokenAzione** (V3): token, scadenza (`TIMESTAMPTZ`), usato, tipo (`INVITO` / `RESET_PASSWORD`); collegato a un `Professionista` **oppure** un `Paziente` (mai entrambi, vincolo CHECK).

## Entità previste ma non ancora progettate (sotto-progetti successivi)

- **Piano alimentare**, **Alimento**, **Pasto** — sotto-progetto "Piano alimentare". Piano strutturato (giorni/pasti/alimenti) con calcolo automatico di calorie/macro; alimenti da dataset pubblico importato + alimenti custom per professionista (dataset specifico da scegliere, vedi [domande-aperte](domande-aperte.md)).
- **Misurazione**, **Appuntamento** — sotto-progetto "Monitoraggio". Appuntamenti con flusso di richiesta (paziente) + conferma (professionista); misurazioni inseribili sia dal professionista sia dal paziente.
- **Messaggio** (chat) — sotto-progetto "Chat".

Vedi anche: [domande-aperte](domande-aperte.md), [stato](stato.md) per la roadmap.
