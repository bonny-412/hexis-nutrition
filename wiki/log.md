# Log — hexis-nutrition

## [2026-08-08] ingest | Scope e stack iniziali del progetto

Prima definizione del progetto: scopo (SaaS gestione studio nutrizionisti, area professionista + area cliente) e stack tecnico (Spring Boot, JWT, Flyway, Testcontainers; due frontend Vue 3 con shadcn-vue+Tailwind; PostgreSQL). Creata la struttura iniziale della wiki: `stato.md`, `architettura.md`, `modello-dati.md`, `api-contracts.md`, `glossario.md`, `domande-aperte.md`, `decisioni/0001-stack-tecnologico.md`. Fonte: [sorgenti/2026-08-08-scope-e-stack-iniziali.md](sorgenti/2026-08-08-scope-e-stack-iniziali.md).

## [2026-08-08] ingest | Brainstorming: scope funzionale e design "Fondamenta"

Sessione di brainstorming sul nucleo funzionale del prodotto: definiti i flussi principali (piani alimentari + monitoraggio per il professionista; consultazione piani + chat + misurazioni per il paziente), risolta la distinzione terminologica "cliente" (business, il professionista) vs "paziente" (persona seguita), e deciso di dividere lo scope in 4 sotto-progetti sequenziali (Fondamenta → Piano alimentare → Monitoraggio → Chat). Progettato in dettaglio il primo sotto-progetto, "Fondamenta" (autenticazione JWT con ruoli, professionisti creati manualmente, anagrafica paziente, invito via email con Resend, reset password self-service) — vedi [decisioni/0002-autenticazione-e-onboarding](decisioni/0002-autenticazione-e-onboarding.md). Aggiornate le pagine architettura, modello-dati, api-contracts, glossario, domande-aperte, stato. Fonte: [sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md](sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md).

## [2026-08-08] handoff | Piano di implementazione backend "Fondamenta" scritto, in attesa di esecuzione

Scritto (skill `superpowers:writing-plans`) il piano di implementazione TDD per il backend del sotto-progetto "Fondamenta": `backend/docs/superpowers/plans/2026-08-08-fondamenta-backend.md`. 9 task in ordine di dipendenza — bootstrap Maven/Spring Boot 3.3.4/Java 21 con Testcontainers, entità Professionista/Paziente/TokenAzione con migrazioni Flyway, JwtService, EmailSender con integrazione Resend, SecurityConfig con filtro JWT e ruoli, endpoint di autenticazione (login/password-dimenticata/reset-password), endpoint pazienti/inviti (crea/lista/dettaglio/invito/attiva) — ciascuno con test scritti prima del codice. Piano non ancora eseguito: nessun codice scritto nei repo. Aggiornato `stato.md` con il prossimo passo consigliato per la sessione successiva (scegliere tra `subagent-driven-development` ed `executing-plans` per eseguire il piano).

## [2026-08-08] handoff | Piano "Fondamenta" (backend) eseguito interamente, in attesa di test reali e commit

Eseguito il piano di implementazione con `superpowers:subagent-driven-development`: tutti i 9 task implementati e revisionati singolarmente (un subagent implementer + un subagent reviewer per task, verificati anche indipendentemente dal controller), seguiti da una revisione finale whole-branch (modello opus) e un'unica ondata di fix per i finding trovati (1 Critical, 4 Important, 2 Minor), con re-review mirata che ha confermato tutti i finding risolti senza nuove rotture.

Adattamenti rispetto al processo standard, concordati con Andrea: (1) niente `git commit` durante l'esecuzione — solo `git add`, i commit li fa Andrea manualmente; le review hanno usato `git diff HEAD -- <file>` invece di intervalli di commit, dato che `HEAD` è rimasto fisso all'unico commit preesistente per tutta l'esecuzione; (2) Docker non disponibile nell'ambiente di sviluppo, quindi tutti i test basati su `AbstractIntegrationTest` (Testcontainers/Postgres reale) sono stati verificati solo per compilazione e ritracciati a mano, mai eseguiti per davvero — solo i test senza dipendenza da Postgres (`JwtServiceTest`, `ResendEmailSenderTest`) sono stati eseguiti realmente; (3) scoperto e corretto durante l'esecuzione che il JDK di sistema di default è Java 8 mentre il progetto richiede Java 21 (installato ma non di default) — ogni comando Maven richiede `JAVA_HOME` esplicito.

Trovato e corretto durante l'esecuzione un errore nel testo del piano stesso (import sbagliato di `UsernamePasswordAuthenticationFilter` nel Task 7). Trovato dalla revisione finale e risolto nella fix wave: JWT secret con default insicuro in `application.yml` (ora l'app non si avvia senza `JWT_SECRET` impostato), colonne `TIMESTAMP` invece di `TIMESTAMPTZ` per campi `Instant` (rischio di scadenza token calcolata erroneamente su timezone non-UTC), enumerazione email possibile tramite fallimento di invio email nel reset password, mancanza di `@Transactional` su operazioni multi-scrittura, 4 test mancanti su percorsi di sicurezza critici (ora aggiunti, incluso un test end-to-end invito→attivazione→login). Aggiornate `api-contracts.md` e `modello-dati.md` per riflettere l'implementazione reale.

**Aperto per la prossima sessione**: nessun test è mai stato eseguito con Postgres reale (solo compilazione + ritracciamento a mano) — va eseguita `mvn test` con Docker attivo prima di fidarsi del codice o di committare. Vedi `stato.md` per i passi dettagliati.

## [2026-08-09] ingest | Migrazione a un repo git unico per progetto

I quattro repo GitHub separati (`backend`, `frontend-professionisti`, `frontend-cliente`, `wiki`) sono stati sostituiti da un unico repo `bonny-412/hexis-nutrition`, con le rispettive cartelle diventate sottocartelle dello stesso repo. Storia precedente **non** preservata (scelta esplicita: 7 commit con messaggi poco informativi, `git subtree` giudicato non conveniente); il monorepo parte dal commit iniziale `70e2141`, i quattro repo vecchi sono stati archiviati su GitHub e restano l'unico posto dove leggere la storia precedente. Andrea ha poi confermato che la regola vale per **tutti** i progetti del workspace: vedi [decisioni/0003-repo-unico-per-progetto](decisioni/0003-repo-unico-per-progetto.md).

Aggiornati di conseguenza: `CLAUDE.md` del workspace (sezioni *Struttura*, *Nuovo progetto*, *Come aprire una sessione*) e di progetto, `.gitignore` del repo radice (ora ignora `progetti/*/`), e la conseguenza obsoleta di [decisioni/0001](decisioni/0001-stack-tecnologico.md) che parlava di "tre repository indipendenti da mantenere". Corretto anche `stato.md`, che dava il backend di "Fondamenta" come "in staging, mai committato": ora è committato e pushato. Aggiunta una domanda aperta su CI con filtri di percorso in caso di rilasci indipendenti. Fonte: [sorgenti/2026-08-09-migrazione-a-repo-unico.md](sorgenti/2026-08-09-migrazione-a-repo-unico.md).

## [2026-08-09] ingest | Test spostati su PostgreSQL locale — suite eseguita per la prima volta, 33 test verdi

Andrea ha deciso di creare i database a mano con pgAdmin invece di usare Docker. Chiarito che questo copriva l'esecuzione dell'**app** ma non i **test**, che con Testcontainers avrebbero continuato a richiedere Docker ignorando il database creato a mano; scelta quindi la migrazione dei test su Postgres locale — vedi [decisioni/0004-test-su-postgres-locale](decisioni/0004-test-su-postgres-locale.md).

Modifiche al backend: rimosse le dipendenze Testcontainers e il BOM da `pom.xml`; `AbstractIntegrationTest` non avvia più container, attiva il profilo `test` e svuota le tabelle con `TRUNCATE ... RESTART IDENTITY CASCADE` in `@BeforeEach` (i test pulivano solo in `@AfterEach`, insufficiente ora che il database è persistente); nuovo `backend/src/test/resources/application-test.yml` che punta a `hexis_test` con override via `TEST_DB_URL`/`TEST_DB_USERNAME`/`TEST_DB_PASSWORD` e `clean-on-validation-error` per ricreare lo schema quando una migrazione già applicata cambia.

**Risultato: `mvn test` → 33 test, 0 fallimenti, 0 errori, BUILD SUCCESS.** È la **prima esecuzione reale** della suite da quando il backend è stato scritto: fino a ieri era stata solo compilata e ritracciata a mano. Verificato anche che le migrazioni V1–V3 si applicano su database reale e che passa `resetPasswordConTokenScadutoRestituisceErrore`, il test più sensibile al fix `TIMESTAMPTZ`.

Da tenere presente: i test girano su **PostgreSQL 13.23** locale, non più su `postgres:16-alpine` — differenza registrata in [domande-aperte](domande-aperte.md). Aggiornati `decisioni/0001`, `CLAUDE.md` di progetto, `stato.md`, `api-contracts.md`, `modello-dati.md` e il piano di implementazione dell'8 agosto (nota in testa: la sua sezione su Testcontainers è superata). Fonte: [sorgenti/2026-08-09-test-su-postgres-locale.md](sorgenti/2026-08-09-test-su-postgres-locale.md).

## [2026-08-09] ingest | Docker previsto solo per la produzione

Precisazione di Andrea sulla portata della decisione precedente: il PostgreSQL locale creato con pgAdmin non è un ripiego in attesa di Docker, ma la modalità prevista per **tutta la fase di sviluppo** e per le verifiche manuali del prodotto, che curerà lui sulla stessa macchina. **Docker si userà in produzione**, dove però non è ancora deciso nulla (immagini, orchestrazione, provider, gestione delle variabili d'ambiente).

Aggiornati: [decisioni/0004](decisioni/0004-test-su-postgres-locale.md) con la portata della scelta, [architettura](architettura.md) con una sezione "Ambienti" che distingue sviluppo/test da produzione, e [domande-aperte](domande-aperte.md) — dove la questione della versione PostgreSQL si precisa: dato che la produzione girerà in Docker, l'immagine è una scelta libera e fissarla su `postgres:13` è il modo più semplice per non avere ambienti disallineati. Fonte: [sorgenti/2026-08-09-docker-solo-in-produzione.md](sorgenti/2026-08-09-docker-solo-in-produzione.md).

## [2026-08-09] query | A cosa serve la tabella `token_azione`

Domanda di Andrea sul ruolo di `token_azione`, dopo il primo avvio dell'app e l'inserimento manuale del primo professionista. Risposta ricostruita leggendo il codice (`TokenAzione`, `AuthService`, `PazienteService`, `V3__create_token_azione.sql`): è la tabella dei permessi temporanei per chi non può autenticarsi — il paziente invitato che non ha ancora una password e chiunque debba reimpostarla — dove la prova d'identità è il possesso della casella email. Un `tipo` (`INVITO` / `RESET_PASSWORD`), un riferimento alternativo a professionista **oppure** paziente (vincolo CHECK), scadenza di 7 giorni per l'invito e 1 ora per il reset, flag `usato` che lo rende monouso. Il filtro sul tipo in `resetPassword`/`attiva` impedisce di spendere un token di invito su un endpoint di reset.

Dalla lettura sono emersi tre comportamenti mai discussi in fase di progettazione, spostati in [domande-aperte](domande-aperte.md): token salvati in chiaro, nessuna pulizia dei token scaduti/usati, richieste di reset ripetute che non invalidano le precedenti.

La risposta è stata archiviata come [moduli/inviti-e-token](moduli/inviti-e-token.md) — **prima pagina della cartella `moduli/`**, finora vuota — con struttura della tabella, i due flussi endpoint per endpoint, e la spiegazione delle due condizioni di validità (`isValido` + filtro sul tipo) e di cosa proteggono. Aggiunta a [index](index.md).

## [2026-08-09] handoff | Repo unico su GitHub, suite di test finalmente verde, backend in esecuzione

Sessione dedicata all'infrastruttura, non a nuove funzionalità: nessuna riga di logica applicativa è cambiata.

**Fatto**: (1) i quattro repo GitHub separati sono diventati un solo repo `bonny-412/hexis-nutrition`, i vecchi archiviati, schema del workspace aggiornato — [decisioni/0003](decisioni/0003-repo-unico-per-progetto.md); (2) test migrati da Testcontainers a un PostgreSQL locale e **suite eseguita per la prima volta in assoluto: 33 test, 0 fallimenti** — [decisioni/0004](decisioni/0004-test-su-postgres-locale.md); (3) primo avvio dell'app da Spring Tool Suite, tabelle create su `hexis`, primo account professionista inserito a mano; (4) archiviata [moduli/inviti-e-token](moduli/inviti-e-token.md).

**Stabilito su come lavorare**: i test automatici sono responsabilità dell'agente dopo ogni modifica al backend (regola scritta in `backend/CLAUDE.md`, così vale anche nelle sessioni future); le verifiche manuali del prodotto le fa Andrea, e l'agente non avvia l'app per provarla; i commit restano solo di Andrea.

**Aperto**: i tre punti su `token_azione` (token in chiaro, nessuna pulizia, reset multipli non invalidanti), la versione di PostgreSQL per la produzione, il deploy in Docker, e la convenzione sulle credenziali nella wiki. Il flusso reale del prodotto non è ancora stato provato contro l'app in esecuzione.

**Prossimo passo**: scrivere i piani per i due frontend di "Fondamenta" — vedi [stato](stato.md), riscritto in questa sessione per essere l'unico file necessario a ripartire.
