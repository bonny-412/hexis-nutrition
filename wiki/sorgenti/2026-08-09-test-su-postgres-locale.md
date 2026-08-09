# Fonte — Test di integrazione su PostgreSQL locale (9 agosto 2026)

Tipo: conversazione con Andrea, stessa sessione della migrazione a repo unico.

## Richiesta

> Okay, ho cambiato idea. Per il DB lo creo io a mano sul pc con PGAdmin.

Chiarita l'ambiguità: creare il database a mano risolve l'esecuzione dell'**applicazione**, ma non i **test**, che usavano Testcontainers e quindi richiedevano comunque Docker, ignorando il database creato in pgAdmin.

Alla domanda su come gestire i test, con tre opzioni (lasciare Testcontainers e aspettare Docker / spostare i test su Postgres locale / doppia modalità con fallback), Andrea ha scelto: **test su Postgres locale**.

## Stato della macchina rilevato

- Servizio `postgresql-x64-13` **in esecuzione**, in ascolto su `localhost:5432`. Versione **PostgreSQL 13**, mentre Testcontainers usava `postgres:16-alpine`.
- `psql` presente in `C:\Program Files\PostgreSQL\13\bin\psql.exe`.
- Utente `hexis` **non esistente** al momento della decisione (autenticazione fallita): da creare a mano insieme ai due database.
- JDK 21 in `C:\Program Files\Java\jdk-21` (il JDK di default di sistema resta Java 8). Maven in `C:\Maven\apache-maven-3.9.2`.
- Docker non disponibile: è la ragione per cui la suite non era mai stata eseguita (vedi voce di log dell'8 agosto 2026).

## Configurazione concordata

- `application.yml` (già esistente, non modificato) punta a `jdbc:postgresql://localhost:5432/hexis`, utente `hexis`, password `hexis` — quindi per l'app basta creare quel database e quell'utente.
- Per i test, un secondo database `hexis_test`, con profilo Spring `test` e override via variabili d'ambiente `TEST_DB_URL` / `TEST_DB_USERNAME` / `TEST_DB_PASSWORD`.
- Entrambi i database vanno creati **vuoti**: le tabelle le crea Flyway.
