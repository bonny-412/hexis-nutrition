# Design — frontend-professionisti, fase "Fondamenta" (login, sessione, shell, pazienti)

Data: 2026-08-30. Stato: approvato da Andrea in chat, in attesa di piano di implementazione (`superpowers:writing-plans`).

## Contesto

Il backend del sotto-progetto "Fondamenta" è completo, testato (36 test, vedi [wiki/stato.md](../../../wiki/stato.md)) e mai consumato da un frontend reale. `frontend-professionisti/` contiene solo il `CLAUDE.md` placeholder. Andrea ha fornito un set di mockup (Claude Design canvas) per Login, Dashboard Professionisti, Pazienti, Agenda, Alimenti — con le decisioni di stile già fissate (sidebar verde scura, Fraunces/Figtree, token CSS, vedi il file di note incluso nell'export dei mockup).

**Mismatch mockup ↔ backend**: i mockup di Dashboard e Pazienti sono disegnati per lo stato futuro del prodotto — mostrano dati di Agenda (visite), Piano alimentare (piani/aderenza) e Chat (messaggi) che non esistono ancora lato backend. Solo il mockup di Login è immediatamente implementabile 1:1. Questo design scopa la prima fase del frontend professionisti a **ciò che il backend supporta oggi**: autenticazione e anagrafica pazienti. Il resto della UI (Agenda, Piani alimentari, Chat, Analytics) resta visibile in navigazione come riferimento verso il roadmap, ma non funzionante, finché i rispettivi sotto-progetti non vengono implementati.

## Obiettivo di questa fase

Un professionista può: accedere, vedere la propria sessione ripristinata dopo un refresh di pagina, recuperare la password se dimenticata, vedere una dashboard con la shell di navigazione definitiva, gestire l'anagrafica dei propri pazienti (creare, elencare, vedere il dettaglio, invitarli).

## Architettura frontend

### Stack e tooling

- Vite + Vue 3 + TypeScript (i tipi sui DTO che arrivano dal backend Java prevengono una classe di bug di integrazione — campo rinominato, null non gestito — prima che arrivino a runtime).
- `vue-router` per il routing, Pinia **solo** per lo stato di sessione.
- Tailwind + shadcn-vue, coerenti con [decisioni/0001](../../../wiki/decisioni/0001-stack-tecnologico.md).
- ESLint + Prettier con configurazione minima (niente regole esotiche): l'obiettivo è codice chiaro, non conformità a uno stile complesso.
- Vitest + Vue Test Utils per i test.

### Struttura cartelle

Piatta, senza livelli architetturali prematuri:

```
frontend-professionisti/
  src/
    api/          client.ts, auth.ts, pazienti.ts
    stores/       auth.ts
    router/       index.ts
    views/        LoginView.vue, PasswordDimenticataView.vue, ResetPasswordView.vue,
                  DashboardView.vue, PazientiListView.vue, PazienteNuovoView.vue, PazienteDettaglioView.vue
    components/   AppShell.vue, AppSidebar.vue, AppHeader.vue, (componenti shadcn-vue generati)
    App.vue, main.ts
```

Niente cartelle `domain/`, `infrastructure/`, `services/` finché la complessità reale non le giustifica.

### Client API

Un solo `api/client.ts`: wrapper minimo su `fetch` che aggiunge la base URL (da variabile d'ambiente Vite, `VITE_API_BASE_URL`), inietta l'header `Authorization: Bearer <token>` leggendo dallo store di sessione, e normalizza gli errori in un tipo comune `ApiError { status: number; messaggio: string }`. Ogni modulo di dominio (`api/auth.ts`, `api/pazienti.ts`) espone funzioni tipizzate che usano questo client e mappano richiesta/risposta sui DTO reali del backend (vedi sezione Pagine). Niente libreria di data-fetching esterna: le pagine chiamano direttamente queste funzioni dentro `onMounted`/gestori di evento e tengono loro stesse lo stato di caricamento/errore con `ref`. Si introduce qualcosa di più (TanStack Query o simile) solo se la casistica reale — liste paginate, invalidazione cache tra pagine — lo giustifica.

### Sessione e autenticazione

Store Pinia `auth`:

```ts
interface Sessione {
  token: string | null
  professionista: { id: string; nome: string; cognome: string; email: string; ruolo: string } | null
}
```

- **Login**: `POST /auth/login` → `{ token, ruolo }`. Il token si salva in `localStorage` se "Ricordami" è spuntato, altrimenti in `sessionStorage` — il token ha comunque una scadenza fissa lato server (`app.jwt.expiration-minutes`); "Ricordami" decide solo se la sessione sopravvive alla chiusura del browser, non allunga la scadenza.
- **Ripristino sessione**: all'avvio dell'app, se c'è un token in una delle due storage, lo store lo carica e chiama `GET /auth/me` (nuovo endpoint, vedi sotto) per validarlo e popolare `professionista`. Su 401, si pulisce lo storage e si resta su `/login`.
- **Logout**: pulisce lo store e entrambe le storage, redirect a `/login`.
- **Guardie di routing**: rotte con `meta: { requiresAuth: true }` controllano `auth.token` prima di ogni navigazione (`router.beforeEach`); se assente, redirect a `/login` con `redirect` query param per tornare alla pagina richiesta dopo il login. `/login` fa il percorso inverso: se già autenticato, redirect a `/`.

## Pagine di questa fase

### `/login`

Dal mockup `Hexis Login.dc.html`, integrato: submit chiama `authApi.login(email, password)`. Successo → popola lo store, redirect a `/` (o al `redirect` query param). Errore 401 → banner "Email o password non corrette" (già nel mockup). Il link "Password dimenticata?" naviga a `/password-dimenticata` con l'email eventualmente già digitata precompilata.

### `/password-dimenticata`

Form con un solo campo email, stile coerente con la login (stesso pannello verde a sinistra, stesso font). Submit chiama `POST /auth/password-dimenticata`, che risponde **sempre** 204: il messaggio mostrato è generico ("Se l'indirizzo esiste, riceverai un'email con le istruzioni") indipendentemente dall'esito reale, per non contraddire la scelta anti-enumerazione del backend.

### `/reset-password`

Legge il token dalla query string (`?token=...`, così com'è nel link dell'email). Form con nuova password + conferma (validazione lato client: minimo 8 caratteri, coerente con `@Size(min = 8, max = 72)` di `ResetPasswordRequest`). Submit chiama `POST /auth/reset-password`. Successo → redirect a `/login` con messaggio di conferma. Errore 400 (token non valido/scaduto/già usato) → messaggio con link per richiederne uno nuovo (`/password-dimenticata`).

### `/` — Dashboard

Shell di navigazione definitiva (`AppShell` + `AppSidebar` + `AppHeader`, dal mockup `Hexis Dashboard Professionisti.dc.html`): sidebar verde scura con logo, voci Dashboard / Agenda / Pazienti / Piani alimentari / Chat / Analytics / Alimenti (sezione Risorse); header con ricerca (solo UI, non funzionante), chip profilo con nome da `auth.professionista`, voce menu "Esci dall'account" che fa logout.

Solo **Dashboard** e **Pazienti** sono voci cliccabili; le altre sono visibili ma disabilitate (stile attenuato, cursore non-interattivo, senza tooltip "in arrivo" per non promettere una data). Le card della dashboard:
- "Pazienti attivi": dato reale, contando lato frontend i risultati di `GET /pazienti` con `statoAccount === 'ATTIVO'` (nessun endpoint di conteggio dedicato: la lista è già scaricata per la pagina Pazienti, e il numero di pazienti previsto in questa fase è basso — un endpoint di statistiche è prematuro).
- "Visite oggi", "Piani in scadenza", "Messaggi non letti", "Aderenza media": placeholder con testo "Disponibile a breve" — dipendono da moduli non ancora costruiti (Agenda, Piano alimentare, Chat). Nessun dato finto o mock.

### `/pazienti`

Tabella essenziale, non quella ricca del mockup: colonne Nome, Cognome, Email, Stato account (badge `MAI_INVITATO` / `INVITATO` / `ATTIVO`), azione per riga (`Invita` se `MAI_INVITATO`, `Reinvia invito` se `INVITATO`, nulla se `ATTIVO`) che chiama `POST /pazienti/{id}/invito`. Pulsante "Nuovo paziente" verso `/pazienti/nuovo`. Nessun filtro per obiettivo/piano (non esistono ancora quei dati) — solo una ricerca testuale lato client su nome/cognome/email, dato che tutta la lista arriva in un'unica `GET /pazienti` senza paginazione server-side.

### `/pazienti/nuovo`

Form che rispecchia `CreaPazienteRequest`: nome, cognome, email (obbligatori), telefono, data di nascita, sesso, altezza in cm (opzionali). Submit → `POST /pazienti`, successo → redirect a `/pazienti/{id}` del paziente appena creato.

### `/pazienti/:id`

Dettaglio essenziale: dati anagrafici da `GET /pazienti/{id}`, stato account, pulsante invita/reinvia coerente con la lista. Nessuna sezione piano alimentare/misurazioni (non esistono ancora).

## Modifiche al backend

Tre aggiunte mirate; login, password-dimenticata, reset-password e il CRUD pazienti sono già completi e testati.

### CORS

Non esiste nessuna configurazione CORS oggi: `SecurityConfig` non chiama `.cors(...)` sulla `HttpSecurity`, quindi il browser bloccherebbe qualunque chiamata da un'origine diversa da quella del backend. Si aggiunge un bean `CorsConfigurationSource` che permette richieste dall'origine del frontend (property `app.frontend-professionisti.url`, default `http://localhost:5173` in dev — la porta di default di Vite), metodi GET/POST, header `Authorization` e `Content-Type`. Si abilita con `.cors(Customizer.withDefaults())` nella filter chain.

### `GET /auth/me`

Nuovo endpoint, autenticato (qualunque ruolo — riutilizzabile in futuro da `frontend-cliente`). Legge principal e ruolo dal contesto di sicurezza (stesso meccanismo di `@AuthenticationPrincipal` già usato in `PazienteController`), carica l'entità corrispondente (`ProfessionistaRepository` se `ROLE_PROFESSIONISTA`, `PazienteRepository` se `ROLE_PAZIENTE`) e restituisce:

```json
{ "id": "uuid", "nome": "Anna", "cognome": "Bianchi", "email": "...", "ruolo": "PROFESSIONISTA" }
```

401 se il token manca o non è valido (già gestito dal filtro JWT esistente + `authenticationEntryPoint`). Serve al chip profilo nell'header e a ripristinare la sessione dopo un refresh di pagina.

### URL del link di reset password configurabile

`AuthService.corpoResetPassword` costruisce il link con un placeholder hardcoded (`https://app.hexisnutrition.example/...`). Si sposta su una property (`app.frontend-professionisti.url`). La pagina `/reset-password` è pubblica e agnostica rispetto al ruolo (riceve solo token + nuova password, senza richiedere una sessione) — funziona quindi sia per il link inviato a un professionista sia per quello inviato a un paziente, pur ospitata in `frontend-professionisti`. Necessario ora che il frontend esiste davvero e il link deve portare a una pagina che funziona.

Il link di **attivazione invito** (`PazienteService.invita`, endpoint `/inviti/{token}/attiva`) resta invece sul placeholder: è specifico per il paziente che sceglie la password iniziale e appartiene naturalmente a `frontend-cliente`, non ancora costruito. Fissarlo ora punterebbe a un'app inesistente — si sistema quando `frontend-cliente` viene avviato.

## Gestione errori

Un solo pattern in tutta l'app: ogni chiamata di `api/*` che fallisce lancia `ApiError`; ogni vista la intercetta localmente (try/catch nel gestore di submit o in `onMounted`) e mostra il messaggio nel punto pertinente della UI (banner nel form, stato vuoto nella lista). Un 401 su **qualunque** chiamata (non solo `/auth/me`) fa scattare il logout automatico dello store e il redirect a `/login` — gestito centralmente nel client API, non ripetuto in ogni vista.

## Testing

- **Backend**: TDD come per il resto del progetto. Nuovi test per `GET /auth/me` (professionista trovato, 401 senza token) e, se necessario, un test che verifica la presenza dell'header CORS sulla risposta a una richiesta preflight.
- **Frontend**: Vitest + Vue Test Utils sulla logica non banale — store `auth` (login/logout/ripristino sessione, gestione remember-me), validazione dei form (email, password, campi obbligatori), mapping degli errori API nelle viste. Niente test end-to-end (Playwright/Cypress) in questa fase: prematuri con una sola pagina reale, si aggiungono quando la superficie da coprire end-to-end lo giustifica.

## Esplicitamente fuori scope (rimandato)

- Tabella Pazienti ricca (obiettivo, piano attivo, aderenza, filtri avanzati, paginazione server-side, import/export CSV) — dipende da "Piano alimentare"/"Monitoraggio".
- Dashboard con dati reali su visite/piani/messaggi/aderenza — idem.
- Agenda, Alimenti/Piano alimentare, Chat, Analytics come pagine funzionanti.
- Dark mode (decisa nei mockup ma non necessaria per validare il flusso Fondamenta).
- Breakpoint mobile: l'app è desktop-first per un professionista in studio, coerente con l'impostazione a sidebar dei mockup; non è stato richiesto supporto mobile per questa fase.
- Test end-to-end.

## Ordine di implementazione consigliato

1. Scaffold del progetto (Vite+Vue3+TS+Router+Pinia+Tailwind+shadcn-vue), lint/format, struttura cartelle.
2. Backend: CORS, `GET /auth/me`, URL email configurabili (piccola modifica, TDD).
3. Client API + store `auth` + guardie di routing (infrastruttura, senza UI ancora rifinita).
4. Pagina Login, integrata.
5. Pagine password dimenticata / reset password.
6. Shell dell'app (sidebar+header) + Dashboard con placeholder.
7. Pazienti: lista, crea, dettaglio, invito.

Il piano di implementazione dettagliato (task singoli, TDD, criteri di completamento) va scritto con `superpowers:writing-plans` a partire da questo ordine.
