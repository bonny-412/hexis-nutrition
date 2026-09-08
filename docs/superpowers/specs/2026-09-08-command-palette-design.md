# Design — Command palette globale (⌘K)

Data: 2026-09-08
Percorso: architetturale (brainstorming → questo spec → piano di implementazione)
Fonti: mockup `Hexis Dashboard Professionisti.dc.html` fornito da Andrea in `Hexis Nutrition.zip` (non versionato, materiale grezzo esterno al repo) — bottone header "Cerca ovunque (⌘K)" e pannello palette con gruppi "Azioni"/"Pazienti".
Nota: rimandata esplicitamente da [2026-09-02-lista-pazienti-paginata-design.md](2026-09-02-lista-pazienti-paginata-design.md) ("Niente command palette (⌘K): rimandato a una sessione futura") — questa è quella sessione.

## Obiettivo

Aggiungere un punto d'accesso rapido, globale, da tastiera (Ctrl/Cmd+K) o da bottone in header, per: eseguire le azioni di creazione più comuni e cercare un paziente per nome senza dover prima navigare alla lista Pazienti. Solo per `frontend-professionisti` (non `frontend-cliente`).

Esplicitamente **fuori scope** per questo giro (deciso in brainstorming):
- Ricerca alimenti nella palette — una volta inserito un alimento raramente serve ricercarlo; resta solo l'azione "Nuovo alimento".
- Navigazione con tastiera tra i risultati (frecce/invio) — solo click/mouse, come primo taglio.
- Ricerca di "piani alimentari" o altre entità non ancora esistenti nel prodotto.

## Stato attuale (per contesto)

- `AppHeader.vue`: nessuna ricerca globale oggi; header con menu mobile, campanella notifiche, menu profilo.
- Dashboard (`DashboardView.vue`) ha già un menu "Crea nuovo" (`DropdownMenu`) con 4 voci: **Nuovo paziente** (`/pazienti/nuovo`), **Nuova visita** (`/pazienti/visite/nuova`), **Nuovo appuntamento** e **Nuovo piano alimentare** (entrambe disabilitate, funzionalità non ancora costruite).
- `api/pazienti.ts` espone già `cerca(criteri): Promise<PaginaPazienti>` (usata da `PazientiListView.vue`), con supporto a `ricerca`, `dimensione`, `archiviato`.
- Gli alimenti non hanno una route di dettaglio propria: si creano/modificano tramite `AlimentoFormDialog.vue`, aperto da `AlimentiListView.vue` (`apriCreazione()` / `apriDettaglio()`), mai navigando a un URL diverso da `/alimenti`.
- `PazientiListView.vue` non legge oggi alcun parametro dalla query string all'avvio.

## Decisioni prese in brainstorming

- Solo `frontend-professionisti`.
- Le "Azioni" mostrate di default (palette vuota) sono le **stesse 4 voci** del menu "Crea nuovo" della dashboard, compreso lo stato disabilitato di "Nuovo appuntamento" e "Nuovo piano alimentare" — più una quinta, **Nuovo alimento**, assente dal menu dashboard ma richiesta qui.
- Niente ricerca alimenti: solo l'azione di creazione.
- Ricerca pazienti: primi **5** risultati, **solo pazienti non archiviati**, con riga finale "Vedi tutti i risultati" se ce ne sono di più.
- L'azione "Nuovo alimento" naviga su `/alimenti` **aprendo automaticamente** il dialog di creazione (non lascia solo la lista, richiede l'utente clicchi di nuovo).
- Il pannello della palette **non** riusa il `Dialog` condiviso di shadcn-vue (il suo `DialogContent.vue` blocca esplicitamente la chiusura al click esterno, comportamento sbagliato per una palette) — overlay dedicato, costruito da zero nel nuovo componente.

## Frontend

### Nuovo componente `CommandPalette.vue`

Componente autonomo (`src/components/CommandPalette.vue`), montato una sola volta in `AppHeader.vue` come `<CommandPalette />`, senza props né emit: possiede tutto il proprio stato (apertura, testo digitato, risultati) e non necessita di coordinamento con il genitore.

**Trigger**: bottone nell'header, stile mockup (icona lente, testo "Cerca ovunque", `<kbd>` con la scorciatoia), apre la palette al click.

**Scorciatoie da tastiera**: un listener `keydown` globale (montato/smontato con `onMounted`/`onUnmounted`, come già fatto altrove nel progetto per pattern simili):
- `Ctrl+K` (Windows/Linux) o `Cmd+K` (Mac, `metaKey`) → `preventDefault()` + toggle apertura, azzera il testo digitato.
- `Escape` → chiude, solo se aperta.

**Overlay**: backdrop a tutto schermo (semi-trasparente, blur leggero come da mockup) che chiude al click; pannello centrato in alto (non al centro verticale) con `@click.stop` per non propagare il click al backdrop. Colori/bordi/radius coerenti con il resto dell'app (`--surf`, `--bd`, `--div`, ecc.), niente nuovi token.

**Contenuto del pannello**:
1. Campo di input in cima (autofocus all'apertura), icona lente, placeholder "Cerca pazienti o un'azione…".
2. Gruppo **Azioni**: lista statica di 5 voci (Nuovo paziente, Nuova visita, Nuovo appuntamento [disabilitata], Nuovo piano alimentare [disabilitata], Nuovo alimento), filtrata client-side per sotto-stringa case-insensitive sul testo digitato. Sempre visibile (anche a query vuota).
3. Gruppo **Pazienti**: visibile solo con testo non vuoto. Chiamata debounced (300ms, stesso pattern di `PazientiListView.vue`) a `cerca({ ricerca: testo, dimensione: 5, archiviato: false })`. Righe con iniziali + nome cognome, come nella lista pazienti. Se `totaleElementi > 5`, riga finale "Vedi tutti i risultati (N)".
4. Stato vuoto: se il testo non è vuoto e sia le azioni filtrate sia i pazienti trovati sono zero → "Nessun risultato per «testo»".

**Selezione di una voce** (click): chiude la palette (`aperta = false`, reset testo) e poi naviga:
- Azione abilitata → `router.push` alla sua rotta (`Nuovo alimento` → `/alimenti?azione=nuovo`).
- Azione disabilitata → nessuna interazione (stile e `disabled`/`aria-disabled` coerenti col menu dashboard).
- Paziente → `/pazienti/:id`.
- "Vedi tutti i risultati" → `/pazienti?ricerca=<testo>`.

### Modifiche puntuali ad altre viste

- **`AppHeader.vue`**: aggiunge `<CommandPalette />` accanto agli elementi esistenti (a sinistra della campanella, come nel mockup).
- **`AlimentiListView.vue`**: al mount, se `route.query.azione === 'nuovo'`, chiama `apriCreazione()` per aprire subito il dialog di creazione. Nessun'altra modifica.
- **`PazientiListView.vue`**: al mount, se `route.query.ricerca` è presente (stringa), precompila `ricercaInput` con quel valore — il debounce/watch esistente si occupa del resto (nessuna nuova logica di ricerca). Nessun'altra modifica.

Nessuna modifica al backend: si riusa integralmente l'endpoint `GET /pazienti/ricerca` già esistente.

## Fuori scope (esplicito)

- Ricerca alimenti nella palette.
- Navigazione da tastiera tra i risultati (solo mouse/click in questo giro).
- Ricerca di entità non ancora esistenti (piani alimentari, appuntamenti).
- `frontend-cliente`.

## Testing

Da concordare con Andrea prima di scrivere qualunque test: potrebbe scriverli lui stesso. Il piano di implementazione non deve includere step di test se non dopo conferma esplicita.
