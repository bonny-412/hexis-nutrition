# Sostituisci alimento — piano di implementazione

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** su ogni riga di un pasto, un popup "Sostituisci alimento" che cambia l'alimento con un altro tenendo fermo un valore (kcal, proteine, carboidrati o grassi) e calcolando i grammi.

**Architecture:** solo frontend (`frontend-professionisti/`). Funzioni pure per i calcoli in `src/utils/`, la ricerca di "Aggiungi alimento" estratta in un composable condiviso, un dialog nuovo, il collegamento in `PianoAlimentareFormView.vue`. Backend, API, migrazioni e stampa non cambiano.

**Tech Stack:** Vue 3 + TypeScript, shadcn-vue (Reka UI), Tailwind v4, `@lucide/vue`, vitest.

**Spec:** [`docs/superpowers/specs/2026-11-28-sostituisci-alimento-design.md`](../specs/2026-11-28-sostituisci-alimento-design.md)

## Global Constraints

- Mai `git commit`: solo `git add`, i commit li fa Andrea.
- **Test**: Andrea non ha ancora deciso se scriverli lui. Nessun task scrive test nuovi; ogni task chiude con `vue-tsc` e la suite esistente verde. Se Andrea li chiede, si aggiungono a parte.
- Pulsanti sempre `Button` di shadcn; `<button>` nativo solo per righe cliccabili (come i risultati di ricerca di `AggiungiAlimentoDialog.vue`).
- Mai `style="..."` inline: classi Tailwind con le variabili Hexis (`text-(--fg3)`, `bg-(--mint)`, …).
- Testi: "Sostituisci alimento", "Mantieni uguale", "Da sostituire", "Sostituto", "Sostituisci", "Annulla"; sentence case, niente maiuscolo nelle etichette.
- Grammi del sostituto: interi, minimo 1 g. Valore mantenuto di partenza: Kcal.
- Comandi da `frontend-professionisti/`: `npx vue-tsc --noEmit` (pulito), `npx vitest run` (oggi 369/369, codice 0).

## Review Focus

1. Riga di partenza con 0 per il valore scelto (albume → grassi): l'opzione è disattivata, mai una divisione per zero o "Infinity g".
2. Candidato con 0 per il valore scelto: in lista ma grigio e non selezionabile; cambiando valore mantenuto con un sostituto già scelto che ora vale 0, il sostituto si azzera.
3. Dopo la sostituzione il campo grammi mostra i grammi nuovi, non il buffer digitato prima (`grammiInputPerRiga`), e un vecchio errore della riga sparisce.
4. Casella "anche nelle altre righe": le righe da toccare si individuano **prima** di modificare la prima, altrimenti la riga di partenza, già cambiata, non fa più da confronto.
5. Riaprire il popup su un'altra riga riparte pulito (ricerca vuota, nessun sostituto, Kcal o primo valore disponibile, casella spenta).

Senza test automatici (vedi sopra), questi cinque punti si provano a mano nel Task 5.

---

### Task 1: Calcoli della sostituzione

**Files:**
- Create: `frontend-professionisti/src/utils/sostituzioneAlimento.ts`

**Interfaces:**
- Produces:
  ```ts
  export type ValoreMantenuto = 'kcal' | 'proteine' | 'carboidrati' | 'grassi'
  export const VALORI_MANTENIBILI: readonly { valore: ValoreMantenuto; etichetta: string; unita: 'kcal' | 'g'; nomeAssente: string }[]
  //  kcal → 'Kcal', 'kcal', 'kcal'; proteine → 'Proteine', 'g', 'proteine'; carboidrati → 'Carboidrati', 'g', 'carboidrati'; grassi → 'Grassi', 'g', 'grassi'
  /** Dati di un alimento in una riga del piano, valori per 100 g (stessa forma dell'evento `aggiunto` di AggiungiAlimentoDialog). */
  export interface DatiAlimentoRiga { alimentoId: string | null; nome: string; kcal100g: number; proteine100g: number; carboidrati100g: number; grassi100g: number; zuccheri100g: number | null; fibre100g: number | null; ferro100mg: number | null; calcio100mg: number | null; acqua100g: number | null }
  export interface RigaAlimento extends DatiAlimentoRiga { grammi: number }
  export function datiRigaDaAlimento(alimento: Alimento): DatiAlimentoRiga   // da '@/api/alimenti'
  export function valoriRiga(riga: RigaAlimento): Record<ValoreMantenuto, number>
  export function valorePer100g(dati: DatiAlimentoRiga, chiave: ValoreMantenuto): number
  export function grammiEquivalenti(valoreDaMantenere: number, valorePer100gSostituto: number): number | null
  export function primoValoreDisponibile(riga: RigaAlimento): ValoreMantenuto | null
  export function differenze(partenza: Record<ValoreMantenuto, number>, sostituto: Record<ValoreMantenuto, number>, mantenuto: ValoreMantenuto): { chiave: ValoreMantenuto; delta: number }[]
  export function stessoAlimento(a: DatiAlimentoRiga, b: DatiAlimentoRiga): boolean
  export function applicaSostituzione(riga: RigaAlimento, sostituto: DatiAlimentoRiga, mantenuto: ValoreMantenuto): boolean
  ```

- [ ] **Step 1: Implementare il modulo**
  - `datiRigaDaAlimento`: la conversione oggi in `selezionaRisultato` di `AggiungiAlimentoDialog.vue:113-128` (fattore `100 / quantitaG`, i facoltativi restano `null`), senza `grammi`.
  - `valoriRiga`: i quattro valori ai grammi della riga, come `macroRiga` in `PianoAlimentareFormView.vue:739`.
  - `grammiEquivalenti`: `null` se uno dei due argomenti è `<= 0` o non finito; altrimenti `Math.max(1, Math.round(valoreDaMantenere / valorePer100gSostituto * 100))`.
  - `primoValoreDisponibile`: il primo in ordine kcal, proteine, carboidrati, grassi con `valoriRiga(riga)[chiave] > 0`; `null` se nessuno.
  - `differenze`: per le tre chiavi diverse da `mantenuto`, `delta = Math.round(sostituto[k]) - Math.round(partenza[k])` (stesso arrotondamento della tabella); escluse quelle con `delta === 0`.
  - `stessoAlimento`: se entrambi hanno `alimentoId`, confronto degli id; se entrambi `null`, confronto dei nomi; altrimenti `false`.
  - `applicaSostituzione`: calcola i grammi su `valoriRiga(riga)[mantenuto]` e `valorePer100g(sostituto, mantenuto)`; se `null` lascia la riga com'è e restituisce `false`; altrimenti `Object.assign(riga, sostituto, { grammi })` (muta in place: la riga tiene `clientId` e posizione) e `true`.

- [ ] **Step 2: Verificare**

Run: `npx vue-tsc --noEmit`
Expected: nessun errore.

- [ ] **Step 3: Staging**

```bash
git add frontend-professionisti/src/utils/sostituzioneAlimento.ts
```

---

### Task 2: Ricerca condivisa e selettore con opzioni disattivate

**Files:**
- Create: `frontend-professionisti/src/composables/useRicercaAlimenti.ts`
- Modify: `frontend-professionisti/src/components/pianiAlimentari/AggiungiAlimentoDialog.vue` (righe 41-75 e 113-128)
- Modify: `frontend-professionisti/src/components/ui/SelettoreSegmentato.vue`
- Test esistente: `frontend-professionisti/src/components/pianiAlimentari/AggiungiAlimentoDialog.spec.ts` deve restare verde.

**Interfaces:**
- Consumes: `datiRigaDaAlimento` (Task 1).
- Produces:
  ```ts
  export function useRicercaAlimenti(): { query: Ref<string>; risultati: Ref<Alimento[]>; azzera(): void }
  ```
  `SelettoreSegmentato`: le `opzioni` accettano un campo facoltativo `disabilitata?: boolean`.

- [ ] **Step 1: Composable**: sposta in `useRicercaAlimenti` il watch su `query` con debounce di 300 ms, `eseguiRicerca` (testo vuoto → `[]`; altrimenti `cerca({ ricerca, ordinaPer: 'pertinenza', dimensione: 30 })`) e il `clearTimeout` in `onUnmounted`. `azzera()` svuota `query` e `risultati` e annulla il timer.
- [ ] **Step 2: `AggiungiAlimentoDialog`** usa il composable (`resetStato` chiama `azzera()`) e `selezionaRisultato` emette `{ ...datiRigaDaAlimento(alimento), grammi: 100 }`. Nessun cambiamento visibile.
- [ ] **Step 3: `SelettoreSegmentato`**: `:disabled="o.disabilitata"` su `RadioGroupItem`, con `data-disabled:cursor-not-allowed data-disabled:opacity-40 data-disabled:hover:text-(--fg3)`. Gli usi attuali (`AgendaToolbar`, `VistaListaToggle`) non cambiano.
- [ ] **Step 4: Verificare**

Run: `npx vue-tsc --noEmit` e `npx vitest run`
Expected: nessun errore di tipo; suite verde (369/369), compreso `AggiungiAlimentoDialog.spec.ts`.

- [ ] **Step 5: Staging** dei tre file.

---

### Task 3: Dialog "Sostituisci alimento"

**Files:**
- Create: `frontend-professionisti/src/components/pianiAlimentari/SostituisciAlimentoDialog.vue`

**Interfaces:**
- Consumes: Task 1 (tutto), `useRicercaAlimenti` e `SelettoreSegmentato` (Task 2), `nomeAlimentoLeggibile` (`@/utils/testo`).
- Produces:
  ```ts
  defineProps<{ open: boolean; riga: RigaAlimento | null; altreRigheUguali: number }>()
  defineEmits<{
    'update:open': [valore: boolean]
    sostituito: [sostituto: DatiAlimentoRiga, mantenuto: ValoreMantenuto, ancheAltreRighe: boolean]
  }>()
  ```

- [ ] **Step 1: Prima di scrivere il markup**, usa la skill `frontend-design` sul popup, dentro il sistema esistente (sezione "Design visivo" della spec): l'elemento forte è il confronto tra "Da sostituire" e "Sostituto", impaginate uguali con le colonne Kcal / Prot. / Carb. / Grassi allineate e la colonna del valore mantenuto evidenziata (`bg-(--mint)` / `text-(--green-d)`) in entrambe; il resto sobrio.
- [ ] **Step 2: Implementare il componente.**
  - Contenitore come `AggiungiAlimentoDialog` (`DialogContent` ancorato in alto: `sm:top-[10dvh] sm:max-h-[calc(90dvh-1rem)] sm:translate-y-0`, `sm:max-w-lg` o più largo se il confronto lo chiede), titolo "Sostituisci alimento", focus sul campo di ricerca all'apertura.
  - All'apertura (`watch` su `open` → `true`): `azzera()`, nessun sostituto, casella spenta, `mantenuto = 'kcal'` se disponibile, altrimenti `primoValoreDisponibile(riga)`.
  - Sezione "Da sostituire": nome leggibile, grammi, `valoriRiga(riga)` arrotondati.
  - "Mantieni uguale": `SelettoreSegmentato` con `VALORI_MANTENIBILI`, `disabilitata` dove `valoriRiga(riga)[chiave] <= 0`, `prefissoTest="mantieni"`. Se `primoValoreDisponibile` è `null`: al posto di selettore e ricerca, il testo "Questo alimento non ha kcal né macronutrienti: non c'è un valore da mantenere."
  - Ricerca: `Input` con placeholder "Cerca per nome o categoria…", stessi messaggi vuoti di `AggiungiAlimentoDialog` (righe 193-211). Ogni risultato è un `<button>` riga (`data-test="risultato-sostituzione"`) con nome, categoria e, a destra, `"{grammi} g · {valore} {unità} {nomeAssente abbreviato}"`; se `grammiEquivalenti` è `null`: `disabled`, `opacity-50`, testo "non ha {nomeAssente}". Il risultato scelto resta evidenziato.
  - Sostituto (`datiRigaDaAlimento(risultato)` + grammi calcolati): stessa impaginazione della riga di partenza; sotto, `differenze(...)` come "+8 kcal · −1 g prot." (segno meno tipografico `−`). Cambiando `mantenuto`, i grammi si ricalcolano; se ora sono `null`, il sostituto si azzera.
  - Casella (shadcn `Checkbox` + `Label`), solo se `altreRigheUguali > 0`: "Sostituisci anche nelle altre {N} righe con «{nome leggibile}»" (con N = 1: "nell'altra riga").
  - Footer: `Button variant="neutral"` "Annulla" con `X`; `Button` "Sostituisci" con l'icona scelta per la riga, `disabled` senza sostituto; al clic emette `sostituito` e `update:open(false)`.
- [ ] **Step 3: Verificare**

Run: `npx vue-tsc --noEmit`
Expected: nessun errore.

- [ ] **Step 4: Staging** del file.

---

### Task 4: Collegamento nell'editor del piano

**Files:**
- Modify: `frontend-professionisti/src/views/pianiAlimentari/PianoAlimentareFormView.vue` (righe ~916-944 per lo stato, 1857-1863 per la riga mobile, 1921-1925 per la riga della tabella, in fondo al template accanto a `AggiungiAlimentoDialog`)

**Interfaces:**
- Consumes: `SostituisciAlimentoDialog` (Task 3), `stessoAlimento`, `applicaSostituzione` (Task 1).

- [ ] **Step 1: Stato e handler**
  - `rigaInSostituzione = ref<{ pastoClientId: string; rigaClientId: string } | null>(null)` e `sostituzioneAperta = ref(false)`; `apriSostituisci(pastoClientId, rigaClientId)`.
  - `rigaDaSostituire` (computed: la `RigaLocale` puntata) e `altreRigheUguali` (computed: righe di **tutti** i `pastiLocali` con `stessoAlimento(r, rigaDaSostituire)`, esclusa lei).
  - `onSostituito(sostituto, mantenuto, ancheAltreRighe)`: raccoglie **prima** le righe bersaglio (solo la riga, oppure lei più `altreRigheUguali`), poi per ognuna `applicaSostituzione` e, se `true`, toglie il suo `clientId` da `grammiInputPerRiga`, `erroriGrammi` e `grammiNegativi`.
- [ ] **Step 2: Pulsante sulla riga**, in tabella e su mobile, prima della X e solo se `!solaLettura`: `Button variant="ghost" size="icon-sm"`, icona `ArrowLeftRight` (`:size="14"`), `aria-label` e `title` "Sostituisci alimento", `data-test="sostituisci-alimento"`. Allargare la colonna azioni della tabella (`th.w-10`) quanto basta per due icone.
- [ ] **Step 3: Montare il dialog** accanto ad `AggiungiAlimentoDialog`, con `v-model:open="sostituzioneAperta"`, `:riga="rigaDaSostituire ?? null"`, `:altre-righe-uguali="altreRigheUguali.length"`, `@sostituito="onSostituito"`.
- [ ] **Step 4: Verificare**

Run: `npx vue-tsc --noEmit` e `npx vitest run`
Expected: nessun errore di tipo; suite verde.

- [ ] **Step 5: Staging** del file.

---

### Task 5: Prova nel browser e wiki

**Files:**
- Modify: `wiki/stato.md`, `wiki/log.md`, `wiki/domande-aperte.md` (la voce "Sostituisci alimento" passa a risolta, con link alla spec)

- [ ] **Step 1: Prova a mano** sul backend e Vite di prova (`hexis_test`, porte 8081/5174), con un piano attivo e un piano in bozza, desktop e mobile (375 px), chiaro e scuro:
  - sostituzione semplice per ciascuno dei quattro valori, controllando il calcolo a mano su un caso (es. 50 g di pane per carboidrati);
  - i cinque punti della Review Focus;
  - casella accesa con lo stesso alimento in due giorni con grammi diversi: grammi diversi anche nel sostituto;
  - piano concluso: nessun pulsante; dopo la sostituzione, uscire senza salvare chiede conferma; "Salva" e ricarica mostrano la riga nuova.
- [ ] **Step 2: Screenshot** del popup con un sostituto scelto (chiaro e scuro) da mostrare ad Andrea.
- [ ] **Step 3: Wiki**: voce in `stato.md` e in `log.md` (`## [2026-11-28] handoff | Sostituisci alimento`); `api-contracts.md` non cambia (nessuna API nuova).
- [ ] **Step 4: Staging** di tutto, poi segnalare ad Andrea che è pronto da committare.
