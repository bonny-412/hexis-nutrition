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

## [2026-09-02] handoff | Rifiniture Plicometria, fix TypeScript sulle Select, riorganizzazione `views/`

Sessione di piccole correzioni e una richiesta di struttura, tutte presentate una alla volta da Andrea dopo la sua verifica manuale. Nessuna modifica al backend.

**Plicometria — tripla misurazione obbligatoria** (`PlicaInput.vue`): se si seleziona "tripla misurazione", tutte e tre le misure diventano obbligatorie per salvare. Causa radice: il watcher su `mediaCalcolata` emetteva il valore medio solo quando tutte e tre le misure erano compilate, ma **non lo svuotava** se una veniva poi cancellata — un valore medio "congelato" restava valido anche a tripla incompleta. Fix: il watcher ora emette sempre il valore aggiornato (media o stringa vuota) quando la tripla è attiva, così la validazione già esistente in `PlicometriaForm.vue` (campo obbligatorio per il protocollo) blocca correttamente il salvataggio. TDD: test rosso poi verde.

**Errore che sparisce alla correzione** (`PlicometriaForm.vue`): come già avviene altrove nel form, l'errore su una plica ora sparisce non appena il valore viene corretto (nuovo `watch` per campo + helper `erroreCampo` condiviso con `valida()`), senza aspettare un nuovo tentativo di salvataggio.

**Due rifiniture visive su `PlicaInput.vue`**: (1) in caso di errore, anche i tre campi della tripla misurazione si evidenziano di rosso (`aria-invalid`), non solo il campo singolo; (2) la media calcolata ora si mostra con la virgola italiana (`12,57`) invece del punto (`12.57`) — `toFixed(2)` seguito da `.replace('.', ',')`, come già fatto per il valore emesso.

**Fix di 4 errori TypeScript segnalati da Andrea via VS Code** (Volar), tutti su `Select` di reka-ui: (1) tre handler (`onProtocolloChange`, `onProtocolloVitaChange`, `onSessoChange` in `PlicometriaForm.vue`/`DatiVisitaForm.vue`/`PazienteNuovoView.vue`) erano tipizzati `(valore: string) => void`, troppo stretto rispetto al tipo reale emesso da `@update:model-value`, `AcceptableValue` (`string | number | bigint | Record<string, any> | null`) — corretti importando `type { AcceptableValue } from 'reka-ui'`; (2) un quarto errore, apparentemente scollegato (`erroreCampo(campo, valoriPerCampo[campo].value)` con `'string | undefined' non assegnabile a 'string'`), aveva causa diversa: `ReturnType<typeof ref<string>>` **non** risolve a `Ref<string>` come sembrerebbe — TypeScript, su una funzione overloaded come `ref`, seleziona per le instantiation expression l'overload `ref<T = any>(): Ref<T | undefined>` invece di `ref<T>(value: T): Ref<T>`, producendo `Ref<string | undefined>`. Verificato empiricamente con un file di test isolato prima di correggere. Fix: tipo esplicito `Record<Campo, Ref<string>>` (import `type Ref` da `'vue'`).

**Riorganizzazione di `src/views/` in sottocartelle per sezione** (richiesta di Andrea, con `pazienti/` come esempio): spostati con `git mv` (history preservata) `PazientiListView`, `PazienteNuovoView`, `PazienteDettaglioView` (+ spec) in `views/pazienti/`; `LoginView`, `PasswordDimenticataView`, `ResetPasswordView` (+ spec) in `views/auth/` (raggruppamento aggiunto di iniziativa, coerente con "per sezioni" — non richiesto esplicitamente per l'auth, segnalato ad Andrea). `DashboardView.vue` resta alla radice (unica pagina della sua sezione). Aggiornati i path in `src/router/index.ts`, unico punto che referenziava le view per percorso file — tutti gli altri import usano l'alias `@/...` e non hanno richiesto modifiche.

**Select "Tipo lavoro" azzerabile** (`PazienteNuovoView.vue`): applicato lo stesso pattern `VALORE_SELEZIONA` già usato per "Protocollo" e "Protocollo vita" (voce "Seleziona" in cima alla lista, per poter tornare allo stato non selezionato). TDD: test che seleziona un valore poi torna su "Seleziona" e verifica `tipoLavoro: undefined` nel payload — rosso (restava la stringa interna `'__seleziona__'`) poi verde.

**Verifica**: frontend `npx vitest run` → **90/92 verdi** (stessi 2 test preesistenti e scollegati di `LoginView.spec.ts`, non toccati), `npx tsc --noEmit` pulito dopo ogni fix. Nessuna verifica manuale in browser (per convenzione, tocca ad Andrea).

**Handoff esplicito**: Andrea farà lui stesso il commit di tutto il lavoro in staging (`git add` fatto su tutti i file toccati in questa sessione) e aprirà una nuova sessione per lavorare sulla lista dei pazienti (`PazientiListView.vue`, ora in `views/pazienti/`) — coerente con quanto già annunciato il 1° settembre. Aggiornato `stato.md`.

## [2026-09-02] ingest+handoff | Aggiunto il Codice Fiscale all'anagrafica paziente

Prima di aprire la nuova sessione annunciata, Andrea si è accorto che mancava un dato importante nell'anagrafica paziente: il Codice Fiscale. Brainstorming bounded in chat (4 domande mirate, una alla volta) → design breve approvato → implementazione TDD diretta, backend e frontend nello stesso passaggio.

**Decisioni** (da `AskUserQuestion`): campo **obbligatorio** alla creazione, come nome/cognome/email/dataNascita/sesso; **validato con pattern completo** (16 caratteri alfanumerici) sia lato client sia lato server — la prima volta che questo progetto aggiunge una validazione di formato `@Pattern` lato backend per un campo testuale (gli altri, nome/cognome/telefono/lavoro, restano senza pattern server-side, gap noto e non affrontato qui, vedi [domande-aperte](domande-aperte.md)); **non univoco** (nessun vincolo a DB, a differenza dell'email); mostrato **solo in creazione e dettaglio**, non nella lista pazienti; posizionato **subito dopo Cognome**, prima di Data di nascita/Sesso/Email.

**Backend**: `Paziente.java` (campo `codiceFiscale`, colonna `codice_fiscale`, nel costruttore dopo `cognome`), `CreaPazienteRequest.java` (`@NotBlank @Pattern(regexp = "^[A-Za-z0-9]{16}$")`), `PazienteResponse.java` (campo + mapping), migrazione `V14__paziente_codice_fiscale.sql` (`ALTER TABLE pazienti ADD COLUMN codice_fiscale VARCHAR(16) NOT NULL` in un solo passaggio, sicuro solo perché la tabella `pazienti` su `hexis` risultava vuota all'ultimo controllo del 1° settembre — da riverificare prima di far partire l'app). Aggiornati **18 punti di test** che costruiscono `Paziente` direttamente (5 file: `PazienteControllerTest`, `AuthControllerTest`, `TokenAzioneRepositoryTest`, `PazienteRepositoryTest`, `VisitaRepositoryTest`) e **22 body JSON** in `PazienteControllerTest` (20 text-block + 2 one-liner con virgolette escaped), tutti con un CF placeholder valido (`RSSMRA80A01H501U`), via trasformazione regex automatizzata (PowerShell, non disponibile Python nell'ambiente) invece che a mano — verificato il conteggio delle sostituzioni prima di fidarsene. Aggiunti 2 nuovi test (`creaPazienteSenzaCodiceFiscaleRestituisce400`, `creaPazienteConCodiceFiscaleNonValidoRestituisce400`) + un'asserzione sul campo nella risposta 201.

**Frontend**: `validators.ts` (`REGEX_CODICE_FISCALE`, `filtraCodiceFiscale` — maiuscolo automatico, alfanumerico, max 16 — `erroreCodiceFiscale`), `api/pazienti.ts` (campo in `Paziente` e `CreaPazienteRequest`), `PazienteNuovoView.vue` (nuovo campo con lo stesso pattern `conFiltro`/errore-che-sparisce-alla-correzione degli altri campi), `PazienteDettaglioView.vue` (mostrato come riga sotto l'email). Aggiornati tutti i mock `Paziente`/`CreaPazienteRequest` nei test esistenti che ne richiedevano il tipo (`PazienteNuovoView.spec.ts`, `pazienti.spec.ts`, `DashboardView.spec.ts`, `PazienteDettaglioView.spec.ts`, `PazientiListView.spec.ts`) — nessuno di questi testava il CF in sé, quindi il valore placeholder è bastato ovunque tranne nei nuovi test dedicati.

**Verifica**: backend `mvn test` → **94/94 verdi, BUILD SUCCESS** (92 precedenti + 2 nuovi); frontend `npx vitest run` → **89/91 verdi** (stessi 2 test preesistenti e scollegati di `LoginView.spec.ts`), `npx tsc --noEmit` pulito. Nessuna verifica manuale in browser (per convenzione, tocca ad Andrea) — in particolare da provare a mano: il campo Codice Fiscale nel form reale, e che la migrazione V14 si applichi senza errori sul database `hexis` (tabella confermata vuota l'ultima volta, ma non riverificato in questa sessione).

Aggiornati `modello-dati.md` e `api-contracts.md`. Lavoro **non ancora messo in staging** — tocca a un prossimo passaggio prima del commit di Andrea.

## [2026-09-02] handoff | Lista pazienti paginata: redesign UI, ricerca/filtri lato server, archiviazione paziente

Percorso completo: brainstorming (architetturale, con due giri di scope-creep gestiti in corsa — prima paginazione reale invece che client-side, poi archiviazione paziente con menu riga e conferma) → spec → piano → esecuzione con `superpowers:subagent-driven-development` in un **worktree isolato** (`.claude/worktrees/lista-pazienti-paginata`, branch `worktree-lista-pazienti-paginata`) — prima volta che questa skill gira in worktree su questo progetto invece che direttamente su master. Spec: `docs/superpowers/specs/2026-09-02-lista-pazienti-paginata-design.md`; piano: `docs/superpowers/plans/2026-09-02-lista-pazienti-paginata.md`; ledger completo (10 task, ogni revisione, ogni ruling): `.claude/worktrees/lista-pazienti-paginata/.superpowers/sdd/2026-09-02-lista-pazienti-paginata/progress.md` (non ancora rimosso, vedi nota sotto).

**Backend**: nuovo flag `archiviato` su `Paziente` (soft-delete logico, migrazione V15), `PazienteService.invita(...)` ora rifiuta i pazienti archiviati (400), nuovi endpoint idempotenti `POST /pazienti/{id}/archivia` e `/de-archivia`. Nuova classe `PazienteSpecifications` (filtri JPA componibili: ricerca testo, stato account, sesso, intervallo data di nascita, archiviato, isolamento per professionista) e nuovo endpoint `GET /pazienti/ricerca` (paginato/filtrato/ordinato, `Pageable`/`Sort` con whitelist esplicita dei campi ordinabili) — separato dal `GET /pazienti` esistente, che resta invariato e continua a essere usato da `DashboardView.vue`.

**Frontend**: `PazientiListView.vue` riscritta interamente — ricerca con debounce, chip di stato account, pannello filtri avanzati (sesso, intervallo data di nascita, toggle "mostra archiviati"), header ordinabili, paginazione server-side, stati di caricamento/errore/vuoto (due varianti). Nuovo componente `PazienteRigaAzioni.vue` (icona apri cartella, bottone invita/reinvia invito, menu con voce "Nuova visita" disabilitata e azione archivia/de-archivia con conferma tramite nuovo componente shadcn-vue `AlertDialog`, generato per l'occasione).

**Adattamento di processo esplicito**: nessun `git commit` in nessun momento (regola assoluta del progetto) — ogni task ha fatto solo `git add`; i pacchetti di revisione per-task sono stati costruiti con snapshot manuali pre-dispatch + `git diff --cached`/`git diff --no-index` invece che con intervalli di commit, dato che `HEAD` è rimasto fisso sul commit di partenza del worktree per l'intera esecuzione (stesso adattamento già usato nella sessione Plicometria dell'1 settembre, esteso qui alla granularità per-task oltre che whole-branch).

**Revisione finale whole-branch** (Opus): verificata in autonomia l'isolamento multi-professionista (mai condizionale, in ogni percorso che tocca il database, incluso il nuovo endpoint di ricerca), l'allineamento dei contratti tra i 10 task (nomi parametri query, forma della risposta paginata, eventi emessi dal componente riga vs listener della view), la sicurezza della migrazione V15 (colonna additiva con default, più sicura della precedente V14 che aggiungeva `NOT NULL` senza default). Trovati e risolti in un'unica fix wave 4 Important: (1) archiviazione/de-archiviazione senza gestione errori — fallimento silenzioso; (2) indice di pagina non riallineato dopo aver archiviato l'ultimo elemento dell'ultima pagina, con conseguenza reale (mostrava "Nessun paziente, per ora" a un professionista con decine di pazienti); (3) `wiki/modello-dati.md` mai aggiornato per la nuova colonna (gap del piano, non del codice); (4) badge di stato account/attività che mostravano il valore enum grezzo invece delle etichette leggibili e colorate richieste dalla spec (difetto ereditato letteralmente dal testo del piano, corretto secondo l'autorità della spec). Più 2 pulizie a costo zero su file generati dalla CLI shadcn-vue (CSS duplicato/font inutilizzato, un refuso di classe Tailwind). Re-review mirata: tutti e 6 i fix confermati corretti, nessuna nuova rottura.

**Verifica**: backend `mvn test` → **116/116 verdi, BUILD SUCCESS**; frontend `npx vitest run` → **113/115 verdi** (2 falliti = gli stessi test preesistenti e scollegati di `LoginView.spec.ts` segnalati dal 31 agosto 2026, non toccati), `npx tsc --noEmit` pulito. Nessuna verifica manuale in browser in questa sessione.

**Segnalato, non affrontato in questa sessione** (9 Minor parcheggiati dalla revisione finale, dettaglio nel ledger): nessun test sul clamping `dimensione` 1-100 né sullo scenario "pagina oltre l'ultima"; nessun test controller multi-tenant dedicato su `/ricerca`; `PazienteRigaAzioni` decide le azioni disponibili in base al contesto della vista (`mostraArchiviati`) invece che sul singolo `paziente.archiviato`, equivalente oggi ma non riutilizzabile altrove; nessun sequencing su richieste concorrenti (solo la ricerca testuale è debounced); mancanza di `aria-sort` sugli header ordinabili. **Nuova domanda di prodotto aperta**: il conteggio "pazienti attivi" della dashboard include anche i pazienti archiviati, dato che `GET /pazienti` (usato solo lì) non è mai stato filtrato — vedi [domande-aperte](domande-aperte.md).

**Nota sul worktree**: la cartella `.claude/worktrees/lista-pazienti-paginata/.superpowers/sdd/2026-09-02-lista-pazienti-paginata/` (ledger, brief, report, diff di ogni revisione) è stata **deliberatamente non cancellata** al termine, contrariamente al comportamento di default della skill — perché, non essendoci alcun commit, la git history non cattura ancora questo lavoro come "registro storico". Resterà lì come riferimento finché Andrea non avrà revisionato e committato.

Lavoro **interamente in staging nel worktree** (42 file, `git add` fatto), **nessun commit** — tocca ad Andrea rivedere ed eventualmente integrare (merge/rebase su `master`, dalla sua macchina) lui stesso.

## [2026-09-02] handoff | Toast di notifica globale, bug fix invito, rifiniture lista pazienti

Sessione diretta su `master` (lavoro della lista pazienti già copiato lì dal worktree). Richieste rapide di Andrea una alla volta, nessuna nuova ceremonia di brainstorming/piano.

**Fix TypeScript**: `onSessoChange` in `PazientiListView.vue` tipizzato troppo stretto (`string` invece di `AcceptableValue` di reka-ui) — corretto sullo stesso pattern già usato in `PazienteNuovoView.vue`. Spiegato ad Andrea perché `tsc --noEmit` non lo intercetta: niente `vue-tsc` in questo progetto, quindi i binding nei template `.vue` non sono mai type-checkati da riga di comando.

**Rifiniture UI lista pazienti**: ordinamento a 3 click con icone sempre visibili, colonna "Attività" rimossa, colore avatar allineato al badge stato account, hover riga più chiaro, chip stato account passati da verde pieno a mint/verde testo.

**Bug "Invita" — debug sistematico, due cause radice**: (1) `PazienteService.invita()` non catturava i fallimenti di `emailSender.invia(...)` (Resend), causando un crash — fix con nuova `InvioEmailFallitoException` (502), test TDD dedicato, `log.warn` nel catch (stesso pattern di `AuthService.richiediResetPassword`); (2) con l'API key Resend non configurata, il client vedeva comunque 401 invece di 502 — causa: Spring Security bloccava il forward interno di Tomcat verso `/error` (mai reso `permitAll()` in `SecurityConfig`), che veniva riautenticato e restituiva 401 al posto del body del 502. Fix non verificabile con `mvn test` (MockMvc non riproduce il forward reale) — verificato a mano da Andrea. Spiegata la configurazione `RESEND_API_KEY`/`RESEND_FROM_EMAIL`; **rimandata esplicitamente** da Andrea ("per il momento lasciamo stare il server email").

**Sistema di notifiche toast globale** (richiesta di Andrea: sostituire i banner di errore/successo inline con alert a comparsa auto-dismiss 5s, per tutti i ritorni dal server, su tutte le pagine). Adottato `sonner` di shadcn-vue (wrapper `vue-sonner`), montato in `App.vue`. Migrate 6 view + relativi `.spec.ts` (`LoginView`, `PasswordDimenticataView`, `ResetPasswordView`, `PazienteDettaglioView`, `PazienteNuovoView`, `PazientiListView`): banner di esito azione → toast; stati di caricamento pagina falliti rimasti inline per scelta. Due bug trovati e risolti dalla verifica manuale di Andrea: toast sotto la sidebar (mancava `<Teleport to="body">`, la libreria non si teleporta da sola) e toast senza stile (CSS distribuito in un file separato, `vue-sonner/style.css`, mai importato dal componente generato dalla CLI). Rifiniture successive: posizione in alto a destra, bottone chiudi ghost dentro il bordo della card (override delle variabili di posizionamento della libreria), sfondo colorato per tipo (`richColors` + variabili `--success-*`/`--error-*` mappate sui token brand) perché prima success ed error erano indistinguibili a colpo d'occhio.

**Trovato per caso, non risolto**: `PazienteRigaAzioni.spec.ts` ha 3 test che falliscono solo se eseguiti insieme nel file (isolati passano) — sintomo di un problema di isolamento tra test, non un bug della UI. Confermato indipendente dal lavoro di questa sessione (`git stash` delle modifiche proprie, il fallimento resta). Sembra legato a modifiche non ancora in staging fatte a mano da Andrea su `PazienteRigaAzioni.vue`. Segnalato, non indagato oltre su richiesta di Andrea.

**Verifica**: frontend `npx tsc --noEmit` pulito; `npx vitest run` → **112/115 verdi** (3 falliti = il problema di isolamento appena descritto). Backend invariato oltre al fix invito, **117/117 verdi** dalla verifica precedente nella stessa sessione. Nessuna verifica manuale in browser fatta dall'agente — quella del flusso invito e dei toast l'ha fatta Andrea stesso durante la sessione.

**Handoff esplicito**: Andrea ha chiesto di salvare tutto — `git add -A` su tutto il repo, incluse modifiche non ancora in staging fatte a mano da Andrea (`PazienteRigaAzioni.vue`, `AppHeader.vue`, `DashboardView.vue`, `PazienteNuovoView.vue`, ritocchi CSS su `Sonner.vue`) — **nessun commit**, per aprire una nuova sessione dedicata alla **pagina di dettaglio del paziente** (`PazienteDettaglioView.vue`). Aggiornato `stato.md`.

## [2026-09-02] handoff | Sezione "Andamento" e redesign completo della pagina di dettaglio paziente

Percorso completo (brainstorming architetturale → spec → piano → esecuzione con `superpowers:subagent-driven-development`, direttamente su `master` per scelta esplicita di Andrea, nessun worktree), seguito da diverse sessioni di rifiniture bounded richieste da Andrea una alla volta. Spec: `docs/superpowers/specs/2026-09-02-dettaglio-paziente-andamento-design.md`; piano: `docs/superpowers/plans/2026-09-02-dettaglio-paziente-andamento.md`.

**Backend**: nuovo endpoint di sola lettura `GET /pazienti/{id}/visite` — storico visite del paziente ordinato cronologicamente (`VisitaRepository.findAllByPazienteIdOrderByDataVisitaAsc`), con `plicometria` annidata opzionale (`VisitaResponse`, nuovi record annidati `Circonferenze`/`PlicometriaResponse`). `PazienteService.visite()` riusa `dettaglio()` per il controllo di ownership (stesso 404 su paziente di un altro professionista). `wiki/api-contracts.md` aggiornato.

**Frontend — 4 task**: tipi TS + funzione `visite()` in `api/pazienti.ts`; funzione pura `prepareAndamento()` (`utils/andamento.ts`) che trasforma le visite in punti/ultimo/delta per metrica; componente `AndamentoChart.vue`; wiring nella view. **Deviazione dal piano scoperta durante l'esecuzione**: lo stile shadcn-vue di questo progetto (`reka-nova`) non ha il componente `chart-line` preconfezionato — verificato contro il registry live (404 per `reka-nova`, 200 per `new-york`/`default`). Ruling del coordinatore: installate le primitive reali `chart` (`ChartContainer`/`ChartConfig`) e composto il grafico a linee direttamente in `AndamentoChart.vue` con `@unovis/vue` grezzo, invece di scrivere a mano un finto file "generato da CLI" (violerebbe la convenzione di `frontend-professionisti/CLAUDE.md`).

**Revisione finale whole-branch** (Opus) → "ready to merge, with fixes": 5 Important trovati, il più rilevante è che con una sola visita per paziente (situazione di *ogni* paziente oggi, dato che il flusso "Nuova visita" su paziente esistente non esiste) il grafico non mostrava alcun punto visibile (nessun marker sulla linea). Fix wave unica (Opus, per la delicatezza del punto sul fuso orario) → aggiunti marker (`VisScatter`), asse X basato su timestamp reale invece dell'indice array (con `formattaDataItaliana`/`isoATimestamp`/`timestampAIso` estratti in `utils/data.ts`, mai `new Date()` per testo visibile — bug di fuso orario evitato deliberatamente), copertura di test per la formattazione date e per i rami delta positivo/zero, stato di caricamento per non mostrare uno stato vuoto ingannevole. Re-review confermata pulita.

**Redesign iterativo della pagina** (bounded, una richiesta di Andrea alla volta, dopo che Andrea stesso ha riscritto a mano gran parte della view partendo dal lavoro dell'agente): Andrea ha giudicato la prima versione (solo sezione Andamento aggiunta, resto invariato) troppo disomogenea col resto dell'app e ha riscritto a mano header, card "Dati Anagrafici"/"Contatti e Recapiti", card "Andamento Clinico" — poi l'agente ha allineato test e corretto bug reali introdotti nell'editing manuale (icona `Utensils` non importata, `new Date().toLocaleDateString('it-IT')` per la data di nascita — stesso bug di fuso orario già evitato altrove, sistemato riusando l'approccio sicuro). Su richiesta di Andrea e mostrando un mockup generato con IA come riferimento: card header ridisegnata con 4 bottoni azione sempre visibili (Modifica/Nuova visita/Nuovo piano alimentare disabilitati, nessun flusso costruito) e striscia di 5 statistiche rapide (Peso attuale/BMI con delta colorato, Ultima visita — dati reali dalle visite salvate; Prossima visita/Piano alimentare — "Presto disponibile", nessun modulo Agenda/Piani alimentari esiste); bottone "Invita" spostato da riga-azioni a link testuale accanto al badge di stato; nuova card "Visite" (elenco dalla più recente, "Prima visita" sulla più vecchia/"Visita di controllo" sulle altre — distinzione derivata dall'ordine cronologico, non dati inventati; badge "Completata" fisso perché ogni visita salvata è per definizione già avvenuta, nessun concetto di visita futura). Aggiunta `formattaDataItalianaConMese` (`utils/data.ts`, "01 ago 2026") per le date nella card in alto, sempre senza `new Date()`.

**Bug segnalato da Andrea durante il testing manuale** (striscia statistiche vuota nonostante una visita salvata) indagato con `superpowers:systematic-debugging`: ipotesi principale (backend non riavviato dopo l'aggiunta dell'endpoint) esclusa da Andrea; si è risolto da sé prima di isolare la causa reale. Trovato però un gap di codice reale e sistemato comunque: la card in alto non mostrava alcun messaggio se il caricamento delle visite falliva (mostrava silenziosamente "—" ovunque), a differenza della sezione Andamento più sotto — ora entrambe mostrano lo stesso errore.

**Verifica**: backend `mvn test` → **122/122 verdi**; frontend `npx vitest run` → **156/159 verdi** (3 falliti = `PazienteRigaAzioni.spec.ts`, isolamento tra test, pre-esistente e non toccato, vedi handoff precedente); `npx tsc --noEmit` pulito. Nessuna verifica manuale in browser fatta dall'agente — fatta da Andrea stesso, che ha anche segnalato e fatto verificare il bug della striscia statistiche.

**Handoff esplicito**: Andrea ha chiesto di salvare tutto (`git add`, tutto in staging, **nessun commit**) per riprendere domani con due flussi che oggi sono bottoni/voci disabilitate su questa stessa pagina: **modifica anagrafica paziente** e **inserimento nuova visita su paziente esistente** (quest'ultimo già segnalato come mancante fin dalla sessione lista-pazienti del 2 settembre). Aggiornato `stato.md`.

## [2026-09-03] ingest/handoff | Aggiunti campi Note (paziente e visita) e Obiettivo (visita)

Bounded (brainstorming mirato, 4 domande) → design approvato in chat → TDD diretto, backend e frontend. Nessun nuovo endpoint: i campi sono valorizzabili solo alla creazione del paziente/prima visita (`POST /pazienti`), dato che non esiste ancora un flusso di modifica anagrafica né di nuova visita su paziente esistente.

**Decisioni**: `Paziente.note` opzionale (testo libero, in fondo ai dati anagrafici). `Visita.note` opzionale (in fondo al form visita) e `Visita.obiettivo` (`DIMAGRIMENTO`/`IPERTROFIA`/`RICOMPOSIZIONE`/`MANTENIMENTO`/`NUTRIZIONE_CLINICA`) con default `MANTENIMENTO` preselezionato in UI (diverso dal pattern "Seleziona" già usato per Protocollo vita/Tipo lavoro, su richiesta esplicita di Andrea). Nessuna validazione server-side su `note` (stesso gap noto degli altri campi testuali liberi); su richiesta di Andrea a metà sessione, aggiunto un limite di 500 caratteri con contatore solo lato client (`maxlength` + contatore `n/500` sotto entrambe le textarea).

**Backend**: migrazione `V16__paziente_visita_note_e_obiettivo.sql` (`pazienti.note TEXT` nullable, `visite.note TEXT` nullable, `visite.obiettivo VARCHAR(30) NOT NULL DEFAULT 'MANTENIMENTO'`), nuovo enum `ObiettivoVisita`. `Paziente`/`Visita`/`CreaPazienteRequest`/`VisitaRequest`/`PazienteResponse`/`VisitaResponse`/`PazienteService` estesi. Le circa 45 chiamate dirette a `new Paziente(...)`/`new Visita(...)` nei test sono state aggiornate con uno script Node ad hoc (parser a parentesi bilanciate, non una regex piatta, per gestire gli argomenti annidati come `LocalDate.of(...)`) che inserisce l'argomento aggiuntivo prima della parentesi di chiusura di ogni chiamata — verificato a campione dopo l'esecuzione. 2 nuovi test (note/obiettivo persistiti e restituiti; default `MANTENIMENTO` quando `obiettivo` non è passato).

**Frontend**: aggiunto il componente shadcn `Textarea` (non esisteva ancora). `PazienteNuovoView.vue` (campo Note), `DatiVisitaForm.vue` (select Obiettivo nella griglia principale, textarea Note in fondo), `PazienteDettaglioView.vue` (Note mostrata nella card Dati Anagrafici se presente), tipi in `api/pazienti.ts` estesi.

**Verifica**: backend `mvn test` → **124/124 verdi, BUILD SUCCESS**. Frontend `npx vitest run` → **156/159 verdi** (3 falliti = `PazienteRigaAzioni.spec.ts`, isolamento tra test pre-esistente e non toccato, segnalato per la quarta volta consecutiva); `npx tsc --noEmit` pulito; `npm run build` pulito. Nessuna verifica manuale in browser (tocca ad Andrea).

Aggiornati `modello-dati.md` e `api-contracts.md`. Lavoro **in staging, nessun commit** — tocca ad Andrea.

## [2026-09-03] ingest | Revisione dei valori di ObiettivoVisita

Andrea ha rivisto i valori dell'enum `ObiettivoVisita` introdotto nella voce precedente: `NUTRIZIONE_CLINICA` sostituita da `EDUCATIVO` e `PREPARAZIONE_SPORTIVA`, aggiunta `AUMENTO_PESO`. Valori finali: `DIMAGRIMENTO`, `AUMENTO_PESO`, `IPERTROFIA`, `RICOMPOSIZIONE`, `MANTENIMENTO`, `EDUCATIVO`, `PREPARAZIONE_SPORTIVA` (default resta `MANTENIMENTO`). Aggiornati enum backend, tipi/select frontend, `modello-dati.md`, `api-contracts.md`, `stato.md`. Nessuna migrazione aggiuntiva necessaria (colonna `VARCHAR`, nessun vincolo CHECK sui valori). Backend `mvn test` → 124/124 verdi; frontend `npx vitest run` → 156/159 verdi (stessi 3 falliti pre-esistenti); `tsc --noEmit` pulito.

## [2026-09-03] handoff | Rifiniture UI a mano su Note/Obiettivo, prossimo passo: dettaglio poi modifica paziente

Andrea ha rifinito a mano `PazienteNuovoView.vue` e `DatiVisitaForm.vue` dopo le due voci precedenti: placeholder più descrittivi sulle due textarea Note ("Allergie, patologie, indicazioni utili…" per il paziente; "Note operative, aderenza al piano, piccoli obiettivi fino alla prossima visita..." per la visita), select "Obiettivo" spostata in cima alla griglia della visita (prima di data/altezza/peso), textarea Note visita spostata subito dopo quella griglia invece che in fondo al componente dopo gli accordion. Riverificato `tsc --noEmit` pulito e `npx vitest run` 156/159 (stesse 3 failure pre-esistenti di `PazienteRigaAzioni.spec.ts`), nessuna regressione. Tutto rimesso in staging.

**Handoff esplicito**: Andrea vuole riprendere con la **pagina di dettaglio del paziente**, per poi passare alla **modifica del paziente** — aggiornato `stato.md` (sezione "Prossimo passo consigliato") di conseguenza.

## [2026-09-03] handoff | Modifica anagrafica paziente e creazione/modifica visita

Le due funzionalità annunciate come prossimo passo sono state entrambe costruite: `superpowers:brainstorming` (bounded per entrambe) → TDD diretto, nessuna orchestrazione multi-agente.

**Modifica anagrafica paziente**: nuovo `PUT /pazienti/{id}` (solo anagrafica, niente visita — scelta corretta da Andrea a metà brainstorming rispetto alla proposta iniziale di modificare anche la prima visita nella stessa pagina); nuovo `ModificaPazienteDialog.vue` (componente shadcn `dialog` aggiunto per la prima volta) collegato al pulsante matita nel dettaglio paziente. **Bug trovato da Andrea al primo test manuale**: la `PUT` era bloccata da CORS (`SecurityConfig` permetteva solo `GET`/`POST`) — fix one-liner, confermato funzionante.

**Creazione/modifica visita**: tre nuovi endpoint `POST`/`GET {visitaId}`/`PUT {visitaId}` sotto `/pazienti/{id}/visite` — un paziente può ora avere più visite nel tempo. Collegati tutti e quattro gli entry point previsti (dettaglio paziente, menu lista pazienti, storico misurazioni, e la Dashboard — quest'ultima scoperta solo a metà brainstorming, ha richiesto un nuovo `SelezionaPazienteCombobox.vue` da zero perché lì non c'è ancora un paziente in contesto). Nuova vista condivisa `VisitaFormView.vue` con rotte dedicate (pagina, non dialog, per restare navigabile dalla lista pazienti), titolo + sottotitolo `[Nome Cognome] · ultima visita [data] con peso [peso]` su richiesta di Andrea. Due gap di contratto scoperti e corretti in corsa: `PlicometriaResponse` non esponeva gli input grezzi (solo i risultati calcolati) e `VisitaResponse` non esponeva affatto `protocolloVita` — in entrambi i casi necessario per precompilare correttamente la modifica, risolto con TDD. Bug Hibernate trovato in corsa: eliminare la plicometria precedente e ricrearla nella stessa transazione falliva per vincolo di unicità, perché Hibernate esegue sempre gli insert prima dei delete nello stesso flush — risolto con un `flush()` esplicito.

**Verifica**: backend `mvn test` → **139/139 verdi, BUILD SUCCESS**. Frontend `npx vitest run` → **216/216 verdi** (i 3 test di `PazienteRigaAzioni.spec.ts` segnalati falliti-se-insieme dalle sessioni precedenti non si sono ripresentati, causa non indagata); `npm run build` (tsc + vite) pulito. Nessuna verifica manuale in browser per il flusso visita; **Andrea ha verificato di persona il flusso di modifica anagrafica** dopo il fix CORS.

Aggiornati `api-contracts.md`, `modello-dati.md`, `stato.md`. Lavoro **in staging** (`git add` fatto su tutto il repo), **nessun commit** — tocca ad Andrea verificare a mano i flussi appena costruiti e indicare il prossimo passo.

## [2026-09-03] handoff | Altezza e obiettivo suggeriti dall'ultima visita in creazione

Ultima rifinitura della sessione, richiesta da Andrea: in creazione di una nuova visita, altezza e obiettivo si precompilano con i valori dell'ultima visita del paziente (valori che cambiano raramente), restando modificabili. Nuove prop opzionali su `DatiVisitaForm.vue` (`altezzaSuggeritaCm`/`obiettivoSuggerito`), usate solo quando non c'è una visita esistente da modificare. Corretta in corsa una race condition nel flusso "Nuova visita dalla Dashboard" (il form poteva comparire prima del caricamento dell'ultima visita, perdendo il suggerimento). TDD, frontend `npx vitest run` → 220/220 verdi, `npm run build` pulito. Aggiornato `stato.md`. Lavoro **in staging**, **nessun commit**.

## [2026-09-04] handoff | Rifiniture dettaglio/lista paziente, "Elimina visita", filtri lato server ridisegnati

Sessione lunga di richieste puntuali di Andrea, una alla volta, quasi tutte bounded (TDD diretto, nessuna orchestrazione multi-agente salvo un fork/agent di ricerca per capire la strategia di query sull'ultima visita per paziente).

**Dettaglio paziente — tab Panoramica/Confronto visite**: numeri Peso/BMI in `font-heading`; nuova card grafica `ConfrontoBarChart.vue` (bar chart raggruppato per Plicometria, barre orizzontali per Circonferenze — SVG puro, non Unovis, perché sono confronti punto-a-punto tra due visite, non serie temporali) nella tab Confronto Visite, ispirata a un mockup fornito da Andrea; le sezioni Plicometria/Circonferenze (sia grafico che tabella) ora spariscono del tutto se nessuna delle due visite ha quei dati, invece di mostrare tabelle vuote; riga "Obiettivo" aggiunta in tabella; colore delle variazioni uniformato a un unico "colore di avviso" invece del verde/rosso automatico buono/cattivo (richiesta esplicita: la valutazione clinica spetta al dietista, non all'app) — tranne BMI, mai menzionato nella richiesta, lasciato con la sua semantica precedente dove serviva.

**Bug reale trovato e risolto (`Calendar.vue`)**: il date-picker si apriva sempre sul mese odierno invece che sul mese del valore già selezionato. Causa: il wrapper shadcn-vue di `Calendar` inizializza un proprio `placeholder` locale sempre a `today()`, bypassando la logica interna di reka-ui che lo avrebbe derivato correttamente da `modelValue`. Fix con test di regressione dedicato (prima volta in questo progetto che si scrive un test per un componente `ui/`, deviazione dalla convenzione "niente test sui primitivi shadcn" perché qui il bug è specifico dell'adattamento fatto in questo progetto, non del comportamento di libreria).

**Colonna "Obiettivo" nella lista pazienti** (al posto di "Prossima visita", aggiunta e poi sostituita nella stessa sessione — l'agenda/appuntamenti non esiste ancora, quella colonna mostrava sempre `—`): mostra l'obiettivo dell'ultima visita di ogni paziente + "dal {data}". Serviva un dato che oggi non esiste su nessun endpoint paginato: nuovo metodo `VisitaRepository.findAllByPazienteIdIn` (una query per l'intera pagina, non N+1) + riduzione in Java a "ultima visita per paziente" in `PazienteService`, `PazienteResponse` esteso con `obiettivoUltimaVisita`/`dataUltimaVisita` (null se il paziente non ha visite).

**Precisione decimale**: peso, tutta la plicometria e le circonferenze mostrati con 2 cifre decimali invece di 1 (richiesta esplicita); il BMI resta a 1 cifra ovunque, gestito con un parametro `decimali` esplicito nei punti dove BMI condivide la stessa infrastruttura di formattazione di peso/plicometria (es. `PazienteTabConfrontoVisite.vue`, dove Peso e BMI stanno nello stesso array `Riga`).

**Funzione "Elimina visita"** (nello storico misurazioni, esplicitamente fuori scope nella sessione del 3 settembre — ora costruita): nuovo `DELETE /pazienti/{id}/visite/{visitaId}` (elimina prima la plicometria associata, FK senza cascade, poi la visita), `AlertDialog` di conferma sostituisce il bottone disabilitato. CORS non permetteva ancora `DELETE` (stesso tipo di gap già capitato con `PUT` in una sessione precedente) — corretto in `SecurityConfig`. **Bug reale trovato con TDD**: `AlertDialogAction` di reka-ui è internamente una `DialogClose` e chiude la dialog **prima** che il gestore `@click` custom giri; legare lo stato "quale visita eliminare" allo stesso v-model di apertura/chiusura lo azzerava troppo presto, silenziando la chiamata API senza errori visibili. Risolto disaccoppiando i due stati.

**Bottoni dei dialog di conferma**: "Annulla" ora sempre `variant="neutral"` (variant creata da Andrea), "Conferma" in rosso/danger per le azioni distruttive (elimina visita, archivia paziente — non de-archivia, reversibile e non distruttiva).

**Bug controlli input Plicometria**: stesso tipo di bug del date-picker ma nei campi `PlicaInput.vue` — non replicavano il workaround già usato in `DatiVisitaForm.vue` (marcatore invisibile temporaneo per forzare il resync del DOM quando il filtro produce lo stesso valore già presente, es. lettera scartata): un carattere non valido digitato restava visibile a schermo anche se il valore "vero" non cambiava. Stesso fix applicato a tutti e 4 gli input del componente (singolo + tripla misurazione).

**Filtro "Mostra pazienti archiviati"**: da checkbox testuale dentro i filtri avanzati a chip pill in stile danger (rosso), coerente visivamente con i chip di stato account sopra.

**Filtri di ricerca ridisegnati** — sostituiti due filtri anagrafici con due filtri clinici, su richiesta esplicita: `sesso` → `obiettivo` (dell'ultima visita), `dataNascitaDa`/`dataNascitaA` → `dataUltimaVisitaDa`/`dataUltimaVisitaA`. Nessuna relazione JPA tra `Paziente` e `Visita` (FK grezza): entrambi implementati come subquery correlate in `PazienteSpecifications` (`conObiettivoUltimaVisita`, `conDataUltimaVisitaTra`, che condividono un helper `dataUltimaVisita()` per la subquery `MAX(dataVisita)` per paziente). Un paziente senza visite non compare mai quando uno di questi due filtri è attivo. `sesso`/`dataNascita` restano campi anagrafici normali (creazione, dettaglio, età) — è sparita solo la possibilità di usarli come filtro di ricerca.

**Rifinitura finale**: il date-picker ora si chiude in automatico alla selezione di una data (prima richiedeva un secondo click fuori dal popover) — `Popover` passato da stato non controllato a `v-model:open`, chiuso da un `watch` su `modelValue`. Nuovo `DatePicker.spec.ts` (non ne aveva ancora uno).

**Modifiche esterne non ispezionate in questa sessione**: `git status` a fine sessione mostra anche `CalcoliPlicometria.java`, `JacksonPollock3Calcolatore.java`, `JacksonPollock7Calcolatore.java` e `AppSidebar.vue` modificati — non toccati da nessuna delle richieste di questa sessione, presumibilmente rifiniture fatte a mano da Andrea fuori da questa conversazione (pattern già capitato in sessioni precedenti). Non revisionati, presi così come sono su indicazione delle note di sistema ("prendilo come stato corrente").

**Verifica**: backend `mvn test` → **146/146 verdi, BUILD SUCCESS** (rieseguito ad ogni cambiamento di schema/query). Frontend `npx vitest run` → **248/248 verdi**, `tsc --noEmit` pulito, `npm run build` pulito, rieseguiti dopo ogni modifica. Nessuna verifica manuale in browser in nessun punto della sessione (nessun tool di automazione collegato) — segnalato ad Andrea ogni volta che rilevante.

Aggiornato `api-contracts.md` (nuovo endpoint DELETE, campi `obiettivoUltimaVisita`/`dataUltimaVisita` su `PazienteResponse`, filtri di ricerca sostituiti). Lavoro **in staging** (`git add` fatto su tutto il repo), **nessun commit** — tocca ad Andrea. Richiesta esplicita di chiudere la sessione qui ("Perfetto, funziona tutto. Salva tutto che riprendiamo più tardi").

## [2026-09-05] ingest | Brainstorming: design catalogo Alimenti (BDA + custom)

Sessione di brainstorming (skill `superpowers:brainstorming`) sul primo pezzo del sotto-progetto "Piano alimentare": il catalogo Alimenti. Esaminato il vecchio progetto `ProgettoEdo` (dataset BDA-IEO, 1109 alimenti in JSON, gestione alimenti custom in localStorage) e il mockup UI già pronto della pagina Alimenti. Deciso: tabella unica `alimenti` con `professionista_id` nullable (NULL = BDA globale immutabile, valorizzato = custom del professionista, visibile a lui e ai suoi pazienti), campi nutrizionali per 100g (kcal/proteine/grassi/carboidrati obbligatori + acqua/fibre/zuccheri/ferro/calcio/sodio opzionali), categoria testo libero, import BDA via migrazione Flyway generata da script una tantum, azione "duplica come personalizzato" sugli alimenti BDA. Scope di questa iterazione: solo backend + `frontend-professionisti`, nessuna UI cliente, nessun collegamento a "Piano alimentare" (ancora da progettare). Risolta la domanda aperta sul dataset da usare. Vedi [decisioni/0005-alimenti-bda-e-custom](decisioni/0005-alimenti-bda-e-custom.md). Aggiornate le pagine modello-dati, domande-aperte, index. Fonte: [sorgenti/2026-09-05-brainstorming-alimenti-design](sorgenti/2026-09-05-brainstorming-alimenti-design.md). Design approvato da Andrea, prossimo passo: piano di implementazione (skill `superpowers:writing-plans`).

## [2026-09-05] handoff | Catalogo Alimenti (BDA-IEO + custom): revisione finale e fix wave

Piano di 8 task (`docs/superpowers/plans/2026-09-05-alimenti.md`) eseguito in un worktree isolato (`.claude/worktrees/alimenti`), seguito dalla revisione finale whole-branch e da un'unica fix wave prima dell'integrazione su `master`.

**Implementato (8 task)**: tabella unica `alimenti` (V18, `professionista_id` nullable senza FK verso `professionisti`, indice su `professionista_id`); seed di 1109 alimenti BDA-IEO da `ProgettoEdo` (V19, script Node una tantum non incluso nel repo); `AlimentoSpecifications` (`visibilePer`/`conRicerca`/`soloBda`/`soloPersonalizzati`); `AlimentoService`/`AlimentoController` con `GET /alimenti/ricerca`, `GET /alimenti/{id}`, `POST /alimenti`, `PUT /alimenti/{id}`, `DELETE /alimenti/{id}`, `POST /alimenti/{id}/duplica` (ruolo PROFESSIONISTA, immutabilità BDA con 409 `AlimentoNonModificabileException`); `AlimentiListView.vue`, `AlimentoFormDialog.vue`, `AlimentoRigaAzioni.vue` in `frontend-professionisti`.

**Revisione finale — 1 Critical + 6 Important, tutti risolti in un'unica fix wave**:
1. **Critical**: `zuccheri_g` in V19 derivava dalla somma dei tre monosaccaridi della fonte (`zuccheri.glucosio_g`/`fruttosio_g`/`galattosio_g`) invece del campo corretto `valori_per_100g.carboidrati_solubili_g` (zuccheri totali) — sbagliato su circa il 37% delle 1109 righe (es. il latte intero risultava con 0g zuccheri invece del lattosio; almeno un pesce mostrava più zuccheri del carboidrato totale). Rigenerata la migrazione con uno script Node corretto (stessa logica per il resto, incluso `codice_bda` derivato dal nome del file JSON e non dal campo `id` interno — tre file del dataset hanno un `id` duplicato tra loro). Database `hexis_test` resettato (`DELETE FROM flyway_schema_history WHERE version='19'` + `TRUNCATE alimenti`) e riseminato pulito con la migrazione corretta.
2. **Important**: in `AlimentoRepositoryTest`/`AlimentoSpecificationsTest`/`AlimentoControllerTest`, diversi test creavano un alimento BDA-shaped (`professionista_id` nullo) e lo eliminavano come ultima riga del metodo, dopo le asserzioni — se un'asserzione falliva, la riga fasulla restava permanentemente nel database di test condiviso (`alimenti` è escluso dal `TRUNCATE` globale). Fix: nelle tre classi, un campo `bdaCreatiInQuestoTest` (lista di UUID) traccia ogni riga BDA-shaped creata da un test (nell'helper `creaAlimento` o inline), ripulito in `@AfterEach` indipendentemente dall'esito delle asserzioni; rimossi i delete manuali a fine metodo, ora ridondanti.
3. **Important**: `wiki/api-contracts.md` non documentava i 6 nuovi endpoint — aggiunta una sezione dedicata con la stessa tabella/stile usato per `/pazienti/*`.
4. **Important**: `wiki/modello-dati.md` elencava ancora `Alimento` sotto "Entità progettate, non ancora implementate" — spostato sotto "Entità implementate" (V18/V19), lasciando "Piano alimentare"/"Pasto" (genuinamente non costruite) dove erano.
5. **Important**: nessun handoff registrato in `stato.md`/`log.md` per il lavoro degli 8 task — aggiornati entrambi in questo passaggio.
6. **Important**: `backend/CLAUDE.md` non documentava l'eccezione al `TRUNCATE` per tabelle di riferimento seminate — aggiunta una frase che nomina esplicitamente `durnin_womersley_coefficienti` e `alimenti`, con rimando all'ADR 0005.
7. **Important**: zero test per i 3 nuovi componenti Vue (~600 righe) nonostante i loro analoghi diretti (`PazientiListView.vue`/`PazienteRigaAzioni.vue`) ne avessero — aggiunti `AlimentoFormDialog.spec.ts` (3 test: branching BDA/custom/creazione su input disabilitati e bottoni visibili), `AlimentoRigaAzioni.spec.ts` (4 test: voci di menu BDA vs custom, emit `apri`, conferma prima di `elimina`) e `AlimentiListView.spec.ts` (5 test: stato di caricamento → tabella popolata, chiamata iniziale a `cerca()`, chip fonte che cambia stato attivo e rifà la ricerca, stato di errore/riprova, stato vuoto).

**Verifica**: backend `mvn test` → **169/169 verdi, BUILD SUCCESS** (rieseguito dopo il reset del database e dopo la fix wave). Frontend `npx vitest run` → **267/267 verdi, 35 file di test**, `npx tsc --noEmit` pulito. Nessuna verifica manuale in browser (tocca ad Andrea, per convenzione — in particolare tutto il flusso `AlimentiListView`/`AlimentoFormDialog`/`AlimentoRigaAzioni` non è mai stato provato contro l'app in esecuzione).

Aggiornati `wiki/api-contracts.md`, `wiki/modello-dati.md`, `backend/CLAUDE.md`, `wiki/stato.md`. Lavoro **in staging** nel worktree isolato (`git add` fatto sui file toccati), **nessun commit** — tocca ad Andrea integrare su `master` e fare la verifica manuale del catalogo Alimenti.

## [2026-09-05] handoff | Alimenti: aggiunta quantità di riferimento (`quantitaG`)

Committato (eccezione una tantum autorizzata da Andrea, per vedere subito il codice nel checkout principale) e unito a `master` (`150bc6b`). Segnalato da Andrea: mancava un campo esplicito per la quantità a cui si riferiscono i valori nutrizionali (finora sempre implicitamente "per 100g"). Aggiunta migrazione `V20` (`quantita_g NUMERIC(6,2) NOT NULL DEFAULT 100` — valorizza retroattivamente le 1109 righe BDA già seminate), campo su entità/DTO/risposta/frontend, modificabile solo sugli alimenti personalizzati (i BDA restano fissi a 100, già immutabili). Nel form (`AlimentoFormDialog.vue`) precompilato a `100` in creazione, il titolo della sezione valori nutrizionali ora è dinamico ("· per {quantità} g"). "Duplica come personalizzato" copia anche `quantitaG` dell'originale.

**Verifica**: backend `mvn test` → **173/173 verdi, BUILD SUCCESS**. Frontend `npx vitest run` → **269/269 verdi**, `npx tsc --noEmit` pulito. Lavoro **in staging** su `master` (`git add` fatto sui file toccati), **nessun commit** — tocca ad Andrea.

## [2026-09-05] handoff | Ordinamento/pulisci-filtri Alimenti, controlli mancanti in AlimentoFormDialog, dialog delle form ridisegnati

Continuazione diretta su `master` dopo l'integrazione del catalogo Alimenti, richieste puntuali di Andrea una alla volta, nessun brainstorming/piano formale.

**Lista Alimenti**: colonna "Alimento" ordinabile (stesso pattern a 3 click asc→desc→nessuno di "Paziente" in `PazientiListView.vue`) — nuovi `ordinaPer`/`direzione` su `GET /alimenti/ricerca`, nuovi `CampoOrdinamentoAlimenti` (solo `nome` per ora) e `DirezioneOrdinamento` nel package `alimenti` (non riusati da `pazienti`, per non accoppiare i due package). Bottone "Pulisci filtri" aggiunto allo stato vuoto (mancava, presente in pazienti). Colonna "Quantità (g)" aggiunta poi rimossa su indicazione di Andrea nello stesso giro: la quantità di riferimento ora compare solo per gli alimenti personalizzati, come nota inline accanto alla categoria (i BDA sono sempre 100g). Rimossa la dicitura fissa "· valori per 100 g" dal footer, non più vera in generale.

**`AlimentoFormDialog.vue` non validava nulla prima di questa sessione**: `nome` poteva essere salvato vuoto, e i campi numerici con asterisco (Quantità, Kcal, Proteine, Grassi, Carboidrati) non erano davvero obbligatori — un campo vuoto veniva inviato silenziosamente come `0` (`Number(valore) || 0`). Anche `categoria` era `@NotBlank` lato backend ma mai validata in UI (gap preesistente scoperto e colmato nello stesso passaggio). Fix: `Nome`/`Categoria` obbligatori con errore sotto al campo; tutti i campi numerici (quantità + 10 nutrienti) ora si comportano come quelli della form paziente/visita — richiesta esplicita di Andrea — con filtro live in stile italiano (virgola decimale, `filtraDecimaleItaliano`, `type="text" inputmode="decimal"` invece di `type="number"`), errore che sparisce alla correzione. Nuovi validatori generici in `validators.ts`: `erroreNumeroDecimale`/`erroreNumeroDecimaleObbligatorio` (messaggio generico, non solo pazienti), `erroreNomeAlimento`/`erroreCategoriaAlimento` (obbligatorietà, nessuna restrizione a sole lettere — un nome di alimento può contenere cifre/virgole).

**Bug trovato in corsa, non da un test preesistente**: `@update:model-value="onCampoNutrienteInput(campo)"` nel loop dei 10 nutrienti chiamava la funzione come gestore invece di passarle l'evento (gotcha classico dei gestori inline di Vue: un'espressione già una chiamata di funzione viene eseguita direttamente, il valore di ritorno scartato) — i campi numerici non si aggiornavano mai da input reale. Scoperto solo perché un test verificava il payload realmente inviato a `crea()`. Fix: `onCampoNutrienteInput(campo)($event)`. Aggiunto anche un `id` stabile a ciascun input nutriente (mancava, rompeva l'associazione `label`/`input` via `for`).

**Dialog delle form ridisegnati** (due richieste separate): (1) non devono più chiudersi al click fuori, solo con il bottone di chiusura o Escape — `@pointer-down-outside.prevent` sul wrapper condiviso `components/ui/dialog/DialogContent.vue` (gli `AlertDialog` di conferma lo prevenivano già di default, non toccati); (2) non erano responsive — wrapper condiviso ora `flex flex-col` con `max-h-[calc(100%-2rem)]` e `overflow-hidden` sul bordo esterno, header (con bottone chiudi) e footer `shrink-0`, solo il corpo dei campi scrolla (`flex-1 min-h-0 overflow-y-auto`) — su richiesta esplicita di Andrea dopo un primo giro in cui scrollava tutto insieme. Applicato a entrambi i dialog di form del progetto (`AlimentoFormDialog.vue`, `ModificaPazienteDialog.vue`). Corretta anche la griglia nutrienti (`grid-cols-2` fisso anche su mobile → `sm:grid-cols-2`).

**Convenzione stabilita da Andrea per tutte le form future**: campi obbligatori sempre con errore sotto al campo che sparisce alla correzione; campi numerici sempre come nel form paziente/visita (virgola decimale italiana, filtro live, `type="text" inputmode="decimal"`, mai `type="number"` nativo).

**Verifica**: backend `mvn test` → **174/174 verdi, BUILD SUCCESS**. Frontend `npx vitest run` → **276/276 verdi, 35 file**, `npx tsc --noEmit` pulito. Nessuna verifica manuale in browser (tocca ad Andrea) — in particolare il click-outside e il comportamento responsive/scroll dei due dialog non sono mai stati provati contro un browser reale.

Aggiornati `wiki/api-contracts.md`, `wiki/domande-aperte.md`, `wiki/stato.md`. Lavoro **in staging** (`git add -A` su tutto il repo), **nessun commit** — tocca ad Andrea.

## [2026-09-05] handoff | Ordinamento/pulisci-filtri Alimenti, controlli mancanti in AlimentoFormDialog, dialog delle form ridisegnati

Continuazione diretta su `master` dopo l'integrazione del catalogo Alimenti, richieste puntuali di Andrea una alla volta, nessun brainstorming/piano formale.

**Lista Alimenti**: colonna "Alimento" ordinabile (stesso pattern a 3 click asc→desc→nessuno di "Paziente" in `PazientiListView.vue`) — nuovi `ordinaPer`/`direzione` su `GET /alimenti/ricerca`, nuovi `CampoOrdinamentoAlimenti` (solo `nome` per ora) e `DirezioneOrdinamento` nel package `alimenti` (non riusati da `pazienti`, per non accoppiare i due package). Bottone "Pulisci filtri" aggiunto allo stato vuoto (mancava, presente in pazienti). Colonna "Quantità (g)" aggiunta poi rimossa su indicazione di Andrea nello stesso giro: la quantità di riferimento ora compare solo per gli alimenti personalizzati, come nota inline accanto alla categoria (i BDA sono sempre 100g). Rimossa la dicitura fissa "· valori per 100 g" dal footer, non più vera in generale.

**`AlimentoFormDialog.vue` non validava nulla prima di questa sessione**: `nome` poteva essere salvato vuoto, e i campi numerici con asterisco (Quantità, Kcal, Proteine, Grassi, Carboidrati) non erano davvero obbligatori — un campo vuoto veniva inviato silenziosamente come `0` (`Number(valore) || 0`). Anche `categoria` era `@NotBlank` lato backend ma mai validata in UI (gap preesistente scoperto e colmato nello stesso passaggio). Fix: `Nome`/`Categoria` obbligatori con errore sotto al campo; tutti i campi numerici (quantità + 10 nutrienti) ora si comportano come quelli della form paziente/visita — richiesta esplicita di Andrea — con filtro live in stile italiano (virgola decimale, `filtraDecimaleItaliano`, `type="text" inputmode="decimal"` invece di `type="number"`), errore che sparisce alla correzione. Nuovi validatori generici in `validators.ts`: `erroreNumeroDecimale`/`erroreNumeroDecimaleObbligatorio` (messaggio generico, non solo pazienti), `erroreNomeAlimento`/`erroreCategoriaAlimento` (obbligatorietà, nessuna restrizione a sole lettere — un nome di alimento può contenere cifre/virgole).

**Bug trovato in corsa, non da un test preesistente**: `@update:model-value="onCampoNutrienteInput(campo)"` nel loop dei 10 nutrienti chiamava la funzione come gestore invece di passarle l'evento (gotcha classico dei gestori inline di Vue: un'espressione già una chiamata di funzione viene eseguita direttamente, il valore di ritorno scartato) — i campi numerici non si aggiornavano mai da input reale. Scoperto solo perché un test verificava il payload realmente inviato a `crea()`. Fix: `onCampoNutrienteInput(campo)($event)`. Aggiunto anche un `id` stabile a ciascun input nutriente (mancava, rompeva l'associazione `label`/`input` via `for`).

**Dialog delle form ridisegnati** (due richieste separate): (1) non devono più chiudersi al click fuori, solo con il bottone di chiusura o Escape — `@pointer-down-outside.prevent` sul wrapper condiviso `components/ui/dialog/DialogContent.vue` (gli `AlertDialog` di conferma lo prevenivano già di default, non toccati); (2) non erano responsive — wrapper condiviso ora `flex flex-col` con `max-h-[calc(100
## [2026-09-08] handoff | Categorie BMI OMS, command palette globale (⌘K), gestione UI pazienti archiviati

**1. Categorie BMI OMS aggiornate**: su richiesta di Andrea, verificato che le 8 fasce ufficiali OMS in `FASCE_BMI` (`frontend-professionisti/src/utils/visita.ts`) avevano già le soglie numeriche corrette — corretta solo l'etichetta "Sovrappeso" in "Sovrappeso (pre-obesità)". Su richiesta successiva, `categoriaBmi()` (usata nello storico misurazioni, prima con una propria logica a 4 categorie duplicata) ora deriva da `fasciaBmi()`/`FASCE_BMI`, unica fonte di verità.

**2. Command palette globale (⌘K)** — percorso completo: brainstorming architetturale (dal mockup `Hexis Dashboard Professionisti.dc.html`) → spec (`docs/superpowers/specs/2026-09-08-command-palette-design.md`) → piano (`docs/superpowers/plans/2026-09-08-command-palette.md`, 3 task) → esecuzione con `superpowers:subagent-driven-development` **direttamente su `master`** (worktree rifiutato esplicitamente da Andrea) → revisione finale whole-branch → fix wave.

Nuovo componente autonomo `CommandPalette.vue` (trigger nell'header + overlay dedicato, non il `Dialog` condiviso — il suo `DialogContent.vue` blocca la chiusura al click esterno, comportamento sbagliato per una palette), montato una volta in `AppHeader.vue`: scorciatoia Ctrl/Cmd+K, Esc per chiudere, gruppo "Azioni" statico (le stesse 4 voci del menu "Crea nuovo" della dashboard + una quinta, "Nuovo alimento", assente da quel menu), gruppo "Pazienti" con ricerca debounced (300ms) su `cerca()` esistente, max 5 risultati + "Vedi tutti i risultati" se di più, **niente ricerca alimenti** (solo l'azione di creazione, decisione esplicita di Andrea: "una volta inserito difficilmente serva cercarlo"). Due deep-link completano il flusso: `/alimenti?azione=nuovo` apre in automatico il dialog di creazione, `/pazienti?ricerca=...` precompila la ricerca della lista.

**Revisione finale whole-branch (Opus) — 2 Important + 3 Minor bundled, tutti risolti in un'unica fix wave**: i due deep-link, letti solo in `onMounted`, erano un no-op silenzioso se si era già sulla pagina di destinazione (Vue Router riusa l'istanza a parità di route, query diversa, route flat senza `<router-view :key>`) — fix: `watch(() => route.query.X, ..., { immediate: true })` al posto di `onMounted`, più `router.replace` per ripulire il parametro consumato dall'URL (altrimenti restava incollato, ri-innescando il comportamento al reload). Rifiniture bundle: stato "in ricerca" per evitare il flash di "Nessun risultato" durante il debounce; `aria-label` sul bottone trigger (nessun nome accessibile sotto `lg`); padding orizzontale sull'overlay per non toccare i bordi su mobile. 3 Minor rimasti parcheggiati (nessuna dipendenza a valle): race su risposte stale nella ricerca pazienti, overlay senza `role="dialog"`/`aria-modal`, `$el?.focus()` corretto ma fragile se `Input.vue` cambiasse struttura.

**Incidente di processo — violazione della regola "solo Andrea committa"**: i tre subagent implementatori dei task 1-3 hanno eseguito `git commit` (il template standard della skill lo prevede), producendo 3 commit reali su `master` prima che l'agente coordinatore se ne accorgesse — nonostante una memoria di una sessione precedente documentasse già esattamente questo stesso errore in un contesto di worktree. Un quarto subagent (la fix wave finale) si è accorto da solo del conflitto con `CLAUDE.md` e ha fatto `git reset --soft` per restare solo in staging, comportamento che tutti e quattro avrebbero dovuto avere. Segnalato ad Andrea, che ha scelto di lasciare i 3 commit così come sono. Memoria dell'agente aggiornata per rendere il controllo operativo ad ogni dispatch, non solo caricato una volta a inizio sessione.

**3. Gestione UI pazienti archiviati** (bounded, design breve in chat approvato): nella lista, "Nuova visita" sparisce dal menu riga per i pazienti archiviati (`!mostraArchiviati`, stesso pattern già usato per Archivia/De-archivia). Nella scheda paziente, per `paziente.archiviato`: nascosti Modifica anagrafica, Invita/Reinvia invito, Nuovo piano, Nuova visita, Modifica/Elimina visita (quest'ultime due in `PazienteTabStoricoMisurazioni.vue`, nuova prop `archiviato: boolean`); badge e avatar in intestazione diventano rossi (`--danger`) con l'icona archivio e il testo "Paziente archiviato" al posto di "Nessuna visita programmata"; aggiunto un bottone "De-archivia paziente" con conferma (stesso `AlertDialog` della lista), assente prima nella scheda.

**Verifica**: frontend `npx vitest run` → **276/276 verdi, 35 file** (invariato — nessun nuovo test scritto, su richiesta esplicita di Andrea di essere avvisato prima: "al massimo gli faccio io", salvato in memoria), `npm run build` pulito dopo ogni gruppo di modifiche. Nessuna verifica manuale in browser dall'agente. Nessuna modifica al backend in questa sessione.

**Committato e pushato da Andrea stesso** (`84a2018`, su `origin/master`) — prima volta in questo progetto che una sessione termina con lavoro già integrato invece che in staging. Ledger completo dell'esecuzione SDD (ogni task, review, ruling) in `.superpowers/sdd/2026-09-08-command-palette/` (cartella locale, ignorata da git) prima della pulizia post-merge.

## [2026-09-12] handoff | Sotto-progetto "Piano alimentare": editor e lista

Percorso completo: brainstorming architetturale → spec → piano → esecuzione con `superpowers:subagent-driven-development`, 14 task in worktree isolato (`.claude/worktrees/piano-alimentare`, branch `worktree-piano-alimentare`). Backend package `pianialimentari` — 5 tabelle (piani_alimentari, pasti, piano_giorno_macro_target, piano_esempi, piano_alimento_righe, V21), motore CalcolatoreTdee (BMR Mifflin-St Jeor/Katch-McArdle, TDEE con aggiustamento sempre 0% per decisione di Andrea), CRUD completo per tre modalità di piano (Pasti/Macro target/Esempi — scelta fissa). Un solo ATTIVO per paziente. Valori nutrizionali copiati come snapshot (risolve domanda aperta del 5 settembre: snapshot, non runtime reference, vedi [domande-aperte](domande-aperte.md)).

Frontend: `PianoAlimentareFormView.vue` (editor tre modalità), `PianiAlimentariListView.vue` (lista paginata), `AggiungiAlimentoDialog.vue` (ricerca catalogo + custom). Abilitati 4 entry point predisposti (sidebar, dashboard dropdown, command palette, bottone dettaglio paziente).

Due bug reali: (1) alimento personalizzato con quantità ≠100g salvato con valori nutrizionali sbagliati (normalizzazione fix in `AggiungiAlimentoDialog.vue`); (2) "Attiva piano" poteva attivare un piano il cui salvataggio era fallito silenziosamente (`salva()` ora ritorna booleano).

Verifica: backend `mvn test` → **199/199 verdi, BUILD SUCCESS**. Frontend `npx vitest run` → **300/300 verdi, 39 file**; `tsc --noEmit` e `npm run build` puliti. 14/14 task approvati, alcuni dopo fix round, più revisione finale whole-branch con una fix wave di 6 correzioni (1 Critical: `flush()` mancante in `sostituisciGiorniMacro`, stesso bug Hibernate insert-prima-di-delete già risolto per `Plicometria`). Nessuna verifica manuale in browser (tocca ad Andrea).

**Addendum, stessa sessione**: su richiesta di Andrea, rimossi 7 test extra aggiunti da un implementer in Task 3 oltre a quello richiesto; aggiunte 5 funzionalità previste dalla spec ma omesse dal piano (filtro `pazienteId` su `GET /piani-alimentari/ricerca`, card macro del giorno in modalità Pasti, rinomina pasto, nota testuale del pasto in UI, nome piano suggerito `"{Obiettivo} · fase {n}"`); integrazione su `master` fatta direttamente. Verifica finale: backend **195/195 verdi**, frontend **300/300 verdi**, `tsc --noEmit`/`npm run build` puliti. Dettaglio completo in [stato](stato.md#sessione-del-12-settembre-2026--sotto-progetto-piano-alimentare-editor-e-lista).

Lavoro copiato su `master` (stesso schema di "alimenti"/"lista-pazienti-paginata"), **nessun commit** — tocca ad Andrea rivedere e committare.

## [2026-09-13] handoff | Piano alimentare: lista e nuovo piano ridisegnati, calorie suggerite pre-salvataggio, micronutrienti, rifiniture UI

Sessione diretta su `master` (il lavoro del 12 settembre era già lì, in staging), tutta a richieste puntuali di Andrea una alla volta, nessun brainstorming/piano formale.

**Lista piani alimentari** ridisegnata per essere uguale a `PazientiListView.vue`: header con sottotitolo, card filtri (ricerca + chip stato), card risultati con skeleton/errore/tabella/vuoto e overlay durante il refresh, paginazione dentro la card, intestazioni ordinabili (Piano/Scadenza, l'API già le supportava), badge colorati per stato.

**Pagina "Nuovo piano" riprogettata da zero come pagina unica** (era: seleziona paziente → crea subito una bozza vuota sul server → poi editor separato). Ora: la ricerca paziente usa lo stesso `SelezionaPazienteCombobox`/header di "Nuova visita"; scelto il paziente, sotto compare subito il selettore di modalità (Piano con pasti/Solo target macro/Esempi intercambiabili) e l'intero builder, **tutto locale finché non si preme "Salva"** — il piano si crea sul server solo in quel momento, non più alla scelta di paziente/modalità. Questo ha richiesto scoprire e correggere un bug che la riprogettazione avrebbe introdotto: il backend genera automaticamente, alla creazione, un template (7 giorni × 5 pasti standard per "Piano con pasti", 7 righe macro vuote, 5 esempi standard) — con "Salva" che ora fa `crea()` seguito subito da `aggiorna()` con il contenuto della bozza locale, quel template sarebbe stato sovrascritto e perso al primo salvataggio se la bozza locale fosse partita vuota. Fix: la bozza locale parte pre-popolata con lo stesso identico template, così l'utente lo vede e lo edita da subito e il salvataggio non lo cancella.

**Calorie giornaliere suggerite anche prima del salvataggio**: prima comparivano solo dopo aver creato il piano (il calcolo lo fa il backend alla creazione). Ora calcolate anche lato client con la stessa formula di `CalcolatoreTdee.java` (Katch-McArdle se l'ultima visita ha una plicometria con massa magra, altrimenti Mifflin-St Jeor su peso/altezza/età/sesso, stesso fattore di attività e stessa soglia di sicurezza), mostrate come placeholder dentro il campo "Obiettivo giornaliero" (su richiesta esplicita di Andrea, non più come riga di testo separata). Dopo il salvataggio il valore mostrato torna quello del backend (fonte di verità).

**Bug reale trovato durante questo lavoro**: `GET /pazienti/{id}` non restituisce mai `obiettivoUltimaVisita`/`dataUltimaVisita` (sempre `null`) — solo `GET /pazienti/ricerca` fa il join con le visite e li valorizza. Il nome del piano suggerito ("{Obiettivo} · fase {n}", aggiunto il 12 settembre) restava quindi bloccato su "Nuovo piano" **solo** quando si apriva la pagina dalla scheda paziente (che usa `dettaglio()`), non selezionando il paziente dalla combobox (che usa `cerca()`). Non toccato il backend: risolto lato frontend derivando obiettivo e nome suggerito dall'ultima visita già scaricata via `GET /pazienti/{id}/visite` (endpoint usato comunque per la stima TDEE), non più dal campo incompleto di `Paziente`. Segnalato ad Andrea come bug più ampio (`GET /pazienti/{id}` incompleto per qualunque altro consumatore futuro), non affrontato lì.

**Nuovo campo "Data di partenza"** (default oggi, `DatePicker`, immutabile dopo il salvataggio come modalità e paziente): `data_inizio` esisteva già in tabella (sempre `LocalDate.now()`) ma non era mai impostabile dalla richiesta — aggiunto `dataInizio` opzionale a `CreaPianoAlimentareRequest` (default invariato se omesso), setter sull'entità.

**Micronutrienti Fibra/Ferro/Calcio/Acqua aggiunti end-to-end** (richiesti nelle card di riepilogo e nella tabella del pasto): erano già sul catalogo Alimenti ma **mai copiati nello snapshot di riga del piano** — nuova migrazione `V22` (4 colonne nullable su `piano_alimento_righe`), `PianoAlimentoRiga`/`RigaAlimentoRequest`/`RigaAlimentoResponse` estesi, `AggiungiAlimentoDialog.vue` esteso sia per la ricerca dal catalogo sia per l'inserimento manuale (4 nuovi campi, con gli stessi controlli degli altri). Card di riepilogo: Zuccheri/Fibra/Ferro/Calcio/Acqua. Colonne tabella pasto, nell'ordine richiesto: Qtà/Kcal/Prot./Gras./Carb./Zucc./Fibra/Ferro/Calcio (Acqua resta solo nella card, non richiesta in tabella).

**Conferme prima di perdere dati non salvati**: cambiare modalità (Pasti/Macro/Esempi) prima del primo salvataggio ora chiede conferma **solo se** la sezione lasciata contiene già qualcosa (alimenti aggiunti, target macro compilati, pasti/esempi extra oltre al template) — altrimenti nulla verrebbe mai inviato al server per quella sezione, dato che solo la modalità attiva al salvataggio ha una struttura persistita. Stesso principio per rimuovere un pasto/esempio: chiede conferma solo se contiene già alimenti, altrimenti si rimuove subito.

**Rifiniture UI puntuali** (una richiesta alla volta): `DatePicker` condiviso al posto di `<input type="date">` nativo per "Data di partenza"/"Fine piano" — trovato e corretto un bug reale di `dark:` mode (verificato con `tailwind-merge` direttamente, non solo a occhio): l'override colore/sfondo passato al componente vinceva in tema chiaro ma non in scuro, perché la variante `outline` del `Button` sottostante ha le proprie classi `dark:border-input`/`dark:bg-input/30` che una classe senza prefisso `dark:` non sovrascrive — risolto aggiungendo esplicitamente `dark:border-(--sage)`/`dark:bg-(--surf)`. Popup "Aggiungi alimento" allineato agli altri dialog del progetto (`DialogFooter` per il bottone di submit, dimensione a breakpoint invece di un pixel fisso). Card giorni laterale: non più allungata dallo stretch di default della grid (`items-start`), collassata a `NativeSelect` su schermo piccolo (stesso componente già usato altrove, mai prima in questo pattern "lista/tab su desktop → select su mobile"). Icona a matita sui titoli modificabili (nome piano/pasto/esempio). Tutti i campi numerici del piano allineati alla convenzione già stabilita il 5 settembre per le form (obbligatorietà dove ha senso — es. Grammi nell'inserimento manuale, prima non validato affatto —, `aria-invalid` + messaggio d'errore sotto al campo, placeholder di esempio) **più un livello aggiuntivo su richiesta esplicita di Andrea**: blocco a livello di tasto (`@keydown`) dei caratteri non numerici, non solo il filtro post-digitazione già in uso ovunque — con normalizzazione del punto (spesso inviato dal tastierino numerico indipendentemente dal layout) a virgola in `filtraDecimaleItaliano`, condivisa da tutte le form del progetto.

**Verifica**: backend `mvn test` → **195/195 verdi, BUILD SUCCESS** (rieseguito dopo ogni migrazione/modifica). Frontend: nessuna esecuzione dell'intera suite in questa sessione, solo i file toccati ad ogni passaggio (ultimo giro: 24/24 verdi su `pianiAlimentari`/`AggiungiAlimentoDialog`/`date-picker`), `npx vue-tsc --noEmit` pulito ad ogni giro (unico errore residuo, preesistente e non toccato: `pazienteId` non ancora nel tipo `CriteriRicercaPianiAlimentari`, noto dal 12 settembre). Nessuna verifica manuale in browser: niente Claude in Chrome collegato in questa sessione (segnalato più volte ad Andrea).

Aggiornati `wiki/api-contracts.md`, `wiki/modello-dati.md`. Lavoro **in staging** (`git add` su tutti i file toccati), **nessun commit** — tocca ad Andrea, in particolare la verifica manuale dell'intero flusso "Nuovo piano" (mai provato contro l'app in esecuzione, né prima né dopo questa riprogettazione).

## [2026-09-14] ingest | Editor piano alimentare: fix cambio modalità, validazione obbligatori, nota su Macro/Esempi

Serie di correzioni/aggiunte puntuali su `PianoAlimentareFormView.vue`, una richiesta alla volta:

- **Fix `pazienteId` mancante** nel tipo `CriteriRicercaPianiAlimentari` del frontend (bug noto dal 12 settembre, segnalato nella sessione precedente): il backend lo supportava già in `GET /piani-alimentari/ricerca`, mancava solo nel tipo/query-string TS.
- **Fix reset sezione al cambio modalità**: `confermaCambioModalita()` cambiava `modalitaScelta` ma non svuotava mai `pastiLocali`/`giorniMacroLocali`/`esempiLocali` della sezione abbandonata, nonostante il dialog di conferma dichiari esplicitamente che i dati "andranno persi" — tornando sulla stessa modalità i vecchi dati riapparivano. Estratti i template di default (`pastiTemplate`/`giorniMacroTemplate`/`esempiTemplate`) e richiamati alla conferma del cambio.
- **Tooltip obiettivo kcal**: il `title` dell'icona "i" era statico e bacato (usava `{{ }}` dentro un attributo HTML, che Vue non interpola — mostrava le graffe letterali). Sostituito con un computed `titleObiettivoKcal` che copre tutti i casi (formula usata, sotto soglia, nessun suggerimento) e assorbe anche i 4 `<p>` esplicativi rimossi da sotto il campo.
- **Campi obbligatori per il salvataggio**: data di partenza, data di fine e obiettivo giornaliero ora bloccano il salvataggio se vuoti, con lo stesso pattern di validazione già in uso in `PazienteNuovoView.vue`/`DatiVisitaForm.vue` (errore live sotto al campo, pulito appena il valore diventa valido, mai solo a submit). Nuovi validatori `erroreDataInizioPiano`/`erroreDataFinePiano` in `validators.ts`; per l'obiettivo kcal riusato `erroreNumeroDecimaleObbligatorio` già esistente.
- **Esempi intercambiabili**: grammi dell'alimento ora editabili inline (prima solo testo statico), stesso componente/handler già usato in "Piano con pasti" (`grammiVisualizzati`/nuovo `onGrammiInputEsempio`, che scrive su `esempiLocali` invece che su `pastiLocali`). Card per categoria ingrandite (min-width colonna 280px→340px, padding e font aumentati).
- **Campo Nota su Macro ed Esempi**: prima esisteva solo su `Pasto` (`pasti.nota`, V21). Aggiunto lo stesso campo libero opzionale a `PianoGiornoMacroTarget` (`piano_giorno_macro_target.nota`) e `PianoEsempio` (`piano_esempi.nota`) — migrazione `V23__piano_macro_ed_esempi_nota.sql`, entità/record `Request`/`Response` aggiornati, `PianoAlimentareService` (creazione template, sostituzione integrale) aggiornato. Frontend: nuovo `<Textarea>` nel pannello target macro (per giorno, propagato anche da "Applica a tutti i giorni" perché fa già uno spread dell'oggetto corrente) e in ciascuna card esempio. `sezioneValorizzata()` ora considera anche `nota` per MACRO/ESEMPI nel dialog di conferma cambio modalità.

**Verifica**: backend `mvn test` → **195/195 verdi, BUILD SUCCESS** dopo la migrazione V23. Frontend `npx tsc --noEmit` pulito ad ogni passaggio; `npx vitest run` → **300/300 verdi** (suite intera, non solo i file toccati). Due test preesistenti in `PianoAlimentareFormView.spec.ts` sono stati aggiornati per riflettere i cambi di comportamento intenzionali di questa sessione, non un regressione nascosta: la fixture `pianoVuoto` aveva `dataFine: null` (ora obbligatoria per salvare, vedi sopra — bloccava "Attiva piano" nei test che passano da `salva()`); l'asserzione sul messaggio "sotto la soglia" cercava testo visibile, ora nel `title` dell'icona info (aggiunto `data-test="obiettivo-kcal-info"` per un selettore stabile). Nessuna verifica manuale in browser (nessun Claude in Chrome collegato in questa sessione, come nella precedente).

Aggiornati `wiki/api-contracts.md` (riga sul `PUT /piani-alimentari/{id}`) e `wiki/modello-dati.md` (righe `PianoGiornoMacroTarget`/`PianoEsempio`). Lavoro **non committato** — tocca ad Andrea, inclusa la verifica manuale del flusso completo (editor piano, tutte e 3 le modalità) e l'esecuzione di `flyway migrate`/riavvio backend per applicare V23 sul DB locale prima di testare Macro/Esempi con note.

## [2026-09-14] ingest | Lista piani: azioni per riga, "torna indietro" corretto; lista pazienti: colonne visita/piano ordinabili

Serie di richieste puntuali su liste e navigazione, una alla volta:

- **Lista piani alimentari**: aggiunto un avatar a iniziali (colorato per stato del piano, stesso `CLASSI_STATO` del badge) nella colonna Paziente, come nella lista pazienti. Aggiunto un menu "···" per riga (`PianoAlimentareRigaAzioni.vue`, stesso pattern di `PazienteRigaAzioni.vue`) con "Attiva piano"/"Elimina" (solo su `BOZZA`) e un bottone "Apri piano" (`FolderOpen`) sempre visibile — **rimosso il click sull'intera riga**, che era l'unica lista del progetto con quel comportamento (le altre usano un bottone esplicito).
- **Fix "torna indietro" su `VisitaFormView.vue`/`PianoAlimentareFormView.vue`**: entrambe le pagine sono raggiungibili da più punti (inclusa la Command Palette, Ctrl+K, disponibile da qualunque pagina), ma il link tornava sempre a un fallback fisso (dashboard/lista piani) invece che alla pagina chiamante reale. Corretto con `router.back()` quando c'è cronologia, fallback fisso solo se non c'è; l'**etichetta** ora riflette onestamente la destinazione — nuova label generica "Torna indietro" quando si usa `router.back()` (destinazione non prevedibile), non più "Torna alla dashboard"/"Torna ai piani alimentari" quando in realtà si torna altrove. In `PianoAlimentareFormView.vue` corretto anche un bug distinto: `pazienteIdQuery` veniva ignorato non appena il piano veniva salvato (route passata da `?pazienteId=X` a `/piani-alimentari/:id`), riportando sempre alla lista piani invece che al paziente di provenienza.
- **Validazione data fine > data inizio** sul piano alimentare: `erroreDataFinePiano` ora accetta la data di inizio e rifiuta una data di fine uguale o precedente; dato che dopo il salvataggio `dataInizioInput` non riflette più la data di partenza reale (campo non più editabile né aggiornato), introdotta `dataInizioEffettiva` (usa `piano.dataInizio` quando il piano esiste, altrimenti il draft locale) perché il controllo resti corretto sia in creazione sia in modifica.
- **Lista pazienti — avatar danger per archiviati**: l'avatar a iniziali ora usa il colore "danger" quando `paziente.archiviato`, invece del colore legato allo stato account.
- **Lista pazienti — due nuove colonne ordinabili, "Ultima/prossima visita" e "Piano alimentare"** (dopo un giro di chiarimento sulle opzioni, vedi sotto): entrambe richiedono dati non presenti come colonna su `Paziente` (derivati da `Visita`/`PianoAlimentare`), quindi non ordinabili nativamente via `Specification`+`Pageable`. `PazienteService.cerca` ora intercetta questi due campi di ordinamento e, solo per loro, carica l'intero risultato filtrato, lo ordina in Java (nulls sempre in fondo) e lo pagina manualmente (`cercaConOrdinamentoDerivato`) — scelta pragmatica per il volume di pazienti per professionista di questo dominio (non pensata per liste di migliaia di righe), evita una query nativa con subquery correlate. `piano` ordina alfabeticamente sul nome del piano più di recente creazione del paziente (qualunque sia il suo stato — bozza/attivo/scaduto/terminato — non solo l'attivo, su richiesta esplicita di Andrea "mostra l'ultimo piano... anche se scaduto o terminato indicandolo", ispirato al mockup `Hexis Pazienti.dc.html`); `PazienteResponse` espone `pianoNome`/`pianoStato`/`pianoDataFine`. La colonna "Piano alimentare" mostra il nome colorato per stato + una nota (scadenza/bozza/terminato); "Ultima/prossima visita" mostra la data (o "Oggi") con sotto, in rosso, "N giorni/settimane fa" per le date passate — **niente ramo per una data futura o un "nome appuntamento"**: chiarito esplicitamente con Andrea che non esiste ancora un modulo appuntamenti (sotto-progetto "Appuntamento" non implementato), quindi non si è costruita UI per dati inventati nonostante la richiesta iniziale lo suggerisse.
- **Colonna "Obiettivo" — sottotitolo "da quando"**: aggiunta `dataInizioObiettivo` a `PazienteResponse` — non la data dell'ultima visita, ma la data della prima visita (risalendo dall'ultima) con lo stesso obiettivo, cioè da quando è impostato l'obiettivo attuale. Calcolata in `PazienteService.dataInizioObiettivoPerPazienti`: una query batch su tutte le visite (non solo l'ultima), raggruppate per paziente, percorse a ritroso finché l'obiettivo non cambia.
- **Rimossa la colonna "Età"** dalla lista pazienti su richiesta esplicita.

**Verifica**: backend `mvn test` → **195/195 verdi, BUILD SUCCESS** (nessuna migrazione in questo giro: solo nuove query/campi calcolati, nessuna colonna DB aggiunta). Frontend `npx tsc --noEmit` pulito ad ogni passaggio; `npx vitest run` → **300/300 verdi**. Diversi test preesistenti aggiornati per riflettere i cambi intenzionali di questa sessione (non regressioni nascoste): fixture `Paziente` in 8 file di test integrate con i nuovi campi obbligatori (`pianoNome`/`pianoStato`/`pianoDataFine`/`dataInizioObiettivo`); in `PianoAlimentareFormView.spec.ts`/`PianiAlimentariListView` i test di "attiva"/"elimina" aggiornati per aprire il menu "···" prima di raggiungere la voce (ora teleportata, non più un bottone diretto); `vi.mock('@/api/pazienti')` aggiunto a `PianoAlimentareFormView.spec.ts` per evitare una chiamata di rete reale introdotta dal caricamento dell'ultima visita anche in modifica.

Aggiornato `wiki/api-contracts.md` (nuovi campi/opzioni di ordinamento su `GET /pazienti/ricerca` e `PazienteResponse`). Lavoro **non committato** — tocca ad Andrea, inclusa la verifica manuale in browser (nessun Claude in Chrome collegato in questa sessione).

## [2026-09-14] handoff | Fine sessione: tutto in staging, nessun commit

Andrea ha chiesto di salvare tutto per chiudere la sessione. `git add -A` eseguito su tutto il repo `hexis-nutrition`: include tutto il lavoro delle voci di log di oggi (editor piano alimentare, azioni/navigazione liste, nuove colonne lista pazienti) più alcune modifiche pre-esistenti non toccate da questo agente in nessuna sessione recente (`CommandPalette.vue`, `AggiungiAlimentoDialog.vue`, `ui/button/index.ts`, `DashboardView.vue`, `PazienteDettaglioView.vue`, già segnalate come modificate su disco durante la sessione — verosimilmente lavoro manuale di Andrea in un'altra finestra). Nessun commit, come da regola del progetto.

Aggiornato `wiki/stato.md`: nuova sezione di sessione, "Prossimo passo consigliato" riscritto per riflettere lo stato corrente (verifica manuale del piano alimentare + delle rifiniture di oggi come priorità, in particolare il campo Note su Macro/Esempi che richiede la migrazione V23 applicata).

## [2026-10-06] handoff | Tab Piani alimentari e Storico visite della scheda paziente, conteggio dashboard

Sessione diretta su `master` a richieste puntuali di Andrea (dopo il commit `36361b5`). Dettaglio completo in [stato](stato.md#sessione-del-6-ottobre-2026--scheda-paziente-tab-piani-alimentari-e-storico-visite-conteggio-dashboard).

- **Decisione**: "paziente attivo" = **non archiviato**, a prescindere da `statoAccount` (invito/app facoltativi). Nuovo `GET /pazienti/conteggio-attivi` per il tile della dashboard; domanda aperta del 2 settembre chiusa. Prima lettura sbagliata (anche filtro `statoAccount = ATTIVO`), corretta da Andrea.
- **Tab "Piani alimentari"** (prima placeholder) dal mockup `Hexis Dettaglio Paziente.dc.html`: card piano attivo con avanzamento e scadenza, **Duplica** (nuovo `POST /piani-alimentari/{id}/duplica`, bozza server-side), tabella "Piani precedenti" paginata lato server con icona lente, **senza colonna Stato e senza bozze**. Ricerca piani: nuovi `modalita`/`dataInizio` nelle righe, `ordinaPer=dataInizio`, filtri `escludiAttivo`/`escludiBozze`.
- **Tab "Storico misurazioni" → "Storico visite"**: 5 visite per volta con "Carica altre visite" che accoda (stile chat), lato server (`GET /pazienti/{id}/visite/storico`, `posizione` e variazioni calcolate dal server). Prima proposta (rivelare client-side le visite già in memoria) scartata da Andrea: "meglio caricarle 5 per volta".
- **Verifica**: backend 195/195, frontend 300/300, nessun test nuovo (li scrive Andrea), nessuna verifica manuale in browser. Test esistenti adattati dove il comportamento è cambiato intenzionalmente.
- **Processo**: riscritture multi-riga via `node -e` nella shell sono fallite più volte in modo silenzioso o hanno mangiato i backtick — verificare sempre con `grep` il risultato; parte dei file usa CRLF, preservare il line ending.
- Aggiornati `wiki/api-contracts.md`, `wiki/domande-aperte.md`, `wiki/stato.md`. In staging, nessun commit.

## [2026-10-10] ingest | Modalità del piano alimentare modificabile finché è in bozza

Decisione di Andrea: la modalità (Piano con pasti / Macros / Esempi intercambiabili) si può cambiare **solo su un piano nuovo o in bozza**; per i piani attivi, scaduti o terminati resta bloccata. Questo supera la regola precedente ("immutabile dopo la creazione") in [modello-dati](modello-dati.md) e [api-contracts](api-contracts.md), aggiornati.

- **Backend**: `AggiornaPianoAlimentareRequest` ha un campo `modalita` opzionale; `PianoAlimentareService.aggiorna` lo accetta solo se `stato = BOZZA` (altrimenti `PianoAlimentareModalitaNonModificabileException`, 409), svuota la struttura della vecchia modalità (`svuotaStruttura`, estratta da `elimina`) e popola la nuova con il contenuto della richiesta. `PianoAlimentare.setModalita` aggiunto. Suite backend 195/195 eseguita, nessun test nuovo.
- **Frontend** (`PianoAlimentareFormView.vue`): il selettore di modalità compare anche per le bozze già salvate, con la stessa conferma di perdita dati della creazione; la sezione della nuova modalità parte dal template se vuota (`preparaSezione`); `salva()` invia sempre `modalita`. `vue-tsc` pulito, nessuna verifica manuale in browser.
- **Da provare a mano** (Andrea): cambio modalità su una bozza salvata (con e senza dati nella sezione), salvataggio e riapertura; selettore assente su un piano attivo.

## [2026-10-10] ingest | Lista piani alimentari: filtri avanzati su data inizio/scadenza, colonna Inizio, esclusi i pazienti archiviati

Richieste di Andrea sulla lista dei piani alimentari ([api-contracts](api-contracts.md) aggiornato).

- **Filtri avanzati** (stesso schema della lista pazienti: sezione richiudibile con contatore "N attivi", "Pulisci filtri"): data inizio da/a e data scadenza da/a. Backend: `GET /piani-alimentari/ricerca` accetta `dataInizioDa`/`dataInizioA`/`dataFineDa`/`dataFineA` (ISO, estremi inclusivi, ciascuno opzionale; piani senza data di fine esclusi quando si filtra per data di fine). `CriteriRicercaPianiAlimentari` esteso, `PianoAlimentareSpecifications.conDataInizioTra`/`conDataFineTra`.
- **Colonna "Inizio"** ordinabile (prima di "Scadenza") e date nel formato `31 dic 2026`.
- **Pazienti archiviati**: la lista non mostra più i loro piani (`diPazientiNonArchiviati`, subquery correlata). Vale solo **senza `pazienteId`**: la tab piani della scheda paziente passa `pazienteId` e continua a mostrare i piani anche di un paziente archiviato.
- **Verifica**: backend 195/195 eseguito, `vue-tsc` pulito, nessun test nuovo, nessuna verifica manuale in browser (riavviare il backend prima di provare).

## [2026-10-10] handoff | Grafici Panoramica/Confronto, date digitabili, modalità del piano modificabile in bozza, filtri lista piani

Sessione a richieste puntuali di Andrea, su `master` dopo `00f725b`. Dettaglio completo in [stato](stato.md#sessione-del-10-ottobre-2026--grafici-panoramicaconfronto-date-digitabili-modalità-del-piano-modificabile-in-bozza-filtri-lista-piani). **Nessun commit, nessun `git add`**: tutto non in staging.

- **Scheda paziente**: grafici limitati alle ultime 5 visite; card unica "Massa magra e % grasso" (`ComposizioneChart.vue`, variazione % dalla prima visita mostrata, senza asse Y — la normalizzazione min-max faceva coincidere le linee); Confronto visite con barre più strette, colore A verde chiaro, tooltip; tab piani con "bozza più recente" e senza "Nuovo piano" ridondante; corretti i 3 errori `vue-tsc` preesistenti.
- **Pagina piano**: modalità modificabile solo su piani nuovi/bozza (backend `PUT` con `modalita`, 409 altrimenti); `DatePicker` `digitabile` e date `GG mmm AAAA`; bordi neri delle card dei pasti (variabile `--div2` mai definita); card paziente con avatar; fix kcal dei giorni in modalità Macros.
- **Lista piani**: colonna Inizio, stile tabella come i pazienti, filtri avanzati su data inizio/scadenza (backend `dataInizioDa/A`, `dataFineDa/A`), piani dei pazienti archiviati esclusi (solo senza `pazienteId`); vincolo "A non prima di Da" anche nella lista pazienti (`DatePicker` `min`/`max`).
- **Verifica**: backend 195/195, `vue-tsc` 0 errori; **suite frontend completa non eseguita** (1 fallimento atteso in `PianiAlimentariListView.spec.ts`: frase dello stato vuoto cambiata). Nessun test nuovo (li scrive Andrea, che li riprende nella prossima sessione). Verifiche manuali di Andrea: confermato il salvataggio dopo il riavvio del backend.
- **Incidente**: backend non riavviato dopo la modifica del `PUT` → il campo `modalita` veniva ignorato in silenzio e `pasti: null` svuotava i pasti di una bozza. Riavviare sempre il backend dopo modifiche a DTO/endpoint.
- **Processo**: in PowerShell `R` è alias di `Invoke-History` (una funzione con quel nome non parte); Python non è installato (usare `node -e`); due test temporanei di riproduzione (backend e frontend) scritti e cancellati.
- Aggiornati `wiki/stato.md`, `wiki/api-contracts.md`, `wiki/modello-dati.md`.

## [2026-10-20] handoff | Ordinamenti di default, stati del piano, note di aggiornamento, riepilogo visita precedente

Sessione a richieste puntuali di Andrea, su `master` dopo `3f30765`. Dettaglio completo in [stato](stato.md#sessione-del-20-ottobre-2026--ordinamenti-di-default-stati-del-piano-note-di-aggiornamento-riepilogo-visita-precedente). **Nessun commit, nessun `git add`**: tutto non in staging.

- **Ordinamenti**: piani per ultima modifica (`aggiornato_il` ora aggiornato davvero), pazienti per cognome, alimenti invariati.
- **Stati piano**: unificati in `BOZZA`/`ATTIVO`/`CONCLUSO` (scaduto e terminato sono la stessa cosa); DB invariato.
- **Pagina piano**: sola lettura oltre la bozza (anche `PUT` → 409), badge e avatar colorati per stato, header riordinato, riquadro informativo; "Duplica" solo frontend (endpoint backend rimosso).
- **Note di aggiornamento** (`piano_note`, V24): solo su piano attivo, leggibili su ogni piano non in bozza; autore salvato ma non mostrato; note sulla visita scartate.
- **Riepilogo precedente** nella pagina visita: visita precedente + note dei piani in corso tra le due visite (regola per date, non per `visita_id`).
- **Verifica**: backend 195/195, `vue-tsc` 0 errori, frontend 172/173 sui piani (fallimento noto) + 8 test dei grafici falliti non legati; **nulla provato in browser dall'agente**. Nessun test nuovo.
- **Incidente**: disco `E:` momentaneamente non disponibile a metà sessione, due modifiche rifatte, nessun dato perso. Riavviare il backend (migrazione V24).
- **Da riprendere con l'agenda**: stato dei pazienti, data di fine del nuovo piano = data della prossima visita.
- Aggiornati `wiki/stato.md`, `wiki/api-contracts.md`, `wiki/modello-dati.md`.

## [2026-10-22] handoff | Anagrafica paziente, circonferenze con parte dominante, colori degli stati

Sessione a richieste puntuali di Andrea, su `master` dopo `f6df1d7`. Dettaglio completo in [stato](stato.md#sessione-del-22-ottobre-2026--anagrafica-paziente-circonferenze-e-parte-dominante-colori-degli-stati).

- **Paziente**: nuovi `medicoRiferimento` (V25), `cellulare` (V26), `trattamenti` (V27); `telefono` = fisso; `note` mostrato come "Storia paziente" (2000 caratteri solo client).
- **Visita**: rimossi collo, avambraccio, caviglia, protocollo vita (**V28 elimina colonne e dati**); circonferenze riordinate con **parte dominante** e misure facoltative sulla parte dominante (V29); le 4 colonne esistenti = parte non dominante.
- **Lista pazienti**: colonne "Ultima visita" / "Prossima visita" (sempre "â") e filtro "Prossima visita" disattivato in attesa dell'agenda; badge/avatar account rosso/giallo/verde.
- **Piani alimentari**: badge/avatar Bozza giallo, Attivo verde, Concluso rosso. Rimosso "Aderenza piano".
- **Verifica**: backend 194/194, `vue-tsc` 0 errori, frontend: solo fallimenti noti nelle ultime esecuzioni, ma **suite intera non rieseguita a fine sessione**; **nulla provato in browser dall'agente**. Nessun test nuovo.
- **Aperto**: riavvio backend (V28 cancella dati), verifiche manuali, 4 decisioni lasciate ad Andrea (parte dominante obbligatoria? archiviati vs "Mai invitato"? colore nome piano in lista pazienti? regola 10 cifre telefono fisso).
- Aggiornati `wiki/stato.md`, `wiki/modello-dati.md`, `wiki/api-contracts.md`.

## [2026-11-10] handoff | Piano attivo modificabile, "Annulla" con conferma, rifiniture UI

Sessione a richieste puntuali di Andrea, su `master` dopo `3069c95`. Dettaglio completo in [stato](stato.md#sessione-del-10-novembre-2026--piano-attivo-modificabile-annulla-con-conferma-rifiniture-ui).

- **Piano attivo modificabile**: `PUT /piani-alimentari/{id}` risponde 409 solo per i piani `CONCLUSO` (prima: tutto tranne `BOZZA`); modalità modificabile anche sull'attivo; **`dataInizio` resta non modificabile** (richiesta di Andrea). Frontend: sola lettura solo per i concluso, riquadro informativo solo lì; le note di aggiornamento restano invariate. Un 409 visto in sessione era dovuto al backend non riavviato.
- **"Annulla" con conferma**: pulsanti "Annulla"/"Salva" in fondo alla pagina del piano; dialog "Uscire senza salvare?" su piano (confronto con ultimo salvataggio) e su visita (`modificato()` in `DatiVisitaForm`/`PlicometriaForm`). Il dialog scatta anche dal link "Torna al paziente" in alto (`@click.capture.prevent`, altrimenti il `router-link` naviga per primo).
- **UI**: etichetta fascia BMI rimossa da form visita, storico visite e riepilogo; icona occhio in "Piani precedenti"; badge stato account nel dettaglio paziente accanto a "Nessuna visita programmata", stesso stile e icone `MailX`/`MailCheck`/`UserCheck`, le stesse icone nel badge della lista pazienti (formato compatto).
- **Verifica**: backend 194/194, `vue-tsc` 0 errori, form piano 10/10 e dettaglio paziente 16/16; restano il fallimento noto di `PianiAlimentariListView.spec.ts` e 8 test dei grafici (mock `VisTooltip`), non toccati. **Nulla provato in browser dall'agente.** Nessun test nuovo, nessun commit/`git add`.
- **Aperto**: uscite senza conferma da sidebar/Command Palette/tasto indietro (nessuna guardia di navigazione), verifiche manuali (salvataggio piano attivo dopo riavvio backend, lato paziente).
- Aggiornati `wiki/stato.md` e `wiki/api-contracts.md`.

## [2026-11-10] ingest | Piano attivo modificabile (ADR 0006)

Fonte: [sorgenti/2026-11-10-piano-attivo-modificabile.md](sorgenti/2026-11-10-piano-attivo-modificabile.md) (richieste di Andrea in sessione). Nuovo [ADR 0006](decisioni/0006-piano-attivo-modificabile.md): bozza e attivo modificabili, solo il concluso in sola lettura, nessuna versione, `data_inizio` immutabile. Le due scelte proposte dall'agente (concluso resta bloccato, note invariate) sono state **confermate da Andrea** il 2026-11-10 e così indicate nell'ADR. Aggiornati `wiki/index.md` e `wiki/stato.md` (tolto il punto aperto "ADR non scritto").

## [2026-11-10] handoff | Agenda: design e piano, implementazione nella prossima sessione

Dettaglio in [stato](stato.md#sessione-del-10-novembre-2026-parte-2--agenda-design-e-piano). Nessun codice scritto, nessun commit.

- **Design** `docs/superpowers/specs/2026-11-10-agenda-design.md` (revisione builder: nessuna domanda importante) e **piano** `docs/superpowers/plans/2026-11-10-agenda.md` (11 task).
- **Deciso**: tutto il mockup compreso "Blocca fascia oraria"; "Avvia visita" apre il form di visita e salvandola l appuntamento diventa `CONCLUSA`; sovrapposizioni con avviso e conferma del professionista; annullate visibili, grigie e barrate; archiviare un paziente elimina visite e promemoria futuri; link facoltativo sul videoconsulto; data fine nuovo piano = prossima visita; giorni festivi evidenziati; calendario costruito a mano, nessuna libreria.
- **Stato paziente**: chiuso, resta solo mai invitato / invitato / attivo (gia implementato).
- **Aperto**: modalita di esecuzione (consigliata subagent-driven), riconferma dell eccezione `:style` per posizione e dimensione degli eventi in griglia, piano senza test per scelta di Andrea.
- Aggiornato `wiki/stato.md`.

## [2026-11-15] handoff | Agenda

Implementata l'Agenda (11 task, esecuzione subagent-driven sul design e il piano del 2026-11-10), su `master` dopo `f093d4e`. Dettaglio in [stato](stato.md#sessione-del-15-novembre-2026--agenda-implementata). **Tutto in staging (`git add`), nessun commit.**

- **Backend**: pacchetto `agenda/` (V30, entità `Appuntamento`, `AgendaController` con sei endpoint, `AgendaService`, 409 `{"conflitti":[...]}` sulle sovrapposizioni), `prossimaVisita` in `PazienteResponse`, filtro `prossimaVisitaEntroGiorni` (7/14/21/30), archiviazione che elimina visite programmate e promemoria futuri, `POST /pazienti/{id}/visite?appuntamentoId=` che conclude l'appuntamento. `mvn test` 194/194 verdi al Task 5.
- **Frontend**: pagina `/agenda` con viste Giorno/Settimana/Mese (costruite a mano), dialog appuntamento, menu azioni, festività; colonna e filtro "Prossima visita" in lista, badge e "Fissa appuntamento" nel dettaglio, "Nuovo appuntamento"/"Nuovo promemoria"/"Blocca fascia oraria" attivi in palette e dashboard, data di fine del piano precompilata dalla prossima visita. `vue-tsc` 0 errori; suite intera 291/300, i 9 fallimenti sono quelli noti non correlati.
- **Nessun test scritto per scelta di Andrea**; adattati solo gli spec esistenti (rotta `agenda` nei router di test, `prossimaVisita: null` nei mock, terzo argomento di `creaVisita`).
- **Da segnalare ad Andrea**: `:style` anche per il `top` della linea rossa "ora corrente" in `AgendaSettimana` (estensione dell'eccezione approvata per posizione e dimensione degli eventi), da confermare.
- **Aperto**: riavviare il backend (V30), verifiche manuali in browser (elenco in stato), test candidati dal "Review Focus" del piano, Minor rinviati dalle review.
- Nuovo [ADR 0007](decisioni/0007-appuntamenti-e-visite-registrate.md). Aggiornati `wiki/api-contracts.md`, `wiki/modello-dati.md`, `wiki/index.md`, `wiki/glossario.md`, `wiki/architettura.md`, `wiki/stato.md`.

## [2026-11-15] handoff | Agenda: revisione finale, selettori data/ora, test alla prossima sessione

Dettaglio in [stato](stato.md#sessione-del-15-novembre-2026--agenda-implementata). Nessun commit; tutto in staging.

- **Revisione finale** (whole-branch, Opus): "Ready to merge", nessun Critical/Important. Corretti 4 Minor: filtro per giorno in `AgendaGiorno`, guardia di staleness e "Salva" disabilitato in `AppuntamentoDialog`, toast specifico in `VisitaFormView` per `appuntamentoId` non valido, `oggiIso()` in `DatiVisitaForm`.
- **Selettori**: il `DatePicker` digitabile si apre con un click sul campo; nuovo `TimePicker` (`components/ui/time-picker/`) al posto di `type="time"`, con lo stesso aspetto del `DatePicker`.
- **Verifica**: backend 194/194, `vue-tsc` 0 errori, frontend 291/300 (9 falliti noti non correlati). Nulla provato in browser, nessun test nuovo.
- **Prossima sessione**: Andrea scrive i test (candidati in `stato.md`, incluso `TimePicker`); prima riavviare il backend (V30) e fare le verifiche a mano. Aperti: OK sull'eccezione `:style` della linea rossa, durata massima 120 minuti nel dialog.
- Aggiornato `wiki/stato.md`.

## [2026-11-16] handoff | Notifiche dell'agenda

- Orario calendario 07:00-20:00, Textarea per testo/note, dialog di dettaglio degli eventi con link alla scheda paziente.
- Notifiche dell'agenda con campanello, puntino in sidebar e Web Push generiche: backend (V31, V32, scheduler, `WebPushService`) e frontend (store, lista, service worker). Decisione in [decisioni/0008](decisioni/0008-notifiche-agenda.md).
- Backend 194/194, `vue-tsc` pulito, frontend 291/300 (9 falliti noti). Nessun commit, nessun test nuovo. Da fare a mano: chiavi VAPID, riavvio backend, prove su Windows e Mac.
- Aggiornati `api-contracts.md`, `modello-dati.md`, `glossario.md`, `index.md`, `stato.md`, ADR 0007.

## [2026-11-16] handoff | Notifiche dell'agenda: revisione e fine sessione

- Revisione finale con agente separato: nessun Critical; corretti I1 (timeout push + 2 thread dello scheduler), I3 (endpoint push validato: https, allowlist, max 10 dispositivi) e l'ordine della richiesta di permesso (Safari). Backend 194/194, `vue-tsc` pulito.
- Chiavi VAPID di sviluppo impostate nella configurazione di avvio di STS; `GET /notifiche/push/chiave` risponde correttamente. Nulla provato nel browser: troppo tardi, riprendere da lì.
- Aperto: avviso push soppresso con scheda visibile (rischio Safari); Minor rimandati M1-M8 (in `stato.md`), in particolare M7 e M5. Test da scrivere a cura di Andrea.
- Aggiornati `stato.md` (sezione del 16 novembre e "Prossimo passo"), ADR 0008. Nessun commit.

## [2026-11-16] handoff | Palette petrolio + lime, rifiniture UI e blocchi colorati

- Applicata la palette "Verde petrolio + Lime" via token (chiaro e scuro), con rossi/ambra nuovi, contrasti rivisti, hover dei pulsanti centralizzato, etichette dei campi in minuscolo. Decisione in [decisioni/0009](decisioni/0009-palette-verde-petrolio-lime.md).
- Cinque blocchi petrolio: banner Dashboard, intestazione scheda paziente, card piano attivo, tessera Energia del piano, fascia "Oggi" dell'agenda.
- Rifiniture: dialog di dettaglio con titolo fisso e link alla scheda paziente, campanello più grande, orario 07:00-20:00.
- Tema scuro rifatto in grafite neutra dopo il rifiuto di Andrea; feedback atteso. Nulla visto nel browser da Claude. `vue-tsc` pulito, frontend 291/300 (9 falliti noti). Nessun commit, nessun test nuovo.
- Aggiornati `stato.md`, `index.md`, nuova ADR 0009.

## [2026-11-16] handoff | Revisione UI: tema scuro "Notte di petrolio", grafici, dashboard

- Revisione dell'app in Chrome con la skill frontend-design, chiaro e scuro. Tema chiaro intatto; tema scuro rifatto (terza versione) dopo prova dal vivo nel browser: fondo quasi nero, sidebar più scura della pagina, hero in petrolio profondo, pillole hero su token `--hero-pill*` al posto di `bg-white`. Aggiornata [decisioni/0009](decisioni/0009-palette-verde-petrolio-lime.md).
- Dashboard: tolti i placeholder "Disponibile a breve"; nuovi blocchi "Prossime visite" (7 giorni) e "Piani in bozza"; link di navigazione con freccia (solo navigazione, non azioni).
- Agenda: tolta la fascia "Oggi"; appuntamenti sovrapposti sfalsati a cascata.
- Scheda paziente: grafici solo da 2 visite in su, senza il valore ripetuto (resta nella riga di indicatori).
- Tabelle: intestazioni ed etichette in minuscolo, nomi in Figtree, nomi BDA leggibili. "Concluso" grigio; avatar dello stesso colore del badge.
- Palette grafici con quattro tinte distinte; confronto visite grigio/petrolio.
- Test: adeguati gli spec esistenti di AndamentoChart, PazienteTabPanoramica, Dashboard (comportamento cambiato). Frontend 289/296: i 7 falliti sono preesistenti (mock `VisTooltip` in ConfrontoBarChart, stato vuoto della lista piani). Nessun commit.
- Aggiunta: logo e favicon ricolorati su petrolio + lime (favicon in tessera scura), pulsanti testo senza sottolineatura con freccia che scorre all'hover. ADR 0009 aggiornata.
- Aggiunta: tolte le ombre dalle card nella pagina (restano solo sugli elementi sovrapposti); tutti i pulsanti azione scendono di 2 px al click, come "Crea nuovo". ADR 0009 aggiornata. Frontend sempre 289/296 (7 falliti preesistenti).
- Aggiunta: link "Torna indietro" uniformati (freccia da 14 px che scorre a sinistra all'hover, anche in "Password dimenticata"). ADR 0009 aggiornata.

## [2026-11-16] handoff | Fine sessione revisione UI

- `stato.md`: nuova sezione "Sessione del 16 novembre 2026 (terza parte)" e voce in cima a "Prossimo passo consigliato"; segnalato che sostituisce tema scuro grafite, hero uguali nei due temi e fascia "Oggi" della sezione precedente.
- Aperto: pagine non verificate (favicon, login mobile, editor del piano e dialog senza ombre, agenda Mese/Giorno in scuro), 7 test preesistenti da sistemare, pressione su chip e selettori da decidere, token `--shadow` inutilizzato. ADR 0009 resta `in-discussione`.
- Nessun commit.

## [2026-11-17] handoff | Stima del progetto e proposta di collaborazione

- Sessione senza codice: stima del valore di Hexis per l'amico che lo finanzia. Ore di mercato: già fatto 620–950 h, da fare 730–1.280 h; prodotto pronto da vendere 47–112 k€ (freelance) / 81–178 k€ (agenzia). Ore reali ricostruite da commit e diario: ~90–130 h in 20 giornate.
- Decisioni a voce: app paziente anche in Flutter, accanto alla webapp Vue; il software sarà venduto, con modello di vendita da decidere; hosting, dominio e store a carico del finanziatore.
- Prodotti in `docs/` (non tracciati): `stima-progetto-hexis.html` + `.pdf` (privato per Andrea) e `proposta-collaborazione-hexis.html` (per l'amico, con campi "da definire"). Entrambi pubblicati anche come artifact privati. Riletti da un subagent indipendente, correzioni applicate.
- Aggiornati `stato.md` (nuova sezione e voce in cima a "Prossimo passo consigliato") e `domande-aperte.md` (Flutter, modello di vendita, verifiche legali BDA-IEO/MDR/GDPR, accordo). Nessun commit.

## [2026-11-17] handoff | Proposta: sezione manutenzione e PDF

- Aggiunta a `docs/proposta-collaborazione-hexis.html` la sezione "Il software va mantenuto nel tempo": correzione bug, sicurezza e aggiornamenti tecnici, compatibilità iOS/Android, server e backup, assistenza, adeguamenti normativi (canone) e nuove funzionalità (monte ore o preventivo). Card "Manutenzione e assistenza" con canone + monte ore mensile; voce "Manutenzione" in "Da mettere per iscritto".
- Generato `docs/proposta-collaborazione-hexis.pdf` (7 pagine A4, campi "da definire" ancora vuoti, compreso monte ore e tempi di risposta per i bug bloccanti). Artifact della proposta ripubblicato.
- `stato.md` aggiornato in "Prossimo passo consigliato". File di `docs/` non tracciati, nessun commit.

## [2026-11-19] ingest | JSON alimenti BDA aggiornati

- Copiati in `sorgenti/2026-11-19-alimenti-bda-json/` i 1109 JSON da `E:\ProgettoEdo_files\Hexis-Alimenti-BDA` (copia verificata con hash, 3,2 MB). Rispetto ai JSON da cui è stato generato V19 (`ProgettoEdo\public\foods`), Andrea ha aggiunto nuovi dati e corretto valori errati.
- Da fare: nuova migrazione (V33) che aggiunge le colonne nuove e corregge le esistenti con `UPDATE ... FROM (VALUES ...)` su `codice_bda`. V19 non si tocca (checksum Flyway) e le righe non si reinseriscono (gli id sono referenziati dai piani). Lo script di generazione è in `docs/superpowers/plans/2026-09-05-alimenti.md`, da adattare.

## [2026-11-19] ingest | Reimport completo alimenti BDA (V33)

- Verificati i JSON in `sorgenti/2026-11-19-alimenti-bda-json/`: 1109 file identici byte per byte a `E:\ProgettoEdo_files\Hexis-Alimenti-BDA`, tutti con le stesse 103 voci. `note_alimento` vale `0` quando manca: importato come NULL.
- Sostituisce il "da fare" della voce precedente. Rimossi `V33__correggi_alimenti_bda.sql` (solo `UPDATE` delle colonne storiche) e `sorgenti/2026-11-19-alimenti-bda/`, creati da un'altra sessione da una copia dei JSON ormai vecchia (633 file diversi). Decisione di Andrea.
- Nuova `V33__reimporta_alimenti_bda_completi.sql`: elimina tutti i piani, svuota `alimenti` (anche i personalizzati), aggiunge 81 colonne (`parte_edibile_percentuale`, `anno`, `note` + 78 nutrienti) e reinserisce i 1109 alimenti. Sul DB di sviluppo `hexis` cancellerà 3 piani e 1 alimento personalizzato al prossimo avvio.
- Backend: record `@Embeddable` `ComposizioneEstesa`, `Minerali`, `Zuccheri`, `AcidiGrassi`, `Amminoacidi`, `Vitamine` su `Alimento`; esposti in `AlimentoResponse`; copiati da "duplica". Frontend: tipi opzionali in `src/api/alimenti.ts`, nessuna vista.
- `mvn clean test`: 194/194 verdi (`hexis_test` ricreato da Flyway). Dati seminati confrontati a campione con i JSON. `vue-tsc --noEmit` pulito. Aggiornati `modello-dati.md`, `api-contracts.md`, ADR 0005. Nessun commit.

## [2026-11-19] query | Dati estesi alimenti solo nel DB

- Su indicazione di Andrea (non mandare al frontend dati non usati) annullata la parte applicativa della voce precedente: tolti i record `@Embeddable`, i campi da `AlimentoResponse` e i tipi opzionali dal frontend. La duplicazione copia di nuovo solo i campi storici. Le 81 colonne della V33 restano, valorizzate per i BDA e mai lette dal codice.
- Unica modifica di codice rimasta: `V33__reimporta_alimenti_bda_completi.sql`. `mvn clean test` 194/194, `vue-tsc --noEmit` pulito. Aggiornati `modello-dati.md` e `api-contracts.md`. Nessun commit.

## [2026-11-19] handoff | Reimport completo alimenti BDA

- `stato.md`: nuova sezione "Sessione del 19 novembre 2026", riga in "Dove siamo" e voce in cima a "Prossimo passo consigliato".
- Aperto: riavviare il backend per applicare V33 a `hexis` (cancella 3 piani e 1 alimento personalizzato), verifica a mano di catalogo e piani, commit di V33 + JSON in `sorgenti/` + wiki. I nutrienti nuovi si espongono solo quando una vista ne avrà bisogno.
- Nessun commit.

## [2026-11-25] handoff | Revisione UI in Chrome, liste a schede, contrasti, editor piano

- Giro completo di `frontend-professionisti` in Chrome (chiaro, scuro, mobile a 390 px con iframe nella stessa origine). Report e lavoro rimanente in `docs/ui-review-2026-11-25.md`.
- Fatto: utility condivisa per iniziali e colori avatar; `pazienteNome`/`pazienteCognome` nelle risposte dei piani (backend, `mvn test` 194/194, `api-contracts.md` aggiornato); virgola e unità in Alimenti; dialog "Aggiungi alimento" (focus, posizione, pertinenza, nomi BDA); titoli e footer dei dialog uniformi; BMI come valore calcolato; maiuscole residue tolte; icone su 4 misure; toast coerenti; errori sotto il campo nelle pagine di accesso; liste Pazienti e Piani a schede (mobile sempre, interruttore su desktop) e Alimenti a righe su mobile; contrasti dei due temi (`--fg4` solo non-testo in chiaro, superfici e bordi alzati in scuro); varianti `hero`/`accent` del `Button`, `outline` → `neutral`, `aria-label` sui pulsanti solo icona, pulsanti "aggiungi" con icona `Plus`.
- Editor piano provato con dati veri: corretti obiettivo kcal vuoto (barre a 0%), scroll al primo errore su "Salva", obiettivo nella tessera Energia, unità in tabella, matita del pasto, pulsanti di rimozione più grandi, X degli esempi visibile da touch, tab di modalità da mobile. Resta aperto il ridisegno degli esempi e la tabella del pasto da mobile.
- ADR 0009 aggiornata con le regole nuove (contrasti, pulsanti, icone, dialog, liste). `stato.md`: nuova sezione e voce in cima a "Prossimo passo consigliato".
- Verifiche: `vue-tsc --noEmit` pulito, `vitest` 289/296 (7 falliti noti e preesistenti). Bozza di prova salvata nel DB `hexis` per Beatrice De Luca. Riavviare il backend. Nessun commit.

## [2026-11-25] handoff | Esempi intercambiabili ridisegnati, tabella del pasto da mobile

- Esempi: sezione per categoria con conteggio, schede bianche "Esempio N" (nome predefinito cambiato lato frontend; il backend crea ancora gli esempi con il nome della categoria, ma il primo salvataggio li sostituisce), kcal per alimento e totali kcal/macro per scheda, eliminazione nel menu "⋯", "Nuovo esempio" come scheda tratteggiata nella griglia.
- Pasto: sotto 768 px righe compatte al posto della tabella (X sempre a portata, macro sotto); da 768 px resta la tabella.
- Verifiche: `vue-tsc --noEmit` pulito, `vitest` 289/296 (7 falliti noti). Provato in Chrome su desktop e a 390 px. Nessun commit.

## [2026-11-25] handoff | Editor piano (punto 1) ed editor visita provato con dati

- Piano: riepilogo e giorni sticky, pallini dei pasti tolti, margine allineato, intestazione mobile, Macros con controllo kcal↔macro, "Duplica pasto" e "Copia negli altri giorni", scelta paziente con pazienti recenti, `autocomplete="off"` sui campi di ricerca. Data di fine dalla prossima visita già presente.
- Visita: provata con dati (visita salvata per Beatrice De Luca). Corretti scroll al primo errore (`utils/form.ts`, anche in nuovo paziente e piano), tripla misurazione che conserva il valore, esempi delle circonferenze.
- Il watcher di Vite si è bloccato a metà: le ultime modifiche non sono state viste in Chrome. Riavviare `npm run dev` e ricontrollare.
- Verifiche: `vue-tsc --noEmit` pulito, `vitest` 289/296 (7 falliti noti). Nessun commit.

## [2026-11-25] handoff | Verifiche dopo il riavvio di Vite, barre di scorrimento

- Dopo il riavvio di `npm run dev` verificati in Chrome: scroll al primo errore (con focus), tripla misurazione che conserva il valore, esempi delle circonferenze, titolo e tab del piano da mobile.
- Barre di scorrimento colorate in tutta l'app (token `--scroll-thumb`, `main.css`), provate in chiaro e scuro. ADR 0009 aggiornata.
- Nessun commit.

## [2026-11-25] handoff | Chiusura sessione revisione UI

- `stato.md`: sezione del 25 novembre aggiornata (aperti residui, dati di prova da eliminare, nota sul watcher di Vite) e "Prossimo passo consigliato" riscritto: commit, pulizia dati di prova, poi barra azioni fissa, grafici della scheda paziente, stati vuoti/Dashboard, mobile, pagine di accesso.
- `docs/ui-review-2026-11-25.md`: aperti ripuliti, nuovo ordine consigliato (§6) e riepilogo di quanto fatto dopo la prima stesura (§7).
- Verifiche finali: backend `mvn test` 194/194 (dopo l'aggiunta dei campi dei piani), frontend `vue-tsc --noEmit` pulito, `vitest` 289/296 (7 falliti noti e preesistenti). Tutto in staging, nessun commit.

## [2026-11-25] query | Lime desaturato in scuro: scartato

- Valutato con la skill frontend-design: il lime resta `#C7F36B` anche in scuro (solo segnali piccoli e pulsante `accent` sui blocchi hero). Decisione in ADR 0009; aperti tolti da `stato.md` e `docs/ui-review-2026-11-25.md`. Nessun codice toccato, nessun commit.

## [2026-11-25] handoff | Barra azioni fissa nei form lunghi

- Nuovo `components/form/BarraAzioniForm.vue`: pillola flottante `rounded-2xl` con bordo e ombra, `sticky` in fondo al `<main>`, "Modifiche non salvate" con punto lime alla prima modifica, "Annulla"/"Salva" con etichette e `data-test` invariati. Usata in nuovo paziente, visita ed editor piano (non in sola lettura). Prima versione a fascia piena, poi resa flottante e arrotondata su richiesta di Andrea.
- Design e piano: `docs/superpowers/specs/2026-11-25-barra-azioni-form-design.md`, `docs/superpowers/plans/2026-11-25-barra-azioni-form.md`. ADR 0009 aggiornata.
- Verifiche: provata in Chrome (chiaro, scuro, 390 px via iframe) sui tre form; `vue-tsc --noEmit` pulito; `vitest` 289/296 (7 falliti noti). Nessun test nuovo. In staging, nessun commit.

## [2026-11-25] handoff | Grafici della scheda paziente

- `utils/obiettivo.ts`: verso "buono" di ogni parametro per obiettivo (tabella approvata da Andrea). Indicatori della scheda e colonna Variazione della tabella del Confronto ora verdi/rossi solo se l'obiettivo dà una direzione, grigi altrimenti (prima: peso giù sempre verde, tabella sempre ocra).
- Panoramica: valore su ogni punto dei grafici Peso e BMI, ultimo punto più grande e con unità; Massa magra e % grasso con il valore reale sull'ultimo punto.
- Confronto visite: provato un grafico a barre divergenti delle variazioni, scartato da Andrea. `ConfrontoBarChart.vue` ridisegnato in HTML/CSS (A in tinta chiara, B pieno, valori sulle barre, scala per parametro).
- Verifiche: Chrome chiaro/scuro/390 px, obiettivi Mantenimento, Dimagrimento e Aumento peso (questi due simulati solo nel browser, nessun dato cambiato). `vue-tsc` pulito; `vitest` 289/296: 6 falliti in `ConfrontoBarChart.spec.ts` (testa la vecchia implementazione Unovis, da riscrivere) + 1 noto. In staging, nessun commit.

## [2026-11-25] handoff | Spec di ConfrontoBarChart riscritto

- `ConfrontoBarChart.spec.ts` riscritto su richiesta di Andrea per la versione HTML/CSS: 9 test (titolo, messaggio senza misure, righe a un lato escluse, valori senza unità in verticale e con unità in orizzontale, scala per coppia, tinta di A e colore di B, orientamento, legenda). Aggiunti `data-test="barra"` e `data-colore` alle barre del componente.
- `vue-tsc` pulito; `vitest` 296/297, resta solo il fallito noto di `PianiAlimentariListView.spec.ts`. In staging, nessun commit.

## [2026-11-25] handoff | Suite frontend tutta verde

- `PianiAlimentariListView.spec.ts`: il test dello stato vuoto cercava "Nessun piano trovato", testo vecchio; la pagina senza filtri mostra "Nessun piano presente" con il pulsante "Nuovo piano" (con filtri: "Nessun risultato trovato"). Allineato il test, pagina invariata.
- `vitest` 297/297 verde per la prima volta da agosto; `stato.md` e review aggiornati. In staging, nessun commit.

## [2026-11-25] handoff | Chiusura seconda parte: barra azioni, grafici, suite verde

- `stato.md`: nuova sezione "Sessione del 25 novembre 2026 (seconda parte)" (decisioni: lime invariato, verso dei parametri per obiettivo, confronto A/B senza colori per obiettivo; fatto; non verificato), riga in "Dove siamo" e nuova voce in cima a "Prossimo passo consigliato".
- Aperti: commit di tutto lo staging, eliminazione dei dati di prova di Beatrice De Luca; poi stati vuoti e Dashboard, mobile (agenda Giorno, scheda paziente compatta), pagine di accesso, piccoli (titolo conferme, "Invita" doppione, card BIA, command palette ".com", pertinenza alimenti nel backend, anteprima % grasso e BIA nella visita).
- Stato finale: `vue-tsc` pulito, `vitest` 297/297. Nessun commit.

## [2026-11-25] handoff | Stati vuoti e Dashboard orientata alle azioni

- Brainstorming (con frontend-design) → design breve → piano → executing-plans inline su `master` (nessun worktree: lo staging di oggi non sarebbe stato incluso; nessun commit; nessun test nuovo per preferenza di Andrea).
- Nuovi `components/StatoVuoto.vue` e `components/dashboard/DaFareCard.vue`; modificati Dashboard, 4 tab della scheda paziente, `AgendaSettimana`, `AgendaGiorno`, `AgendaView`; adeguati 2 test ai nuovi testi.
- Revisione indipendente (Opus): corretti 2 Important ("Avvia visita" spariva all'ora di inizio; "libera" mostrato con un filtro attivo). Minor rimandati: settimana di soli festivi mostra "Settimana libera"; "Nuovo appuntamento" dalla settimana propone il giorno di riferimento e non oggi; titolo di `StatoVuoto` è un `<p>` e non un heading; Dashboard carica i blocchi in sequenza; una risposta `undefined` di `cerca` blocca la card (succede solo con mock incompleti).
- Verifiche: `vue-tsc` pulito, `vitest` 297/297, Chrome chiaro/scuro/390 px. In staging, nessun commit.

## [2026-11-25] handoff | Minor della revisione su stati vuoti e Dashboard

- Corretti su richiesta di Andrea i 5 Minor: titolo di `StatoVuoto` ora è un heading (prop `livello`, `h4` nelle tab della scheda paziente); Dashboard carica conteggio, agenda e "Da fare" in parallelo e tratta una risposta senza contenuto come errore del gruppo; striscia "Settimana libera" che dice quanti giorni sono festivi; "Nuovo appuntamento" dagli stati vuoti propone oggi se la settimana lo contiene, altrimenti il primo giorno non festivo (dalla vista Giorno, quel giorno).
- Verifiche: `vue-tsc` pulito, `vitest` 297/297, Chrome (settimana 21-27 dicembre: "2 sono festivi", dialog su 21 dic; Dashboard carica). In staging, nessun commit.

## [2026-11-25] handoff | Mobile e pagine di accesso

- Brainstorming e frontend-design → design breve → piano → esecuzione inline su `master` (nessun commit, nessun test nuovo). Spec e piano: `docs/superpowers/specs|plans/2026-11-25-mobile-e-accesso*`.
- Agenda in vista Giorno da mobile; testata del paziente compatta da mobile con menu "⋯"; dati clinici prima del profilo; stato account + invito in un'unica pillola; `AccessoLayout.vue` per le tre pagine di accesso; regola `:-webkit-autofill`; import mancante di `ShieldCheck` corretto.
- Revisione indipendente (Opus): corretti 1 Important (autocompletamento scavalcato al focus, fondo diverso in scuro) e 5 Minor. Rimandati: area di tocco di "Invita" nella pillola (~24 px); colori `white/*` del pannello di accesso non a token; `h2` del pannello prima dell'`h1` della pagina; vista dell'agenda decisa solo all'apertura.
- Verifiche: `vue-tsc` pulito, `vitest` 297/297; Chrome a 390 px (scuro) e desktop: reset, agenda, scheda paziente; password dimenticata con un indirizzo inesistente (nessuna email). Autocompletamento reale non verificabile da automazione. In staging, nessun commit.

## [2026-11-25] handoff | Piccoli della review e minori rimandati

- Frontend: titolo delle conferme (`AlertDialogTitle`) in Fraunces corsivo 20 px come i dialog; BIA nella Panoramica come riga sottile invece di una card da un terzo di riga (gli altri segnaposto BIA restano, i test li richiedono); "Invita" nella pillola con area di tocco più ampia e `Loader2` durante l'invio; pannello di accesso con token (`--on-hero`, `--side-fg`, `--side-act-bg`, `--side-bd`) al posto dei bianchi e motto come `<p>` invece di `<h2>`; dialog "Aggiungi alimento" che chiede `ordinaPer=pertinenza` al server (via il riordino nel browser).
- Backend: `ordinaPer=pertinenza` su `GET /alimenti/ricerca` (`AlimentoSpecifications.ordinataPerPertinenza`, pagina senza `Sort`); ricerca pazienti con email che combacia solo dall'inizio (la command palette non trova più tutti con ".com"). `api-contracts.md` aggiornato. Nessun test nuovo (da decidere con Andrea).
- Dati di prova di Beatrice De Luca: restano, decisione di Andrea.
- Verifiche: `mvn test` 196/196 (con i due test nuovi), `vue-tsc` pulito, `vitest` 297/297 (adeguato il test della BIA nella Panoramica); Chrome per titolo delle conferme, riga BIA e pannello di accesso. **Da provare dopo il riavvio del backend**: palette con ".com", "Aggiungi alimento" con "pasta". In staging, nessun commit.

## [2026-11-25] handoff | Test per pertinenza alimenti e ricerca per email

- Su richiesta di Andrea: `AlimentoControllerTest.ricercaPerPertinenzaMettePrimaINomiCheInizianoColTestoEUltimiIMatchSullaCategoria` (ordine a 4 livelli e paginazione con conteggio) e `PazienteSpecificationsTest.conRicercaConfrontaLEmailSoloDallInizio` (prefisso sì, ".com" e "example" no). Scritti dopo l'implementazione: con il codice precedente fallirebbero (ordine alfabetico; email confrontata in qualunque punto), ma non sono stati eseguiti contro di esso.
- `mvn test` 196/196. In staging, nessun commit.

## [2026-11-25] handoff | Fine giornata: sezione 6 della revisione chiusa

- `stato.md`: riga in "Dove siamo" e nuova voce in cima a "Prossimo passo consigliato" (commit, riavvio backend e prove a mano, aperti di prodotto, nuovo giro di revisione).
- Stato finale: backend `mvn test` 196/196, frontend `vue-tsc` pulito e `vitest` 297/297; 54 file in staging; nessun commit.
- Subito dopo: nuovo giro di revisione completo in Chrome con frontend-design più una revisione indipendente del codice UI da un secondo agente.

## [2026-11-25] query | Secondo giro di revisione UI

- Giro visivo in Chrome (Claude) più revisione indipendente del codice UI (Opus), entrambi con frontend-design. Report: `docs/ui-review-2026-11-25-giro2.md`.
- Verdetto: coerente in buona parte, moderna sì, accattivante con misura, chiara quasi. Primi da fare: togliere "Chat"/"Analytics" dalla barra laterale, glossario delle azioni, conferme distruttive, doppione "Invita" nella lista pazienti, stati vuoti delle liste, "Comprimi" che non resta.
- Nessun codice modificato.

## [2026-11-25] handoff | Chiusura chat

- Andrea ha committato il lavoro della giornata (`cb8d482`). Restano in staging solo `docs/ui-review-2026-11-25-giro2.md`, `wiki/log.md` e `wiki/stato.md`.
- Prossima sessione: riavviare il backend e provare a mano palette con ".com", "Aggiungi alimento" con "pasta" e autocompletamento al login; poi i punti del secondo giro di revisione in ordine (voci "Chat"/"Analytics", glossario delle azioni, conferme distruttive, doppione "Invita" nella lista pazienti, stati vuoti delle liste, "Comprimi"). Regole: frontend-design per la UI, percorso superpowers, revisione indipendente, mai commit.

## [2026-11-26] query | Terzo giro di revisione UI

- Giro visivo nel browser integrato (Claude in Chrome non collegato), con frontend-design. Report: `docs/ui-review-2026-11-26-giro3.md`.
- Punti principali: lista Pazienti tutta rossa per "Mai invitato"; card Circonferenze piena di trattini; colori delle variazioni nello Storico non legati all'obiettivo; glossario delle azioni ancora aperto; date sovrapposte nei grafici.

## [2026-11-26] handoff | Card Plicometria, Circonferenze e Profilo; toni di stato

- Nuove `CirconferenzeCard.vue`, `PlicometriaCard.vue` e `ProfiloPazienteCard.vue`: indici in testa su menta, solo i dati presenti, i mancanti in una riga in fondo. Decisioni di Andrea: niente soglie sugli indici (riferimenti di letteratura, non dati del paziente), niente variazioni nelle card (c'è la tab Confronto). Usate in Panoramica, Storico visite e riepilogo della visita precedente.
- Confronto visite: nascoste le circonferenze assenti in entrambe le visite e i grafici senza misure in comune.
- Nuova [decisione 0010](decisioni/0010-toni-di-stato-badge-e-avatar.md): tre toni per badge e avatar (`utils/toniStato.ts`); "Mai invitato" ambra, "Invitato" e "Archiviato" neutri a contorno, il rosso non è più uno stato. Allineati anche Piani e Alimenti.
- Verifiche: `vue-tsc` pulito, `vitest` 301/301 (adeguati alcuni test esistenti, nessuno nuovo); browser in chiaro e in scuro. Nessun commit.
- Dopo: badge tutti in grassetto; filtro archiviati più visibile; pillola neutra per i pazienti archiviati; intestazione delle tabelle fissa da 1280 px (niente scroll verticale interno, scelta di Andrea); CF tolto dalla riga sotto il nome nella tabella Pazienti; email sotto il nome nella tabella Piani (nuovo campo `pazienteEmail` nel backend, `api-contracts.md` aggiornato). Riavviare il backend.
- BMI in petrolio come il Peso; palette base invariata (scelta del dietologo, decisione 0009).

## [2026-11-26] handoff | Terzo giro: tutti i punti aperti

- Su "fai tutto" di Andrea: glossario delle azioni (nuova sezione in [glossario](glossario.md)) e diciture allineate; variazioni dello Storico secondo l'obiettivo; "Chat"/"Analytics" con lucchetto; asse dei grafici con etichette corte e diradate; paginazione nascosta con una pagina; un solo selettore a segmenti; testata paziente stabile; pillole della prossima visita cliccabili (`/agenda?data=`); variante `rimuovi` del `Button`; pagina di accesso più asciutta; Dashboard con striscia della settimana e contesto paziente nelle prossime visite.
- Scelte di Andrea durante il lavoro: lucchetto invece di togliere Chat/Analytics; "Archivia paziente" resta rosso; icona cartella per aprire le righe e nome non cliccabile nelle tabelle Pazienti e Piani.
- Backend: `GET /pazienti/{id}` riempie anche obiettivo, ultima visita e ultimo piano (prima `null`); `api-contracts.md` aggiornato. Riavviare il backend.
- Verifiche: `vue-tsc` pulito, `vitest` 301/301 (adeguate alcune asserzioni di testo e formato, nessun test nuovo), `mvn test` senza errori; browser in chiaro, scuro, 1280/1024 px e mobile per Dashboard e scheda paziente. Nessun commit.

## [2026-11-26] handoff | Verifica scuro, mobile, dialog e accessibilità; icone sui pulsanti

- Verificati tema scuro e mobile delle altre pagine, viste Mese e Giorno, dialog di creazione, accessibilità dei form (dettaglio in `docs/ui-review-2026-11-26-giro3.md`).
- Corretti: riga della vista Giorno fuori schermo da mobile; conteggio del Mese con la parola; maiuscolo nel tipo di evento; etichette del profilo unificate ("Livello di attività", "Storia del paziente", "Terapie in corso", "Non indicato"); nomi accessibili mancanti nell'editor del piano; gerarchia dei titoli nelle card.
- Su richiesta di Andrea: dialog e conferme come pannelli dal basso sotto i 640 px; icone su tutti i pulsanti con testo (anche "Annulla"); freccia e ultima visita nelle prossime visite della Dashboard; hover ritagliato dentro la card.
- Verifiche: `vue-tsc` pulito, `vitest` 301/301. Nessun commit. Riavviare il backend (contesto paziente in Dashboard, email nella lista piani).

## [2026-11-26] handoff | Chiusura del terzo giro di revisione UI

- `stato.md`: nuova voce in cima a "Prossimo passo consigliato" con il giudizio (app chiara, moderna, accattivante e coerente; si torna alle funzionalità), le azioni a mano (commit, riavvio del backend) e quattro piccoli da fare decisi da Andrea: promemoria di oggi e pazienti da richiamare in Dashboard, domeniche di un colore diverso in Agenda, sfondo al passaggio del mouse sui pulsanti testo con icona.
- Stato finale: `vue-tsc` pulito, `vitest` 301/301, `mvn test` senza errori; tutto non committato.
- Rivisto con Andrea: promemoria di oggi dentro "Prossime visite" (gruppo "Oggi") e pazienti da richiamare come card sotto "Prossime visite", non come quarta scheda di "Da fare" (troppo pesante).


## [2026-11-26] handoff | Domeniche e festivi in Agenda e Dashboard

- Ricontrollo a occhio con frontend-design, in chiaro e in scuro: il grigio ardesia delle domeniche (#E5E8EE / #29323D) tendeva al blu ed era più evidente dei festivi. `--agenda-domenica` ora è #EDF3F0 in chiaro e #0F1A18 in scuro.
- `SettimanaStrip.vue`: i festivi hanno lo sfondo ambra come in Agenda, con la stessa precedenza tra oggi, festivo e domenica.
- Su richiesta di Andrea: nella vista Giorno di un festivo, il disco dell'icona dello stato vuoto non è più bianco su ambra. `StatoVuoto` ha il nuovo prop `tono` (`neutro` | `avviso`).
- Verifiche: `vue-tsc` pulito, `vitest` 305/305, browser in chiaro e in scuro. Nessun commit.
- Su richiesta di Andrea: nella striscia della settimana in Dashboard c'è **un solo pallino per tipo** (verde se ci sono visite, ambra se ci sono promemoria), qualunque sia il numero. Tolti `MAX_PALLINI` e il "+n". I conteggi restano nel testo sotto (da 640 px) e nell'`aria-label`. La vista Mese compatta dell'Agenda aveva già un pallino per tipo con il numero accanto: invariata.

## [2026-11-26] handoff | Fine sessione: rifiniture agenda, si passa alla BIA

- Fatte in questa sessione: tinta delle domeniche nella palette, festivi in ambra nella striscia della Dashboard, disco ambra dello stato vuoto sui festivi, un pallino per tipo nella striscia. `vue-tsc` pulito, `vitest` 305/305 prima dell'ultima modifica e test Dashboard verdi dopo; browser in chiaro, scuro e mobile. Nessun commit.
- Decisioni di Andrea: l'app è chiara, moderna, coerente e accattivante, si torna alle funzionalità; i dati di prova restano; backend già riavviato.
- Prossimo passo: BIA in una nuova sessione. In `stato.md` ci sono i punti del codice dove oggi c'è il segnaposto e le domande da porre nel brainstorming.
- Dopo l'handoff, su richiesta di Andrea, tre rifiniture dell'Agenda: (1) giorni della settimana in minuscolo ("lun") nelle viste Mese e Settimana, come nella striscia della Dashboard; prima erano in maiuscolo spaziato. (2) Legenda più leggibile: testo `--fg2`, campioni da 14 px, lista accessibile. (3) Barra: i filtri Visite/Promemoria hanno il pallino del colore dell'evento e sono separati dal selettore delle viste da un divisore, così i due gruppi non si confondono. `vue-tsc` pulito, `vitest` 305/305, browser in chiaro e scuro.

## [2026-11-26] handoff | Gerarchia dei riquadri su Dashboard e scheda paziente

- Su richiesta di Andrea (punto 4 del suo giudizio sull'app: "card ovunque"), proposta mia applicata senza giro di domande, con frontend-design. Nuova [decisione 0011](decisioni/0011-gerarchia-dei-riquadri.md), `in-discussione`.
- Dashboard: "Prossimi impegni" con ombra `--elev`; contatori senza riquadro, numeri in Fraunces, divisi da un filetto; "Pazienti da richiamare" appoggiata allo sfondo; "Visite Lun-Dom" diventa "Visite lun-dom".
- Scheda paziente: fascia dei quattro indicatori senza riquadri, numeri più grandi; grafici della Panoramica con ombra; Plicometria e Circonferenze vuote in un riquadro tratteggiato come la BIA.
- Verifiche: `vue-tsc` pulito, `vitest` 305/305, browser in chiaro, scuro e mobile. Nessun commit.
- Andrea non approva "Pazienti da richiamare" senza riquadro: torna card piatta (file riportato alla versione committata). Regola aggiunta alla decisione 0011: i blocchi con azioni per riga restano in card, il livello contesto è solo per numeri e informazioni da leggere.
- Segnalato da Andrea: il selettore Mese/Settimana/Giorno dell'Agenda non aveva icone, mentre Tabella/Schede delle liste sì (stesso `SelettoreSegmentato`). Aggiunte `CalendarDays`, `CalendarRange` e `Calendar1` in `AgendaToolbar.vue`. L'Agenda non ha test (scelta del 15 novembre); `vue-tsc` pulito.

## [2026-11-26] handoff | Revisione indipendente (Sonnet) e rifiniture prima della demo

- Revisione indipendente con un agente Sonnet e frontend-design, in sola lettura, su tutte le pagine comprese le visite. Niente di bloccante. Applicati: etichette del grafico Peso (unità tolta dai punti, ora nel titolo "Peso (kg)", come chiesto da Andrea); sottotitolo della modifica visita ("visita del 25 nov 2026" invece di "ultima visita…", che si confondeva col riepilogo della precedente); campioni della legenda dell'Agenda più visibili. Rimandato: "Composizione corporea" con un solo dato (sottotitolo "Variazione %" senza senso). Scartati: titoli troncati in Settimana (dati di prova), "Mai invitato" ambra (ADR 0010).
- Header mobile, su indicazione di Andrea: avatar tolto sotto i 1024 px (troppe icone); il profilo (tema scuro, uscita) sta in fondo al menu laterale come menu a comparsa, con la freccia verso il basso come nell'header desktop (`AppSidebar` prop `conProfilo`, logica di uscita in `composables/useEsci.ts`). Header mobile con filetto e ombra `--elev`.
- Verifiche: `vue-tsc` pulito, `vitest` 305/305, browser desktop e mobile. Nessun commit.
- Tooltip shadcn al posto dei ~30 `title=` nativi: proposto da Andrea, poi **scartato** da lui. Troppo lavoro per il beneficio, e c'era il rischio di incastri con i menu a comparsa prima della demo.

## [2026-11-26] handoff | Chiusura sessione: rifiniture UI, gerarchia dei riquadri, verso la BIA

- `stato.md`: voce finale in cima a "Prossimo passo consigliato". Contiene: commit dei 23 file in staging, nessun riavvio del backend, il riepilogo della sessione, i due aperti piccoli ("Composizione corporea" con un solo dato; tooltip nativi, scelta definitiva) e la domanda da fare ad Andrea sulla demo di stasera, che decide lo stato della decisione 0011.
- Prossimo passo: BIA in una nuova sessione. Punto di partenza e domande del brainstorming in `stato.md`.

## [2026-11-26] query | BIA: primo brainstorming sull'importazione da BodyGram

- Il nutrizionista usa Akern BodygramPlus. Andrea ha fornito due PDF di esempio, non salvati nel repo perché contengono dati personali: "Composizione Corporea" e "Report confronto" per distretti. Dai manuali Akern online: l'esportazione CSV esiste (archivio, tabella dei test, gruppo, singolo esame), ma le colonne non sono documentate.
- Decisioni di Andrea: si carica uno o più CSV dal form della visita; i valori calcolati da BodyGram si salvano così come sono, niente formule nostre; i valori si possono modificare e la BIA si può cancellare; se il file contiene più misurazioni, errore (provvisorio); se peso o altezza non coincidono con la visita, si salva e lo si segnala. Non si sa ancora se il report per distretti servirà.
- In pausa finché non arrivano CSV veri. Dettaglio in `stato.md`, "Prossimo passo consigliato".
- Su richiesta di Andrea, fatta subito **solo l'interfaccia**: `BiaCaricamento.vue` (sezione a comparsa con area di caricamento di più CSV, elenco con rimozione, nota "per ora i file non vengono salvati") al posto del segnaposto in `DatiVisitaForm.vue`. Nessuna lettura, nessun backend. `vue-tsc` pulito, `vitest` 305/305, verificato nel browser. Nessun commit.
- Rifiniture chieste da Andrea in `BiaCaricamento.vue`: icona del file in un cerchio menta tipo avatar, pulsante "Scarica" per ogni file (scarica la copia locale, `URL.createObjectURL`), X grigia che diventa rossa solo al passaggio del mouse (variante `rimuovi`, come per gli alimenti del piano). `vue-tsc` pulito.
- Proposta di Claude approvata da Andrea: quando c'è almeno un file, l'area grande di caricamento sparisce e resta una riga "Aggiungi altri file · o trascinali qui". Si può trascinare su tutta la sezione. Nella sezione a comparsa i testi sono in `<div>`, non in `<p>`: `AccordionContent` aggiunge `mb-4` ai `<p>` che non sono ultimi e lo spazio si sommava al gap. `vue-tsc` pulito, verificato nel browser.
- Bug segnalato da Andrea: in "Nuovo paziente" il link "Torna alla lista pazienti" andava a capo rispetto alla freccia e aveva il testo troppo grande. Causa: `PazienteNuovoView.vue` usava `<Button as-child>` senza importare `Button` (dal commit `18c59b2`), quindi Vue lo mostrava come un `<button>` nativo senza stili. Aggiunto l'import; nessun'altra vista ha lo stesso problema (controllate tutte le `.vue` che usano `<Button`).

## [2026-11-26] lint | Componenti shadcn al posto dei pulsanti nativi, doppio scroll

- **Doppio scroll** (segnalato da Andrea in "Nuovo paziente", aprendo le sezioni): il campo file nascosto (`sr-only`, quindi `position: absolute`) di `BiaCaricamento.vue` non aveva un antenato posizionato e allungava il documento. Aggiunto `relative` al contenitore. Le altre pagine principali non allungano il documento.
- **Componenti usati senza import**: controllate tutte le `.vue`. L'unico caso era `Button` in `PazienteNuovoView.vue`, già corretto.
- **Pulsanti nativi → shadcn (gruppo A, scelto da Andrea)**: filtri a pillola di Pazienti, Piani, Alimenti e Agenda (nuove varianti `chip` per variante e dimensione, stato acceso da `aria-pressed`); "Filtri avanzati" (`ghost`, con `aria-expanded`) e "Pulisci filtri" (nuova variante `tratteggiato`) in Pazienti e Piani; intestazioni ordinabili delle tabelle (`ghost` `xs`); freccia di ordinamento delle schede (`outline` `icon`); occhio della password al login; "Esc" della ricerca globale; "Vedi note" del piano (`link`); "Invita" nella pillola dello stato account (`link` con le classi della pillola); "Comprimi" della sidebar. "Mostra pazienti archiviati" diventa uno `Switch` con `Label`; le linguette di "Aggiungi alimento" diventano `Tabs`.
- Aggiunti i componenti shadcn `switch` e `tabs`. La CLI più recente richiede Node ≥ 22.15 (qui c'è 22.12): lanciata con `npx -p node@24`. La CLI aveva importato `cn` da un pacchetto npm sbagliato invece che da `@/lib/utils` e modificato `main.css` (font Geist da Google, stili base): corretto e ripristinato. Restano aggiornati `reka-ui` (2.11) e `@lucide/vue` (1.81) in `package.json`.
- Restano nativi, per scelta, 20 pulsanti che sono aree cliccabili e non pulsanti: celle e appuntamenti della vista Mese, risultati della ricerca globale e degli alimenti, notifiche, righe dello storico visite, voci della scelta paziente, nome nella lista alimenti, campo che apre la ricerca, menu del profilo.
- Bug trovato durante il lavoro: nella lista Piani il badge dei filtri avanzati mostrava " attivi" senza il numero. Corretto.
- Test adeguati (non nuovi): `AlimentiListView.spec.ts` verifica `aria-pressed` invece della classe del bordo; `AggiungiAlimentoDialog.spec.ts` usa `mousedown` sulle linguette, perché le Tabs di reka si attivano lì. `vue-tsc` pulito, `vitest` 305/305, browser: Pazienti, Piani, dialog alimento, ricerca globale. Non provati nel browser: login (occhio della password), "Vedi note" (solo piani in sola lettura), tema scuro. Nessun commit.

## [2026-11-26] lint | Doppio scroll, verifica dei pulsanti convertiti in shadcn

- **Doppio scroll nel dettaglio paziente** (segnalato da Andrea su Beatrice De Luca, scheda Panoramica): i testi `sr-only` di FMI e FFMI in `PlicometriaCard.vue` sono `position: absolute` e, senza un antenato posizionato, allungavano il documento (1110 px su 622). Stessa causa del caso di `BiaCaricamento.vue`. Corretto alla radice: `relative` sul `<main>` di `AppShell.vue`, che ora contiene tutti gli `sr-only` delle pagine, e anche sulla card. Controllate tutte le pagine dell'app a 375, 746 e 1440 px, con ogni scheda e ogni sezione aperta: nessun doppio scroll, né verticale né orizzontale.
- **Verifica nel browser del "gruppo A"** (pulsanti nativi convertiti in shadcn), rimasta in sospeso: l'occhio della password al login mostra e nasconde e cambia l'etichetta per lo screen reader, con il focus visibile nei due temi; "Vedi note" di un piano in sola lettura apre il pannello delle note (stato "concluso" simulato solo nella pagina, nessun dato modificato); nel tema scuro filtri a pillola, "Pulisci filtri", `Switch`, "Invita" e "Comprimi" sono leggibili.
- **Difetto trovato**: nel tema scuro la linguetta attiva di "Aggiungi alimento" perdeva la sottolineatura verde, perché la regola shadcn `dark:group-data-[variant=line]/tabs-list:data-active:border-transparent` è più specifica della nostra. Corretto in `AggiungiAlimentoDialog.vue` con lo stesso selettore. Le linguette della Dashboard e del dettaglio paziente non avevano il problema.
- Etichetta nascosta del pulsante di chiusura di `SheetContent` e `DialogScrollContent` tradotta: "Close" → "Chiudi".
- Il "gruppo B" (aree cliccabili rimaste `<button>` nativi) resta così, confermato da Andrea.
- `vue-tsc` pulito, `vitest` 305/305. Nessun commit.
- Pillole cliccabili del dettaglio paziente ("Prossima visita", "Nessuna visita programmata"): aggiunta una freccia `›` finale che si sposta un poco al passaggio del mouse (ferma con `prefers-reduced-motion`). Le distingue dagli stati non cliccabili come "Mai invitato" e "Paziente archiviato". Scelta di Andrea tra freccia e pillola a segmenti. Verificato in chiaro e mobile.

## [2026-11-26] handoff | Fine sessione: doppio scroll, verifica shadcn, pillole cliccabili

- `stato.md` aggiornato: conteggio dei file in staging (26), avviso sulle modifiche non in staging dell'altra sessione del 26 novembre (gruppo A, `BiaCaricamento.vue`, `switch`/`tabs`, `package.json`), riepilogo dell'ultima parte e regola sugli elementi `absolute`.
- Prossima sessione: BIA, in pausa finché non arrivano CSV veri da BodyGram. Da chiedere ad Andrea com'è andata la demo, per lo stato della decisione 0011.
- Nessun commit.

## [2026-11-27] ingest | Accordo con Edoardo: primo rilascio solo area professionista, 30 €/h

- Fonte: [sorgenti/2026-11-26-accordo-con-edoardo.md](sorgenti/2026-11-26-accordo-con-edoardo.md), conversazione di Andrea con Edoardo, l'amico che finanzia Hexis del 26 novembre.
- Nuova ADR [decisioni/0012](decisioni/0012-primo-rilascio-solo-area-professionista.md): si pubblica solo l'area del professionista. Chat, webapp e app mobile del paziente rimandate.
- Compenso: 30 €/h, 100 h dichiarate per la prima parte (3.000 €). Diverso dalla proposta "soci" consigliata il 17 novembre: segnalato in `stato.md`.
- Aggiornati `stato.md` (riga in "Dove siamo", sezione dedicata, "Prossimo passo consigliato"), `domande-aperte.md` (Flutter rimandata, accordo in parte deciso, nuove voci su invito paziente e messa online), `architettura.md`, `index.md`.
- Nessun codice. `docs/proposta-collaborazione-hexis.html` non toccato.

## [2026-11-27] lint | Nascosti invito, stato account e voci Chat/Analytics

- Richiesta di Andrea dopo la [decisioni/0012](decisioni/0012-primo-rilascio-solo-area-professionista.md): nascondere il pulsante "Invita", la colonna e il badge dello stato account, e le voci "Chat" e "Analytics" della sidebar.
- Nuovo `frontend-professionisti/src/config/funzioni.ts` con `AREA_PAZIENTE_ATTIVA = false`. Lo usano `AppSidebar.vue` (Chat; Analytics tolta del tutto), `PazientiListView.vue` (pillole di filtro, ordinamento, colonna, badge nelle schede, testi), `PazienteDettaglioView.vue` (badge nella testata), `DaFareCard.vue` e `DashboardView.vue` (linguetta e richiesta "Inviti", testo dello stato vuoto), `utils/paziente.ts` (avatar tutti `attivo`, archiviati `neutro`).
- Test esistenti adeguati, non nuovi: `vi.mock('@/config/funzioni')` con l'interruttore acceso nei tre spec che provano gli inviti. `vue-tsc` pulito, `vitest` 305/305.
- Non provato nel browser: backend non avviato. Nessun commit.

## [2026-11-27] ingest | Richieste di Edoardo: Google Calendar, blocco su più giorni, sostituzione alimento

- Fonte: [sorgenti/2026-11-26-richieste-di-edoardo.md](sorgenti/2026-11-26-richieste-di-edoardo.md). Andrea dice "due cose", ma le richieste elencate sono tre.
- Registrate in `domande-aperte.md` (nuova sezione "Richieste di Edoardo da discutere") con le domande da chiarire per ciascuna, e in `stato.md`. Da discutere con Andrea prima di qualsiasi design: nessuna decisione, nessun codice.
- Nessuna contraddizione con le pagine esistenti. Il blocco su più giorni cambia una regola di [decisioni/0007](decisioni/0007-appuntamenti-e-visite-registrate.md) e di `api-contracts.md` (blocco in un solo giorno, 5–480 minuti, 07:00–20:00), da aggiornare quando si decide.

## [2026-11-27] ingest | Da fare per la messa online: GDPR, log, email di recupero password

- Fonte: [sorgenti/2026-11-27-da-fare-gdpr-log-email.md](sorgenti/2026-11-27-da-fare-gdpr-log-email.md).
- Registrati in `domande-aperte.md` (nuova sezione "Da fare per la messa online") e in `stato.md`, con quello che c'è oggi nel codice: nessuna configurazione dei log, tre classi che loggano solo `warn`; email di reset di una riga con mittente segnaposto; `server.error.include-message: always` da rivedere. Nessuna decisione, nessun codice.

## [2026-11-27] ingest | Da fare per la messa online: dominio, email, Docker, accesso a log e DB, prova di 7 giorni

- Fonte: [sorgenti/2026-11-27-da-fare-dominio-docker-admin.md](sorgenti/2026-11-27-da-fare-dominio-docker-admin.md).
- Aggiunte cinque voci alla sezione "Da fare per la messa online" di `domande-aperte.md`, con le domande per ciascuna; aggiornato `stato.md`. Nessun `Dockerfile` né `docker-compose` nel repo oggi.
- Da segnalare: l'app di amministrazione con account di prova cambia [decisioni/0002](decisioni/0002-autenticazione-e-onboarding.md) (professionisti creati solo a mano) e dipende dal modello di vendita non ancora deciso. Nessuna decisione, nessun codice.

## [2026-11-27] ingest | Webapp installabile sul Mac, pagina Privacy da fare, nome di Edoardo

- Fonte: [sorgenti/2026-11-27-webapp-mac-e-privacy.md](sorgenti/2026-11-27-webapp-mac-e-privacy.md).
- Su richiesta di Andrea "l'amico che finanzia" / "il finanziatore" diventa **Edoardo** in tutte le pagine e nelle voci di log di oggi; rinominate le fonti in `2026-11-26-accordo-con-edoardo.md` e `2026-11-26-richieste-di-edoardo.md`. Le voci di log precedenti restano com'erano.
- App per Mac: per ora no (Andrea). Fatto subito: manifest, icone PNG e meta tag per installare `frontend-professionisti` come webapp (Dock del Mac, Home dell'iPhone, "Installa" di Chrome). Manifest e icone verificati nel browser; installazione vera non provata (serve l'HTTPS). Dettagli in `stato.md`.
- Pagina Privacy: segnata in `domande-aperte.md` ("Da fare per la messa online"), dipende dalle decisioni GDPR. Annotato che i font da Google Fonts mandano l'IP del visitatore a Google.

## [2026-11-27] handoff | Fine sessione: accordo con Edoardo, area paziente nascosta, webapp, lista di cose da discutere

- `stato.md`: riscritto il "Prossimo passo consigliato" con decisioni, codice fatto, cose segnate e ordine suggerito.
- Nessuna domanda nuova oltre a quelle già registrate oggi in `domande-aperte.md`.
- Tutto in staging, nessun commit.


## [2026-11-27] ingest | Area paziente nascosta provata nel browser, ordine dei lavori

- Provate nel browser, con backend acceso: Dashboard, sidebar (desktop e mobile), lista Pazienti (tabella, schede, filtri, ordinamento, menu azioni), scheda paziente, tema scuro. Niente di legato a inviti, stato account, Chat o Analytics resta visibile; nessun errore in console. Nessun codice cambiato.
- Fonte: [sorgenti/2026-11-27-ordine-dei-lavori.md](sorgenti/2026-11-27-ordine-dei-lavori.md). Il dominio lo compra Edoardo; intanto si prepara Docker. Le tre richieste di Edoardo si fanno tutte. Aggiornati `stato.md` e `domande-aperte.md`.

## [2026-11-27] ingest | Blocco su più giorni

- Prima richiesta di Edoardo, fatta con brainstorming, spec e piano ([spec](../docs/superpowers/specs/2026-11-27-blocco-su-piu-giorni-design.md)). Un blocco ha inizio e fine con data e ora, anche su giorni diversi, e "Giornate intere"; avvisa (409) delle visite programmate che copre, solo se in modifica cambia l'intervallo.
- Codice: migrazione V34 (`data_fine`, `ora_fine`, `giornata_intera`), `/agenda` con i campi nuovi e `GET` che include i blocchi iniziati prima di `da`, `TrattoBlocco` nel backend, `utils/blocchi.ts` e viste, form e dettaglio nel frontend. In staging, non committato, nessun test nuovo; backend 196/196, frontend `vue-tsc` pulito e `vitest` 305/305. **Riavviare il backend (V34).** Non provato a mano: i 5 controlli da fare sono in `stato.md`. Revisione finale del ramo ancora da fare.
- Aggiornati [modello-dati](modello-dati.md), [api-contracts](api-contracts.md), [domande-aperte](domande-aperte.md) (voce spostata in "Risolte") e [stato](stato.md).
- Fuori dal piano: su mobile il logo "Hexis" dell'header è un link alla Dashboard (richiesta di Andrea, verificato nel browser).
- Revisione finale dell'intero ramo fatta (modello Opus): un problema importante (nel form la fine non seguiva l'inizio) e quattro minori corretti in un giro; aggiornati anche il contratto delle sovrapposizioni in [api-contracts](api-contracts.md) e un Javadoc del backend.

## [2026-11-27] ingest | Blocco su più giorni provato a mano

- Andrea ha riavviato il backend (V34 applicata) e fatto le cinque prove a mano della Review Focus: "mi sembra tutto ok". Aggiornato `stato.md`. Resta il commit, che fa Andrea.

## [2026-11-27] ingest | Blocchi più leggibili, striscia della Dashboard che apre il giorno

- Su richiesta di Andrea: righe diagonali, lucchetto e "Tutto il giorno" per i blocchi in tutte le viste e nella striscia; clic su un giorno della striscia → Agenda in vista Giorno. Verificato nel browser (chiaro e scuro). Aggiornato `stato.md`. In staging, non committato.

## [2026-11-27] ingest | Bug: le conferme dell'agenda non facevano nulla

- Segnalato da Andrea: eliminare una fascia bloccata non funzionava. Causa: `AlertDialogAction` di reka-ui chiude il dialog (`v-model:open = false`) prima di eseguire il `@click`; in `AgendaView.vue` il setter dell'apertura azzerava l'elemento scelto, quindi la conferma trovava `null` e non chiamava il backend. Colpiva anche "Annulla appuntamento" e "Conferma comunque" di "Sposta a domani", fin dall'introduzione dell'agenda. Corretto con flag di apertura separati. Verificato nel browser (DELETE 204, blocco sparito, toast); `vue-tsc` pulito, `vitest` 305/305. **Regola per il futuro:** mai legare il dato di un AlertDialog al suo `v-model:open` se la conferma usa `AlertDialogAction`.
- Aggiunto `src/views/agenda/AgendaView.spec.ts` (su richiesta di Andrea): due test di regressione su "Elimina fascia bloccata" e "Annulla appuntamento"; falliscono con il codice di prima, passano con la correzione. Suite 307/307.
- Trovato nei test: `AppSidebar.vue` usava `<Button>` ("Comprimi sidebar") senza importarlo, quindi veniva disegnato come `<button>` nativo. Aggiunto l'import; verificato nel browser che comprime ed espande come prima.
- Su richiesta di Andrea: il pulsante di conferma dei dialog distruttivi (variante `destructive` di `buttonVariants`: Elimina, Annulla appuntamento, Rimuovi…) è ora **rosso pieno** (`--danger`, hover `--danger-d`) con testo `--on-danger` (bianco in chiaro, scuro al buio), così pesa più di "Annulla". Le voci di menu rosse non cambiano. Verificato nel browser in chiaro e scuro.

## [2026-11-27] handoff | Fine sessione: blocco su più giorni, rifiniture agenda, bug delle conferme

- `stato.md`: aggiornato il blocco "Aggiornato il 27 novembre 2026, sera" con l'ultima parte della sessione (bug delle conferme e test, import in `AppSidebar`, conferme distruttive in rosso pieno), l'elenco completo da committare e le rifiniture minori lasciate stare.
- Decisioni della sessione: ordine dei lavori (dominio a carico di Edoardo, intanto Docker; tutte e tre le richieste di Edoardo si fanno); blocco su più giorni con approccio "una riga con data e ora di fine"; subagent su Sonnet, revisione finale sul modello più potente (memoria di Claude).
- Prossimo passo: brainstorming di "Sostituisci alimento", poi Google Calendar, poi la preparazione Docker.
- Tutto in staging, nessun commit.

## [2026-11-27] lint | Domande aperte superate

- `domande-aperte.md`: spostate tra le risolte "colore del delta fisso" (risolto il 25 novembre con i colori secondo l'obiettivo) e "perché Spring Boot" (chiusa, la scelta sta nell'ADR 0001); aggiornata la voce "Inviato al paziente" agli stati attuali del piano (Bozza / Attivo / Concluso).

## [2026-11-27] handoff | Chiusura sessione dopo la pulizia delle domande aperte

- `stato.md`: l'elenco da committare include la pulizia di `domande-aperte.md`. Nient'altro di nuovo rispetto all'handoff precedente: prossimo passo il brainstorming di "Sostituisci alimento", poi Google Calendar, poi Docker.
- Tutto in staging, nessun commit.
