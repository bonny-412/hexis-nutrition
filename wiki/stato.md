---
title: Stato del progetto
tags: [stato]
stato: stabile
creato: 2026-08-08
aggiornato: 2026-08-30
fonti: [sorgenti/2026-08-08-scope-e-stack-iniziali.md, sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md, sorgenti/2026-08-09-migrazione-a-repo-unico.md, sorgenti/2026-08-09-test-su-postgres-locale.md, sorgenti/2026-08-09-docker-solo-in-produzione.md, sorgenti/2026-08-30-hash-pulizia-invalidazione-token.md]
---

# Stato — hexis-nutrition

## Dove siamo

Il backend del sotto-progetto **"Fondamenta"** (autenticazione JWT con ruoli, anagrafica paziente, invito via email, reset password) è **scritto, revisionato, testato su database reale e su GitHub**. I due frontend non esistono ancora: `frontend-professionisti/` e `frontend-cliente/` contengono solo un `CLAUDE.md`.

Il progetto vive in **un unico repo git**, `bonny-412/hexis-nutrition`, con la radice in `progetti/hexis-nutrition/`.

## Come far girare il backend su questa macchina

- **Database**: PostgreSQL 13 locale (servizio `postgresql-x64-13`). Due database creati a mano con pgAdmin, tabelle generate da Flyway: `hexis` per l'applicazione, `hexis_test` per la suite. Credenziali note ad Andrea.
- **JDK**: il JDK di sistema di default è Java 8, va **sempre** sovrascritto: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"` prima di ogni comando Maven.
- **Test**: `mvn test` da `backend/`. Niente Docker richiesto.
- **Avvio dell'app**: serve la variabile d'ambiente `JWT_SECRET`, obbligatoria (l'app non parte senza, per scelta di sicurezza post-revisione) e lunga **almeno 32 caratteri** — `Keys.hmacShaKeyFor` con HS256 richiede una chiave da 256 bit e lancia `WeakKeyException` se è più corta. Da Spring Tool Suite: importare `backend/` come progetto Maven esistente, poi impostarla in `Run Configurations → Spring Boot App → Environment` (non in *VM arguments*). `RESEND_API_KEY` serve solo per l'invio email reale: senza, l'invito paziente fallisce, mentre il reset password risponde comunque 204 per scelta anti-enumerazione.
- **Account di sviluppo**: esiste un professionista inserito a mano nel database `hexis` (`andrea@hexisnutrition.local`), password nota ad Andrea e deliberatamente non scritta qui. Non c'è self-signup, per scelta ([decisioni/0002](decisioni/0002-autenticazione-e-onboarding.md)): altri account si creano con una `INSERT` e un hash BCrypt.

## Cosa è stato implementato — backend "Fondamenta"

Tutto in `backend/`, committato in `70e2141`. Piano seguito: [`backend/docs/superpowers/plans/2026-08-08-fondamenta-backend.md`](../backend/docs/superpowers/plans/2026-08-08-fondamenta-backend.md), eseguito interamente con `superpowers:subagent-driven-development` (9 task, ognuno implementato e revisionato da subagent separati, più una fix wave finale).

- **Bootstrap**: Maven, Spring Boot 3.3.4, Java 21, `AbstractIntegrationTest` su PostgreSQL locale (profilo `test`, database `hexis_test`).
- **Entità**: `Professionista`, `Paziente` (con `StatoAccountPaziente`), `TokenAzione` (con `TipoToken`), migrazioni Flyway V1-V3.
- **Auth**: `JwtService` (JWT con claim ruolo), `SecurityConfig` + `JwtAuthenticationFilter` (stateless, ruoli PROFESSIONISTA/PAZIENTE).
- **Email**: `EmailSender` / `ResendEmailSender` (Resend via RestClient) / `FakeEmailSender` (test).
- **Endpoint**: `/auth/login`, `/auth/password-dimenticata`, `/auth/reset-password`, `/pazienti` (crea/lista/dettaglio/invito), `/inviti/{token}/attiva` — tabella completa in [api-contracts](api-contracts.md), flussi di invito e reset spiegati in [moduli/inviti-e-token](moduli/inviti-e-token.md).
- **Revisione finale whole-branch**: risolti 1 Critical (JWT secret con default insicuro) e diversi Important (`TIMESTAMP`→`TIMESTAMPTZ` per la scadenza dei token, enumerazione email via fallimento invio, `@Transactional` su `invita`/`attiva`/`resetPassword`, 4 test aggiunti su percorsi critici scoperti).
- Dettaglio di ogni task, revisione e fix: `backend/.superpowers/sdd/2026-08-08-fondamenta-backend/` (cartella locale, ignorata da git).

## Sessione del 9 agosto 2026 — cosa è stato deciso e perché

**Un repo unico per progetto** ([decisioni/0003](decisioni/0003-repo-unico-per-progetto.md)). I quattro repo GitHub separati (`backend`, `frontend-professionisti`, `frontend-cliente`, `wiki`) sono stati sostituiti da un solo repo `hexis-nutrition` e archiviati. Motivo: un cambiamento che tocca API, interfaccia e wiki sta in un solo commit invece di sparpagliarsi su repo che si disallineano. La storia precedente **non** è stata preservata (7 commit con messaggi poco informativi): resta leggibile nei repo archiviati, che quindi non vanno eliminati. La regola vale per **tutti** i progetti del workspace, presenti e futuri — lo schema in `CLAUDE.md` del workspace è stato aggiornato di conseguenza.

**Test su PostgreSQL locale invece di Testcontainers** ([decisioni/0004](decisioni/0004-test-su-postgres-locale.md)). Docker non è disponibile su questa macchina, e la conseguenza era che la suite **non era mai stata eseguita**: il codice è finito su GitHub solo compilato e ritracciato a mano. Rimosse le dipendenze Testcontainers, i test ora girano sul Postgres locale. **Risultato: `mvn test` → 33 test, 0 fallimenti, BUILD SUCCESS**, prima esecuzione reale in assoluto. Verificato anche che le migrazioni si applicano su database vero e che passa `resetPasswordConTokenScadutoRestituisceErrore`, il test più sensibile al fix `TIMESTAMPTZ`.

**Docker solo in produzione.** Il Postgres locale non è un ripiego temporaneo: è la modalità prevista per tutta la fase di sviluppo. Docker si userà in produzione, dove però non è ancora deciso nulla.

**Primo avvio dell'app riuscito** da Spring Tool Suite: Flyway ha creato le tabelle anche su `hexis`, ed è stato inserito il primo account professionista.

**Archiviata** la pagina [moduli/inviti-e-token](moduli/inviti-e-token.md), prima di `moduli/`: struttura di `token_azione`, i due flussi (invito paziente, reset password) e le condizioni di validità che impediscono di spendere un token di invito su un endpoint di reset.

## Sessione del 30 agosto 2026 — cosa è stato deciso e perché

**Chiusi i tre punti aperti su `token_azione`** ([moduli/inviti-e-token](moduli/inviti-e-token.md)): token salvati come hash SHA-256 invece che in chiaro (colonna `token`→`token_hash`, migrazione V4), cancellazione immediata del token alla consumazione invece del flag `usato` (più un job schedulato notturno, `TokenAzionePulizia`, per i token scaduti mai usati), e le richieste di reset password ora invalidano quelle precedenti per la stessa persona. Bounded task, design approvato in chat, implementato in tre cicli TDD. `mvn test` → **36 test, 0 fallimenti, BUILD SUCCESS**. Dettaglio in [sorgenti/2026-08-30-hash-pulizia-invalidazione-token](sorgenti/2026-08-30-hash-pulizia-invalidazione-token.md).

## Cosa resta aperto

Elenco completo e ragionato in [domande-aperte](domande-aperte.md). I punti che toccano il codice esistente:

- **Versione di PostgreSQL**: sviluppo e test girano sulla 13, la produzione sarà in Docker e potrà usare qualunque immagine. Fissarla su `postgres:13` allinea gli ambienti; qualsiasi altra scelta introduce una differenza che nessun test copre.
- **Deploy**: previsto Docker, nient'altro deciso (immagini, orchestrazione, provider, gestione di `JWT_SECRET` e `RESEND_API_KEY`).

Il flusso reale del prodotto — login, creazione paziente, invito via email con Resend, attivazione — **non è mai stato provato contro l'app in esecuzione**: è coperto solo dai test automatici.

## Prossimo passo consigliato

1. Apri Claude Code su `progetti/hexis-nutrition/` (non sulla radice del workspace, non dentro `backend/` da solo).
2. Scrivere con `superpowers:writing-plans` i piani per `frontend-professionisti/` e `frontend-cliente/` di "Fondamenta" — è il primo pezzo di lavoro non ancora iniziato. Poi il brainstorming di "Piano alimentare", prossimo sotto-progetto della roadmap (vedi [architettura](architettura.md)).

## Divisione dei compiti

- **Commit**: li fa **sempre e solo Andrea**. L'agente prepara e fa `git add`, poi segnala.
- **Test automatici**: responsabilità dell'agente, da eseguire dopo ogni modifica al backend, riportando l'esito reale (vedi `backend/CLAUDE.md`).
- **Verifiche manuali del prodotto** (avviare l'app, provare i flussi, controllare le email): le fa **Andrea**. L'agente non avvia l'applicazione per provarla né usa quella verifica come criterio di completamento.

Vedi anche: [domande-aperte](domande-aperte.md), [api-contracts](api-contracts.md), [modello-dati](modello-dati.md), [architettura](architettura.md).
