# Blocco su più giorni — piano di implementazione

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** un blocco dell'agenda ha inizio e fine (data e ora) anche su giorni diversi, con l'opzione "Giornate intere".

**Architecture:** tre colonne nuove su `appuntamenti` (`data_fine`, `ora_fine`, `giornata_intera`), usate solo dai blocchi. Il backend restituisce i blocchi che intersecano l'intervallo richiesto e controlla le sovrapposizioni nei due sensi. Il frontend "proietta" ogni blocco sui giorni che copre con una sola funzione (`eventiDelGiorno`), così Giorno, Settimana, Mese e Dashboard continuano a lavorare su eventi di un giorno con `oraInizio` + `durataMinuti`.

**Tech Stack:** Spring Boot 3.3 / Java 21 / Flyway / PostgreSQL 13; Vue 3 + TypeScript + shadcn-vue.

**Spec:** [docs/superpowers/specs/2026-11-27-blocco-su-piu-giorni-design.md](../specs/2026-11-27-blocco-su-piu-giorni-design.md)

## Global Constraints

- **Mai `git commit`**: a fine task solo `git add` dei file toccati. I commit li fa Andrea.
- **Nessun test nuovo** salvo richiesta di Andrea. Le suite esistenti devono restare verdi: backend `mvn test` (con `$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"`, da `backend/`), frontend `npx vue-tsc --noEmit` e `npx vitest run` (da `frontend-professionisti/`).
- Durata massima di un blocco: **62 giorni** (`ChronoUnit.DAYS.between(data, dataFine) <= AgendaService.GIORNI_MASSIMI`).
- Fascia: `AgendaTempo.INIZIO_GRIGLIA` 07:00 – `FINE_GRIGLIA` 20:00. Per un blocco con orari: `oraInizio` in [07:00, 20:00), `oraFine` in (07:00, 20:00], e (`data`,`oraInizio`) strettamente prima di (`dataFine`,`oraFine`).
- Messaggi di errore in italiano, con `AppuntamentoNonValidoException` (400).
- Controlli shadcn (`Checkbox`, `DatePicker`, `TimePicker`, `Button`), mai HTML nativo; classi Tailwind, mai `style=` inline; errori live sui campi come il resto del dialog.
- Visite e promemoria non cambiano comportamento.
- Ogni endpoint o colonna toccati si riflettono in `wiki/api-contracts.md` e `wiki/modello-dati.md` (Task 6).

## Review Focus

Senza test nuovi, questi casi si verificano nel browser a fine lavoro (Andrea, o Claude se Andrea lo chiede):

1. Un blocco iniziato nel mese precedente compare nella vista Mese e Settimana del mese corrente (`GET /agenda` con `data < da`).
2. Una visita creata in un giorno **intermedio** di un blocco lungo dà la conferma di sovrapposizione.
3. Modificare solo il motivo di un blocco che copre visite **non** chiede di nuovo la conferma; cambiare le date sì.
4. Un blocco esistente modificato in "Giornate intere" e poi riportato a orari salva e mostra i valori giusti (orari a `null` quando intero).
5. I blocchi di un giorno già presenti prima della V34 si vedono e si modificano come prima (stesso orario).

---

### Task 1: Modello dati e API dei blocchi (backend)

**Files:**
- Create: `backend/src/main/resources/db/migration/V34__blocco_su_piu_giorni.sql`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/agenda/Appuntamento.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/agenda/AppuntamentoRequest.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/agenda/AppuntamentoResponse.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/agenda/AppuntamentoRepository.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/agenda/AgendaService.java` (`elenco`, `applica` ramo `BLOCCO`)

**Interfaces:**
- Produces (API): request e response con `dataFine` (`YYYY-MM-DD` | null), `oraFine` (`HH:mm` | null), `giornataIntera` (boolean). Per i blocchi `durataMinuti` è sempre `null` in uscita.
- Produces (Java): `Appuntamento.getDataFine(): LocalDate`, `getOraFine(): LocalTime`, `isGiornataIntera(): boolean` (+ setter); `AppuntamentoRepository.elencoNelPeriodo(UUID professionistaId, LocalDate da, LocalDate a): List<Appuntamento>`.

- [ ] **Step 1: Migrazione V34**

```sql
ALTER TABLE appuntamenti
    ADD COLUMN data_fine DATE,
    ADD COLUMN ora_fine TIME,
    ADD COLUMN giornata_intera BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE appuntamenti
SET data_fine = data,
    ora_fine = ora_inizio + make_interval(mins => durata_minuti),
    durata_minuti = NULL
WHERE tipo = 'BLOCCO';

CREATE INDEX idx_appuntamenti_professionista_data_fine ON appuntamenti(professionista_id, data_fine);
```

- [ ] **Step 2: Entità e DTO**

Campi `dataFine` (`data_fine`), `oraFine` (`ora_fine`), `giornataIntera` (`giornata_intera`, `nullable = false`) su `Appuntamento`. `AppuntamentoRequest` aggiunge `LocalDate dataFine`, `@JsonFormat(pattern = "HH:mm") LocalTime oraFine`, `Boolean giornataIntera`. `AppuntamentoResponse` aggiunge `LocalDate dataFine`, `@JsonFormat(pattern = "HH:mm") LocalTime oraFine`, `boolean giornataIntera`, popolati in `da(...)`.

- [ ] **Step 3: Lettura per periodo**

`AppuntamentoRepository.elencoNelPeriodo` con `@Query`: appuntamenti del professionista dove (`tipo <> BLOCCO` e `data between :da and :a`) oppure (`tipo = BLOCCO` e `data <= :a` e `dataFine >= :da`), ordinati per `data`, `oraInizio`. `AgendaService.elenco` la usa al posto di `findAllByProfessionistaIdAndDataBetweenOrderByDataAscOraInizioAsc` (che resta se usata altrove).

- [ ] **Step 4: Validazione e copia del ramo `BLOCCO` in `applica`**

Regole, nell'ordine, con questi messaggi:
- `dataFine` null → "La data di fine è obbligatoria"
- `dataFine` prima di `data` → "La fine deve seguire l'inizio"
- più di 62 giorni → "Un blocco può durare al massimo 62 giorni"
- se `giornataIntera` è vero: `oraInizio` e `oraFine` salvati a `null`
- altrimenti: `oraInizio` o `oraFine` null → "Ora di inizio e di fine sono obbligatorie"; fuori fascia (vedi Global Constraints) → "L'orario deve essere compreso tra le 07:00 e le 20:00"; inizio non prima della fine → "La fine deve seguire l'inizio"

Imposta sempre `durataMinuti = null`. Nei rami visita e promemoria: `dataFine = null`, `oraFine = null`, `giornataIntera = false`. `verificaFascia` resta per le visite.

- [ ] **Step 5: Suite**

Run: `mvn test` da `backend/` (JDK 21). Expected: BUILD SUCCESS, nessun fallimento (nessun test esistente usa `BLOCCO`).

- [ ] **Step 6: Staging** — `git add` dei file del task.

---

### Task 2: Sovrapposizioni nei due sensi (backend)

**Files:**
- Create: `backend/src/main/java/com/hexisnutrition/backend/agenda/TrattoBlocco.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/agenda/AppuntamentoRepository.java`
- Modify: `backend/src/main/java/com/hexisnutrition/backend/agenda/AgendaService.java` (`verificaSovrapposizione`, `crea`, `aggiorna`)

**Interfaces:**
- Consumes: campi e `elencoNelPeriodo` del Task 1.
- Produces: `record TrattoBlocco(LocalTime inizio, LocalTime fine)` con `static Optional<TrattoBlocco> nelGiorno(Appuntamento blocco, LocalDate giorno)`; `AppuntamentoRepository.blocchiCheCopronoIlGiorno(UUID professionistaId, LocalDate giorno): List<Appuntamento>`.

- [ ] **Step 1: `TrattoBlocco.nelGiorno`**

Vuoto se `giorno` è fuori da [`data`, `dataFine`]. Giornata intera → 07:00–20:00. Altrimenti inizio = `oraInizio` se `giorno == data`, sennò 07:00; fine = `oraFine` se `giorno == dataFine`, sennò 20:00. Vuoto se inizio non è prima di fine.

- [ ] **Step 2: Visita contro blocchi**

In `verificaSovrapposizione`, per una visita: i conflitti con altre visite restano come oggi (stesso giorno); i blocchi si prendono da `blocchiCheCopronoIlGiorno` (`tipo = BLOCCO`, `data <= giorno <= dataFine`) e si confronta l'orario della visita con `TrattoBlocco.nelGiorno(blocco, giorno)` (intervalli semiaperti). Il ramo che oggi filtra i blocchi su `oraInizio`/`durataMinuti` dello stesso giorno va sostituito da questo.

- [ ] **Step 3: Blocco contro visite**

Nuovo ramo per `BLOCCO`: visite (`isVisita()`, stato `PROGRAMMATA`) con `data` tra `blocco.data` e `blocco.dataFine` (si può riusare `elencoNelPeriodo` e filtrare), che si sovrappongono a `TrattoBlocco.nelGiorno(blocco, visita.data)`. Se ce ne sono: `SovrapposizioneException(rispondi(conflitti))`, ordinati per data e ora. Escludere l'appuntamento stesso per id.

- [ ] **Step 4: Avviso in modifica solo se cambia l'intervallo**

In `aggiorna`, prima di `applica`, se l'appuntamento è un blocco, salvare (`data`, `oraInizio`, `dataFine`, `oraFine`, `giornataIntera`). Dopo `applica`, se tutti e cinque sono uguali, non chiamare il controllo per il blocco (trattare come `confermaSovrapposizione = true`).

- [ ] **Step 5: Suite** — `mvn test`, BUILD SUCCESS.

- [ ] **Step 6: Staging** — `git add`.

---

### Task 3: Proiezione dei blocchi sui giorni e viste dell'agenda (frontend)

**Files:**
- Modify: `frontend-professionisti/src/api/agenda.ts`
- Create: `frontend-professionisti/src/utils/blocchi.ts`
- Modify: `frontend-professionisti/src/components/agenda/AgendaGiorno.vue:24`, `AgendaSettimana.vue:57`, `AgendaMese.vue:19`
- Modify: `frontend-professionisti/src/views/agenda/AgendaView.vue` (`apriModifica`, `onAzione`)

**Interfaces:**
- Consumes: campi API del Task 1.
- Produces: in `Appuntamento` i campi `dataFine: string | null`, `oraFine: string | null`, `giornataIntera: boolean`; in `AppuntamentoRichiesta` gli stessi come opzionali. In `utils/blocchi.ts`:
  - `trattoNelGiorno(blocco: Appuntamento, giorno: string): { inizio: string; fine: string } | null` (stessa regola di `TrattoBlocco.nelGiorno`, orari `HH:mm`);
  - `eventiDelGiorno(appuntamenti: Appuntamento[], giorno: string): Appuntamento[]` — i non-blocchi con `data === giorno` invariati, più per ogni blocco con un tratto quel giorno una copia `{ ...blocco, data: giorno, oraInizio: tratto.inizio, durataMinuti: minuti del tratto }` (stesso `id`);
  - `coperturaIntera(blocco: Appuntamento, giorno: string): boolean` — tratto 07:00–20:00.

- [ ] **Step 1: Tipi API e `utils/blocchi.ts`** come da Interfaces. Riusa `minutiDaOra` di `utils/agenda.ts`; confronti di date come stringhe ISO.

- [ ] **Step 2: Viste** — sostituire i tre `filter((a) => a.data === …)` con `eventiDelGiorno(props.appuntamenti, iso)`. Il resto delle viste resta com'è: il tratto ha già `oraInizio` e `durataMinuti`. Unica eccezione: in `AgendaGiorno.vue:63` per i blocchi si mostra solo l'orario di fine, senza "· N min" (su una giornata intera sarebbe "780 min").

- [ ] **Step 3: Originale per id** — in `AgendaView.vue`, `apriModifica(a)` e il ramo `MODIFICA`/`ELIMINA` di `onAzione` usano `appuntamenti.value.find((x) => x.id === a.id) ?? a`, perché dalle viste arriva la copia proiettata. Il dettaglio usa già l'id (`dettaglioId`).

- [ ] **Step 4: Tipi dei test** — `npx vue-tsc --noEmit`; se i mock `Appuntamento` negli `*.spec.ts` non compilano per i campi nuovi, aggiungere `dataFine: null, oraFine: null, giornataIntera: false`. Poi `npx vitest run`. Expected: nessun errore di tipo, tutti verdi.

- [ ] **Step 5: Staging** — `git add`.

---

### Task 4: Form "Blocca fascia oraria" (frontend)

**Files:**
- Modify: `frontend-professionisti/src/components/agenda/AppuntamentoDialog.vue`

**Interfaces:**
- Consumes: tipi del Task 3.

- [ ] **Step 1: Stato e inizializzazione** — nuovi `ref`: `dataFine`, `oraFine`, `giornataIntera`. In `inizializza`, per un blocco esistente: `data`/`ora` da `data`/`oraInizio`, `dataFine`/`oraFine`/`giornataIntera` dall'appuntamento. In creazione: `dataFine = data`, `ora` come oggi, `oraFine` = `ora` + 60 minuti (vuota se `ora` è vuota; massimo 20:00). Aggiungere i tre valori a `firmaCorrente`.

- [ ] **Step 2: Template per `eBlocco`** — sostituire, solo per i blocchi, la griglia Data/Ora e la Durata con:
  - riga "Inizio*": `DatePicker` (`data`) + `TimePicker` (`ora`), ids `appuntamento-data`, `appuntamento-ora`;
  - riga "Fine*": `DatePicker` (`dataFine`) + `TimePicker` (`oraFine`), ids `appuntamento-data-fine`, `appuntamento-ora-fine`;
  - `Checkbox` "Giornate intere" (`appuntamento-giornata-intera`) che nasconde i due `TimePicker`.
  Visite e promemoria restano identici.

- [ ] **Step 3: Validazione live** — chiavi nuove `dataFine`, `oraFine` in `CHIAVI` e `erroreCampo`, con le regole del Task 1 Step 4 e gli stessi messaggi (con il punto finale, come gli altri del dialog). Per i blocchi `erroreOra` non applica più il controllo sulla durata. Aggiungere `dataFine`, `oraFine`, `giornataIntera` al `watch` che pulisce gli errori.

- [ ] **Step 4: Inizio oltre la fine** — `watch` su `data` e `ora` (solo blocchi): se il nuovo inizio supera la fine, la fine si sposta della stessa differenza, senza superare 20:00 nell'ora.

- [ ] **Step 5: Richiesta** — ramo `eBlocco` di `costruisciRichiestaBase`: `data`, `oraInizio` (null se `giornataIntera`), `dataFine`, `oraFine` (null se `giornataIntera`), `giornataIntera`, `durataMinuti: null`.

- [ ] **Step 6: Conflitti con data** — `etichettaConflitto` antepone la data (`formattaDataBreve(c.data)` da `utils/data.ts`) quando il dialog sta salvando un blocco. Il titolo dell'AlertDialog di sovrapposizione per un blocco: "Questo blocco copre N visite" (N = `conflitti.length`, "1 visita" al singolare).

- [ ] **Step 7: Verifica** — `npx vue-tsc --noEmit`, `npx vitest run`: verdi.

- [ ] **Step 8: Staging** — `git add`.

---

### Task 5: Dettaglio e striscia della Dashboard (frontend)

**Files:**
- Modify: `frontend-professionisti/src/components/agenda/AppuntamentoDettaglioDialog.vue` (`dataEstesa`, `orario`, riga `dettaglio-data`)
- Modify: `frontend-professionisti/src/components/dashboard/SettimanaStrip.vue`

**Interfaces:**
- Consumes: `eventiDelGiorno`, `coperturaIntera` del Task 3.

- [ ] **Step 1: Dettaglio di un blocco** — riga data/orario:
  - un giorno, con orari: come oggi, "13:00–14:00" (calcolato da `oraInizio`/`oraFine`, senza "· N min");
  - più giorni, con orari: "Dal 4 dic, 13:00 al 9 dic, 18:00";
  - giornate intere: "Dal 4 al 9 dicembre · giornate intere" (stesso mese) o "Dal 30 nov al 2 dic · giornate intere"; un solo giorno: data estesa · "giornata intera".
  Usare i formattatori di `utils/data.ts`.

- [ ] **Step 2: Striscia** — in `SettimanaStrip.vue` gli eventi del giorno vengono da `eventiDelGiorno`. Nuovo campo per giorno `blocco: string | null` = motivo (o "Bloccato") del primo blocco con `coperturaIntera` quel giorno. Etichetta: se `blocco` e nessuna visita/promemoria → il motivo al posto di "Libero"; se `blocco` con visite o promemoria → si aggiunge il motivo dopo i conteggi. Pallino: tratto `bg-(--fg4)` accanto agli altri quando `blocco`. `descrizione()` aggiunge ", bloccato: <motivo>". I blocchi brevi non cambiano nulla.

- [ ] **Step 3: Verifica** — `npx vue-tsc --noEmit`, `npx vitest run`: verdi.

- [ ] **Step 4: Staging** — `git add`.

---

### Task 6: Wiki

**Files:**
- Modify: `wiki/modello-dati.md`, `wiki/api-contracts.md`, `wiki/domande-aperte.md`, `wiki/stato.md`, `wiki/log.md`

- [ ] **Step 1:** `modello-dati.md`: colonne V34 e significato per i blocchi. `api-contracts.md`: campi nuovi di `/agenda`, regole dei blocchi, `GET` che include i blocchi iniziati prima, 409 anche per un blocco che copre visite. `domande-aperte.md`: voce "Blocco su più giorni" spostata in "Risolte" con rimando allo spec. `stato.md` e `log.md`: sessione, **riavviare il backend (V34)**, cosa provare a mano (Review Focus).
- [ ] **Step 2: Staging** — `git add wiki`.
