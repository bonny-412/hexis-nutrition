# Design — Piano alimentare (editor + lista)

Data: 2026-09-12
Percorso: architetturale (brainstorming → questo spec → piano di implementazione)
Fonti: mockup `Hexis Piano Alimentare.dc.html` e `Hexis Piani Alimentari.dc.html` (in `Hexis Nutrition.zip`, materiale grezzo esterno al repo), `specifica-tdee.docx` (documento clinico fornito da Andrea per il calcolo del suggerimento calorico).

## Obiettivo

Primo pezzo strutturale del sotto-progetto "Piano alimentare" dopo il catalogo Alimenti (vedi [decisioni/0005](../../wiki/decisioni/0005-alimenti-bda-e-custom.md)): modello dati, calcolo del suggerimento calorico (TDEE) e le due viste principali — l'editor di un piano (creazione + modifica) e la lista dei piani. Riprende la struttura del mockup fornito da Andrea, incluse tutte e tre le modalità di piano (decisione presa in brainstorming, vedi sotto), risolvendo anche la domanda aperta su snapshot vs riferimento a runtime dei valori nutrizionali (vedi [domande-aperte](../../wiki/domande-aperte.md)).

**Ordine di implementazione** (riflesso nel piano che seguirà): backend (modello dati + calcolo TDEE) → editor, partendo dalla modalità "Piano con pasti" → poi "Solo target macro" ed "Esempi intercambiabili" → infine la lista.

## Decisioni prese in brainstorming

- **Tutte e tre le modalità** (Piano con pasti / Solo target macro / Esempi intercambiabili) in questa prima iterazione — scelta esplicita di Andrea, non decomposta in iterazioni separate.
- **Modalità fissata alla creazione**, non cambiabile dopo (strutture dati troppo diverse tra le tre).
- **Snapshot dei valori nutrizionali**: quando un alimento viene aggiunto a un piano, i valori per 100g vengono copiati al momento dell'inserimento. Un piano storico resta stabile anche se l'alimento sorgente viene modificato o eliminato dopo (stessa scelta già fatta da `ProgettoEdo`).
- **Un solo piano `ATTIVO` alla volta per paziente**: attivarne uno nuovo disattiva automaticamente quello precedente (→ `TERMINATO`).
- **Stati**: `BOZZA` → `ATTIVO` (azione esplicita) → `SCADUTO` (calcolato, non persistito come transizione: `ATTIVO` con `data_fine` passata) oppure `TERMINATO` (chiuso anticipatamente da un nuovo piano attivato). Niente stato "Inviato" separato per ora: `frontend-cliente/` non esiste, quindi "Attivo" e "Inviato" coincidono — se in futuro `frontend-cliente/` renderà il piano visibile al paziente, valutare se serve uno stato dedicato (nota lasciata in [domande-aperte](../../wiki/domande-aperte.md)).
- **Tre punti di ingresso** per la creazione: lista Piani alimentari (paziente da scegliere), scheda paziente, dashboard (voce "Nuovo piano alimentare", oggi disabilitata in entrambe — va abilitata).
- **Suggerimento calorico (TDEE)**: segue `specifica-tdee.docx` con queste decisioni di prodotto fissate su richiesta di Andrea (diverse da quanto proposto inizialmente nel documento):
  - **Nessun aggiustamento percentuale per nessun obiettivo**: sempre 0%, il valore mostrato è sempre il **TDEE puro**, per tutti gli obiettivi non esclusi (inclusi Dimagrimento e Aumento peso, per cui il documento proponeva rispettivamente −20%/+15%).
  - Obiettivo **Educativo**: mostra comunque il TDEE calcolato come riferimento informativo.
  - **Patologia clinica** e **Gravidanza/allattamento** restano **esclusi** dal calcolo (nessun numero mostrato, campo libero) — unica eccezione rimasta rispetto a "mostra sempre il TDEE".
  - Sesso "Altro" nella formula di Mifflin-St Jeor: coefficiente −78 (media tra M e F), come proposto nel documento.
  - Fattore di attività di default se `stileDiVita` non compilato: 1.375 (leggermente attivo), come proposto nel documento.
  - Soglia di sicurezza minima (avviso non bloccante): 1200 kcal (F), 1500 kcal (M/Altro), come da documento.
- **Eliminazione piano**: permessa solo per piani in `BOZZA` (409 altrimenti) — un piano attivato fa parte dello storico clinico, coerente con l'assenza di un "elimina paziente" (solo archiviazione).
- **Salvataggio esplicito**, nessun autosave: l'editor accumula modifiche in stato locale, `PUT` sostituisce sempre l'intera struttura figlia (stesso pattern già usato per `Visita`: cancella e ricrea, mai merge incrementale).

## Modello dati

Migrazioni Flyway a partire da `V21` (l'ultima esistente è `V20__alimenti_quantita_g.sql`).

### `piani_alimentari`

| Colonna | Tipo | Note |
|---|---|---|
| `id` | UUID PK | |
| `paziente_id` | UUID FK → `pazienti` | NOT NULL |
| `professionista_id` | UUID FK → `professionisti` | NOT NULL, denormalizzato dal paziente al momento della creazione — evita una `JOIN`/subquery su `pazienti` per isolare i piani per professionista in ogni ricerca, stessa esigenza già risolta da `alimenti.professionista_id` (qui però **con** vincolo FK: a differenza di `alimenti`, questa tabella va comunque svuotata ad ogni test insieme a `professionisti`, quindi il `TRUNCATE ... CASCADE` esistente non pone lo stesso problema) |
| `visita_id` | UUID FK → `visite` | nullable — la visita usata per il calcolo TDEE, per tracciabilità ("calcolato sulla visita del gg/mm/aaaa"); `null` se il paziente non aveva ancora nessuna visita alla creazione |
| `nome` | VARCHAR | NOT NULL |
| `modalita` | VARCHAR enum (`PASTI`/`MACRO`/`ESEMPI`) | NOT NULL, immutabile dopo la creazione |
| `stato` | VARCHAR enum (`BOZZA`/`ATTIVO`/`TERMINATO`) | NOT NULL — `SCADUTO` non è un valore persistito, si deriva da `ATTIVO` + `data_fine` passata |
| `data_inizio` | DATE | NOT NULL, default oggi alla creazione |
| `data_fine` | DATE | nullable |
| `obiettivo_kcal` | NUMERIC | il valore in vigore (sempre modificabile dal dietista); `null` solo per Patologia clinica/Gravidanza-allattamento finché non impostato a mano |
| `obiettivo_kcal_suggerito` | NUMERIC | nullable, snapshot del suggerimento calcolato alla creazione |
| `bmr_calcolato` | NUMERIC | nullable |
| `tdee_calcolato` | NUMERIC | nullable |
| `formula_bmr_usata` | VARCHAR enum (`KATCH_MCARDLE`/`MIFFLIN_ST_JEOR`) | nullable |
| `sotto_soglia_sicurezza` | BOOLEAN | NOT NULL default `false` — per mostrare l'avviso senza ricalcolare |
| `created_at`/`updated_at` | TIMESTAMPTZ | |

### `pasti` (solo `modalita = PASTI`)

`id`, `piano_id` FK, `giorno_settimana` (enum `LUNEDI`..`DOMENICA`), `nome`, `tipo` (enum `COLAZIONE`/`SPUNTINO_MATTINA`/`PRANZO`/`SPUNTINO_POMERIGGIO`/`CENA`/`ALTRO` — determina colore/icona in UI), `nota` (TEXT, nullable), `ordine` (INT).

### `piano_giorno_macro_target` (solo `modalita = MACRO`)

`id`, `piano_id` FK, `giorno_settimana`, `kcal_target`/`proteine_target`/`carboidrati_target`/`grassi_target` (NUMERIC, tutti nullable — il dietista può lasciare un giorno vuoto). Vincolo `UNIQUE(piano_id, giorno_settimana)`.

### `piano_esempi` (solo `modalita = ESEMPI`)

`id`, `piano_id` FK, `tipo_pasto` (stesso enum di `pasti.tipo`), `nome`, `ordine`.

### `piano_alimento_righe` (righe di alimenti, condivisa tra pasti ed esempi)

Invece di due tabelle quasi identiche (`pasto_alimenti`/`piano_esempio_alimenti`), una tabella unica con due FK nullable e un vincolo di esclusività — stesso pattern già usato da `TokenAzione` (collegato a `Professionista` **oppure** `Paziente`, mai entrambi, vincolo CHECK):

- `id`, `pasto_id` FK nullable, `esempio_id` FK nullable, `CHECK ((pasto_id IS NOT NULL) <> (esempio_id IS NOT NULL))`.
- `alimento_id` UUID **nullable, senza vincolo FK** verso `alimenti` — stessa scelta già presa per `alimenti.professionista_id` (evitare cascade/truncate nei test, vedi [decisioni/0005](../../wiki/decisioni/0005-alimenti-bda-e-custom.md)): serve solo come riferimento debole "da dove viene", `null` per un alimento manuale.
- Valori nutrizionali **copiati come istantanea** al momento dell'inserimento: `nome`, `kcal_100g`, `proteine_100g`, `carboidrati_100g`, `grassi_100g` (NOT NULL), `zuccheri_100g` (nullable, come in `Alimento`).
- `grammi` (NUMERIC, modificabile liberamente dopo l'inserimento), `ordine` (INT).

Nessuna di queste tabelle è seminata/di riferimento: vanno **aggiunte al `TRUNCATE`** di `AbstractIntegrationTest`, a differenza di `alimenti`/`durnin_womersley_coefficienti`.

## Calcolo del suggerimento TDEE

Nuovo servizio `CalcolatoreTdee` in `backend/.../pianialimentari/`, segue l'ordine di `specifica-tdee.docx` §6:

1. Se `Visita.obiettivo` è `PATOLOGIA_CLINICA` o `GRAVIDANZA_ALLATTAMENTO` → nessun calcolo, tutti i campi suggerimento restano `null`.
2. Età = anni completi tra `Paziente.dataNascita` e **`Visita.dataVisita`** (non la data odierna).
3. BMR: `370 + 21.6 × massaMagraKg` (Katch-McArdle) se `Visita.plicometria.massaMagraKg` è presente, altrimenti `10×peso + 6.25×altezza − 5×età + s` (Mifflin-St Jeor), con `s = +5` (M) / `−161` (F) / `−78` (Altro).
4. TDEE = BMR × fattore di attività da `Paziente.stileDiVita` (`SEDENTARIO`=1.2, `POCO_ATTIVO`=1.375, `ATTIVO`=1.55, `MOLTO_ATTIVO`=1.725, `ESTREMAMENTE_ATTIVO`=1.9 — nomi enum esistenti, vedi `StileDiVita.java`; `null` → 1.375 di default).
5. Calorie suggerite = TDEE (aggiustamento sempre 0%, per decisione di Andrea — vedi sopra).
6. Soglia di sicurezza: `sottoSogliaSicurezza = calorieSuggerite < (sesso == F ? 1200 : 1500)` — solo avviso, non blocca nulla.

Il calcolo gira **una sola volta, alla creazione del piano** (`POST /piani-alimentari`), aggancia l'ultima visita registrata del paziente in quel momento e persiste lo snapshot. Non esiste un endpoint di ricalcolo: una nuova valutazione richiede un nuovo piano (fase successiva), coerente con come il prodotto tratta già le fasi (vedi nome piano suggerito sotto). Se il paziente non ha nessuna visita, tutti i campi suggerimento restano `null` e `obiettivo_kcal` parte vuoto, modificabile a mano.

## Contratto API

Nuovo package `pianialimentari`, endpoint sotto `/piani-alimentari` (ruolo PROFESSIONISTA).

- **`POST /piani-alimentari`** — `{ pazienteId, nome, modalita }`. Aggancia l'ultima visita, calcola il suggerimento TDEE, crea il piano in `BOZZA` con la struttura figlia vuota (i primi 7 giorni × 5 pasti template per `PASTI`, i 7 giorni vuoti per `MACRO`, le 5 categorie vuote per `ESEMPI` — stessi template del mockup). Nome di default proposto lato frontend: `"{Obiettivo ultima visita} · fase {n}"`, contando i piani già esistenti del paziente con lo stesso obiettivo.
- **`GET /piani-alimentari/{id}`** — dettaglio completo per popolare l'editor.
- **`PUT /piani-alimentari/{id}`** — sostituisce nome, `dataFine`, `obiettivoKcal` (override manuale) e l'intera struttura figlia pertinente alla modalità (`pasti[]` con `righe[]`, oppure `giorniMacroTarget[]`, oppure `esempi[]` con `righe[]`) — cancella e ricrea, mai merge incrementale.
- **`POST /piani-alimentari/{id}/attiva`** — porta il piano in `ATTIVO`; se il paziente ha già un piano `ATTIVO`, quello passa a `TERMINATO` nella stessa transazione.
- **`DELETE /piani-alimentari/{id}`** — solo se `stato = BOZZA`, altrimenti 409.
- **`GET /piani-alimentari/ricerca`** — lista paginata (stesso pattern di `/alimenti/ricerca`/`/pazienti/ricerca`): `pagina`, `dimensione`, `ordinaPer`, `direzione`, `ricerca` (nome paziente o nome piano), `stato` (`BOZZA`/`ATTIVO`/`SCADUTO`/`TERMINATO` — `SCADUTO` calcolato anche qui, non da colonna), `pazienteId` opzionale (per un futuro storico in scheda paziente). Nuovi `CampoOrdinamentoPianiAlimentari`/`DirezioneOrdinamento` propri del package (stessa scelta già presa per `alimenti`, per non accoppiare i package tra loro).

Nessun nuovo endpoint per la ricerca alimenti nel modal "Aggiungi alimento": riusa `GET /alimenti/ricerca`, già esistente.

## Backend

- Entità: `PianoAlimentare` (aggregate root, `@OneToMany(cascade = ALL, orphanRemoval = true)` su pasti/target/esempi a seconda della modalità — in pratica solo una delle tre collezioni sarà mai popolata per un dato piano), `Pasto`, `PianoGiornoMacroTarget`, `PianoEsempio`, `PianoAlimentoRiga` (condivisa, con `pastoId`/`esempioId` nullable). Enum: `ModalitaPiano`, `StatoPiano`, `FormulaBmr`, `GiornoSettimana`, `TipoPasto`.
- `CalcolatoreTdee`: puro, testabile in isolamento (nessuna dipendenza da repository), input `Paziente` + `Visita` (con eventuale `Plicometria`) + `ObiettivoVisita`, output un record `SuggerimentoTdee` (o `null`/vuoto per gli obiettivi esclusi).
- `PianoAlimentareService`: `creaBozza(...)`, `aggiorna(id, request)` (sostituisce struttura figlia da zero, ricalcola nulla — i valori nutrizionali restano quelli snapshot), `attiva(id)` (transazionale: trova l'eventuale piano `ATTIVO` dello stesso paziente, lo porta a `TERMINATO`, poi attiva questo), `elimina(id)` (verifica `stato = BOZZA`), `cerca(...)`.
- `PianoAlimentareSpecifications`: isolamento per professionista (via `paziente.professionistaId`), ricerca testo, filtro stato (incluso `SCADUTO` calcolato — filtrare per `stato = ATTIVO AND dataFine < oggi`, non una colonna).
- `PianoAlimentareController`: i 6 endpoint sopra, stesso schema di autorizzazione/ownership già usato da `PazienteController`/`AlimentoController`.
- Migrazione: nuove tabelle come sopra, tutte aggiunte al `TRUNCATE` di `AbstractIntegrationTest`.

## Frontend

### Editor (`views/pianiAlimentari/PianoAlimentareFormView.vue`, nuova)

Pattern analogo a `VisitaFormView.vue`: tre modalità di ingresso — paziente noto (da scheda paziente/dashboard), paziente da scegliere (da lista, riusa `SelezionaPazienteCombobox.vue` esistente), modifica di un piano esistente. Rotte: `/piani-alimentari/nuovo` (+ variante con `pazienteId` in query), `/piani-alimentari/:id`.

Creazione: seleziona paziente (se serve) → scegli modalità (segmented control, tre opzioni) → nome precompilato, modificabile → `POST /piani-alimentari` crea la bozza e naviga all'editor.

Nell'editor (ricalca il mockup, **senza** il tab-switcher tra modalità): riquadro "Obiettivo giornaliero" (kcal editabile, con nota "Suggerito: X kcal (TDEE, {formula})" se disponibile, o messaggio esplicativo se obiettivo escluso; avviso soglia di sicurezza se `sottoSogliaSicurezza`) + "Fine piano" (data editabile) + bottone "Stampa PDF" (`window.print()` + CSS `@media print` dedicato sulla pagina).

Contenuto specifico per modalità:
- **PASTI**: sidebar 7 giorni (kcal totale per giorno, calcolato client-side dalle righe) + card macro del giorno selezionato (kcal/proteine/carboidrati/grassi + zuccheri) + lista pasti (nome, kcal totale, menu rinomina/elimina, tabella alimenti con grammi editabile e rimozione riga, nota testuale) + "Aggiungi pasto"/"Aggiungi alimento".
- **MACRO**: sidebar 7 giorni + campi target per il giorno selezionato (kcal/proteine/carboidrati/grassi) + "Applica a tutti i giorni".
- **ESEMPI**: 5 categorie (per `tipo_pasto`), ciascuna con le sue varianti/esempi, ciascuna con righe di alimenti.

Modal "Aggiungi alimento" (condiviso tra le modalità che lo usano): tab "Cerca dal database" (chiama `GET /alimenti/ricerca` con debounce) o "Alimento manuale" (nome + valori per 100g inseriti a mano, mai persistiti nel catalogo Alimenti — solo nella riga del piano).

Salvataggio esplicito (`PUT /piani-alimentari/{id}`). "Attiva piano" (visibile solo se `stato = BOZZA`): salva prima, poi chiama `POST .../attiva`, per non lasciare uno stato ambiguo tra "modifiche non salvate" e "attivato".

### Lista (`views/pianiAlimentari/PianiAlimentariListView.vue`, nuova rotta `/piani-alimentari`)

Stesso pattern di `PazientiListView.vue`/`AlimentiListView.vue`: ricerca testo (paziente o nome piano), chip di stato (Tutti/Bozza/Attivo/Scaduto/Terminato), tabella paginata lato server (Paziente, Piano, Stato badge, Obiettivo kcal, Scadenza, azione "Apri"), bottone "Crea nuovo piano" (apre la selezione paziente se non già in contesto).

### Navigazione

Abilitare le voci oggi disabilitate: "Piani alimentari" in sidebar (già presente ma statica/non cliccabile in alcune viste), "Nuovo piano alimentare" nel dropdown "Crea nuovo" della dashboard, bottone omonimo nella scheda paziente (`PazienteDettaglioView.vue`).

### API client (`api/pianiAlimentari.ts`, nuovo)

`crea()`, `dettaglio(id)`, `aggiorna(id, payload)`, `attiva(id)`, `elimina(id)`, `cerca(criteri)` — stesso stile di `api/alimenti.ts`/`api/pazienti.ts`.

## Fuori scope (esplicito)

- Cambiare la modalità di un piano dopo la creazione.
- Endpoint di ricalcolo del suggerimento TDEE su un piano esistente (si crea un nuovo piano/fase).
- Storico "Piani alimentari" nella scheda paziente (l'endpoint di ricerca supporta già `pazienteId`, ma la UI dedicata in `PazienteDettaglioView.vue` resta per una sessione futura).
- Uno stato "Inviato" distinto da "Attivo" (non ha senso finché `frontend-cliente/` non esiste).
- Qualunque vista in `frontend-cliente/`.
- Percentuali di aggiustamento per obiettivo diverse da 0% (il campo `FormulaBmr`/lo schema restano pronti per reintrodurle in futuro senza migrazione aggiuntiva, ma non calcolate ora).
- Validazione server-side stringente su `nome` del piano/pasto/esempio (stesso gap già noto e non affrontato su altri campi testuali del progetto — solo `@NotBlank`).

## Testing (indicazioni per il piano)

**Backend**
- `CalcolatoreTdeeTest` (unitario, nessun DB): ogni ramo dell'algoritmo — Katch-McArdle vs Mifflin-St Jeor, i tre valori di sesso, i 5 fattori di attività + default 1.375, età calcolata sulla data visita e non su oggi, i due obiettivi esclusi (nessun calcolo), soglia di sicurezza (sopra/sotto, per sesso), l'esempio numerico completo di `specifica-tdee.docx` §7 come test di non-regressione.
- `PianoAlimentareServiceTest`/`PianoAlimentareControllerTest`: creazione per ciascuna modalità (struttura template corretta), creazione senza nessuna visita del paziente (suggerimento tutto `null`), aggiornamento che sostituisce la struttura figlia (righe rimosse davvero rimosse, non orfane), `attiva` (disattiva il piano precedente, transazionalità), `elimina` (409 se non `BOZZA`), isolamento per professionista, 401/403/404.
- `PianoAlimentareSpecificationsTest`: filtro `stato` incluso `SCADUTO` calcolato, ricerca testo, paginazione/ordinamento.

**Frontend**
- `api/pianiAlimentari.spec.ts`: ogni funzione, costruzione query string di `cerca()`.
- `PianoAlimentareFormView.spec.ts`: creazione per le tre modalità, aggiungi/rimuovi pasto e alimento (ricerca e manuale), modifica grammi ricalcola i totali visualizzati, "Applica a tutti i giorni" (MACRO), aggiungi/rimuovi esempio (ESEMPI), salvataggio, attivazione, eliminazione bozza.
- `PianiAlimentariListView.spec.ts`: ricerca, chip di stato, paginazione, stato vuoto, "Crea nuovo piano".

Aggiornare `wiki/api-contracts.md`, `wiki/modello-dati.md` (risolvere la domanda aperta su snapshot vs riferimento) nello stesso passaggio dell'implementazione, come richiesto da `backend/CLAUDE.md`.
