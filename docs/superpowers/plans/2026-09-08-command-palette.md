# Command palette globale (⌘K) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Aggiungere a `frontend-professionisti` una command palette globale (bottone in header + scorciatoia Ctrl/Cmd+K) che mostra le azioni di creazione più comuni e cerca pazienti per nome.

**Architecture:** Un nuovo componente autonomo `CommandPalette.vue` (trigger + overlay, stato interno, nessuna prop/emit) montato una volta in `AppHeader.vue`. Riusa l'endpoint di ricerca pazienti già esistente (`cerca()` in `api/pazienti.ts`). Due piccole aggiunte a viste esistenti (`AlimentiListView.vue`, `PazientiListView.vue`) per leggere un parametro dalla query string e completare i due deep-link generati dalla palette ("Nuovo alimento", "Vedi tutti i risultati").

**Tech Stack:** Vue 3 `<script setup>` + TypeScript, Vue Router, Tailwind CSS + shadcn-vue (`Input`), `@lucide/vue` per le icone.

**Spec:** [docs/superpowers/specs/2026-09-08-command-palette-design.md](../specs/2026-09-08-command-palette-design.md)

## Global Constraints

- Solo `frontend-professionisti` — nessuna modifica a `frontend-cliente`.
- Nessuna ricerca alimenti nella palette — solo l'azione "Nuovo alimento".
- Nessuna navigazione da tastiera tra i risultati in questo giro — solo mouse/click.
- Nessuna modifica al backend — si riusa `GET /pazienti/ricerca` esistente via `cerca()`.
- **Niente test automatici in questo piano**: Andrea ha chiesto esplicitamente di essere avvisato prima che vengano scritti test per questa feature, potrebbe scriverli lui stesso. Ogni task si verifica quindi con `npm run test` (suite esistente, deve restare verde), `npm run build` (typecheck) e verifica manuale nel browser — non con test nuovi.

---

### Task 1: Componente `CommandPalette.vue` e montaggio in `AppHeader.vue`

**Files:**
- Create: `frontend-professionisti/src/components/CommandPalette.vue`
- Modify: `frontend-professionisti/src/components/AppHeader.vue`

**Interfaces:**
- Consumes: `cerca(criteri: CriteriRicercaPazienti): Promise<PaginaPazienti>` e `type Paziente` da `@/api/pazienti` (già esistenti, non modificati in questo task).
- Produces: componente `CommandPalette` (nessuna prop, nessun emit) — utilizzabile come `<CommandPalette />` da qualunque punto dell'albero dei componenti di `frontend-professionisti`.

- [ ] **Step 1: Creare `CommandPalette.vue`**

```vue
<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { Search, UserPlus, ClipboardPlus, CalendarPlus, FileText, Apple } from '@lucide/vue'
import { Input } from '@/components/ui/input'
import { cerca, type Paziente } from '@/api/pazienti'

interface AzionePalette {
  id: string
  etichetta: string
  icona: typeof Search
  href?: string
  disabilitata?: boolean
}

const AZIONI: AzionePalette[] = [
  { id: 'nuovo-paziente', etichetta: 'Nuovo paziente', icona: UserPlus, href: '/pazienti/nuovo' },
  { id: 'nuova-visita', etichetta: 'Nuova visita', icona: ClipboardPlus, href: '/pazienti/visite/nuova' },
  { id: 'nuovo-appuntamento', etichetta: 'Nuovo appuntamento', icona: CalendarPlus, disabilitata: true },
  { id: 'nuovo-piano-alimentare', etichetta: 'Nuovo piano alimentare', icona: FileText, disabilitata: true },
  { id: 'nuovo-alimento', etichetta: 'Nuovo alimento', icona: Apple, href: '/alimenti?azione=nuovo' },
]

const router = useRouter()

const aperta = ref(false)
const testo = ref('')
const inputRef = ref<InstanceType<typeof Input> | null>(null)

const pazientiRisultati = ref<Paziente[]>([])
const pazientiTotale = ref(0)

let debounceHandle: ReturnType<typeof setTimeout> | undefined

const testoNormalizzato = computed(() => testo.value.trim().toLowerCase())

const azioniFiltrate = computed(() =>
  AZIONI.filter((azione) => azione.etichetta.toLowerCase().includes(testoNormalizzato.value)),
)

const nessunRisultato = computed(
  () => testoNormalizzato.value !== '' && azioniFiltrate.value.length === 0 && pazientiRisultati.value.length === 0,
)

watch(testo, (valore) => {
  clearTimeout(debounceHandle)
  const query = valore.trim()
  if (query === '') {
    pazientiRisultati.value = []
    pazientiTotale.value = 0
    return
  }
  debounceHandle = setTimeout(async () => {
    try {
      const risultato = await cerca({ ricerca: query, dimensione: 5, archiviato: false })
      pazientiRisultati.value = risultato.contenuto
      pazientiTotale.value = risultato.totaleElementi
    } catch {
      pazientiRisultati.value = []
      pazientiTotale.value = 0
    }
  }, 300)
})

function apri() {
  aperta.value = true
  testo.value = ''
  pazientiRisultati.value = []
  pazientiTotale.value = 0
  nextTick(() => inputRef.value?.$el?.focus())
}

function chiudi() {
  aperta.value = false
  clearTimeout(debounceHandle)
}

function onKeydown(evento: KeyboardEvent) {
  if ((evento.metaKey || evento.ctrlKey) && evento.key.toLowerCase() === 'k') {
    evento.preventDefault()
    if (aperta.value) chiudi()
    else apri()
  } else if (evento.key === 'Escape' && aperta.value) {
    chiudi()
  }
}

onMounted(() => document.addEventListener('keydown', onKeydown))
onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
  clearTimeout(debounceHandle)
})

function selezionaAzione(azione: AzionePalette) {
  if (azione.disabilitata || !azione.href) return
  chiudi()
  router.push(azione.href)
}

function selezionaPaziente(paziente: Paziente) {
  chiudi()
  router.push(`/pazienti/${paziente.id}`)
}

function vediTuttiRisultati() {
  const query = testo.value.trim()
  chiudi()
  router.push({ path: '/pazienti', query: { ricerca: query } })
}

function inizialiPaziente(paziente: Paziente) {
  return `${paziente.nome[0]}${paziente.cognome[0]}`
}
</script>

<template>
  <button
    type="button"
    class="flex items-center gap-2.5 rounded-lg border border-(--bd2) bg-(--surf) px-3 py-2 text-(--fg4) transition-colors hover:border-(--sage)"
    @click="apri"
  >
    <Search :size="14" />
    <span class="hidden text-xs font-medium lg:inline">Cerca ovunque</span>
    <kbd class="hidden rounded-md border border-(--bd2) px-1.5 py-0.5 text-[10px] font-semibold text-(--fg3) lg:inline">Ctrl K</kbd>
  </button>

  <Teleport to="body">
    <div
      v-if="aperta"
      class="fixed inset-0 z-[60] flex items-start justify-center bg-black/30 pt-24 backdrop-blur-[2px]"
      @click="chiudi"
    >
      <div
        class="w-full max-w-[520px] overflow-hidden rounded-2xl border border-(--bd) bg-(--surf) shadow-2xl"
        @click.stop
      >
        <div class="flex items-center gap-2.5 border-b border-(--div) px-4 py-3.5">
          <Search :size="16" class="text-(--fg3)" />
          <Input
            ref="inputRef"
            v-model="testo"
            type="text"
            placeholder="Cerca pazienti o un'azione…"
            class="h-auto flex-1 border-0 bg-transparent p-0 text-sm shadow-none focus-visible:ring-0"
          />
          <kbd class="rounded-md border border-(--bd2) px-1.5 py-0.5 text-[10px] font-semibold text-(--fg3)">Esc</kbd>
        </div>

        <div class="max-h-80 overflow-y-auto p-2">
          <div v-if="azioniFiltrate.length > 0">
            <div class="px-2.5 pb-1 pt-2 text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Azioni</div>
            <button
              v-for="azione in azioniFiltrate"
              :key="azione.id"
              type="button"
              :disabled="azione.disabilitata"
              class="flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-left text-sm text-(--fg) transition-colors hover:bg-(--soft) disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent"
              @click="selezionaAzione(azione)"
            >
              <span class="flex h-6 w-6 items-center justify-center rounded-md bg-(--mint) text-(--green)">
                <component :is="azione.icona" :size="13" />
              </span>
              {{ azione.etichetta }}
            </button>
          </div>

          <div v-if="pazientiRisultati.length > 0" class="mt-1">
            <div class="px-2.5 pb-1 pt-2 text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Pazienti</div>
            <button
              v-for="paziente in pazientiRisultati"
              :key="paziente.id"
              type="button"
              class="flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-left text-sm text-(--fg) transition-colors hover:bg-(--soft)"
              @click="selezionaPaziente(paziente)"
            >
              <span class="flex h-6 w-6 items-center justify-center rounded-md bg-(--mint) text-[10px] font-bold text-(--green)">
                {{ inizialiPaziente(paziente) }}
              </span>
              {{ paziente.nome }} {{ paziente.cognome }}
            </button>
            <button
              v-if="pazientiTotale > pazientiRisultati.length"
              type="button"
              class="w-full rounded-lg px-2.5 py-2 text-left text-xs font-semibold text-(--green) transition-colors hover:bg-(--soft)"
              @click="vediTuttiRisultati"
            >
              Vedi tutti i risultati ({{ pazientiTotale }})
            </button>
          </div>

          <div v-if="nessunRisultato" class="p-6 text-center text-xs text-(--fg3)">
            Nessun risultato per «{{ testo.trim() }}»
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>
```

- [ ] **Step 2: Montare il componente in `AppHeader.vue`**

In `frontend-professionisti/src/components/AppHeader.vue`, aggiungere l'import e l'elemento nel gruppo di destra dell'header, prima della campanella notifiche:

```diff
 import { Menu, Bell, ChevronDown, LogOut, Moon } from '@lucide/vue'
+import CommandPalette from './CommandPalette.vue'
 import { Button } from '@/components/ui/button'
```

```diff
     <div class="flex items-center gap-2.5">
+      <CommandPalette />
+
       <Button variant="neutral" size="icon" class="relative" aria-label="Notifiche">
         <Bell :size="16" />
```

- [ ] **Step 3: Verificare che la suite esistente resti verde**

Run: `npm run test` (nella cartella `frontend-professionisti`)
Expected: tutti i test passano, incluso `AppHeader.spec.ts` (il nuovo bottone non interferisce con le assertion esistenti).

- [ ] **Step 4: Typecheck**

Run: `npm run build` (nella cartella `frontend-professionisti`)
Expected: nessun errore TypeScript, build completata.

- [ ] **Step 5: Verifica manuale nel browser**

Run: `npm run dev`, apri l'app, effettua il login come professionista.
- Premi `Ctrl+K` (o clicca il bottone "Cerca ovunque" in header): la palette si apre con le 5 azioni visibili e il focus già sul campo di ricerca.
- Digita il nome di un paziente esistente: dopo ~300ms compaiono i risultati (max 5) sotto "Pazienti"; se ce ne sono di più, appare "Vedi tutti i risultati (N)".
- Digita un testo senza corrispondenze (es. "zzz"): compare "Nessun risultato per «zzz»".
- Premi `Esc`, o clicca fuori dal pannello: la palette si chiude.
- Clicca su "Nuovo paziente": la palette si chiude e naviga a `/pazienti/nuovo`.

- [ ] **Step 6: Commit**

```bash
git add frontend-professionisti/src/components/CommandPalette.vue frontend-professionisti/src/components/AppHeader.vue
git commit -m "feat: aggiunge command palette globale (Ctrl/Cmd+K)"
```

---

### Task 2: Deep-link "Nuovo alimento" — apertura automatica del dialog di creazione

**Files:**
- Modify: `frontend-professionisti/src/views/alimenti/AlimentiListView.vue:1-11` (import), area vicino a `onMounted(carica)` (riga 83)

**Interfaces:**
- Consumes: `apriCreazione(): void` (funzione già esistente in questo file, riga 124-127) e `useRoute()` da `vue-router`.
- Produces: nessuna nuova interfaccia pubblica — comportamento aggiuntivo sulla stessa vista.

- [ ] **Step 1: Aggiungere l'apertura automatica del dialog da query string**

In `frontend-professionisti/src/views/alimenti/AlimentiListView.vue`:

```diff
 import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
+import { useRoute } from 'vue-router'
 import { toast } from 'vue-sonner'
```

```diff
 onMounted(carica)
+
+const route = useRoute()
+onMounted(() => {
+  if (route.query.azione === 'nuovo') apriCreazione()
+})
```

- [ ] **Step 2: Verificare che la suite esistente resti verde**

Run: `npm run test`
Expected: tutti i test passano, incluso `AlimentiListView.spec.ts` (nessuna route con `?azione=nuovo` è usata nei test esistenti, quindi il comportamento di default non cambia).

- [ ] **Step 3: Verifica manuale nel browser**

Con `npm run dev` attivo, naviga manualmente a `/alimenti?azione=nuovo`: il dialog di creazione alimento deve aprirsi automaticamente. Naviga a `/alimenti` (senza query): il dialog non deve aprirsi.

- [ ] **Step 4: Commit**

```bash
git add frontend-professionisti/src/views/alimenti/AlimentiListView.vue
git commit -m "feat: apre automaticamente il dialog di creazione alimento da ?azione=nuovo"
```

---

### Task 3: Deep-link "Vedi tutti i risultati" — precompilazione ricerca pazienti

**Files:**
- Modify: `frontend-professionisti/src/views/pazienti/PazientiListView.vue:1-2` (import), area vicino a `onMounted(carica)` (riga 128)

**Interfaces:**
- Consumes: `ricercaInput: Ref<string>` (già esistente in questo file, riga 48) e `useRoute()` da `vue-router`.
- Produces: nessuna nuova interfaccia pubblica — comportamento aggiuntivo sulla stessa vista.

- [ ] **Step 1: Precompilare la ricerca da query string**

In `frontend-professionisti/src/views/pazienti/PazientiListView.vue`:

```diff
 import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
+import { useRoute } from 'vue-router'
 import type { AcceptableValue } from 'reka-ui'
```

```diff
 onMounted(carica)
+
+const route = useRoute()
+onMounted(() => {
+  const ricercaQuery = route.query.ricerca
+  if (typeof ricercaQuery === 'string' && ricercaQuery !== '') {
+    ricercaInput.value = ricercaQuery
+  }
+})
```

Nota: questo genera un primo caricamento senza filtro seguito, ~300ms dopo, da uno con il filtro applicato (il debounce esistente non viene bypassato) — comportamento accettato nello spec, nessuna ottimizzazione aggiuntiva richiesta.

- [ ] **Step 2: Verificare che la suite esistente resti verde**

Run: `npm run test`
Expected: tutti i test passano, incluso `PazientiListView.spec.ts` (nessuna route con `?ricerca=...` è usata nei test esistenti, quindi il comportamento di default non cambia).

- [ ] **Step 3: Verifica manuale nel browser**

Con `npm run dev` attivo, naviga manualmente a `/pazienti?ricerca=<nome di un paziente esistente>`: il campo di ricerca deve risultare precompilato e, dopo poco, la tabella filtrata di conseguenza. Verifica anche il flusso end-to-end: apri la palette, digita un nome con più di 5 corrispondenze, clicca "Vedi tutti i risultati" e controlla di arrivare sulla lista pazienti già filtrata.

- [ ] **Step 4: Commit**

```bash
git add frontend-professionisti/src/views/pazienti/PazientiListView.vue
git commit -m "feat: precompila la ricerca pazienti da ?ricerca= (link \"vedi tutti\" della palette)"
```
