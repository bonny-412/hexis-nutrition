---
title: Stato del progetto
tags: [stato]
stato: stabile
creato: 2026-08-08
aggiornato: 2026-08-31
fonti: [sorgenti/2026-08-08-scope-e-stack-iniziali.md, sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md, sorgenti/2026-08-09-migrazione-a-repo-unico.md, sorgenti/2026-08-09-test-su-postgres-locale.md, sorgenti/2026-08-09-docker-solo-in-produzione.md, sorgenti/2026-08-30-hash-pulizia-invalidazione-token.md, ../docs/superpowers/specs/2026-08-31-nuovo-paziente-con-visita-design.md, log.md#2026-08-31-handoff--bug-fix-controlli-pazientenuovoview--ux-maiuscola-errori-live]
---

# Stato — hexis-nutrition

## Dove siamo

Il backend del sotto-progetto **"Fondamenta"** (autenticazione JWT con ruoli, anagrafica paziente, invito via email, reset password) è **scritto, revisionato, testato su database reale e su GitHub**. Il frontend `frontend-professionisti/` di "Fondamenta" è **scritto, testato (54 test verdi), buildato, e in staging** (login, dashboard, lista/dettaglio/creazione paziente, reset password) — vedi sessione del 30 agosto 2026 (parte 2) più sotto. `frontend-cliente/` non esiste ancora: contiene solo un `CLAUDE.md`.

Il progetto vive in **un unico repo git**, `bonny-412/hexis-nutrition`, con la radice in `progetti/hexis-nutrition/`.

## Come far girare il backend su questa macchina

- **Database**: PostgreSQL 13 locale (servizio `postgresql-x64-13`). Due database creati a mano con pgAdmin, tabelle generate da Flyway: `hexis` per l'applicazione, `hexis_test` per la suite. Credenziali note ad Andrea.
- **JDK**: il JDK di sistema di default è Java 8, va **sempre** sovrascritto: `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"` prima di ogni comando Maven.
- **Test**: `mvn test` da `backend/`. Niente Docker richiesto.
- **Avvio dell'app**: serve la variabile d'ambiente `JWT_SECRET`, obbligatoria (l'app non parte senza, per scelta di sicurezza post-revisione) e lunga **almeno 32 caratteri** — `Keys.hmacShaKeyFor` con HS256 richiede una chiave da 256 bit e lancia `WeakKeyException` se è più corta. Da Spring Tool Suite: importare `backend/` come progetto Maven esistente, poi impostarla in `Run Configurations → Spring Boot App → Environment` (non in *VM arguments*). `RESEND_API_KEY` serve solo per l'invio email reale: senza, l'invito paziente fallisce, mentre il reset password risponde comunque 204 per scelta anti-enumerazione.
- **Account di sviluppo**: esiste un professionista inserito a mano nel database `hexis` (email reale di Andrea, non un indirizzo `@hexisnutrition.local` come riportato in una versione precedente di questa pagina — corretto il 2026-08-30), password nota ad Andrea e deliberatamente non scritta qui. Non c'è self-signup, per scelta ([decisioni/0002](decisioni/0002-autenticazione-e-onboarding.md)): altri account si creano con una `INSERT`/`UPDATE` e un hash BCrypt.

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

## Sessione del 30 agosto 2026 (parte 2) — frontend-professionisti "Fondamenta"

**Piano di 16 task eseguito con `superpowers:subagent-driven-development`** in un worktree isolato, poi **merge locale su master** (un conflitto in `AuthControllerTest.java`, risolto tenendo i test di entrambi i rami). Costruito su Vue 3 + TypeScript + Vite + Tailwind CSS v4 + Pinia, basandosi sul mockup fornito da Andrea (`Hexis Nutrition.zip`). Realizzate: login, dashboard, lista/dettaglio/creazione paziente, password dimenticata, reset password, shell applicativa con sidebar e header. Revisione finale whole-branch: 5 Important risolti (redirect al login su 401, errori invito/login non più ignorati silenziosamente, logout non più scatenato da un semplice errore di rete, controllo del ruolo al login).

**Pivot a metà sessione**: dopo la prima versione (CSS scritto a mano, icone emoji), Andrea ha segnalato una forte infedeltà rispetto al mockup e ha richiesto l'introduzione di **shadcn-vue + Reka UI + `@lucide/vue`** (già previsti nella ADR 0001 originaria ma inizialmente rimandati per YAGNI). Riconvertiti in un solo passaggio tutti i componenti e le view esistenti. Il drawer mobile della sidebar usa il componente `Sheet` di shadcn (portal-based) invece di un CSS hand-rolled che Andrea aveva segnalato come non funzionante; il menu profilo usa `DropdownMenu` di Reka UI, che gestisce nativamente click-outside/Escape.

**Convenzione CSS stabilita da Andrea, ora rispettata ovunque**: mai `style="..."` inline nei componenti Vue — sempre classi Tailwind, incluse le variabili CSS custom via sintassi arbitraria (`text-[var(--fg3)]`, `bg-[var(--surf)]`, ecc.) e l'utility `font-heading` per i titoli in Fraunces. Verificato che Tailwind v4 (JIT) genera `font-heading` solo se referenziata in un template — non è pre-emessa dal solo `@theme`.

Password dell'account di sviluppo reimpostata (Andrea l'aveva dimenticata): nuovo hash BCrypt generato al volo con uno script Java temporaneo, mai committato. Valore nuovo noto ad Andrea, non scritto qui.

Stato finale verificato in questa sessione: `npm run test` → **54 test, 0 fallimenti**; `npm run build` → pulito (TypeScript + Vite), CSS compilato contiene `.font-heading{font-family:Fraunces,serif}`. Tutto in staging (`git add` fatto su tutto il repo, incluso il lavoro backend sui token), **nessun commit** — tocca ad Andrea.

## Sessione del 30 agosto 2026 (parte 3) — rifiniture UI su `frontend-professionisti`

Sessione di piccole correzioni e rifiniture visive su shell e dashboard, richieste una alla volta da Andrea, nessuna modifica di struttura o di flusso.

**Bug fix** (`AppShell.vue`): risolta una linea verticale bianca visibile aprendo la sidebar su schermo piccolo. Causa: il `Sheet` di shadcn applica di default `data-[side=left]:border-r` e `data-[side=left]:w-3/4`; gli override passati da `AppShell.vue` (`border-0`, `w-[246px]`) non avevano lo stesso "scope" di variante, quindi `tailwind-merge` non li deduplicava e in CSS la regola con selettore `[data-side="left"]` vinceva comunque per specificità, lasciando un bordo di 1px visibile. Fix verificato empiricamente con uno script Node che chiama `twMerge` direttamente prima e dopo: ora gli override usano lo stesso prefisso (`data-[side=left]:border-r-0`, `data-[side=left]:w-[246px]`, `data-[side=left]:max-w-none`) e vengono deduplicati correttamente.

**Header** (`AppHeader.vue`): freccia del menu profilo ora dinamica (ruota 180° quando il menu è aperto, via `group` + `group-data-[state=open]:rotate-180`, sfruttando il `data-state` che Reka UI propaga sul trigger tramite `as-child`); su schermi piccoli il bottone profilo mostra solo l'avatar (bordo/sfondo/padding ora `lg:`-only, prima restavano visibili anche se nome e freccia erano già nascosti); logo+"Hexis" mobile spostato fuori dal gruppo sinistro e centrato nell'header con posizionamento assoluto, ingrandito (icona `h-7`→`h-9`, testo `text-base`→`text-xl`).

**Dashboard** (`DashboardView.vue`): saluto "Bentornata" (gendered) sostituito con "Ciao, {nome professionista}" (letto da `auth.professionista`) + data odierna sotto, in italiano esteso (`Intl.DateTimeFormat('it-IT', ...)`, es. "Lunedì 30 Agosto 2026"). Aggiunto a destra un bottone "Crea nuovo" con dropdown (stesso pattern freccia dinamica dell'header): "Nuovo paziente" attivo → `/pazienti/nuovo`, "Nuovo appuntamento" e "Nuovo piano alimentare" disabilitati (Agenda e Piani alimentari non esistono ancora), ciascuna voce con icona (`UserPlus`/`CalendarPlus`/`FileText`). Dimensioni del menu tarate più volte su richiesta di Andrea (troppo stretto → troppo grande → via di mezzo: contenitore `w-60`, voci `px-2.5 py-1.5 gap-2`, icone 16px).

Nessun test automatico dedicato a queste view di dettaglio grafico oltre a quelli già esistenti (`AppShell.spec.ts`, `AppHeader.spec.ts`, `DashboardView.spec.ts`) — verificati verdi dopo ogni modifica, più `tsc --noEmit` pulito. Nessuna verifica visiva in browser reale in questa sessione (niente Claude in Chrome collegato): le correzioni sono basate su lettura del codice e, per il bug della linea bianca, su verifica empirica del comportamento di `tailwind-merge`.

## Sessione del 31 agosto 2026 — "Nuovo paziente con visita"

Brainstorming (percorso architetturale) → spec → piano → esecuzione con `superpowers:subagent-driven-development` in un worktree isolato: 6 task, revisione finale whole-branch, una fix wave. Spec: `docs/superpowers/specs/2026-08-31-nuovo-paziente-con-visita-design.md`; piano: `docs/superpowers/plans/2026-08-31-nuovo-paziente-con-visita.md`. Dettaglio completo in [log](log.md#2026-08-31-handoff--nuovo-paziente-con-visita-anagrafica-estesa--prima-visita-backend-e-frontend).

**Backend**: nuova entità `Visita` (tabella additiva `visite`, V5) — altezza, peso (obbligatori) + 12 misure di circonferenza (opzionali). `Paziente` perde `altezzaCm` (spostata su `Visita`), guadagna `lavoro`/`tipoLavoro` (V6). `POST /pazienti` crea `Paziente`+`Visita` in un'unica transazione, con validazione di range su tutte le misure. **Frontend**: `PazienteNuovoView.vue` riscritta con due card (anagrafica + visita, con placeholder per la futura misurazione BIA). Test finali: backend 48/48, frontend 55/55 + `tsc --noEmit` pulito.

**Merge locale su `master` completato** (in staging, nessun commit — tocca ad Andrea). Durante il merge, scartata una modifica non committata preesistente su `PazienteNuovoView.vue`: verificato che era lo stesso styling già incluso ed esteso nella nuova versione, nessuna perdita.

**Attenzione**: `LoginView.vue`/`LoginView.spec.ts` hanno 2 test rotti (`Failed to resolve component: Loader2`) per una redesign in corso non committata **preesistente a questa sessione e non toccata da essa** — non è una regressione introdotta qui, ma va segnalata perché la suite frontend non è più verde nel suo complesso finché non viene ripresa. `AppSidebar.vue`/`DashboardView.vue` hanno anch'esse modifiche non committate preesistenti, non toccate.

## Sessione del 31 agosto 2026 (parte 2) — bug fix controlli e UX su `PazienteNuovoView`

Andrea ha segnalato che nella pagina "Nuovo paziente" si poteva digitare **qualsiasi carattere in qualsiasi campo**, come se i controlli non funzionassero affatto. Seguito `superpowers:systematic-debugging`: causa radice nel componente shadcn `Input.vue`, che usa `useVModel(..., { passive: true })` di `@vueuse/core` — i suoi `watch` interni non scattano quando il valore filtrato coincide col precedente (es. campo vuoto, carattere non ammesso digitato, il filtro lo rimuove tornando a `''`: nessun cambiamento rilevato, la correzione non arriva mai al campo, che resta visivamente "sporco"). Il test preesistente non lo copriva perché usa `setValue()` (un solo evento con la stringa finale) invece di digitazione tasto per tasto. **Fix**: `conFiltro` ora passa per un valore intermedio invisibile per forzare comunque la propagazione reattiva quando il valore filtrato non cambierebbe.

Nella stessa sessione, due richieste UX di Andrea: (1) **l'errore di un campo sparisce non appena viene corretto** (prima restava visibile fino al submit successivo) — implementato per tutti i 18 campi validati del form, riusando le stesse funzioni di validazione di `validaCampi()`; (2) **maiuscola automatica sulla prima lettera** di Nome, Cognome, Lavoro (quest'ultimo prima non passava da nessun filtro).

Verifica: bug riprodotto con un test che digita tasto per tasto (rosso), poi verde dopo il fix; 3 nuovi test per le due feature (rossi poi verdi). Suite frontend: **57/57 verdi** (esclusi i 2 test preesistenti di `LoginView.spec.ts`, non toccati). `tsc --noEmit` pulito. Nessuna verifica manuale in browser.

**Segnalato, non affrontato**: il backend non valida con un pattern lato server `nome`, `cognome`, `telefono`, `sesso`, `lavoro` in `CreaPazienteRequest` (solo `@NotBlank`/`@Email` dove presenti) — chi chiama l'API bypassando il frontend può ancora inserire qualsiasi carattere in quei campi. Spostato in [domande-aperte](domande-aperte.md).

Dettaglio completo in [log](log.md#2026-08-31-handoff--bug-fix-controlli-pazientenuovoview--ux-maiuscola-errori-live).

## Cosa resta aperto

Elenco completo e ragionato in [domande-aperte](domande-aperte.md). I punti che toccano il codice esistente:

- **Versione di PostgreSQL**: sviluppo e test girano sulla 13, la produzione sarà in Docker e potrà usare qualunque immagine. Fissarla su `postgres:13` allinea gli ambienti; qualsiasi altra scelta introduce una differenza che nessun test copre.
- **Deploy**: previsto Docker, nient'altro deciso (immagini, orchestrazione, provider, gestione di `JWT_SECRET` e `RESEND_API_KEY`).
- **Validazione server-side incompleta su `CreaPazienteRequest`**: `nome`, `cognome`, `telefono`, `sesso`, `lavoro` non hanno un pattern lato backend (a differenza dei campi numerici della visita, ben validati con `@Positive`/`@Digits`/`@Max`) — chi chiama l'API bypassando il frontend può inserire qualsiasi carattere. Segnalato il 31 agosto 2026, non ancora affrontato.

Il flusso reale del prodotto — login, creazione paziente, invito via email con Resend, attivazione — è stato **verificato manualmente da Andrea per il login e per il salvataggio di un paziente** (funzionanti); gli altri flussi (invito paziente, reset password) restano coperti solo dai test automatici, non ancora provati a mano contro l'app in esecuzione.

## Prossimo passo consigliato

Andrea ha confermato il 31 agosto 2026 (parte 2) che il salvataggio di un paziente funziona da `PazienteNuovoView.vue`. **La prossima sessione riparte da qui e lavora sulla pagina della lista dei pazienti** (`PazientiListView.vue`) — dettagli su cosa cambiare da raccogliere a inizio sessione, non ancora specificati da Andrea.

Punti ancora aperti, non bloccanti per il prossimo passo:

1. Apri Claude Code su `progetti/hexis-nutrition/` (non sulla radice del workspace, non dentro una singola sottocartella da sola).
2. Andrea rivede e committa il lavoro in staging (backend token_azione, frontend-professionisti "Fondamenta" + rifiniture UI + "Nuovo paziente con visita" + bug fix controlli/UX del 31 agosto parte 2).
3. Riprendere/completare la redesign non committata di `LoginView.vue` (icona `Loader2` mancante, 2 test rotti) — preesistente, non toccata dalle sessioni del 31 agosto.
4. Verifica manuale in browser dei due `Select` (Sesso, Tipo lavoro) in `PazienteNuovoView.vue` — mai fatta dall'agente.
5. Provare a mano invito via email e reset password contro l'app in esecuzione.
6. Colmare il gap di validazione server-side su `CreaPazienteRequest` (vedi sopra), se Andrea lo ritiene prioritario.
7. Scrivere con `superpowers:writing-plans` il piano per `frontend-cliente/` di "Fondamenta" — non ancora iniziato. Poi il brainstorming di "Piano alimentare", prossimo sotto-progetto della roadmap (vedi [architettura](architettura.md)).

## Divisione dei compiti

- **Commit**: li fa **sempre e solo Andrea**. L'agente prepara e fa `git add`, poi segnala.
- **Test automatici**: responsabilità dell'agente, da eseguire dopo ogni modifica al backend, riportando l'esito reale (vedi `backend/CLAUDE.md`).
- **Verifiche manuali del prodotto** (avviare l'app, provare i flussi, controllare le email): le fa **Andrea**. L'agente non avvia l'applicazione per provarla né usa quella verifica come criterio di completamento.

Vedi anche: [domande-aperte](domande-aperte.md), [api-contracts](api-contracts.md), [modello-dati](modello-dati.md), [architettura](architettura.md).
