---
title: 0004 - Test di integrazione su PostgreSQL locale invece di Testcontainers
tags: [adr, test, database]
stato: stabile
creato: 2026-08-09
aggiornato: 2026-08-09
fonti: [sorgenti/2026-08-09-test-su-postgres-locale.md, sorgenti/2026-08-09-docker-solo-in-produzione.md]
---

# ADR 0004 — Test di integrazione su PostgreSQL locale invece di Testcontainers

## Decisione

- I test di integrazione girano su un **PostgreSQL installato in locale**, non su un container avviato da Testcontainers. Le dipendenze `org.testcontainers` e il relativo BOM sono state rimosse da `pom.xml`.
- Due database, creati a mano (pgAdmin) e mantenuti **vuoti** alla creazione — le tabelle le crea Flyway:
  - `hexis` — usato dall'applicazione in esecuzione (già configurato in `application.yml`);
  - `hexis_test` — usato dalla suite di test.
- I test attivano il profilo Spring `test` (`@ActiveProfiles("test")` su `AbstractIntegrationTest`), configurato in `backend/src/test/resources/application-test.yml`, con override via variabili d'ambiente `TEST_DB_URL`, `TEST_DB_USERNAME`, `TEST_DB_PASSWORD`.
- `AbstractIntegrationTest` svuota il database con un `TRUNCATE ... RESTART IDENTITY CASCADE` in `@BeforeEach`, perché il database non è più usa e getta.
- Nel solo profilo di test, Flyway ha `clean-disabled: false` e `clean-on-validation-error: true`: se una migrazione già applicata viene modificata, lo schema di test viene ricreato da zero invece di far fallire la suite su un errore di checksum.

## Contesto

Docker non è disponibile sulla macchina di sviluppo. La conseguenza, registrata nel log dell'8 agosto 2026, era che **nessun test che tocca il database era mai stato eseguito**: l'intera suite di "Fondamenta" era stata solo compilata e ritracciata a mano, e il codice è finito su GitHub in questo stato. Andrea ha deciso di creare i database a mano con pgAdmin; da lì la scelta di far girare anche i test sul Postgres locale.

Sulla macchina è già attivo il servizio `postgresql-x64-13`.

**Portata della scelta**: non è una soluzione temporanea in attesa di installare Docker. Il PostgreSQL locale è la modalità prevista per **tutta la fase di sviluppo** e per le verifiche manuali del prodotto, che cura Andrea sulla stessa macchina. **Docker è previsto per la produzione**, non per l'ambiente di sviluppo — come, con quali immagini e su quale infrastruttura non è ancora deciso.

## Alternative considerate

- **Lasciare Testcontainers** e aspettare di avere Docker: scartata. Manteneva la configurazione migliore in teoria, ma lasciava la suite non eseguibile a tempo indeterminato — cioè esattamente la situazione che ha portato a committare codice mai testato.
- **Doppia modalità** (Testcontainers se Docker c'è, fallback locale altrimenti): scartata. Copriva entrambi i casi al prezzo di due percorsi di test che possono divergere, con il rischio che quello meno usato smetta silenziosamente di funzionare.

## Motivazione

Una suite di test che non viene mai eseguita non protegge da niente: il valore di isolamento e riproducibilità di Testcontainers è reale, ma è subordinato al fatto che i test girino davvero. Con un solo sviluppatore su una macchina sola, un Postgres locale già installato e in esecuzione rende la suite eseguibile **subito**, che è il problema aperto più urgente del progetto.

## Conseguenze

- Supera la parte "test con Testcontainers" di [decisioni/0001](0001-stack-tecnologico.md), corretta lì.
- **I test girano su PostgreSQL 13** (la versione installata in locale), mentre Testcontainers usava `postgres:16-alpine`. Le migrazioni attuali non usano funzionalità esclusive di PG16, ma da ora la versione su cui i test danno garanzie è la 13. Poiché la produzione girerà in Docker, la versione dell'immagine sarà una scelta libera: fissarla su PostgreSQL 13 è il modo più semplice per non avere dev e produzione su versioni diverse, altrimenti la differenza non è coperta da alcun test. Vedi [domande-aperte](../domande-aperte.md).
- I test **non sono più isolati per costruzione**: dipendono dal `TRUNCATE` in `AbstractIntegrationTest`. Ogni tabella aggiunta in futuro va aggiunta a quell'elenco, altrimenti i dati sopravvivono tra i test — un punto di manutenzione facile da dimenticare, che con i container non esisteva.
- Eseguire la suite richiede un **setup manuale della macchina** (utente e database creati a mano). Su una macchina nuova, o su una CI, i test non partono finché quel setup non viene rifatto: se un giorno servirà una pipeline CI, questa decisione andrà rivista.
- `clean-on-validation-error` attivo nel profilo di test significa che Flyway **può cancellare lo schema** del database a cui punta. È confinato al profilo `test`, ma se `TEST_DB_URL` venisse puntata per errore a un database vero, il contenuto verrebbe distrutto.
- Le due esecuzioni non partono più da un database pulito per costruzione: se una suite viene interrotta a metà, i dati restano lì fino al `TRUNCATE` del test successivo.
