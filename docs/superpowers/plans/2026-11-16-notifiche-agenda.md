# Notifiche dell'agenda — Piano di implementazione

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Avvisare il professionista, con push del browser, lista nel campanello e puntino in sidebar, che una visita o un promemoria con orario sta per iniziare.

**Architecture:** Uno scheduler Spring (ogni minuto) crea righe in `notifiche` per gli eventi `VISITA_STUDIO`/`VIDEOCONSULTO`/`PROMEMORIA` con orario e `notifica = true`, quando `inizio − anticipo` è raggiunto, e per ogni riga nuova invia una Web Push generica. Il frontend legge le notifiche da `GET /notifiche` (polling 60 s), le mostra nel campanello e nel puntino della sidebar, e registra un service worker per le push.

**Tech Stack:** Spring Boot 3.3.4 / Java 21 / PostgreSQL / Flyway; libreria Web Push (`nl.martijndwars:web-push` + BouncyCastle); Vue 3 + TypeScript + Pinia + shadcn-vue; service worker in `public/`.

**Spec:** `docs/superpowers/specs/2026-11-16-notifiche-agenda-design.md`

## Regole del progetto (valgono per ogni task)

- **Niente `git commit`**, mai: a fine task si segnala che è pronto. Si può fare `git add`.
- **Niente test scritti dall'implementatore**: li scrive Andrea dopo. Ogni task elenca i "Casi da coprire", per il lavoro di Andrea; la verifica del task è compilazione + controllo manuale.
- Backend: `backend/src/main/java/com/hexisnutrition/backend/`, migrazioni in `backend/src/main/resources/db/migration/` (ultima esistente: V30), fuso `AgendaTempo.FUSO` (`Europe/Rome`).
- Frontend: `frontend-professionisti/src/`; usare i componenti shadcn-vue di `components/ui/` (se ne manca uno: `npx shadcn-vue@latest add <nome>`); form secondo la convenzione del progetto (obbligatori con errore live, niente `type="number"`).
- Valori fissi dalla spec: anticipi ammessi **120, 60, 45, 30, 15** minuti, default **45**, `notifica` default **true**; testo push generico, senza dati del paziente; gli eventi esistenti alla migrazione passano a `notifica = false`.
- Verifica backend: `mvn -q -DskipTests compile` da `backend/` (o `./mvnw` se presente). Verifica frontend: `npx vue-tsc --noEmit` da `frontend-professionisti/`.

## Review Focus

Condizioni che la spec implica ma nessun test esercita; ognuna è coperta dal "Casi da coprire" del task indicato:

1. Evento creato quando è già dentro la finestra di anticipo e non ancora iniziato → notifica al primo giro dello scheduler; se è già iniziato, nessuna notifica (Task 2).
2. Modifica di un evento senza cambiare orario/anticipo → la notifica esistente (anche già letta) non viene ricreata né ripetuta come push (Task 2).
3. Evento spostato di data/ora, annullato, segnato fatto o eliminato → le notifiche obsolete spariscono (Task 2); eliminare un paziente (cancellazione massiva dei suoi eventi futuri) non lascia notifiche orfane (Task 2).
4. Chiavi VAPID assenti → il backend parte, le push sono disattivate, campanello e puntini funzionano (Task 3).
5. Notifica arrivata mentre la lista è aperta → alla chiusura non viene segnata come letta (Task 5).
6. Browser senza supporto push o permesso negato → l'app funziona e la lista mostra un testo di aiuto, senza errori in console (Task 6).

---

## Struttura dei file

| File | Responsabilità |
|---|---|
| `backend/.../agenda/Appuntamento.java`, `AppuntamentoRequest.java`, `AppuntamentoResponse.java`, `AgendaService.java` | campi `notifica` e `anticipoNotificaMinuti` (T1); ricalcolo notifiche alle modifiche (T2) |
| `backend/.../notifiche/` (nuovo pacchetto) | entità, repository, servizio, scheduler, controller delle notifiche e delle iscrizioni push (T2, T3) |
| `backend/.../auth/SecurityConfig.java` | regola per `/notifiche/**` (T2) |
| `frontend-professionisti/src/api/agenda.ts`, `components/agenda/AppuntamentoDialog.vue`, `views/agenda/AgendaView.vue` | form e tipi (T4) |
| `frontend-professionisti/src/api/notifiche.ts`, `stores/notifiche.ts`, `components/notifiche/NotificheLista.vue`, `AppHeader.vue`, `AppSidebar.vue` | campanello e puntini (T5) |
| `frontend-professionisti/public/sw.js`, `src/utils/push.ts` | service worker e registrazione push (T6) |
| `wiki/…` | documentazione (T7) |

---

### Task 1: Campi `notifica` e `anticipoNotificaMinuti` sugli appuntamenti (backend)

**Files:**
- Create: `backend/src/main/resources/db/migration/V31__appuntamenti_notifica.sql`
- Modify: `backend/.../agenda/Appuntamento.java`, `AppuntamentoRequest.java`, `AppuntamentoResponse.java`, `AgendaService.java` (metodo `applica`)

**Interfaces:**
- Produces: `Appuntamento.isNotifica(): boolean`, `setNotifica(boolean)`, `getAnticipoNotificaMinuti(): int`, `setAnticipoNotificaMinuti(int)`; `AppuntamentoRequest` con in coda `Boolean notifica, Integer anticipoNotificaMinuti` (nullable); `AppuntamentoResponse` con in coda `boolean notifica, int anticipoNotificaMinuti`; costante `AgendaTempo.ANTICIPI_NOTIFICA_MINUTI = Set.of(15, 30, 45, 60, 120)` e `AgendaTempo.ANTICIPO_NOTIFICA_DEFAULT = 45`.

- [ ] **Step 1: Migrazione `V31`.** `ALTER TABLE appuntamenti` aggiunge `notifica BOOLEAN NOT NULL DEFAULT TRUE` e `anticipo_notifica_minuti INTEGER NOT NULL DEFAULT 45` con `CHECK (anticipo_notifica_minuti IN (15, 30, 45, 60, 120))`; subito dopo `UPDATE appuntamenti SET notifica = FALSE;` (gli eventi esistenti non devono notificare).
- [ ] **Step 2: Entità, request, response.** Aggiungi i campi come da Interfaces; `AppuntamentoResponse.da` li copia dall'entità.
- [ ] **Step 3: `AgendaService.applica`.** Dopo lo `switch`, imposta i due campi: il flag è effettivo (`true`) solo se il tipo non è `BLOCCO`, esiste `oraInizio`, e `r.notifica()` è `null` o `true`; altrimenti `false`. L'anticipo è `r.anticipoNotificaMinuti()` o il default se `null`; se non appartiene a `ANTICIPI_NOTIFICA_MINUTI` lancia `AppuntamentoNonValidoException("Anticipo di notifica non valido")`. Per eventi non notificabili salva `false` e il default.
- [ ] **Step 4: Verifica.** Compila; avvia il backend (la migrazione deve applicarsi); `POST /agenda` senza i nuovi campi restituisce `notifica: true, anticipoNotificaMinuti: 45` per una visita e `notifica: false` per un blocco e per un promemoria senza ora.

**Casi da coprire (Andrea):** anticipo non ammesso → 400; blocco e promemoria senza ora salvati con `notifica = false`; richiesta senza i nuovi campi → default; eventi preesistenti con `notifica = false`.

---

### Task 2: Notifiche nel backend (tabella, scheduler, API di lettura)

**Files:**
- Create: `backend/src/main/resources/db/migration/V32__crea_tabelle_notifiche_e_push.sql`
- Create (pacchetto `backend/.../notifiche/`): `Notifica.java`, `NotificaRepository.java`, `NotificheService.java`, `NotificheScheduler.java`, `NotificheController.java`, `NotificaResponse.java`, `NotificheResponse.java`
- Modify: `AppuntamentoRepository.java`, `AgendaService.java`, `auth/SecurityConfig.java`

**Interfaces:**
- Consumes: Task 1 (campi su `Appuntamento`).
- Produces:
  - `NotificheService.generaScadute(): void` — usato dallo scheduler e, in Task 3, esteso con l'invio push.
  - `NotificheService.riallinea(Appuntamento app): void` — chiamato da `AgendaService` dopo ogni `crea/aggiorna/annulla/impostaFatto`.
  - `NotificheService.attive(UUID professionistaId): NotificheResponse`; `segnaLette(UUID professionistaId, Collection<UUID> ids)`; `segnaAgendaViste(UUID professionistaId)`.
  - `record NotificaResponse(UUID id, UUID appuntamentoId, TipoAppuntamento tipo, LocalDate data, LocalTime oraInizio, int anticipoMinuti, String testo, String pazienteNome, String pazienteCognome, Instant scattaIl, boolean letta, boolean agendaVista)`.
  - `record NotificheResponse(List<NotificaResponse> notifiche, long nonLette, long nonVisteInAgenda)`.
  - Endpoint (JWT, ruolo `PROFESSIONISTA`): `GET /notifiche` → `NotificheResponse`; `POST /notifiche/lette` con body `{ "ids": [uuid] }` → 204; `POST /notifiche/agenda-viste` → 204.

- [ ] **Step 1: Migrazione `V32`.** Crea `notifiche` (`id UUID PK`, `professionista_id UUID NOT NULL REFERENCES professionisti(id)`, `appuntamento_id UUID NOT NULL REFERENCES appuntamenti(id) ON DELETE CASCADE`, `scatta_il TIMESTAMPTZ NOT NULL`, `creata_il TIMESTAMPTZ NOT NULL DEFAULT now()`, `letta_il TIMESTAMPTZ`, `agenda_vista_il TIMESTAMPTZ`, `UNIQUE (appuntamento_id, scatta_il)`, indice su `professionista_id`) e `push_subscriptions` (`id UUID PK`, `professionista_id UUID NOT NULL REFERENCES professionisti(id) ON DELETE CASCADE`, `endpoint TEXT NOT NULL UNIQUE`, `p256dh TEXT NOT NULL`, `auth TEXT NOT NULL`, `user_agent VARCHAR(300)`, `creata_il TIMESTAMPTZ NOT NULL DEFAULT now()`). Entrambe le tabelle sono create qui; la seconda si usa dal Task 3.
- [ ] **Step 2: Entità e repository.** `Notifica` (JPA, stile di `Appuntamento`) e `NotificaRepository` con: `List<Notifica> findAllByProfessionistaId(UUID)`, `List<Notifica> findAllByAppuntamentoId(UUID)`, `void deleteByAppuntamentoIdAndScattaIlNot(UUID, Instant)`, `void deleteByAppuntamentoId(UUID)`, e `@Modifying @Query(nativeQuery = true) int inserisciSeAssente(UUID id, UUID professionistaId, UUID appuntamentoId, Instant scattaIl)` basata su `INSERT … ON CONFLICT (appuntamento_id, scatta_il) DO NOTHING` (restituisce 1 solo se ha inserito).
- [ ] **Step 3: `AppuntamentoRepository`.** Aggiungi `List<Appuntamento> findAllByDataBetweenAndNotificaTrue(LocalDate da, LocalDate a)` (tutti i professionisti; il filtro su tipo/stato/fatto/ora si fa nel servizio).
- [ ] **Step 4: Regola di scatto in `NotificheService`.** Metodo privato `Optional<Instant> scatto(Appuntamento a)` che restituisce `data + oraInizio` in `AgendaTempo.FUSO` meno `anticipoNotificaMinuti`, solo se l'evento è *notificabile* (tipo `VISITA_STUDIO`/`VIDEOCONSULTO`/`PROMEMORIA`, `oraInizio != null`, `notifica`, e visita `PROGRAMMATA` oppure promemoria non `fatto`). Metodo privato `Instant inizio(Appuntamento a)`.
- [ ] **Step 5: `generaScadute()`.** Per gli eventi con data tra oggi e domani (fuso `Europe/Rome`), per ognuno con `scatto` presente, `scatto ≤ ora` e `ora < inizio`: genera un `UUID`, chiama `inserisciSeAssente`; il valore restituito è il segnale "nuova" che il Task 3 usa per l'invio push. Il metodo è `@Transactional`.
- [ ] **Step 6: `riallinea(app)`.** Se l'evento non è notificabile elimina tutte le sue notifiche; altrimenti elimina quelle con `scatta_il` diverso dallo scatto corrente (`deleteByAppuntamentoIdAndScattaIlNot`). Non elimina né ricrea una notifica con lo stesso `scatta_il`. In `AgendaService` chiama `riallinea` dopo il `save` in `crea`, `aggiorna`, `annulla`, `impostaFatto`; non serve in `elimina` (cascata). Inietta `NotificheService` nel costruttore.
- [ ] **Step 7: `attive(professionistaId)`.** Carica le notifiche del professionista con i rispettivi appuntamenti e tiene quelle ancora valide: visita se `PROGRAMMATA` e `ora < inizio + durata`; promemoria se non `fatto` e `data` è oggi. Mappa in `NotificaResponse` (nome/cognome del paziente da `PazienteRepository`, come in `AgendaService.rispondi`), ordinate per `scattaIl` decrescente. `nonLette` = quelle con `letta_il` nullo, `nonVisteInAgenda` = quelle con `agenda_vista_il` nullo.
- [ ] **Step 8: Segnatura di lettura.** `segnaLette` imposta `letta_il = now()` **solo** sulle notifiche del professionista il cui id è nella lista; `segnaAgendaViste` imposta `agenda_vista_il = now()` su tutte le sue notifiche con il campo nullo.
- [ ] **Step 9: Scheduler e controller.** `NotificheScheduler` con `@Scheduled(fixedDelay = 60_000)` che chiama `generaScadute()` (`@EnableScheduling` esiste già in `BackendApplication`; stile di `inviti/TokenAzionePulizia`). `NotificheController` (`@RequestMapping("/notifiche")`, principal `@AuthenticationPrincipal UUID professionistaId` come in `AgendaController`). In `SecurityConfig` aggiungi `.requestMatchers("/notifiche/**").hasRole("PROFESSIONISTA")` accanto a `/agenda/**`.
- [ ] **Step 10: Pulizia notturna.** Aggiungi a `NotificaRepository` `void deleteByScattaIlBefore(Instant limite)` e a `NotificheScheduler` un metodo `@Scheduled(cron = "0 0 3 * * *", zone = "Europe/Rome") @Transactional` che elimina le notifiche con `scatta_il` precedente all'inizio del giorno corrente (mezzanotte `Europe/Rome`). Nessuna notifica sopravvive al giorno in cui è scattata, coerente con la regola "fino a fine giornata", e la tabella resta piccola. Stile di `inviti/TokenAzionePulizia`.
- [ ] **Step 11: Verifica.** Compila e avvia. Crea una visita tra 10 minuti con anticipo 15: entro un minuto `GET /notifiche` la restituisce con `nonLette: 1`; modificarne solo il testo non la ricrea; spostarla a un'altra ora la sostituisce; annullarla la fa sparire; `POST /notifiche/lette` con il suo id azzera `nonLette`.

**Casi da coprire (Andrea):** Review Focus 1, 2, 3 (spostamento, annullo, fatto, eliminazione, eliminazione del paziente); pulizia notturna che elimina solo le notifiche scattate prima di oggi; isolamento tra professionisti; `lette` con id non suoi ignorato; promemoria senza ora mai notificato.

---

### Task 3: Web Push dal backend

**Files:**
- Modify: `backend/pom.xml`, `backend/src/main/resources/application.yml`, `NotificheService.java`, `NotificheController.java`
- Create: `backend/.../notifiche/PushSubscription.java`, `PushSubscriptionRepository.java`, `PushService.java`

**Interfaces:**
- Consumes: Task 2 (`inserisciSeAssente` restituisce 1 per le notifiche nuove).
- Produces: `PushService.invia(UUID professionistaId, String titolo, String corpo, String url): void` (best-effort, non lancia); `PushService.chiavePubblica(): Optional<String>`; endpoint `GET /notifiche/push/chiave` → `{ "chiave": string | null }`, `POST /notifiche/push/iscrizioni` body `{ endpoint, p256dh, auth }` → 204 (se l'`endpoint` esiste già, lo riassegna al professionista corrente e aggiorna le chiavi), `DELETE /notifiche/push/iscrizioni` body `{ endpoint }` → 204 (elimina solo se del professionista).

- [ ] **Step 1: Dipendenze e configurazione.** In `pom.xml` aggiungi `nl.martijndwars:web-push` e `org.bouncycastle:bcprov-jdk18on` (versioni stabili correnti compatibili tra loro). In `application.yml` aggiungi `app.push.vapid-public-key: ${VAPID_PUBLIC_KEY:}`, `app.push.vapid-private-key: ${VAPID_PRIVATE_KEY:}`, `app.push.subject: ${VAPID_SUBJECT:mailto:no-reply@hexisnutrition.example}`. Le chiavi si generano una volta (`npx web-push generate-vapid-keys`) e si impostano come variabili d'ambiente, mai nel repo.
- [ ] **Step 2: `PushService`.** Se una delle chiavi è vuota: `chiavePubblica()` restituisce `Optional.empty()` e `invia` non fa nulla (nessun errore all'avvio, nessun log rumoroso). Altrimenti per ogni iscrizione del professionista invia un payload JSON `{"titolo","corpo","url"}`; se il servizio push risponde 404 o 410, elimina l'iscrizione; ogni altro errore si registra e si prosegue.
- [ ] **Step 3: Invio dallo scheduler.** In `generaScadute()`, per ogni notifica *nuova* chiama `PushService.invia(professionistaId, "Hexis", testo, "/agenda")` con testo `"Hai una visita tra N minuti"` per `VISITA_STUDIO`/`VIDEOCONSULTO` e `"Hai un promemoria tra N minuti"` per `PROMEMORIA` (N = `anticipoNotificaMinuti`). Nessun titolo, nome o testo dell'evento nel payload. L'invio avviene dopo il commit dell'inserimento (un errore di rete non deve annullare la creazione della notifica).
- [ ] **Step 4: Endpoint.** Aggiungi i tre endpoint di Interfaces a `NotificheController`.
- [ ] **Step 5: Verifica.** Senza chiavi: il backend parte, `GET /notifiche/push/chiave` risponde `{ "chiave": null }`. Con chiavi generate: risponde la chiave pubblica; (l'invio reale si verifica nel Task 6).

**Casi da coprire (Andrea):** Review Focus 4; payload senza dati del paziente; iscrizione scaduta (410) eliminata; stesso `endpoint` registrato due volte → una riga sola; push inviata una sola volta per notifica.

---

### Task 4: Campi di notifica nel dialog appuntamento (frontend)

**Files:**
- Modify: `frontend-professionisti/src/api/agenda.ts`, `components/agenda/AppuntamentoDialog.vue`, `views/agenda/AgendaView.vue` (funzione `richiestaDa`)

**Interfaces:**
- Consumes: Task 1 (campi API).
- Produces: `Appuntamento.notifica: boolean` e `Appuntamento.anticipoNotificaMinuti: number`; `AppuntamentoRichiesta.notifica?: boolean` e `anticipoNotificaMinuti?: number`; `export const ANTICIPI_NOTIFICA: { valore: number; etichetta: string }[]` in `utils/agenda.ts` con le opzioni `120 → "2 ore"`, `60 → "1 ora"`, `45 → "45 minuti"`, `30 → "30 minuti"`, `15 → "15 minuti"`.

- [ ] **Step 1: Tipi e costante.** Aggiorna `api/agenda.ts` e `utils/agenda.ts` come da Interfaces.
- [ ] **Step 2: Dialog.** In `AppuntamentoDialog.vue` aggiungi `notifica = ref(true)` e `anticipo = ref(45)`, inizializzati da `a?.notifica ?? true` e `a?.anticipoNotificaMinuti ?? 45` come gli altri campi, e inclusi nell'oggetto serializzato dello stato "modificato" (riga con `JSON.stringify([tipo.value, …])`). Sezione visibile solo se `eVisita` oppure (`ePromemoria` **e** `ora` valorizzata): `Checkbox` "Ricevi notifica" e, se attiva, `Select` "Anticipo" con `ANTICIPI_NOTIFICA` (`data-test` `appuntamento-notifica` e `appuntamento-anticipo`). Se la sezione non è visibile, non inviare i campi (il backend li imposta a `false`). Il payload di crea/aggiorna include `notifica` e `anticipoNotificaMinuti`.
- [ ] **Step 3: "Sposta a domani".** `richiestaDa` in `AgendaView.vue` aggiunge `notifica: a.notifica` e `anticipoNotificaMinuti: a.anticipoNotificaMinuti`.
- [ ] **Step 4: Verifica.** `npx vue-tsc --noEmit`; a mano: nuova visita → checkbox attiva e 45 min; promemoria senza ora → sezione assente; promemoria con ora → sezione presente; riaprendo un evento salvato i valori sono quelli salvati.

**Casi da coprire (Andrea):** sezione nascosta per blocco e per promemoria senza ora; passaggio da senza ora a con ora mostra la sezione; "Sposta a domani" conserva i due campi.

---

### Task 5: Campanello, lista e puntini (frontend)

**Files:**
- Create: `frontend-professionisti/src/api/notifiche.ts`, `src/stores/notifiche.ts`, `src/components/notifiche/NotificheLista.vue`
- Modify: `src/components/AppHeader.vue`, `src/components/AppSidebar.vue`, `src/views/agenda/AgendaView.vue`

**Interfaces:**
- Consumes: Task 2 (`GET /notifiche`, `POST /notifiche/lette`, `POST /notifiche/agenda-viste`).
- Produces:
  - `api/notifiche.ts`: `interface Notifica { id; appuntamentoId; tipo: TipoAppuntamento; data: string; oraInizio: string | null; anticipoMinuti: number; testo: string | null; pazienteNome: string | null; pazienteCognome: string | null; scattaIl: string; letta: boolean; agendaVista: boolean }`, `interface NotificheRisposta { notifiche: Notifica[]; nonLette: number; nonVisteInAgenda: number }`, `elencoNotifiche(): Promise<NotificheRisposta>`, `segnaLette(ids: string[]): Promise<void>`, `segnaAgendaViste(): Promise<void>`.
  - `useNotificheStore` (Pinia, stile `stores/theme.ts`): `notifiche`, `nonLette`, `nonVisteInAgenda`, `cliccato` (ref boolean); computed `pulsa = nonLette > 0 && !cliccato`; `carica(): Promise<void>`; `avviaPolling(): void` / `fermaPolling(): void` (ogni 60 s, in pausa con `document.hidden`, ricarico al ritorno in primo piano); `apriLista(): void` (imposta `cliccato = true`); `chiudiLista(): Promise<void>` (segna lette **solo gli id della lista così com'era all'apertura**, poi `cliccato = false` e ricarica); `segnaAgendaViste(): Promise<void>`.

- [ ] **Step 1: Client API e store.** Come da Interfaces. `chiudiLista` memorizza gli id presenti in `apriLista` e usa solo quelli.
- [ ] **Step 2: `NotificheLista.vue`.** Contenuto del popover: elenco delle notifiche (ora e anticipo, titolo dell'evento come in `agendaStile.titolo`, nome del paziente per i promemoria collegati, stile "non letta"), stato vuoto "Nessuna notifica", e in testa lo spazio per l'invito push del Task 6. Clic su una notifica: chiude la lista e naviga a `{ name: 'agenda', query: { evento: id, data } }`.
- [ ] **Step 3: Campanello in `AppHeader.vue`.** Sostituisce il `Button` fisso con un `Popover` (shadcn, aggiungerlo se manca) il cui trigger è lo stesso bottone `aria-label="Notifiche"`. Il puntino compare solo se `nonLette > 0`, con `animate-pulse` quando `store.pulsa`; si apre con `apriLista`, si chiude con `chiudiLista`. `onMounted` → `avviaPolling()`, `onUnmounted` → `fermaPolling()`.
- [ ] **Step 4: Sidebar.** In `AppSidebar.vue`, sulla voce `Agenda` (e anche a barra collassata) un puntino statico quando `store.nonVisteInAgenda > 0`, nello stile del puntino dell'header.
- [ ] **Step 5: `AgendaView.vue`.** `watch(() => store.nonVisteInAgenda, …, { immediate: true })` che chiama `store.segnaAgendaViste()` quando è `> 0` (copre sia l'apertura della pagina sia l'arrivo di nuove notifiche mentre si è già in agenda). Gestisci la query `?evento=<id>&data=<YYYY-MM-DD>` come la query `nuovo`: imposta `riferimento` alla data, ricarica, apre il dettaglio con `apriDettaglio` quando l'evento è presente, poi rimuove la query.
- [ ] **Step 6: Verifica.** `npx vue-tsc --noEmit`; a mano con una visita tra pochi minuti: compare il puntino pulsante sul campanello e quello sulla voce Agenda; il clic ferma il pulse; chiudere la lista spegne il puntino del campanello; aprire la pagina Agenda spegne quello della sidebar; il clic su una notifica apre il dettaglio dell'evento.

**Casi da coprire (Andrea):** Review Focus 5; pulse che riparte dopo ricaricamento con non lette; polling fermo a scheda nascosta; puntino sidebar spento entrando in agenda anche con notifiche arrivate a pagina aperta.

---

### Task 6: Service worker e iscrizione push (frontend)

**Files:**
- Create: `frontend-professionisti/public/sw.js`, `src/utils/push.ts`
- Modify: `src/components/notifiche/NotificheLista.vue`, `src/components/AppHeader.vue` (logout)

**Interfaces:**
- Consumes: Task 3 (`/notifiche/push/*`); Task 5 (`NotificheLista`).
- Produces: `utils/push.ts`: `type StatoPush = 'non-supportato' | 'da-attivare' | 'attivo' | 'negato' | 'non-configurato'`; `statoPush(): Promise<StatoPush>`; `attivaPush(): Promise<StatoPush>` (da chiamare in un gesto utente); `sincronizzaPush(): Promise<void>` (se il permesso è già concesso, ri-registra l'iscrizione al backend); `disattivaPush(): Promise<void>`. API client per `GET /notifiche/push/chiave`, `POST /notifiche/push/iscrizioni`, `DELETE /notifiche/push/iscrizioni` in `api/notifiche.ts`.

- [ ] **Step 1: `public/sw.js`.** Evento `push`: legge il JSON `{titolo, corpo, url}`; **se esiste un client finestra visibile e in primo piano** (`clients.matchAll({ type: 'window' })` con `visibilityState === 'visible'`) non mostra nulla; altrimenti `showNotification(titolo, { body: corpo, icon: '/favicon.svg', data: { url } })`. Evento `notificationclick`: chiude la notifica, mette a fuoco una scheda Hexis esistente (e la porta su `url`) oppure apre una nuova finestra.
- [ ] **Step 2: `utils/push.ts`.** `statoPush`: `'non-supportato'` se mancano `serviceWorker`, `PushManager` o `Notification`; `'non-configurato'` se il backend risponde `chiave: null`; `'negato'` se `Notification.permission === 'denied'`; `'attivo'` se c'è un'iscrizione; altrimenti `'da-attivare'`. `attivaPush`: registra `/sw.js`, `Notification.requestPermission()`, `pushManager.subscribe({ userVisibleOnly: true, applicationServerKey })` con la chiave convertita da base64url a `Uint8Array`, e invia `{ endpoint, p256dh, auth }` (chiavi `getKey` in base64) al backend. Ogni errore diventa uno stato, mai un'eccezione verso l'interfaccia.
- [ ] **Step 3: Invito nella lista.** In `NotificheLista.vue`: `da-attivare` → bottone "Attiva le notifiche su questo dispositivo" (chiama `attivaPush`); `negato` → testo che spiega di riattivarle dalle impostazioni del sito nel browser; `non-supportato` → testo che il browser non supporta le notifiche push; `attivo` e `non-configurato` → niente.
- [ ] **Step 4: Avvio e logout.** All'avvio della sessione (nell'`onMounted` di `AppHeader.vue`) chiama `sincronizzaPush()`. In `onLogout` chiama `disattivaPush()` prima di `auth.logout()` (best-effort), così un computer condiviso non riceve gli avvisi del professionista precedente.
- [ ] **Step 5: Verifica.** `npx vue-tsc --noEmit`; a mano su `localhost` con le chiavi VAPID impostate: attiva dal campanello, crea una visita tra 5 minuti con anticipo 15, **chiudi o metti in secondo piano la scheda** e verifica che arrivi l'avviso di sistema con testo generico; il clic apre l'agenda. Ripeti su Chrome/Edge (Windows) e Safari (macOS 13+). Con permesso negato la lista mostra il testo d'aiuto e non ci sono errori in console.

**Casi da coprire (Andrea):** Review Focus 6; avviso non mostrato con scheda visibile; iscrizione ripristinata dopo riavvio del browser; logout rimuove l'iscrizione.

---

### Task 7: Documentazione (wiki)

**Files:**
- Create: `wiki/decisioni/0008-notifiche-agenda.md`
- Modify: `wiki/decisioni/0007-appuntamenti-e-visite-registrate.md`, `wiki/api-contracts.md`, `wiki/modello-dati.md`, `wiki/index.md`, `wiki/glossario.md`, `wiki/stato.md`, `wiki/log.md`

- [ ] **Step 1: ADR 0008** (frontmatter e stile come le altre decisioni; leggi `wiki/CLAUDE.md`): contesto, approcci A/B1/B2 e perché B1, regole di scadenza, push generiche e perché, limiti (una sola istanza del backend, niente email/iOS), come attivare le chiavi VAPID.
- [ ] **Step 2: Pagine esistenti.** In 0007 sostituisci la riga "notifiche/email di promemoria … fuori scope" con un rimando alla 0008 (solo le email restano fuori); `api-contracts.md`: campi nuovi di `/agenda` e sezione Notifiche; `modello-dati.md`: colonne nuove e tabelle `notifiche` e `push_subscriptions`; `index.md` e `glossario.md` (notifica, push, scatto).
- [ ] **Step 3: Handoff.** Aggiorna `wiki/stato.md` e aggiungi a `wiki/log.md` la voce `## [2026-11-16] handoff | Notifiche dell'agenda`.

---

## Auto-revisione

- **Copertura della spec:** campi (T1); tabelle, regole, scheduler, API (T2); push, VAPID, payload generico, pulizia iscrizioni (T3); checkbox e anticipo, visibilità della sezione (T4); campanello, pulse, puntino sidebar, clic sulla notifica, polling (T5); service worker, permesso, Windows/macOS, logout (T6); documentazione (T7). Gli approcci e i non-obiettivi non richiedono task.
- **Coerenza dei nomi:** `notifica`/`anticipoNotificaMinuti` (T1) → T4; `NotificheResponse`/`NotificaResponse` (T2) → T5; endpoint push (T3) → T6; `riallinea` (T2) usato da `AgendaService`.
- **Nota:** la regola di scatto per i promemoria con ora già passata ma creati tardi segue la spec (notifica solo se l'evento non è ancora iniziato).
