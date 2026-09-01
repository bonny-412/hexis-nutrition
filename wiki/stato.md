---
title: Stato del progetto
tags: [stato]
stato: stabile
creato: 2026-08-08
aggiornato: 2026-09-01
fonti: [sorgenti/2026-08-08-scope-e-stack-iniziali.md, sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md, sorgenti/2026-08-09-migrazione-a-repo-unico.md, sorgenti/2026-08-09-test-su-postgres-locale.md, sorgenti/2026-08-09-docker-solo-in-produzione.md, sorgenti/2026-08-30-hash-pulizia-invalidazione-token.md, ../docs/superpowers/specs/2026-08-31-nuovo-paziente-con-visita-design.md, ../docs/superpowers/specs/2026-09-01-plicometria-circonferenze-design.md, ../docs/superpowers/plans/2026-09-01-plicometria-circonferenze.md, log.md#2026-08-31-handoff--bug-fix-controlli-pazientenuovoview--ux-maiuscola-errori-live, log.md#2026-09-01-handoff--pagina-nuovo-paziente-eta-componente-visita-e-data-nascita-obbligatoria, log.md#2026-09-01-handoff--modulo-plicometria-e-redesign-circonferenze]
---

# Stato — hexis-nutrition

## Dove siamo

Il backend del sotto-progetto **"Fondamenta"** (autenticazione JWT con ruoli, anagrafica paziente, invito via email, reset password) è **scritto, revisionato, testato su database reale e su GitHub**. Il frontend `frontend-professionisti/` di "Fondamenta" è **scritto, testato (57 test verdi), buildato, e in staging** (login, dashboard, lista/dettaglio/creazione paziente, reset password) — vedi sessione del 30 agosto 2026 (parte 2) più sotto. Il salvataggio di un paziente da `PazienteNuovoView.vue` è stato **verificato a mano da Andrea** il 31 agosto 2026 (parte 2), che riparte da lì per lavorare sulla lista pazienti. `frontend-cliente/` non esiste ancora: contiene solo un `CLAUDE.md`.

Sessione del 1° settembre 2026 (parti 3-4): aggiunto il **modulo Plicometria** (6 protocolli clinici) e ridisegnato il modulo **Circonferenze** della visita, poi rifinito dopo la prima verifica manuale di Andrea (build/avvio confermati ok) — vedi sessioni dedicate più sotto. Backend 92/92 verdi, frontend 83/85 (le 2 falliti sono i test preesistenti e scollegati di `LoginView.spec.ts`, non toccati). Tutto **in staging, nessun commit** — Andrea farà alcune modifiche a mano prima del prossimo avvio di sessione.

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

## Sessione del 1 settembre 2026 — pagina "Nuovo paziente": età automatica, componente visita, data di nascita obbligatoria

Bounded task (design breve in chat, approvato da Andrea) → TDD. Tre modifiche in sequenza sulla pagina `PazienteNuovoView.vue`:

**1. Età calcolata in automatico**: nuovo `calcolaEta(dataNascita)` in `frontend-professionisti/src/utils/data.ts` (anni compiuti, `null` se vuota/futura/non valida). Nuovo campo "Età" nei dati anagrafici, subito dopo "Data di nascita", sola lettura (`disabled`).

**2. Sezione "Dati della visita" estratta in componente autonomo**: `frontend-professionisti/src/components/pazienti/DatiVisitaForm.vue` — data visita, altezza, peso, nota BIA, accordion circonferenze, con stato e validazione interni. Espone `valida()` e `ottieniDati()` via `defineExpose`; il genitore lo referenzia con un `ref` e li chiama al submit. Motivazione di Andrea: questo blocco tornerà identico in una futura pagina "aggiungi visita" su un paziente esistente.

**3. Data di nascita resa obbligatoria**, perché Andrea vuole l'età sempre calcolabile in modo affidabile (non salvata come colonna — resterebbe disallineata a ogni compleanno: scelta discussa e confermata con Andrea, vedi [modello-dati](modello-dati.md)). Ha richiesto più del previsto:
- **Backend**: `data_nascita` `NOT NULL` (migrazione V8), `@NotNull` su `CreaPazienteRequest.dataNascita`. 14 istanziazioni di test in 5 file (`AuthControllerTest`, `TokenAzioneRepositoryTest`, `PazienteRepositoryTest`, `VisitaRepositoryTest`, `PazienteControllerTest`) passavano `null` come data di nascita solo per costruire un paziente di supporto ad altri test (non era l'oggetto del test): aggiornate con una data placeholder. 4 body JSON in `PazienteControllerTest` che omettevano `dataNascita` aspettandosi 201: aggiornati. Nuovo test `creaPazienteSenzaDataNascitaRestituisce400`.
- **Frontend**: nuovo `erroreDataNascita` in `validators.ts`, label con asterisco, errore sotto al campo che sparisce alla selezione (stesso pattern UX degli altri obbligatori — il `DatePicker` è passato da `v-model` a `:model-value`/`@update:model-value` per agganciarsi alla pulizia errore). Test esistenti che sottomettevano senza data di nascita aggiornati per selezionarne una.

**Verifica**: backend 52/52 verdi (`mvn test`, database `hexis_test`); frontend 71/73 verdi (`npx vitest run`) — i 2 falliti sono i test preesistenti e scollegati di `LoginView.spec.ts` già segnalati il 31 agosto, non toccati; `npm run build` pulito (tsc + vite). Nessuna verifica manuale in browser: nessun tool di automazione browser collegato in questa sessione (segnalato ad Andrea).

Confermato da Andrea lo stesso giorno: la tabella `pazienti` in `hexis` (il database reale, non di test) è **vuota**, quindi la migrazione V8 non incontra righe esistenti con `data_nascita` nulla — nessun rischio al prossimo avvio.

Lavoro **in staging, nessun commit** (tocca ad Andrea) — tutto backend e frontend-professionisti modificato in questa sessione.

## Sessione del 1 settembre 2026 (parte 3) — modulo Plicometria e redesign Circonferenze

Brainstorming (percorso architetturale, con due giri di scope-creep gestiti in corsa: prima Andrea ha ampliato il piano da 4 a 6 protocolli plicometrici aggiungendo Slaughter/Evans, poi ha corretto a mano un'incoerenza nella formula di Evans che l'agente aveva segnalato) → spec → piano → esecuzione con `superpowers:subagent-driven-development`, **direttamente su `master`, senza worktree** (scelta esplicita di Andrea: `master` aveva già modifiche in staging non committate dalla sessione precedente — data di nascita obbligatoria, componente `DatiVisitaForm.vue` — che un worktree da `HEAD` non avrebbe incluso). Spec: `docs/superpowers/specs/2026-09-01-plicometria-circonferenze-design.md`; piano: `docs/superpowers/plans/2026-09-01-plicometria-circonferenze.md` (10 task, ognuno implementato e revisionato da subagent separati, più revisione finale whole-branch e una fix wave). Dettaglio completo in [log](log.md#2026-09-01-handoff--modulo-plicometria-e-redesign-circonferenze).

**Backend**: nuove tabelle `plicometrie` (1:1 opzionale con `visite`) e `durnin_womersley_coefficienti` (10 righe di riferimento seminate da migrazione, mai troncate nei test). Motore di calcolo a strategy pattern, un calcolatore per protocollo: Jackson-Pollock 3/7, Durnin-Womersley 4, Faulkner 4, Slaughter (pediatrico 7-18 anni), Evans (atleti). `PlicometriaService` valida le pliche obbligatorie per protocollo, calcola età/densità/`%BF`/FM/FFM/FMI/FFMI, applica un limite di sicurezza biologico (`%BF` minimo 3% uomo / 10% donna, su tutti e 6 i protocolli) e persiste, storicizzando protocollo/formula/coefficienti usati per riproducibilità. `Visita` ridisegnata: 11 circonferenze a misura singola (via da coppie dx/sx mai usate in produzione) + `protocollo_vita` + BMI/WHR/WHtR/MAMC calcolati e persistiti. `Paziente.sesso` ora obbligatorio a 3 valori (M/F/ALTRO) — la plicometria è bloccata per ALTRO.

**Frontend**: `Select` sesso a 3 valori, redesign dell'accordion circonferenze in `DatiVisitaForm.vue`, nuovi `PlicaInput.vue` (checkbox tripla misurazione con media live, stesso esempio numerico del PDF di specifica: 12,5/13,0/12,2 → 12,57mm) e `PlicometriaForm.vue` (campi condizionati da protocollo e sesso).

**Revisione finale whole-branch** (Opus): verdetto "ready to merge, with fixes" — verificate a mano tutte e 6 le formule cliniche (nessun errore), la parità di contratto backend↔frontend su tutti i DTO, la sequenza delle 4 migrazioni contro lo schema reale. Trovati e risolti in un'unica fix wave: pliche di un protocollo precedente che sopravvivevano al cambio protocollo e finivano nei dati clinici salvati (fix sia lato servizio sia lato form); i messaggi di errore delle nuove eccezioni 400 non arrivavano mai al client (`server.error.include-message` di default è `never` in Spring Boot); nessun minimo su `altezzaCm` (un valore tipo 17cm mandava in overflow il calcolo del BMI, 500 invece di 400); un vincolo `@DecimalMax` reso morto da un `@Digits` troppo stretto (difetto del piano, non degli implementer); un'asimmetria di tipo su `dataNascita` nel client TypeScript. Re-review mirata sulla fix wave: tutti i 6 finding risolti, nessuna nuova rottura.

Due domande di prodotto emerse in revisione, non risolte automaticamente (spostate in [domande-aperte](domande-aperte.md)): l'accoppiamento tra MAMC e protocollo plicometrico (MAMC mai calcolabile per JP3-uomo o sesso ALTRO), e l'incoerenza tra densità corporea e `%BF` quando scatta il limite di sicurezza.

**Adattamento di processo degno di nota**: la skill `subagent-driven-development` assume che ogni implementer faccia `git commit` (usa gli SHA per i diff tra task) — in conflitto con la regola assoluta di questo progetto. Adattato per l'intera esecuzione: solo `git add`, mai commit; i pacchetti di revisione generati con `git diff --cached` mirato ai file di ciascun task invece che con range di commit.

**Verifica**: backend `mvn test` → **89/89 verdi, BUILD SUCCESS** (verificato due volte, anche dall'agente coordinatore in autonomia con JDK 21 impostato correttamente). Frontend `npm run test` → **83/85 verdi** — i 2 falliti sono gli stessi test preesistenti e scollegati di `LoginView.spec.ts` segnalati dal 31 agosto 2026, non toccati da nessuno dei 10 task; `npx tsc --noEmit` pulito. Nessuna verifica manuale in browser (nessun tool di automazione collegato in questa sessione).

Lavoro **in staging, nessun commit** (tocca ad Andrea) — 63 file toccati tra backend, frontend-professionisti e wiki.

## Cosa resta aperto

Elenco completo e ragionato in [domande-aperte](domande-aperte.md). I punti che toccano il codice esistente:

- **Versione di PostgreSQL**: sviluppo e test girano sulla 13, la produzione sarà in Docker e potrà usare qualunque immagine. Fissarla su `postgres:13` allinea gli ambienti; qualsiasi altra scelta introduce una differenza che nessun test copre.
- **Deploy**: previsto Docker, nient'altro deciso (immagini, orchestrazione, provider, gestione di `JWT_SECRET` e `RESEND_API_KEY`).
- **Validazione server-side incompleta su `CreaPazienteRequest`**: `nome`, `cognome`, `telefono`, `sesso`, `lavoro` non hanno un pattern lato backend (a differenza dei campi numerici della visita, ben validati con `@Positive`/`@Digits`/`@Max`) — chi chiama l'API bypassando il frontend può inserire qualsiasi carattere. Segnalato il 31 agosto 2026, non ancora affrontato.
- **Copertura di test mancante per due fix della revisione finale del modulo Plicometria** (1° settembre 2026, parte 3): nessun test automatico esercita direttamente lo scenario "una plica non richiesta dal protocollo scelto viene esclusa/annullata" né verifica che il body della risposta 400 contenga davvero il messaggio d'errore. Il codice è corretto per ispezione (confermato dal reviewer finale), ma senza un test dedicato una futura regressione non verrebbe intercettata automaticamente.
- **Redesign non committata di `LoginView.vue`**: 2 test rotti (`Servizio non raggiungibile` vs `Servizio temporaneamente non raggiungibile`), preesistente da prima del 31 agosto 2026, mai ripresa.

Il flusso reale del prodotto — login, creazione paziente, invito via email con Resend, attivazione — è stato **verificato manualmente da Andrea per il login e per il salvataggio di un paziente** (funzionanti); gli altri flussi (invito paziente, reset password) restano coperti solo dai test automatici, non ancora provati a mano contro l'app in esecuzione.

## Sessione del 1 settembre 2026 (parte 4) — verifica manuale, flag limite di sicurezza, rifiniture UI Plicometria

Andrea ha lanciato di persona `mvn clean install` (backend) e l'avvio del frontend: **entrambi ok**, prima verifica manuale reale contro il lavoro del modulo Plicometria/Circonferenze.

**Decise le due domande di prodotto aperte dalla revisione finale** (vedi sessione precedente): MAMC↔protocollo confermato "va bene così" (nessuna modifica); per l'incoerenza densità/`%BF` al limite di sicurezza, consigliata e implementata la soluzione che non falsifica mai la densità corporea persistita — aggiunto invece un flag esplicito `Plicometria.limite_sicurezza_applicato` (booleano, migrazione V13) che segnala quando `%BF` è stato corretto e i due valori non si riconciliano più via Siri. Implementazione diretta (bounded, TDD, nessuna orchestrazione multi-agente): nuovo helper `CalcoliPlicometria.limiteSicurezzaApplicato`, wiring in `PlicometriaService`, 2 nuovi test unitari + 2 nuovi test di integrazione (caso normale e caso limite con pliche Evans vicine allo zero). Backend **92/92 verdi**. Entrambe le domande spostate in "Risolte" in [domande-aperte](domande-aperte.md).

**Tre rifiniture UI richieste da Andrea dopo la verifica manuale**: (1) sezione Plicometria spostata in un `AccordionItem` come le Circonferenze, si apre da sola se la validazione fallisce; (2) campo "Età" spostato sulla stessa riga di "Data di nascita" nell'anagrafica; (3) i `Select` "Protocollo" (Plicometria) e "Protocollo vita" (Circonferenze) ora hanno una voce "Seleziona" in cima alla lista, selezionata di default — esattamente come una select HTML normale — al posto del precedente placeholder/preselezione fissa (`protocollo_vita` ora parte vuoto invece che precompilato su OMS; se lasciato vuoto il backend applica comunque OMS di default, nessun cambio di comportamento finale). Trovato e corretto un effetto collaterale: il test end-to-end della plicometria falliva perché i campi ora sono dentro un accordion chiuso di default — sistemato aprendo l'accordion prima di interagire, come già si fa per le circonferenze.

**Verifica**: frontend `npm run test` → **83/85 verdi** (stessi 2 test preesistenti e scollegati di `LoginView.spec.ts`), `npx tsc --noEmit` pulito.

Lavoro in staging, nessun commit — **Andrea farà alcune modifiche a mano** prima del prossimo avvio di sessione.

## Prossimo passo consigliato

Andrea ha annunciato di voler riprendere domani con: (1) **verifica manuale del salvataggio di un paziente** contro l'app in esecuzione (col modulo Plicometria/Circonferenze ridisegnato — non ancora provato a mano end-to-end, solo con `mvn clean install` e avvio frontend); (2) **lavoro sulla lista dei pazienti** (`PazientiListView.vue`, confermato prioritario da Andrea il 31 agosto 2026 parte 2), non ancora iniziato. Nel frattempo farà alcune modifiche a mano non ancora note all'agente — da controllare a inizio sessione con `git status`/`git diff` prima di assumere lo stato descritto qui.

Punti ancora aperti, non bloccanti per il prossimo passo:

1. Apri Claude Code su `progetti/hexis-nutrition/` (non sulla radice del workspace, non dentro una singola sottocartella da sola).
2. Controlla `git status`/`git diff` prima di iniziare: Andrea ha annunciato modifiche a mano non ancora descritte in questa pagina.
3. Andrea rivede e committa il lavoro in staging (l'intera storia dei sotto-progetti fin qui: Fondamenta, Nuovo paziente con visita, età/data di nascita obbligatoria, modulo Plicometria/redesign Circonferenze e le sue rifiniture).
4. Riprendere/completare la redesign non committata di `LoginView.vue` (icona `Loader2` mancante, 2 test rotti) — preesistente, mai ripresa.
5. Provare a mano invito via email e reset password contro l'app in esecuzione.
6. Colmare il gap di validazione server-side su `CreaPazienteRequest` (vedi sopra), se Andrea lo ritiene prioritario.
7. Aggiungere test di regressione dedicati per due fix della revisione finale del modulo Plicometria (esclusione pliche non richieste dal protocollo; presenza del messaggio nel body del 400) — vedi "Cosa resta aperto".
8. Scrivere con `superpowers:writing-plans` il piano per `frontend-cliente/` di "Fondamenta" — non ancora iniziato. Poi il brainstorming di "Piano alimentare", prossimo sotto-progetto della roadmap (vedi [architettura](architettura.md)).

## Divisione dei compiti

- **Commit**: li fa **sempre e solo Andrea**. L'agente prepara e fa `git add`, poi segnala.
- **Test automatici**: responsabilità dell'agente, da eseguire dopo ogni modifica al backend, riportando l'esito reale (vedi `backend/CLAUDE.md`).
- **Verifiche manuali del prodotto** (avviare l'app, provare i flussi, controllare le email): le fa **Andrea**. L'agente non avvia l'applicazione per provarla né usa quella verifica come criterio di completamento.

Vedi anche: [domande-aperte](domande-aperte.md), [api-contracts](api-contracts.md), [modello-dati](modello-dati.md), [architettura](architettura.md).
