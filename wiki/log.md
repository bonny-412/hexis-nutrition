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

## [2026-08-30] query+ingest | Chiusi i tre punti aperti su `token_azione`

Ripresa dai punti aperti del 9 agosto: Andrea ha confermato tutte e tre le proposte (hash, pulizia, invalidazione). Bounded task, design approvato in chat (skill `superpowers:brainstorming`), implementato con `superpowers:test-driven-development` in tre cicli RED→GREEN.

**Fatto**: (1) `TokenAzione` genera ancora un `UUID.randomUUID()` ma persiste solo il suo hash SHA-256 in `token_hash` (rinominata da `token`, migrazione V4), tenendo il valore in chiaro in un campo `@Transient` valido solo sull'istanza appena creata; (2) rimosso il flag `usato` — un token consumato viene cancellato subito in `resetPassword`/`attiva`, più il nuovo componente schedulato `TokenAzionePulizia` (`@Scheduled` notturno) che cancella i token scaduti mai usati; (3) `AuthService.richiediResetPassword` ora cancella i `RESET_PASSWORD` precedenti della stessa persona prima di crearne uno nuovo. Un test end-to-end preesistente che recuperava il token ricaricandolo dal DB è stato corretto per estrarlo dal corpo dell'email (con l'hash il valore in chiaro non è più recuperabile da un'entità ricaricata). `mvn test` → **36 test, 0 fallimenti, BUILD SUCCESS**.

Aggiornate: [moduli/inviti-e-token](moduli/inviti-e-token.md) (struttura, i due flussi, rimossa la sezione "punti aperti"), [domande-aperte](domande-aperte.md) (i tre punti spostati in "Risolte"), `stato.md`. Fonte: [sorgenti/2026-08-30-hash-pulizia-invalidazione-token](sorgenti/2026-08-30-hash-pulizia-invalidazione-token.md).

**Aperto**: versione di PostgreSQL per la produzione, deploy in Docker, convenzione sulle credenziali nella wiki, piani frontend mai iniziati. Modifiche pronte in staging (`git add` fatto), commit da fare da parte di Andrea.

## [2026-08-30] handoff | Frontend-professionisti "Fondamenta" costruito, shadcn-vue adottato, pulizia CSS completata

Piano di 16 task per `frontend-professionisti/` (login, dashboard, pazienti, reset password, shell con sidebar/header) eseguito con `superpowers:subagent-driven-development` in worktree isolato, poi merge locale su master (un conflitto risolto in `AuthControllerTest.java`). A metà sessione Andrea ha segnalato forte infedeltà rispetto al mockup fornito (`Hexis Nutrition.zip`) e ha richiesto l'adozione di **shadcn-vue + Reka UI + `@lucide/vue`** (previsti in [decisioni/0001](decisioni/0001-stack-tecnologico.md) ma inizialmente rimandati per YAGNI): riconvertiti in un solo passaggio tutti i componenti/view esistenti, sostituendo CSS scritto a mano e icone emoji. Corrette più volte, su segnalazione di Andrea, la responsività di sidebar/header (drawer mobile ora basato sul componente `Sheet`, portal-based, non più CSS hand-rolled) e il comportamento del menu profilo (ora `DropdownMenu` di Reka UI, con click-outside/Escape nativi). Stabilita e applicata ovunque la convenzione: mai `style="..."` inline, sempre classi Tailwind (sintassi arbitraria per le variabili CSS custom, utility `font-heading` per i titoli).

Password dell'account di sviluppo reimpostata (Andrea l'aveva dimenticata), nuovo hash generato al volo senza mai committare codice di scarto. Login verificato manualmente da Andrea.

Verifiche finali di questa sessione: `npm run test` → **54 test, 0 fallimenti**; `npm run build` → pulito; confermato che `.font-heading{font-family:Fraunces,serif}` è presente nel CSS compilato. Tutto lo staging del repo (backend token_azione + questo frontend) è pronto con `git add`, nessun commit fatto. Aggiornato `stato.md`.

**Aperto**: `frontend-cliente/` non ancora iniziato; i flussi di creazione/invito paziente e reset password non ancora provati a mano contro l'app in esecuzione; versione PostgreSQL per produzione e deploy Docker restano da decidere.

## [2026-08-30] handoff | Rifiniture UI su shell e dashboard di `frontend-professionisti`

Sessione di correzioni e rifiniture visive richieste una alla volta da Andrea, nessuna modifica di struttura o di flusso.

**Fatto**: (1) risolta una linea verticale bianca nella sidebar mobile (`AppShell.vue`) — il `Sheet` di shadcn applica `data-[side=left]:border-r`/`w-3/4` di default, gli override precedenti (`border-0`, `w-[246px]`) non avevano lo stesso scope di variante e `tailwind-merge` non li deduplicava, lasciando il bordo visibile per specificità CSS; corretto usando override con lo stesso prefisso `data-[side=left]:`, verificato empiricamente con `twMerge` in Node prima e dopo il fix; (2) freccia del menu profilo (`AppHeader.vue`) resa dinamica (rotazione via `group-data-[state=open]`), stesso pattern poi riusato sul bottone "Crea nuovo" della dashboard; (3) su schermi piccoli il bottone profilo mostra solo l'avatar (bordo/sfondo/padding spostati a `lg:`-only); (4) logo+"Hexis" mobile centrato nell'header e ingrandito; (5) `DashboardView.vue`: saluto "Bentornata" (gendered) sostituito con "Ciao, {nome}" + data odierna estesa in italiano; aggiunto bottone "Crea nuovo" con dropdown (Nuovo paziente attivo, Nuovo appuntamento/Nuovo piano alimentare disabilitati, ciascuno con icona), dimensioni tarate più volte su richiesta di Andrea.

Test esistenti (`AppShell.spec.ts`, `AppHeader.spec.ts`, `DashboardView.spec.ts`) verificati verdi dopo ogni modifica, più `tsc --noEmit` pulito. Nessuna verifica visiva in browser reale (Claude in Chrome non collegato in questa sessione): correzioni basate su lettura del codice e, per il bug della linea bianca, su verifica empirica del comportamento di `tailwind-merge`. Aggiornato `stato.md`.

**Aperto**: come sopra, più l'indicazione esplicita di Andrea per la prossima sessione — sulla sezione **Pazienti**, prima le modifiche di UI e poi quelle di struttura (dettagli da raccogliere a inizio sessione). Modifiche pronte in staging (`git add` fatto), commit da fare da parte di Andrea.

## [2026-08-31] handoff | "Nuovo paziente con visita": anagrafica estesa + prima visita, backend e frontend

Sessione completa: brainstorming (percorso architetturale) → spec → piano → esecuzione con `superpowers:subagent-driven-development` in un worktree isolato (6 task, ognuno implementato e revisionato da subagent separati, più revisione finale whole-branch e una fix wave). Spec: [sorgenti/2026-08-31-nuovo-paziente-con-visita-design.md](../docs/superpowers/specs/2026-08-31-nuovo-paziente-con-visita-design.md) (nota: vive in `docs/superpowers/specs/`, non in `wiki/sorgenti/`). Piano: `docs/superpowers/plans/2026-08-31-nuovo-paziente-con-visita.md`.

**Fatto — backend**: nuova entità `Visita` (tabella `visite`, migrazione V5, additiva) con altezza, peso e 12 misure di circonferenza (vita, ombelico, fianchi, petto, coscia dx/sx, polpaccio dx/sx, larghezza spalle, circonferenza spalle, bicipite dx/sx) — solo altezza e peso obbligatori. `Paziente` perde `altezzaCm` (spostata su `Visita`, storicizzata per visita) e guadagna `lavoro` (testo libero) e `tipoLavoro` (enum `SEDENTARIO`/`POCO_ATTIVO`/`ATTIVO`/`MOLTO_ATTIVO`) — migrazione V6. `POST /pazienti` esteso: payload con `visita` obbligatoria, crea `Paziente`+`Visita` nella stessa transazione, 400 se `visita` manca o se altezza/peso al suo interno sono nulli o fuori range (`@Positive`, limiti aggiunti nella fix wave finale). ~19 chiamate `new Paziente(...)` nei test corrette per il nuovo costruttore a 9 argomenti.

**Fatto — frontend**: `PazienteNuovoView.vue` riscritta con due card — "Dati anagrafici" (nome, cognome, sesso, email, telefono, data di nascita, lavoro, tipo lavoro) e "Dati della visita" (altezza, peso, le 12 circonferenze, sotto-sezione placeholder "Misurazione BIA — Sarà disponibile a breve", nessun campo BIA reale). Aggiunto il componente shadcn-vue `select` (non ancora presente in questo frontend).

**Fatto — qualità**: revisione finale whole-branch (modello Opus) ha verificato la catena a 14 campi end-to-end senza trasposizioni, la sicurezza del ruolo PROFESSIONISTA intatta, e trovato 4 Important + alcuni Minor, tutti chiusi in un'unica fix wave (validazione di range, test sulla mappatura completa delle 14 misure sia backend sia frontend, test mancante per `pesoKg` nullo, tipo TypeScript di `tipoLavoro` stretto all'union, wiki aggiornata). Test finali: backend 48/48 (`mvn test`, BUILD SUCCESS), frontend 55/55 + `tsc --noEmit` pulito.

**Merge**: fatto in locale su `master` — **da correggere un errore mio**: il primo tentativo con `git merge` ha fatto un fast-forward che ha portato i commit del worktree direttamente nella storia di `master` (violazione della regola "commit solo di Andrea"); corretto con `git reset --soft` al commit precedente, così tutte le modifiche sono di nuovo in staging senza alcun commit. Nel merge, scartata (con conferma di Andrea) una modifica non committata preesistente su `PazienteNuovoView.vue` — verificato che era lo stesso styling (breadcrumb, box errore, card) già incluso ed esteso dalla nuova versione del file, nessuna perdita di lavoro.

**Aperto**:
- **`LoginView.vue`/`LoginView.spec.ts`**: 2 test falliscono (`Failed to resolve component: Loader2`) a causa di una redesign in corso, non committata e preesistente a questa sessione, di `LoginView.vue` (icona `Loader2` referenziata ma non importata o markup incompleto) — **non toccata da questa sessione**, non è una regressione introdotta qui. Da riprendere/completare da parte di Andrea.
- **`AppSidebar.vue`/`DashboardView.vue`**: altre modifiche non committate preesistenti, non toccate da questa sessione, ancora in sospeso.
- **Verifica manuale in browser** (mai fatta dall'agente, per convenzione): in particolare i due `Select` (Sesso, Tipo lavoro) — inizializzati a `''`, da verificare che il placeholder "Seleziona" si comporti correttamente con reka-ui.
- **Minor parcheggiati** (non bloccanti, vedi ledger ormai cancellato col worktree — riassunti qui): nessun `CHECK` DB su `tipo_lavoro`; `min="0"` frontend sulle circonferenze non coerente con `@Positive` backend (0 passa la validazione HTML ma dà 400 dal server); bump non revisionato di `@lucide/vue` nel lockfile (effetto collaterale del CLI shadcn-vue, innocuo); `wiki/api-contracts.md` cita ancora "33 test verdi" ancorato alla data 2026-08-09 (il numero reale a quella data era 33, ma oggi sono 48 — andrebbe scorporato dalla data o aggiornato).
- Nessuna lettura dei nuovi campi (`lavoro`, `tipoLavoro`, dati di visita) da nessuna view: scelta deliberata, il percorso di lettura è del futuro sotto-progetto "Monitoraggio".

## [2026-08-31] handoff | Bug fix controlli `PazienteNuovoView` + UX (maiuscola, errori live)

Sessione di debug e piccole feature su `PazienteNuovoView.vue`, seguita con `superpowers:systematic-debugging` per il bug e TDD per entrambi gli interventi. Nessuna modifica al backend.

**Bug fix — i controlli non bloccavano nulla a video**: Andrea ha segnalato che si poteva digitare qualsiasi carattere in qualsiasi campo. Causa radice trovata risalendo nel componente shadcn `Input.vue`: usa `useVModel(props, 'modelValue', emit, { passive: true })` di `@vueuse/core`, che tiene il valore visualizzato in un ref interno sincronizzato dalla prop tramite un `watch` — e i `watch` di Vue non scattano se il nuovo valore è identico (`Object.is`) al precedente. Quando un carattere non valido veniva filtrato producendo lo *stesso* valore già presente nel campo (tipicamente: campo vuoto, si digita un carattere non ammesso, il filtro lo rimuove tornando a `''`), il ref del genitore non cambiava, la prop passata a `Input` non cambiava, e il campo restava visivamente "sporco" col carattere rifiutato — finché non arrivava un tasto che produceva un valore realmente diverso (da qui l'effetto "sembra che i controlli non funzionino mai", anche se il valore *validato* in memoria era corretto). Il test preesistente non lo intercettava perché usa `setValue()` (un solo evento con la stringa finale) invece di simulare la digitazione tasto per tasto.

**Fix**: `conFiltro` ora passa per un valore intermedio con un carattere invisibile a larghezza zero quando il valore filtrato coinciderebbe col precedente, forzando comunque la propagazione reattiva verso il campo. Nuovo test che digita tasto per tasto (incluse ripetizioni di caratteri non ammessi in fila) su campo lettere e campo decimale, prima rosso poi verde.

**Feature 1 — l'errore di un campo sparisce non appena corretto**: `conFiltro` accetta ora opzionalmente una `chiave` e una funzione di validazione; dopo ogni digitazione, se il campo aveva un errore attivo e il nuovo valore lo risolve, l'errore viene rimosso da `errori` (mai aggiunto in anticipo mentre si digita, solo rimosso — comportamento non richiesto da Andrea). Le funzioni di validazione per campo (`erroreNome`, `erroreTelefono`, `erroreCirconferenza`, ecc.) sono condivise con `validaCampi()` (chiamata al submit), niente logica duplicata. Applicato a tutti i 18 campi validati del form (anagrafica + visita + le 12 circonferenze), non solo ai tre citati da Andrea nella richiesta.

**Feature 2 — maiuscola automatica su Nome, Cognome, Lavoro**: nuova `capitalizzaPrimaLettera`, composta col filtro caratteri esistente per Nome/Cognome tramite un piccolo `componiFiltri`. Il campo "Lavoro" prima usava `v-model` diretto senza passare da `conFiltro`: convertito allo stesso pattern `:model-value`/`@update:model-value` degli altri campi (senza aggiungere restrizioni sui caratteri, solo la capitalizzazione, come richiesto).

**Verifica**: 3 nuovi test (capitalizzazione live sui tre campi, sparizione errore "obbligatorio", sparizione errore di formato) più il test di riproduzione del bug — tutti confermati rossi prima del fix, verdi dopo. Suite frontend completa: **57/57 verdi** (esclusi i 2 test di `LoginView.spec.ts`, preesistenti e non toccati da questa sessione, vedi sotto). `tsc --noEmit` pulito. Nessuna verifica manuale in browser (per convenzione, tocca ad Andrea).

**Segnalato ma non affrontato (fuori scope)**: il backend valida bene i campi numerici della visita (`@Positive`, `@Digits`, `@Max`), ma `nome`, `cognome`, `telefono`, `sesso` e `lavoro` in `CreaPazienteRequest` non hanno alcun pattern lato server — chi chiama l'API direttamente, bypassando il frontend, può ancora inserire qualsiasi carattere in quei campi. Spostato in [domande-aperte](domande-aperte.md).

**Aperto**: tutti i punti già aperti nell'handoff precedente (LoginView.vue/AppSidebar.vue/DashboardView.vue con modifiche non committate preesistenti, verifica manuale dei due `Select`, gap di validazione server-side sopra) restano tali, non toccati da questa sessione. Andrea riparte dal **salvataggio di un paziente** (revisione/commit del lavoro in staging) e la prossima sessione lavorerà sulla **pagina della lista dei pazienti** (`PazientiListView.vue`).

## [2026-09-01] handoff | Pagina "Nuovo paziente": età, componente visita e data di nascita obbligatoria

Tre richieste di Andrea sulla pagina `PazienteNuovoView.vue`, ciascuna con brainstorming bounded (design breve in chat, approvato) → TDD.

**1. Età automatica**: nuovo `calcolaEta(dataNascita)` in `frontend-professionisti/src/utils/data.ts`, campo "Età" sola lettura nei dati anagrafici, subito dopo "Data di nascita".

**2. Estrazione di "Dati della visita" in componente autonomo**: `frontend-professionisti/src/components/pazienti/DatiVisitaForm.vue`, motivata da Andrea con il riuso futuro in una pagina "aggiungi visita" su un paziente esistente. Design scelto (chiesto esplicitamente ad Andrea con `AskUserQuestion`, tra due opzioni): componente **autonomo** con stato e validazione interni, che espone `valida()`/`ottieniDati()` via `defineExpose`, invece di uno stateless con 16 v-model passati dal genitore — meno boilerplate al riuso.

**3. Data di nascita resa obbligatoria**: Andrea vuole l'età sempre calcolabile in modo affidabile. Scelta discussa e confermata con `AskUserQuestion`: **non** persistere l'età come colonna (andrebbe disallineata a ogni compleanno del paziente) — solo garantire che `dataNascita` sia sempre presente, età calcolata al volo dove serve. Impatto più ampio del previsto: 14 istanziazioni dirette di `Paziente` in 5 file di test backend passavano `null` come data di nascita solo per costruire un paziente di supporto (non erano test sulla data di nascita), più 4 body JSON in `PazienteControllerTest` che la omettevano aspettandosi 201 — tutti aggiornati con una data placeholder o esplicita. Migrazione Flyway V8 (`data_nascita SET NOT NULL`), `@NotNull` su `CreaPazienteRequest.dataNascita`, nuovo validatore `erroreDataNascita` lato frontend con lo stesso pattern UX degli altri campi obbligatori (errore che sparisce alla compilazione).

**Verifica**: backend 52/52 verdi (`mvn test` su `hexis_test`); frontend 71/73 verdi (`npx vitest run`) — i 2 falliti sono i test preesistenti e scollegati di `LoginView.spec.ts`, già noti dal 31 agosto; `npm run build` pulito. Nessuna verifica manuale in browser: nessun tool di automazione browser collegato in questa sessione, segnalato esplicitamente ad Andrea invece di darla per buona.

**Segnalato, non risolto**: la migrazione V8 non è stata verificata contro il database `hexis` reale (solo `hexis_test`) — se un paziente esistente ha `data_nascita` nulla, l'avvio dell'app fallirà finché non viene sistemato a mano. Spostato in [domande-aperte](domande-aperte.md).

**Handoff esplicito**: Andrea ha chiesto di salvare tutto (fatto `git add`, nessun commit) perché apre una nuova sessione per continuare a modificare i dati della visita — presumibilmente su `DatiVisitaForm.vue`, appena estratto. Dettagli specifici non ancora raccolti. Aggiornati `stato.md`, `modello-dati.md`, `api-contracts.md`, `domande-aperte.md`.

**Chiuso subito dopo**: Andrea ha confermato che la tabella `pazienti` in `hexis` (database reale) è vuota, quindi il rischio segnalato sulla migrazione V8 non si applica — spostato in "Risolte" in [domande-aperte](domande-aperte.md), rimosso il punto d'attenzione da qui sopra e dal prossimo passo consigliato.

## [2026-09-01] handoff | Modulo Plicometria e redesign Circonferenze

Sessione nella stessa giornata del componente `DatiVisitaForm.vue`/data di nascita obbligatoria, ma a parte: Andrea ha portato 6 PDF di specifica (letti e analizzati in sequenza, con due correzioni fatte da Andrea stesso durante il brainstorming — prima l'ampliamento da 4 a 6 protocolli plicometrici con Slaughter/Evans, poi la correzione a mano della formula di Evans dopo che era stata segnalata un'incoerenza) per un modulo Plicometria (stima massa grassa da pliche cutanee) e il redesign del modulo Circonferenze. Percorso completo: brainstorming architetturale → spec (`docs/superpowers/specs/2026-09-01-plicometria-circonferenze-design.md`) → piano (`docs/superpowers/plans/2026-09-01-plicometria-circonferenze.md`, 10 task) → esecuzione con `superpowers:subagent-driven-development`.

**Scelta di ambiente non standard**: eseguito **direttamente su `master`, senza worktree**, su richiesta esplicita di Andrea — `master` aveva già modifiche in staging non committate (data di nascita obbligatoria, `DatiVisitaForm.vue`) che un worktree creato da `HEAD` non avrebbe incluso, disallineando il piano dal codice reale.

**Backend**: tabelle `plicometrie` e `durnin_womersley_coefficienti` (dati di riferimento seminati da migrazione); motore di calcolo a strategy pattern con un calcolatore per protocollo (Jackson-Pollock 3/7, Durnin-Womersley 4, Faulkner 4, Slaughter pediatrico, Evans atleti); `PlicometriaService` con validazione, calcolo (età, densità, `%BF`, FM/FFM/FMI/FFMI), limite di sicurezza biologico e persistenza storicizzata; `Visita` ridisegnata (11 circonferenze a misura singola, `protocollo_vita`, BMI/WHR/WHtR/MAMC calcolati); `Paziente.sesso` reso obbligatorio a 3 valori (M/F/ALTRO).

**Frontend**: sesso a 3 valori, redesign circonferenze in `DatiVisitaForm.vue`, nuovi `PlicaInput.vue` (tripla misurazione con media live) e `PlicometriaForm.vue` (campi condizionati da protocollo/sesso).

**Adattamento di processo**: la skill `subagent-driven-development` assume commit ad ogni task (SHA per i diff) — in conflitto con la regola assoluta del progetto. Adattato per tutta l'esecuzione: solo `git add`, mai commit; review package generati con `git diff --cached` mirato ai file di ciascun task.

**Incidente gestito durante l'esecuzione**: l'implementer del Task 7 (sesso frontend) si è interrotto per un rate limit di sessione a metà lavoro, dopo aver trovato un difetto reale nel piano (il `Select` del sesso non cancellava l'errore alla selezione). Verificato nessuna perdita di dati, ricomposto lo stato di staging (`git add -A`), dispatchato un nuovo implementer con la correzione decisa dal coordinatore.

**Revisione finale whole-branch** (Opus): "ready to merge, with fixes". Tutte e 6 le formule cliniche verificate a mano (nessun errore), parità di contratto backend↔frontend confermata su tutti i DTO, sequenza delle 4 migrazioni verificata contro lo schema reale. Trovati 3 problemi importanti, risolti in un'unica fix wave: pliche di un protocollo precedente che sopravvivevano al cambio protocollo (fix sia in `PlicometriaService` sia in `PlicometriaForm.vue`); i messaggi d'errore dei nuovi 400 non arrivavano mai al client (`server.error.include-message` di default `never`); nessun minimo su `altezzaCm` (500 invece di 400 su valori estremi). Più 3 correzioni minori economiche (un `@DecimalMax` reso morto da un `@Digits` troppo stretto — difetto del piano; un'asimmetria di tipo su `dataNascita` nel client; un commento chiarificatore). Re-review mirata: tutti i 6 finding risolti, nessuna nuova rottura. Due domande di prodotto (accoppiamento MAMC↔protocollo; incoerenza densità/`%BF` al limite di sicurezza) spostate in [domande-aperte](domande-aperte.md) invece di essere decise unilateralmente.

**Verifica**: backend `mvn test` → 89/89 verdi, BUILD SUCCESS (ri-verificato in autonomia dal coordinatore); frontend `npm run test` → 83/85 verdi (stessi 2 test preesistenti e scollegati di `LoginView.spec.ts`, non toccati); `npx tsc --noEmit` pulito. Nessuna verifica manuale in browser.

**Segnalato, non risolto**: nessun test di regressione dedicato per due dei fix della revisione finale (esclusione pliche non richieste dal protocollo; presenza del messaggio nel body del 400) — verificati per ispezione del codice dal reviewer, non da un assert automatico. Spostato in "Cosa resta aperto" in [stato](stato.md).

**Handoff esplicito**: lavoro in staging (`git add` fatto, nessun commit) — tocca ad Andrea. Aggiornati `stato.md`, `modello-dati.md`, `api-contracts.md`, `domande-aperte.md`.

## [2026-09-01] query | Decise le due domande aperte del modulo Plicometria

Discusse con Andrea le due domande di prodotto lasciate aperte dalla revisione finale. **MAMC↔protocollo**: confermato che va bene resti "opportunistico" (calcolabile solo quando il protocollo scelto include comunque la tricipitale), nessuna modifica. **Incoerenza densità/`%BF`**: Andrea ha chiesto un consiglio; proposta e implementata l'opzione di non falsificare mai la densità corporea persistita, aggiungendo invece un flag esplicito `limite_sicurezza_applicato` su `Plicometria` (booleano, migrazione V13) che segnala quando il limite di sicurezza ha corretto il `%BF` e i due valori non si riconciliano più via Siri.

Implementazione TDD diretta (bounded, nessuna orchestrazione multi-agente): nuovo helper `CalcoliPlicometria.limiteSicurezzaApplicato(double, Sesso)` con 2 nuovi test; campo + getter + parametro di costruttore su `Plicometria.java`; wiring in `PlicometriaService.elabora()`; 2 nuovi test in `PazienteControllerTest` (uno che conferma `false` nel caso normale, uno con pliche Evans vicine allo zero che fa scattare il limite e verifica `true` + `percentuale_grasso` flooorata a 3.00). Backend `mvn test` → **92/92 verdi, BUILD SUCCESS**. Aggiornati `modello-dati.md` e `domande-aperte.md` (le due domande spostate in "Risolte").

Lavoro in staging, nessun commit — tocca ad Andrea.

## [2026-09-01] handoff | Prima verifica manuale e rifiniture UI del modulo Plicometria

Andrea ha lanciato di persona `mvn clean install` (backend) e l'avvio del frontend — **entrambi ok**, prima verifica manuale reale contro il lavoro delle sessioni precedenti. Dalla prova pratica sono emerse tre richieste UI, tutte bounded, presentate in chat e approvate prima di implementare:

1. **Plicometria in accordion**: come le Circonferenze, un `AccordionItem` (icona `Percent`) che si apre da sé se la validazione fallisce.
2. **Età sulla stessa riga di Data di nascita**: riordinato il blocco Sesso subito dopo, nella griglia anagrafica a 2 colonne.
3. **Select "Protocollo" e "Protocollo vita" azzerabili**: aggiunta una voce "Seleziona" in cima a entrambe le liste, selezionata di default — come una select HTML normale, su richiesta esplicita di Andrea ("basta aggiungere un valore 'Seleziona' con value null ... come funziona una normale select"). `protocollo_vita` ora parte vuoto invece che precompilato su OMS; se lasciato vuoto il backend applica comunque OMS di default (nessun cambio di comportamento finale, solo visivo).

**Effetto collaterale trovato e corretto**: il test end-to-end della plicometria (`PazienteNuovoView.spec.ts`) falliva dopo il punto 1, perché i campi plica sono ora dentro un accordion chiuso di default — sistemato aprendo l'accordion prima di interagire con `Select`/`Input`, stesso pattern già usato per le circonferenze.

**Verifica**: frontend `npm run test` → **83/85 verdi** (stessi 2 test preesistenti e scollegati di `LoginView.spec.ts`, non toccati), `npx tsc --noEmit` pulito. Nessun test backend interessato (modifiche solo frontend).

**Handoff esplicito**: lavoro in staging (`git add` fatto, nessun commit) — Andrea farà alcune modifiche a mano prima del prossimo avvio di sessione, poi si riparte con la verifica manuale del salvataggio di un paziente e il lavoro sulla lista dei pazienti (`PazientiListView.vue`). Aggiornato `stato.md`.
