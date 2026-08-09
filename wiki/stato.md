---
title: Stato del progetto
tags: [stato]
stato: stabile
creato: 2026-08-08
aggiornato: 2026-08-08
fonti: [sorgenti/2026-08-08-scope-e-stack-iniziali.md, sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md]
---

# Stato — hexis-nutrition

## Cosa si stava facendo (sessione dell'8 agosto 2026)

Brainstorming sul nucleo funzionale del prodotto → piano di implementazione TDD per il backend di "Fondamenta" → **piano eseguito per intero** con `superpowers:subagent-driven-development` (9 task + fix wave finale, ognuno implementato e revisionato da subagent separati). Il codice del backend di "Fondamenta" è **scritto, revisionato e in staging git**, ma **non ancora committato** (i commit li fa sempre e solo Andrea) e **non ancora testato con un database reale** (Docker non disponibile nell'ambiente usato per l'esecuzione — vedi sotto).

## Cosa è stato deciso

- Scopo del progetto, stack tecnico ([decisioni/0001](decisioni/0001-stack-tecnologico.md)), autenticazione/onboarding ([decisioni/0002](decisioni/0002-autenticazione-e-onboarding.md)): vedi voci precedenti di questo file nel log, invariate.
- Piano di implementazione: [`backend/docs/superpowers/plans/2026-08-08-fondamenta-backend.md`](../backend/docs/superpowers/plans/2026-08-08-fondamenta-backend.md) — **eseguito interamente**.

## Cosa è stato implementato (backend "Fondamenta")

Tutto in staging in `backend/` (mai committato):

- **Bootstrap**: Maven, Spring Boot 3.3.4, Java 21, `AbstractIntegrationTest` con Testcontainers Postgres.
- **Entità**: `Professionista`, `Paziente` (con `StatoAccountPaziente`), `TokenAzione` (con `TipoToken`), migrazioni Flyway V1-V3.
- **Auth**: `JwtService` (JWT con claim ruolo), `SecurityConfig` + `JwtAuthenticationFilter` (JWT stateless, ruoli PROFESSIONISTA/PAZIENTE).
- **Email**: `EmailSender` / `ResendEmailSender` (Resend via RestClient) / `FakeEmailSender` (test).
- **Endpoint**: `/auth/login`, `/auth/password-dimenticata`, `/auth/reset-password`, `/pazienti` (crea/lista/dettaglio/invito), `/inviti/{token}/attiva` — vedi [api-contracts](api-contracts.md) per la tabella completa.
- **Revisione finale whole-branch** (modello opus): trovato e **risolto** 1 Critical (JWT secret con default insicuro — ora l'app non parte senza `JWT_SECRET` impostato) e diversi Important (colonne `TIMESTAMP`→`TIMESTAMPTZ` per correttezza scadenza token, enumerazione email via fallimento invio corretta, `@Transactional` aggiunto su `invita`/`attiva`/`resetPassword`, 4 test aggiunti su percorsi critici prima scoperti — confusione tipo token, ramo paziente del reset, flusso invito→attivazione→login end-to-end).
- Dettaglio completo di ogni task, revisione e fix: `backend/.superpowers/sdd/2026-08-08-fondamenta-backend/` (cartella locale, non in git — report, brief e diff di ogni singolo task).

## ⚠️ Limite importante: nessun test eseguito su database reale

**Docker non era disponibile nell'ambiente usato per l'esecuzione.** Ogni test che richiede Postgres reale (`AbstractIntegrationTest` e sottoclassi — la maggior parte della suite) è stato **verificato solo per compilazione** (`mvn clean test-compile` sotto JDK 21, mai `mvn test`) e **ritracciato a mano** dagli implementer e dai reviewer, non eseguito per davvero. Solo i test senza dipendenza da Postgres/Docker (`JwtServiceTest`, `ResendEmailSenderTest`) sono stati eseguiti realmente e passano.

**Prima di fidarsi del codice, va eseguita la suite completa con Docker attivo.**

## Prossimo passo consigliato (per chi riprende la sessione)

1. Apri Claude Code su `progetti/hexis-nutrition/` (non sulla radice del workspace, non dentro `backend/` da solo).
2. **Assicurati che Docker sia in esecuzione**, poi esegui: `JAVA_HOME="C:\Program Files\Java\jdk-21" mvn test` da `backend/` (il JDK di sistema di default è Java 8, va sempre sovrascritto con `JAVA_HOME`). Segnala qualsiasi test fallito — in particolare `resetPasswordConTokenScadutoRestituisceErrore`, indicato dal reviewer finale come il test più sensibile al fix TIMESTAMPTZ.
3. Imposta la variabile d'ambiente `JWT_SECRET` (obbligatoria: l'app non si avvia senza, per scelta di sicurezza post-revisione).
4. Crea manualmente il primo account professionista in DB (nessun endpoint di self-signup, per scelta — vedi ADR 0002); usa un hash BCrypt per la password.
5. Se tutto passa, i file sono pronti per essere committati da Andrea (non farlo automaticamente).
6. Dopo un commit riuscito e verificato, scrivere (con `writing-plans`) i piani per `frontend-professionisti/` e `frontend-cliente/` di "Fondamenta", poi passare al brainstorming di "Piano alimentare" (prossimo sotto-progetto della roadmap, vedi [architettura](architettura.md)).

Vedi anche: [domande-aperte](domande-aperte.md), [api-contracts](api-contracts.md), [modello-dati](modello-dati.md).
