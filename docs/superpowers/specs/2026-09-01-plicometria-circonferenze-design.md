# Design — Modulo Plicometria e redesign Circonferenze

Data: 2026-09-01
Percorso: architetturale (brainstorming → questo spec → piano di implementazione)
Fonti: 6 PDF forniti da Andrea in `C:\Users\Andrea Bonaiuti\Downloads\specifiche\` (non versionati, materiale grezzo esterno al repo) — due versioni delle specifiche del modulo Plicometria/Circonferenze (la seconda più precisa sulla tabella coefficienti Durnin-Womersley, sull'età calcolata e sul caso Faulkner), la trascrizione di una conversazione di ricerca sulla BIA/plicometria/circonferenze, la specifica UI/UX della tripla misurazione con media automatica, e un allegato aggiunto in corso d'opera con i protocolli Slaughter (pediatrico) ed Evans (atleti) — quest'ultimo corretto da Andrea a formula unica dopo che la prima versione dichiarava la plica coscia come richiesta senza usarla nell'equazione.

## Obiettivo

Estendere il flusso "Nuovo paziente con visita" (oggi: `PazienteNuovoView.vue` + `DatiVisitaForm.vue` + `POST /pazienti`) con:

1. Un **nuovo modulo Plicometria** (stima massa grassa/magra da pliche cutanee, 4 protocolli).
2. Un **redesign del modulo Circonferenze** esistente, allineato alle sedi e agli indici (WHR/WHtR) descritti nei PDF.

La BIA resta esplicitamente esclusa (placeholder "Sarà disponibile a breve" invariato). Restano fuori scope anche l'andamento tra visite (delta, grafici) e qualunque UI di visualizzazione dei risultati: oggi esiste solo il flusso di creazione, non un dettaglio paziente/visita — arriveranno con il sotto-progetto "Monitoraggio" già previsto in [modello-dati](../../wiki/modello-dati.md).

## Stato attuale (per contesto)

- `Visita` (`backend/.../pazienti/Visita.java`, tabella `visite`, V5+V7): altezza/peso obbligatori + 12 circonferenze opzionali (vita, ombelico, fianchi, petto, coscia dx/sx, polpaccio dx/sx, larghezza spalle, circonferenza spalle, bicipite dx/sx). Nessun calcolo derivato esiste oggi, né in backend né in frontend.
- `Paziente.sesso`: `String` libero, non obbligatorio, ma il `Select` in `PazienteNuovoView.vue` offre solo `M`/`F`.
- Età: mai persistita, calcolata solo in frontend (`calcolaEta`, sempre rispetto a "oggi", mai rispetto alla data della visita) — usata solo per il campo di sola lettura "Età" in anagrafica.
- `DatiVisitaForm.vue`: componente autonomo (espone `valida()`/`ottieniDati()`), usato da `PazienteNuovoView.vue`; contiene data visita, altezza, peso, placeholder BIA, accordion circonferenze.

## Decisioni prese in brainstorming

- Le circonferenze passano a **misura singola** (niente più dx/sx), seguendo lo schema dei PDF.
- I valori derivati (BMI, WHR, WHtR, %BF, FM, FFM, FMI, FFMI, MAMC) vengono **calcolati e persistiti dal backend** al momento della visita — non ricalcolati a runtime da formule che potrebbero cambiare in futuro (storicità/riproducibilità, richiesta esplicita dei PDF).
- Protocolli plicometrici implementati in questo giro: **Jackson-Pollock 3, Jackson-Pollock 7, Durnin-Womersley 4, Faulkner 4, Slaughter (pediatrico 7-18 anni), Evans (atleti d'élite)** — 6 protocolli, tutti con formula completa e verificata nei PDF. ISAK e "solo registrazione manuale" restano fuori.
- Tripla misurazione con media automatica (dal terzo PDF): **solo sulle pliche**, non sulle circonferenze.
- `Paziente.sesso` diventa **obbligatorio**, tre valori: `M` / `F` / `ALTRO`. Se `ALTRO`, l'intera sezione Plicometria è disabilitata nel form (tutte le formule richiedono M/F). WHR/WHtR restano calcolabili comunque (sono rapporti puri); l'etichetta di rischio per soglia sesso-dipendente non si applica in quel caso (nessun dato da persistere per questo — è pura interpretazione a video, rimandata alla futura UI).
- Età: calcolata **lato backend** da `paziente.dataNascita` + `visita.dataVisita` (mai un valore manuale, mai "oggi"), persistita per riproducibilità.
- Nessuna anteprima live dei valori derivati nel form prima del salvataggio (evita di duplicare le formule in TypeScript): l'unico calcolo lato frontend è la media delle 3 sotto-misurazioni di una plica, per UX immediata.
- Nessuna nuova UI di visualizzazione risultati in questo giro: solo raccolta, validazione, calcolo, persistenza.

## Modello dati

### `pazienti` (migrazione V9)

```sql
ALTER TABLE pazienti ALTER COLUMN sesso SET NOT NULL;
ALTER TABLE pazienti ADD CONSTRAINT chk_pazienti_sesso CHECK (sesso IN ('M', 'F', 'ALTRO'));
```

Sicura: `hexis` (produzione) è vuoto, confermato da Andrea l'1 settembre 2026 (vedi [stato](../../wiki/stato.md)); `hexis_test` viene ricreato dai test.

### `visite` — circonferenze (migrazione V10)

Rimosse (nessun dato reale da preservare):

```
circonferenza_ombelico_cm, circonferenza_petto_cm,
circonferenza_coscia_dx_cm, circonferenza_coscia_sx_cm,
circonferenza_polpaccio_dx_cm, circonferenza_polpaccio_sx_cm,
larghezza_spalle_cm, circonferenza_spalle_cm,
circonferenza_bicipite_dx_cm, circonferenza_bicipite_sx_cm
```

Mantenute: `circonferenza_vita_cm`, `circonferenza_fianchi_cm`.

Aggiunte (stesso pattern di naming `circonferenza_*_cm`, `NUMERIC(6,2)`, nullable):

```
circonferenza_addome_cm, circonferenza_braccio_rilassato_cm,
circonferenza_coscia_cm, circonferenza_polpaccio_cm,
circonferenza_collo_cm, circonferenza_torace_cm,
circonferenza_braccio_contratto_cm, circonferenza_avambraccio_cm,
circonferenza_caviglia_cm
```

Più:

```sql
protocollo_vita VARCHAR(20) NOT NULL DEFAULT 'OMS'  -- OMS | OMBELICALE | ALTRO
bmi NUMERIC(5,2)    -- sempre calcolabile (altezza/peso già obbligatori)
whr NUMERIC(4,2)    -- solo se vita e fianchi presenti
whtr NUMERIC(4,2)   -- solo se vita presente
mamc_cm NUMERIC(5,2) -- solo se braccio_rilassato_cm E plica_tricipitale_mm presenti (incrocio con plicometria)
```

`protocollo_vita` è sempre valorizzato (default `OMS`) anche se `vita_cm` è vuoto — evita all'utente una scelta quando non serve, resta coerente se in futuro `vita_cm` viene compilato in un secondo momento.

### `plicometrie` (nuova tabella, migrazione V11) — 1:1 opzionale con `visite`

```sql
CREATE TABLE plicometrie (
    id UUID PRIMARY KEY,
    visita_id UUID NOT NULL UNIQUE REFERENCES visite(id),
    protocollo VARCHAR(30) NOT NULL,  -- JACKSON_POLLOCK_3 | JACKSON_POLLOCK_7 | DURNIN_WOMERSLEY_4 | FAULKNER_4 | SLAUGHTER_PEDIATRICO | EVANS_ATLETI
    formula_versione VARCHAR(40) NOT NULL,  -- es. 'jackson-pollock-1978-3siti', 'durnin-womersley-1974', 'faulkner-1968', 'slaughter-1988', 'evans-2005'
    eta_anni INTEGER NOT NULL,        -- calcolata backend da data_nascita + data_visita, persistita per riproducibilità
    coefficiente_c NUMERIC(6,4),      -- solo Durnin-Womersley: snapshot del coefficiente c effettivamente usato
    coefficiente_m NUMERIC(6,4),      -- solo Durnin-Womersley: snapshot del coefficiente m effettivamente usato
    etnia_atleta VARCHAR(20),         -- solo Evans: CAUCASICO | AFROAMERICANO (default CAUCASICO se non specificata)

    plica_pettorale_mm NUMERIC(5,2),
    plica_ascellare_mm NUMERIC(5,2),
    plica_tricipitale_mm NUMERIC(5,2),
    plica_bicipitale_mm NUMERIC(5,2),
    plica_sottoscapolare_mm NUMERIC(5,2),
    plica_soprailiaca_mm NUMERIC(5,2),
    plica_addominale_mm NUMERIC(5,2),
    plica_coscia_mm NUMERIC(5,2),
    plica_polpaccio_mm NUMERIC(5,2),

    somma_pliche_mm NUMERIC(6,2) NOT NULL,
    densita_corporea NUMERIC(6,4),          -- NULL per Faulkner (produce %BF direttamente)
    percentuale_grasso NUMERIC(5,2) NOT NULL,
    massa_grassa_kg NUMERIC(6,2) NOT NULL,
    massa_magra_kg NUMERIC(6,2) NOT NULL,
    fmi NUMERIC(5,2) NOT NULL,
    ffmi NUMERIC(5,2) NOT NULL,

    creato_il TIMESTAMPTZ NOT NULL
);
```

`plica_polpaccio_mm` (già presente nello schema) diventa un campo attivamente usato da Slaughter, non più solo un placeholder per un futuro ISAK.

Ogni plica salvata contiene il **valore finale** (già mediato se la tripla misurazione era attiva) — non le 3 sotto-misurazioni grezze, per restare "puliti" come indicato esplicitamente nel terzo PDF.

La riga si crea **solo se** il professionista sceglie un protocollo e compila almeno le pliche richieste da quel protocollo (modulo opzionale, coerente con l'assenza della BIA in questo stesso form).

### `durnin_womersley_coefficienti` (nuova tabella di riferimento, migrazione V12)

I PDF richiedono esplicitamente che i coefficienti Durnin-Womersley vivano in una tabella versionata nel database, non come costanti nel codice:

```sql
CREATE TABLE durnin_womersley_coefficienti (
    id UUID PRIMARY KEY,
    sesso VARCHAR(1) NOT NULL,  -- M | F
    eta_min INTEGER NOT NULL,
    eta_max INTEGER,            -- NULL = nessun limite superiore (fascia 50+)
    c NUMERIC(6,4) NOT NULL,
    m NUMERIC(6,4) NOT NULL
);
```

Popolata dalla stessa migrazione con le 10 righe (5 fasce × 2 sessi) dall'allegato del PDF:

| Sesso | eta_min | eta_max | c | m |
|---|---|---|---|---|
| M | 17 | 19 | 1.1620 | 0.0630 |
| M | 20 | 29 | 1.1631 | 0.0632 |
| M | 30 | 39 | 1.1422 | 0.0544 |
| M | 40 | 49 | 1.1620 | 0.0700 |
| M | 50 | NULL | 1.1715 | 0.0779 |
| F | 16 | 19 | 1.1549 | 0.0678 |
| F | 20 | 29 | 1.1599 | 0.0717 |
| F | 30 | 39 | 1.1423 | 0.0632 |
| F | 40 | 49 | 1.1333 | 0.0612 |
| F | 50 | NULL | 1.1339 | 0.0645 |

Lookup: `sesso = ? AND eta_min <= eta AND (eta_max IS NULL OR eta <= eta_max)`, la fascia più alta compatibile vince. Se nessuna riga corrisponde (es. età sotto la fascia più bassa) → calcolo bloccato, 400 esplicito.

I coefficienti `c`/`m` effettivamente risolti da questa lookup vengono **anche** copiati su `plicometrie.coefficiente_c`/`coefficiente_m` al momento del salvataggio (non solo referenziati) — se in futuro un valore in questa tabella viene corretto, le visite storiche restano dimostrabili con i coefficienti realmente usati all'epoca, non quelli attuali. Stessa logica per cui l'output finale (`%BF`, `FM`...) è congelato e non ricalcolato: qui si applica un gradino più a monte, richiesto esplicitamente dalla regola "Riproducibilità" della versione revisionata del PDF ("salvare... formula, versione, coefficienti...").

## Formule (dai PDF, verbatim)

**Siri** (conversione densità → grasso, **non** per Faulkner che produce già `%BF`):
`%BF = (495 / D) − 450`

**Jackson-Pollock 3 — uomini** (pettorale + addominale + coscia):
`D = 1.109380 − 0.0008267 × S3 + 0.0000016 × S3² − 0.0002574 × età`

**Jackson-Pollock 3 — donne** (tricipitale + soprailiaca + coscia):
`D = 1.0994921 − 0.0009929 × S3 + 0.0000023 × S3² − 0.0001392 × età`

**Jackson-Pollock 7 — uomini** (pettorale + ascellare + tricipitale + sottoscapolare + addominale + soprailiaca + coscia):
`D = 1.112 − 0.00043499 × S7 + 0.00000055 × S7² − 0.00028826 × età`

**Jackson-Pollock 7 — donne** (stesse 7 sedi):
`D = 1.097 − 0.00046971 × S7 + 0.00000056 × S7² − 0.00012828 × età`

**Durnin-Womersley 4** (bicipitale + tricipitale + sottoscapolare + soprailiaca):
`D = c − m × log10(S4)` — c, m da `durnin_womersley_coefficienti`

**Faulkner 4** (tricipitale + sottoscapolare + soprailiaca + addominale):
`%BF = (S4 × 0.153) + 5.783` — **nessuna** conversione di Siri

**Slaughter (1988) — pediatrico, 7-18 anni** (tricipitale + polpaccio mediale, `S2` = somma):
```
Maschi:  S2 < 35mm → %BF = 0.735×S2 + 1.0     |  S2 ≥ 35mm → %BF = 1.21×S2 − 0.008×S2² − 1.7
Femmine: S2 < 35mm → %BF = 0.610×S2 + 5.1     |  S2 ≥ 35mm → %BF = 1.21×S2 − 0.008×S2² − 3.4
```
Nessuna conversione di Siri, nessuna densità corporea. Il target d'età (7-18 anni) è informativo — questo giro non blocca la selezione del protocollo se l'età è fuori fascia (i PDF più recenti hanno rimosso la richiesta di warning presente in una bozza intermedia).

**Evans (2005) — atleti d'élite** (tricipitale + addominale + coscia anteriore, `3SKF` = somma) — formula unica corretta da Andrea:
`%BF = 8.997 + (0.24658 × 3SKF) − (6.343 × Sesso) − (1.998 × Etnia)`
dove `Sesso`: Maschio=1, Femmina=0; `Etnia`: Afroamericano=1, Caucasico=0 (default Caucasico se non specificata). Nessuna conversione di Siri, nessuna densità corporea, età non usata in questa formula nonostante compaia nella tabella descrittiva dei campi del PDF (incoerenza minore tra descrizione e formula, non bloccante: si implementa la formula, che è quella marcata "REVISIONE CORRETTA").

**Limite di sicurezza biologico** (tutti i protocolli, per evitare `%BF` implausibili su pliche vicine allo zero): dopo il calcolo, `%BF = max(soglia, %BF calcolato)` con soglia 3.0 per sesso M e 10.0 per sesso F. Il PDF cita Evans/Faulkner come esempio ma non ne limita esplicitamente l'ambito ad essi soli ("limitatori di sicurezza biologici" è presentato come regola generale) — **interpretazione scelta qui: si applica a tutti i 6 protocolli**, non solo a quelli a `%BF` diretto. Segnalalo in revisione se intendevi un ambito più stretto.

**Poi, per tutti i protocolli**:
```
FM_kg  = peso_kg × (%BF / 100)
FFM_kg = peso_kg − FM_kg
FMI    = FM_kg / altezza_m²
FFMI   = FFM_kg / altezza_m²
```

**Circonferenze**:
```
BMI  = peso_kg / altezza_m²
WHR  = vita_cm / fianchi_cm       (solo se entrambe presenti)
WHtR = vita_cm / altezza_cm       (solo se vita presente)
MAMC = braccio_rilassato_cm − (π × plica_tricipitale_mm / 10)   (solo se entrambe presenti)
```

## Backend — architettura del motore di calcolo

**Strategy pattern**: un'interfaccia `CalcolatorePlicometria` con un'implementazione per protocollo (`JacksonPollock3Calcolatore`, `JacksonPollock7Calcolatore`, `DurninWomersley4Calcolatore`, `Faulkner4Calcolatore`, `SlaughterPediatricoCalcolatore`, `EvansAtletiCalcolatore`). Ciascuna implementazione sa quali pliche richiede (eventualmente dipendenti dal sesso, per JP3/JP7) e come calcola densità/`%BF`. Preferito a uno switch-case centralizzato perché ogni protocollo è testabile in isolamento e aggiungere ISAK in futuro non tocca gli altri.

Il metodo di calcolo riceve i dati contestuali (sesso, età, etnia atleta) in un unico record `ContestoPlicometria(Sesso sesso, int etaAnni, EtniaAtleta etniaAtleta)` invece di parametri posizionali separati — solo Evans usa `etniaAtleta`, gli altri 5 calcolatori lo ignorano, ma il contesto resta un solo oggetto invece di crescere ad ogni protocollo con un nuovo parametro.

Dopo che il calcolatore restituisce `%BF`, `PlicometriaService` applica il limite di sicurezza biologico (`max(3.0, %BF)` per M, `max(10.0, %BF)` per F) **prima** di derivare FM/FFM/FMI/FFMI e di persistere — vedi nota sull'interpretazione dell'ambito del limite più sopra.

`PlicometriaService`, invocato da `PazienteService` nella stessa transazione di creazione di `Paziente`+`Visita`:
1. Se la request non contiene un blocco plicometria (nessun protocollo scelto), non fa nulla — modulo opzionale.
2. Se `paziente.sesso == ALTRO` e la request contiene pliche/protocollo → 400 esplicito.
3. Calcola l'età da `paziente.dataNascita` + `visita.dataVisita`.
4. Risolve il `CalcolatorePlicometria` per il protocollo scelto; verifica che tutte le pliche richieste da quel protocollo (per quel sesso) siano presenti — altrimenti 400 con indicazione di quali mancano.
5. Per Durnin-Womersley, effettua la lookup dei coefficienti; se non trova una riga applicabile → 400.
6. Calcola densità (dove prevista) → `%BF` (Siri, tranne Faulkner) → FM/FFM/FMI/FFMI.
7. Salva la riga `Plicometria`, incluso `formula_versione` (costante per protocollo) e, per Durnin-Womersley, lo snapshot di `coefficiente_c`/`coefficiente_m` usati.

`VisitaService` (o l'attuale logica in `PazienteService`) calcola BMI sempre, WHR/WHtR/MAMC solo se i rispettivi input sono presenti, prima di salvare `Visita`.

### DTO

- `VisitaRequest`: campi circonferenze aggiornati (sostituiscono gli attuali) + `protocolloVita` + nuovo campo opzionale `plicometria: PlicometriaRequest`.
- Nuovo `PlicometriaRequest` (record): `protocollo` (stringa/enum) + 9 campi plica opzionali `BigDecimal` (`@Positive @Digits(integer = 3, fraction = 2)`, più un limite superiore plausibile da definire in fase di piano — indicativamente `@DecimalMax("100")` mm).
- Validazione server-side coerente con il pattern già usato per altezza/peso/circonferenze (`@NotNull`/`@Positive`/`@Digits`/`@Max`), non solo client-side.

## Frontend

- `PazienteNuovoView.vue`: `Select` sesso passa da 2 a 3 opzioni (Maschio/Femmina/Altro), reso obbligatorio con lo stesso pattern UX già usato per data di nascita (asterisco, errore che sparisce alla correzione). Nuovo `erroreSesso` in `validators.ts`.
- `DatiVisitaForm.vue`: accordion Circonferenze aggiornato ai nuovi campi (vita, fianchi, addome, braccio rilassato, coscia, polpaccio, collo, torace, braccio contratto, avambraccio, caviglia) + `Select` protocollo vita (default OMS).
- Nuovo componente `PlicometriaForm.vue` (stesso pattern di `DatiVisitaForm.vue`: `valida()`/`ottieniDati()` via `defineExpose`):
  - riceve il `sesso` scelto in anagrafica come prop; se `ALTRO`, l'intera sezione è disabilitata con un messaggio esplicativo (non solo nascosta — il professionista deve capire perché);
  - `Select` protocollo (6 opzioni); cambiando protocollo cambiano dinamicamente le pliche mostrate/obbligatorie (stessa logica di campi condizionali già vista nei PDF);
  - se il protocollo è Evans, mostra anche il `Select` Etnia (Caucasico/Afroamericano, default Caucasico) — nascosto per gli altri 5 protocolli;
  - per ciascuna plica: checkbox "Tripla misurazione" → se attiva, 3 input + media calcolata live in TypeScript (stessa formula del terzo PDF, `(M1+M2+M3)/3`); il valore inviato al backend è sempre e solo il valore finale (medio o singolo).
- Nessuna anteprima dei valori derivati (%BF, FM, WHR...) nel form: si vedono solo se/quando esisterà una UI di dettaglio.

## Fuori scope (esplicito)

- BIA (placeholder invariato).
- UI di visualizzazione dei risultati calcolati (dettaglio paziente/visita non esiste ancora).
- Delta tra visite, andamento nel tempo, grafici — richiedono il sotto-progetto "Monitoraggio" (multi-visita) non ancora iniziato.
- "Rischio vita diretto" con soglie OMS colorate — puro display (vita_cm + sesso, sempre ricomputabile), nessun dato da persistere ora.
- ISAK e protocollo "solo registrazione manuale".
- Il gap di validazione server-side già noto su `nome`/`cognome`/`telefono`/`lavoro` (in [domande-aperte](../../wiki/domande-aperte.md)) — non toccato da questo lavoro, salvo per il nuovo campo `sesso` che qui diventa esplicitamente validato.

## Testing (indicazioni per il piano)

- Backend: un test per calcolatore (6 protocolli) con casi costruiti a mano dalle formule, incluso il limite di sicurezza biologico isolato in un test proprio (nessun dataset di riferimento pubblico verificato nei PDF, quindi verifica algebrica diretta); test di validazione (pliche mancanti per il protocollo, sesso `ALTRO` + plicometria presente, Durnin senza riga di coefficienti applicabile, età sotto la fascia minima); estensione di `PazienteControllerTest` per il nuovo body (circonferenze ridisegnate + blocco plicometria opzionale + sesso obbligatorio).
- Frontend: `PlicometriaForm.spec.ts` nuovo (media live, campi condizionati dal protocollo, disabilitazione per sesso `ALTRO`); `DatiVisitaForm.spec.ts` aggiornato ai nuovi campi circonferenza; nuovi test per `erroreSesso`.
- Aggiornare `wiki/modello-dati.md` e `wiki/api-contracts.md` nello stesso passaggio dell'implementazione (richiesto da `backend/CLAUDE.md`).
