---
title: 0002 - Autenticazione, onboarding e modello di account
tags: [adr, auth, onboarding]
stato: stabile
creato: 2026-08-08
aggiornato: 2026-08-08
fonti: [sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md]
---

# ADR 0002 — Autenticazione, onboarding e modello di account

## Decisione

- Un account SaaS corrisponde a **un singolo professionista** (nessun team/studio con più professionisti che condividono pazienti).
- Gli account professionista sono **creati manualmente** (da Andrea) nella fase di beta chiusa: nessun self-signup pubblico nell'MVP.
- **Un solo sistema di autenticazione JWT**, condiviso tra `frontend-professionisti` e `frontend-cliente`, con un claim di ruolo (`PROFESSIONISTA` / `PAZIENTE`) usato dal backend per autorizzare gli endpoint.
- Il paziente viene sempre creato come anagrafica dal professionista; l'**attivazione dell'account è opzionale** e avviene tramite invito via email (link con token).
- **Recupero password self-service** disponibile per entrambi i ruoli, con lo stesso meccanismo a token+email dell'invito.
- **Provider email**: Resend (API HTTP, piano gratuito 3000 email/mese), integrato dietro un'interfaccia astratta (`EmailSender`) per restare sostituibile.

## Contesto

Prima decisione di design del sotto-progetto "Fondamenta" (il primo di 4 sotto-progetti pianificati: Fondamenta → Piano alimentare → Monitoraggio → Chat), emersa da una sessione di brainstorming sullo scope funzionale del prodotto. Vedi [stato](../stato.md) per la roadmap completa.

## Alternative considerate

- **Self-signup pubblico per i professionisti**: scartato per ora — il prodotto è in fase di beta chiusa, non serve ancora un flusso di registrazione autonoma.
- **Due sistemi di login separati** (uno per professionista, uno per paziente): scartato a favore di un solo sistema JWT con ruoli, più semplice da mantenere con un unico backend.
- **Paziente con account obbligatorio**: scartato — molti pazienti potrebbero non aver bisogno/voglia di un account, l'anagrafica deve poter esistere comunque.
- **Provider email alternativi** (Amazon SES, SendGrid, Postmark): non scelti; Resend preferito per semplicità di integrazione e piano gratuito più che sufficiente in questa fase.

## Motivazione

Il prodotto è pensato, per ora, per professionisti indipendenti (non studi con più collaboratori), il che semplifica il modello dati escludendo la condivisione di pazienti tra professionisti. La beta chiusa con creazione manuale degli account riduce la superficie da costruire subito (niente flusso di registrazione, niente piani di abbonamento). Un solo sistema di autenticazione con ruoli evita di duplicare l'infrastruttura di login per un beneficio marginale.

## Conseguenze

- Se in futuro servirà un modello multi-professionista/studio, sarà necessaria una migrazione del modello dati (introduzione di un'entità `Studio` sopra `Professionista`).
- Gli endpoint del backend devono sempre controllare il ruolo nel JWT, non l'identità del frontend chiamante.
- `EmailSender` va progettato come interfaccia fin dall'inizio, non aggiunta dopo, per evitare un accoppiamento diretto a Resend nel codice applicativo.
- La creazione manuale degli account professionista è un processo temporaneo: andrà rivisto quando si deciderà di aprire il self-signup.
