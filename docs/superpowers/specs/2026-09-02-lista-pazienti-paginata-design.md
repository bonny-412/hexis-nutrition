# Design — Lista pazienti: redesign UI, paginazione e filtri reali

Data: 2026-09-02
Percorso: architetturale (brainstorming → questo spec → piano di implementazione)
Fonti: mockup `Hexis Pazienti.dc.html` fornito da Andrea in `Hexis Nutrition.zip` (non versionato, materiale grezzo esterno al repo).

## Obiettivo

Riprendere la struttura visiva del mockup per `PazientiListView.vue` (oggi una tabella minimale: nome, email, stato, azione invito), mantenendo l'identità grafica (ricerca, chip di stato, filtri avanzati, tabella densa, azioni riga) ma **senza inventare dati che non esistono**: il mockup mostra colonne che presuppongono moduli non ancora costruiti (Piano alimentare, Agenda/visite — obiettivo nutrizionale, stato "In target/A rischio", % aderenza, piano assegnato, ultima/prossima visita). Questo giro sostituisce quelle colonne con dati reali dell'anagrafica paziente già disponibile.

In corso di brainstorming, Andrea ha chiesto **paginazione reale** invece che client-side ("i pazienti potrebbero essere n"), il che richiede spostare anche ricerca e filtri lato server — altrimenti pagina e conteggi risulterebbero sbagliati filtrando solo sulla pagina già caricata. Per questo il lavoro tocca anche il backend, non solo la view.

Esplicitamente **fuori scope** per questa sessione (decisioni prese in brainstorming): selezione multipla/azioni di massa, command palette globale (⌘K).

## Stato attuale (per contesto)

- `PazientiListView.vue`: carica **tutti** i pazienti del professionista con `GET /pazienti` (funzione `lista()` in `api/pazienti.ts`), filtra per nome/cognome/email lato client con un `computed`, tabella shadcn-vue semplice (Nome, Email, Stato, azione Invita/Reinvia invito).
- `DashboardView.vue` usa la **stessa** `lista()` per contare i pazienti con `statoAccount === 'ATTIVO'` — consumer da non rompere: la funzione/endpoint esistenti restano invariati, il nuovo comportamento paginato/filtrato vive in una funzione ed endpoint **separati**.
- `Paziente`/`PazienteResponse` (backend e frontend) espongono: `nome`, `cognome`, `codiceFiscale`, `email`, `telefono`, `dataNascita`, `sesso` (`M`/`F`/`ALTRO`), `lavoro`, `tipoLavoro` (`SEDENTARIO`/`POCO_ATTIVO`/`ATTIVO`/`MOLTO_ATTIVO`, nullable), `statoAccount` (`MAI_INVITATO`/`INVITATO`/`ATTIVO`). Nessun campo su piano alimentare, aderenza o visite è presente in questa risposta.
- `PazienteRepository` è un semplice `JpaRepository`, nessun supporto a query dinamiche o paginazione oggi.
- Utility `calcolaEta(dataNascita)` già esistente in `frontend-professionisti/src/utils/data.ts` (usata in `PazienteNuovoView.vue`).

## Decisioni prese in brainstorming

- **Niente selezione multipla / azioni di massa**: nessuna checkbox riga, nessuna barra di selezione — richiesto esplicitamente da Andrea.
- **Niente command palette (⌘K)**: rimandato a una sessione futura.
- **Colonne del mockup senza corrispondenza nei dati reali** (Obiettivo, Stato clinico In target/Revisione/A rischio, % aderenza, Piano alimentare, Ultima/prossima visita) sono **sostituite** da colonne su dati realmente disponibili (vedi sezione Frontend), non mostrate come placeholder vuoti.
- **Paginazione reale lato server**, non client-side: implica che anche ricerca testuale e filtri (stato account, sesso, intervallo data di nascita) girino lato server insieme a pagina/ordinamento, per risultati corretti a qualunque scala.
- **`tipoLavoro` resta visibile in tabella ma non è filtrabile** (richiesta esplicita di Andrea: rimosso dai parametri di filtro dopo la prima proposta).
- **Endpoint separato dal `GET /pazienti` esistente**, per non rompere `DashboardView.vue` che si aspetta la lista completa non paginata.
- **Archiviazione paziente**: nuovo flag `archiviato` (soft-delete logico, non fisico) su `Paziente`, azionabile dal menu riga. Un paziente archiviato è escluso di default dalla ricerca; esiste un modo per vederli e de-archiviarli (vedi sezioni Contratto API e Frontend).
- **"Nuova visita" rimandata**: la voce compare nel menu riga ma **disabilitata** ("Presto disponibile", stesso pattern già usato per Agenda/Chat in sidebar) — il flusso per aggiungere una visita a un paziente esistente non esiste ancora e non viene costruito in questo giro.

## Contratto API

Nuovo endpoint, **`GET /pazienti/ricerca`** (il `GET /pazienti` esistente resta invariato).

### Query param (tutti opzionali salvo dove indicato)

| Parametro | Tipo | Default | Note |
|---|---|---|---|
| `pagina` | int | `0` | 0-based |
| `dimensione` | int | `20` | |
| `ordinaPer` | `nome`\|`cognome`\|`dataNascita`\|`statoAccount` | `nome` | |
| `direzione` | `asc`\|`desc` | `asc` | |
| `ricerca` | string | — | match case-insensitive su nome, cognome, email, codice fiscale |
| `statoAccount` | `MAI_INVITATO`\|`INVITATO`\|`ATTIVO` | — | |
| `sesso` | `M`\|`F`\|`ALTRO` | — | |
| `dataNascitaDa` / `dataNascitaA` | `LocalDate` (ISO) | — | intervallo inclusivo |
| `archiviato` | boolean | `false` | `false`/assente → mostra i pazienti attivi (esclude gli archiviati); `true` → mostra **solo** gli archiviati |

Ogni filtro passato è combinato in `AND` con gli altri — nessuna logica OR tra filtri diversi. Ogni cambio di filtro/ricerca/ordinamento lato frontend riporta `pagina` a `0`.

### Risposta — nuovo record `PazienteListaPaginataResponse`

```json
{
  "contenuto": [ /* PazienteResponse[], stesso shape di oggi + campo archiviato */ ],
  "paginaCorrente": 0,
  "dimensionePagina": 20,
  "totaleElementi": 142,
  "totalePagine": 8
}
```

### Nuovi endpoint azione (stesso pattern imperativo di `POST /pazienti/{id}/invito`)

- **`POST /pazienti/{id}/archivia`** → `204`, imposta `archiviato = true`. Idempotente (richiamarlo su un paziente già archiviato non è un errore).
- **`POST /pazienti/{id}/de-archivia`** → `204`, imposta `archiviato = false`. Idempotente.
- `PazienteResponse` guadagna il campo `archiviato: boolean`.
- Regola difensiva in `PazienteService.invita(...)`: se `paziente.archiviato == true`, lancia un errore esplicito (400) invece di procedere — non ha senso invitare un paziente archiviato.

## Backend

- `PazienteRepository` guadagna `extends JpaSpecificationExecutor<Paziente>`.
- Nuova classe `PazienteSpecifications` con un metodo statico per ciascun filtro (`delProfessionista`, `conRicerca`, `conStatoAccount`, `conSesso`, `conDataNascitaTra`, `conArchiviato`), combinati con `Specification.allOf(...)`. `delProfessionista` e `conArchiviato` sono **sempre** applicati (isolamento multi-professionista, coerente con `findAllByProfessionistaId` esistente, e default "solo attivi").
- Nuovo metodo `PazienteService.cerca(UUID professionistaId, CriteriRicercaPazienti criteri, Pageable pageable)` → restituisce `Page<Paziente>`, mappato a `PazienteListaPaginataResponse` nel controller.
- `CriteriRicercaPazienti`: record con i filtri opzionali (`ricerca`, `statoAccount`, `sesso`, `dataNascitaDa`, `dataNascitaA`) più `archiviato` (boolean, non opzionale, risolto dal controller a `false` se il parametro query non è presente).
- Nuovi metodi `PazienteService.archivia(UUID professionistaId, UUID pazienteId)` e `deArchivia(...)`: recuperano il paziente con lo stesso controllo di ownership già usato in `dettaglio(...)`, impostano il flag, salvano.
- `PazienteController`: nuovo metodo `ricerca(...)` su `GET /pazienti/ricerca`, costruisce `Pageable` da `pagina`/`dimensione`/`ordinaPer`/`direzione` (whitelist esplicita dei campi ordinabili — non un `Sort` libero da input utente, per evitare di esporre nomi di colonna arbitrari); più `POST /pazienti/{id}/archivia` e `POST /pazienti/{id}/de-archivia`.
- Migrazione: `ALTER TABLE pazienti ADD COLUMN archiviato BOOLEAN NOT NULL DEFAULT false;` — sicura, nessun dato reale da preservare in `hexis` (da riverificare come per le migrazioni precedenti).

## Frontend

### Colonne tabella (nessuna checkbox)

| Colonna | Contenuto |
|---|---|
| Paziente | avatar iniziali + nome cognome (font Fraunces, come mockup) + riga meta "email · codice fiscale" |
| Contatto | telefono, o "—" se assente |
| Attività | `lavoro` (o "—") + badge con `tipoLavoro` leggibile se presente (non filtrabile) |
| Età | calcolata da `dataNascita` con `calcolaEta` |
| Stato account | badge colorato: Mai invitato (grigio) / Invitato (ambra) / Attivo (verde-mint) |
| Azioni | icona "Apri cartella" (→ `/pazienti/:id`) + bottone "Invita"/"Reinvia invito" se `statoAccount !== 'ATTIVO'` (stessa logica di `etichettaAzione` esistente, nascosto se il paziente è archiviato) + menu ⋯ (vedi sotto) |

Header ordinabili (click cambia `ordinaPer`/`direzione`, ricarica): Paziente (nome), Età, Stato account.

### Ricerca, chip, filtri avanzati

- Input ricerca con debounce 300ms, placeholder "Filtra per nome, email o codice fiscale…".
- 4 chip di stato, mappati 1:1 sull'enum reale: **Tutti / Mai invitato / Invitato / Attivo**.
- Pannello "Filtri avanzati" (comparsa, badge "N attivi" come nel mockup): Sesso (select), Data di nascita da/a (due date), **toggle "Mostra pazienti archiviati"** (checkbox — quando attivo, la ricerca mostra *solo* gli archiviati, non un mix con gli attivi). Bottone "Pulisci filtri" resetta ricerca + chip + avanzati (incluso il toggle archiviati) + torna a pagina 0.

### Menu azioni riga (⋯)

`DropdownMenu` di shadcn-vue (già usato nel progetto per il menu profilo in `AppHeader.vue`), aperto dal bottone "⋯" per riga:

- **"Nuova visita"** — voce **disabilitata**, etichetta secondaria "Presto disponibile" (stesso pattern delle voci disabilitate in `AppSidebar.vue`). Il flusso non esiste ancora, non costruito in questo giro.
- **"Archivia paziente"** (vista normale) o **"De-archivia paziente"** (quando il toggle "Mostra pazienti archiviati" è attivo) — solo "Archivia" ha lo stile "pericolo" (colore `--risk-fg`, come nel mockup); "De-archivia" ha stile neutro. Entrambe richiedono **conferma** prima di eseguire, tramite un `AlertDialog` di shadcn-vue (componente non ancora presente nel progetto: va aggiunto con `npx shadcn-vue add alert-dialog`).
- In vista "pazienti archiviati", il bottone "Invita/Reinvia invito" non compare (non ha senso su un paziente archiviato).

### Paginazione

Barra in fondo: conteggio reale ("Mostrati X–Y di N pazienti"), bottoni Precedente/Successivo basati su `paginaCorrente`/`totalePagine` (disabilitati agli estremi).

### API client (`api/pazienti.ts`)

- Nuova funzione `cerca(criteri: CriteriRicercaPazienti): Promise<PaginaPazienti>` che costruisce la query string e chiama `GET /pazienti/ricerca`. `lista()` esistente **non modificata**.
- Nuove funzioni `archivia(id): Promise<void>` e `deArchivia(id): Promise<void>`.
- `Paziente` guadagna il campo `archiviato: boolean`.

### Stati

- **Caricamento iniziale**: skeleton a righe animate (stile mockup).
- **Ricaricamenti successivi** (pagina/filtro/ricerca/ordinamento): nessun flash a piena tabella — la tabella resta visibile con lieve dimming/disabilitazione controlli finché arriva la risposta.
- **Errore di caricamento**: sostituisce il corpo tabella, messaggio "Non è stato possibile caricare i pazienti." + bottone "Riprova" che ripete l'ultima richiesta con gli stessi filtri.
- **Vuoto, primo paziente** (0 risultati, nessun filtro/ricerca attivo): stato illustrato stile mockup, CTA "Nuovo paziente".
- **Vuoto, per filtro** (0 risultati con filtro/ricerca attivi): stesso stile, CTA "Pulisci filtri".
- **Errore invito**: comportamento esistente preservato (messaggio sopra la tabella se `invita()` fallisce).

## Fuori scope (esplicito)

- Selezione multipla, azioni di massa (esporta CSV, invito multiplo).
- Command palette globale (⌘K).
- Flusso "Nuova visita" per paziente esistente (voce di menu presente ma disabilitata).
- Qualunque colonna/filtro legato a Piano alimentare, Agenda/visite, aderenza — arriveranno con i rispettivi sotto-progetti.
- Tema scuro / toggle tema (feature di shell applicativa, non di questa view).
- Colmare il gap noto di validazione server-side su `nome`/`cognome`/`telefono`/`lavoro` (in [domande-aperte](../../wiki/domande-aperte.md)) — non toccato da questo lavoro.

## Testing (indicazioni per il piano)

**Backend**
- `PazienteSpecifications`/repository: ogni filtro isolato (ricerca su nome/cognome/email/CF, `statoAccount`, `sesso`, intervallo `dataNascita`, `archiviato`), combinazione di più filtri insieme, isolamento per `professionistaId`, ordinamento asc/desc su ciascun campo ordinabile, paginazione (conteggi corretti, pagina oltre l'ultima → contenuto vuoto senza errore).
- `PazienteControllerTest`: `GET /pazienti/ricerca` con parametri default (esclude archiviati), con `archiviato=true` (mostra solo archiviati), con ogni altro filtro passato singolarmente, 401 se non autenticato.
- `PazienteServiceTest`/`PazienteControllerTest`: `archivia`/`de-archivia` (idempotenza, isolamento per professionista — non si può archiviare un paziente di un altro professionista), `invita` su paziente archiviato → 400.

**Frontend**
- `api/pazienti.spec.ts`: nuova funzione `cerca()` — query string costruita correttamente (incluso `archiviato`), parsing della risposta paginata; `archivia()`/`deArchivia()`.
- `PazientiListView.spec.ts` (riscritto): colonne con dati reali, cambio pagina, ricerca con debounce, chip di stato, filtri avanzati (sesso, intervallo date, toggle archiviati), pulisci filtri, ordinamento da header, entrambi gli stati vuoti, stato di errore + riprova, invita/reinvia invito (comportamento esistente, nascosto se archiviato), menu riga (voce "Nuova visita" disabilitata, conferma prima di archiviare/de-archiviare, azione eseguita dopo conferma).

Aggiornare `wiki/api-contracts.md` (nuovo endpoint) nello stesso passaggio dell'implementazione, come richiesto da `backend/CLAUDE.md`.
