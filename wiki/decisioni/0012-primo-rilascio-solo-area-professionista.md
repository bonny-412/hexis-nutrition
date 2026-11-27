---
title: 0012 - Primo rilascio solo area professionista
tags: [adr, scope, rilascio, frontend-cliente, chat]
stato: stabile
creato: 2026-11-27
aggiornato: 2026-11-27
fonti: [../sorgenti/2026-11-26-accordo-con-edoardo.md]
---

# ADR 0012 — Primo rilascio solo area professionista

## Contesto

La roadmap ([architettura](../architettura.md)) prevede quattro sotto-progetti, l'ultimo dei quali è la Chat. Dal lato paziente sono previste una webapp Vue (`frontend-cliente/`, oggi vuota) e un'app Flutter per iOS e Android (decisa a voce il 17 novembre 2026). L'area del professionista invece è già in gran parte fatta: pazienti, visite, piani alimentari, alimenti, agenda e notifiche. Il 26 novembre 2026 Andrea ne ha parlato con Edoardo, l'amico che finanzia Hexis.

## Decisione

**Si pubblica per prima solo l'area del professionista** (`backend/` + `frontend-professionisti/`). **Per ora restano fuori**:

- la chat professionista-paziente;
- la webapp del paziente (`frontend-cliente/`);
- l'app mobile del paziente (Flutter).

Sono rimandate, non cancellate. Non c'è una data per riprenderle.

## Conseguenze

- Il prossimo traguardo è **mettere online l'area professionista**: deploy, dominio, GDPR, collaudo (vedi [domande-aperte](../domande-aperte.md)). Le funzioni ancora aperte nell'area professionista, come la BIA, vanno decise rispetto a questo traguardo.
- **Invito e stato account nascosti** (deciso da Andrea il 2026-11-27): senza area paziente l'account del paziente ([decisioni/0002](0002-autenticazione-e-onboarding.md)) non serve. In `frontend-professionisti` un solo interruttore, `AREA_PAZIENTE_ATTIVA = false` in `src/config/funzioni.ts`, nasconde il pulsante "Invita", il badge e la colonna "Stato account" (lista in tabella e a schede, testata della scheda paziente), i filtri e l'ordinamento per stato, la linguetta "Inviti" di "Da fare" in Dashboard (e la sua richiesta) e la voce "Chat" della sidebar. Gli avatar, che prendevano il colore dello stato account ([decisioni/0010](0010-toni-di-stato-badge-e-avatar.md)), diventano tutti `attivo` (archiviati `neutro`). Il backend non cambia: endpoint di invito e `stato_account` restano. Per riaccendere tutto basta mettere `true`.
- **Voce "Analytics" tolta** dalla sidebar: era un segnaposto "in arrivo", non legato all'area paziente.
- Restano in sospeso, finché non si riprende l'area paziente: l'ADR sull'app Flutter (web Vue + Flutter, oppure solo Flutter) e lo stato "inviato al paziente" del piano.
- Per il compenso concordato su questa prima parte vedi [stato](../stato.md), sessione del 27 novembre 2026.
