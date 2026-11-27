<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted, useId } from 'vue'
import { useRouter } from 'vue-router'
import { FocusScope } from 'reka-ui'
import { Apple, CalendarOff, CalendarPlus, ClipboardPlus, RotateCcw, Search, SearchX, StickyNote, UserPlus, Utensils } from '@lucide/vue'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { cerca, type Paziente } from '@/api/pazienti'
import { classiAvatarPaziente, inizialiPaziente } from '@/utils/paziente'

interface AzionePalette {
  id: string
  etichetta: string
  alias?: string
  icona: typeof Search
  href?: string
  disabilitata?: boolean
}

const AZIONI: AzionePalette[] = [
  { id: 'nuovo-paziente', etichetta: 'Nuovo paziente', icona: UserPlus, href: '/pazienti/nuovo' },
  { id: 'nuovo-appuntamento', etichetta: 'Nuovo appuntamento', icona: CalendarPlus, href: '/agenda?nuovo=visita' },
  { id: 'nuovo-promemoria', etichetta: 'Nuovo promemoria', icona: StickyNote, href: '/agenda?nuovo=promemoria' },
  { id: 'blocca-fascia-oraria', etichetta: 'Aggiungi indisponibilità', icona: CalendarOff, href: '/agenda?nuovo=blocco' },
  { id: 'nuova-visita', etichetta: 'Nuova visita', icona: ClipboardPlus, href: '/pazienti/visite/nuova' },
  { id: 'nuovo-piano-alimentare', etichetta: 'Nuovo piano', alias: 'nuovo piano alimentare', icona: Utensils, href: '/piani-alimentari/nuovo' },
  { id: 'nuovo-alimento', etichetta: 'Nuovo alimento', icona: Apple, href: '/alimenti?azione=nuovo' },
]

const router = useRouter()

const aperta = ref(false)
const testo = ref('')
const idRisultati = useId()
const listaRisultati = ref<HTMLElement | null>(null)
const idSelezionato = ref<string>()

const pazientiRisultati = ref<Paziente[]>([])
const pazientiTotale = ref(0)
const ricercaInCorso = ref(false)
const erroreRicerca = ref(false)
const tentativoRicerca = ref(0)
const idCampoRicerca = useId()

const testoNormalizzato = computed(() => testo.value.trim().toLowerCase())

const azioniFiltrate = computed(() =>
  AZIONI.filter((azione) =>
    (azione.alias ?? azione.etichetta).toLowerCase().includes(testoNormalizzato.value),
  ),
)

const nessunRisultato = computed(
  () =>
    testoNormalizzato.value !== '' &&
    !ricercaInCorso.value &&
    !erroreRicerca.value &&
    azioniFiltrate.value.length === 0 &&
    pazientiRisultati.value.length === 0,
)

watch([testo, aperta, tentativoRicerca], ([valore, visibile], _, onCleanup) => {
  idSelezionato.value = undefined
  erroreRicerca.value = false
  pazientiRisultati.value = []
  pazientiTotale.value = 0
  const query = valore.trim()
  ricercaInCorso.value = visibile && query !== ''
  if (!ricercaInCorso.value) return

  let annullata = false
  const timer = setTimeout(async () => {
    try {
      const risultato = await cerca({ ricerca: query, dimensione: 5, archiviato: false })
      if (annullata) return
      pazientiRisultati.value = risultato.contenuto
      pazientiTotale.value = risultato.totaleElementi
    } catch {
      if (annullata) return
      erroreRicerca.value = true
    } finally {
      if (!annullata) ricercaInCorso.value = false
    }
  }, 300)
  onCleanup(() => {
    annullata = true
    clearTimeout(timer)
  })
}, { flush: 'sync' })

function apri() {
  testo.value = ''
  aperta.value = true
}

function chiudi() {
  aperta.value = false
}

function riprovaRicerca() {
  document.getElementById(idCampoRicerca)?.focus()
  tentativoRicerca.value++
}

function navigaRisultati(evento: KeyboardEvent) {
  if (evento.isComposing || !['ArrowDown', 'ArrowUp', 'Enter'].includes(evento.key)) return

  const opzioni = Array.from(listaRisultati.value?.querySelectorAll<HTMLButtonElement>('[role="option"]:not(:disabled)') ?? [])
  if (!opzioni.length) return

  const indice = opzioni.findIndex((opzione) => opzione.id === idSelezionato.value)
  if (evento.key === 'Enter') {
    if (indice >= 0) {
      evento.preventDefault()
      opzioni[indice]?.click()
    }
    return
  }

  evento.preventDefault()
  const passo = evento.key === 'ArrowDown' ? 1 : -1
  const partenza = indice < 0 && passo < 0 ? 0 : indice
  const opzione = opzioni[(partenza + passo + opzioni.length) % opzioni.length]
  idSelezionato.value = opzione?.id
  opzione?.scrollIntoView?.({ block: 'nearest' })
}

function evidenziaConPuntatore(evento: PointerEvent) {
  if (evento.pointerType !== 'mouse' || !(evento.target instanceof Element)) return
  const opzione = evento.target.closest<HTMLButtonElement>('button[role="option"]:enabled')
  if (opzione) idSelezionato.value = opzione.id
}

function onKeydown(evento: KeyboardEvent) {
  if (evento.defaultPrevented || evento.isComposing) return
  if ((evento.metaKey || evento.ctrlKey) && evento.key.toLowerCase() === 'k') {
    evento.preventDefault()
    if (aperta.value) chiudi()
    else apri()
  } else if (evento.key === 'Escape' && aperta.value) {
    evento.preventDefault()
    chiudi()
  }
}

onMounted(() => document.addEventListener('keydown', onKeydown))
onUnmounted(() => document.removeEventListener('keydown', onKeydown))

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

</script>

<template>
  <button
    type="button"
    aria-label="Cerca ovunque"
    data-test="command-palette-trigger"
    class="flex items-center gap-2.5 rounded-lg border border-(--bd2) bg-(--surf) px-3 py-2 text-(--fg3) transition-colors hover:border-(--sage)"
    @click="apri"
  >
    <Search :size="14" />
    <span class="hidden text-xs font-medium lg:inline">Cerca ovunque</span>
    <kbd class="hidden rounded-md border border-(--bd2) px-1.5 py-0.5 text-[10px] font-semibold text-(--fg3) lg:inline">Ctrl K</kbd>
  </button>

  <Teleport to="body">
    <div
      v-if="aperta"
      class="fixed inset-0 z-60 flex items-start justify-center bg-black/30 px-4 pt-24 backdrop-blur-[2px]"
      @click="chiudi"
    >
      <FocusScope
        trapped
        loop
        role="dialog"
        aria-modal="true"
        aria-label="Cerca ovunque"
        class="w-full max-w-130 overflow-hidden rounded-2xl border border-(--bd) bg-(--surf) shadow-2xl"
        @click.stop
      >
        <div class="flex items-center gap-2.5 border-b border-(--div) px-4 py-3.5">
          <Search :size="16" class="text-(--fg3)" />
          <Input
            :id="idCampoRicerca"
            v-model="testo"
            type="text"
            role="combobox"
            aria-label="Cerca pazienti o un'azione"
            aria-autocomplete="list"
            aria-expanded="true"
            :aria-controls="idRisultati"
            :aria-activedescendant="idSelezionato"
            @keydown="navigaRisultati"
            autocomplete="off" placeholder="Cerca pazienti o un'azione…"
            class="flex-1 border-0 bg-transparent shadow-none focus-visible:ring-0"
          />
          <Button
            type="button"
            variant="outline"
            size="xs"
            aria-label="Chiudi ricerca"
            class="h-auto rounded-md border-(--bd2) px-1.5 py-0.5 text-[10px] font-semibold text-(--fg3)"
            @click="chiudi"
          >
            <kbd>Esc</kbd>
          </Button>
        </div>

        <div :id="idRisultati" ref="listaRisultati" role="listbox" aria-label="Risultati di ricerca" class="max-h-80 overflow-y-auto p-2" @pointermove="evidenziaConPuntatore">
          <div v-if="azioniFiltrate.length > 0" role="group" aria-label="Azioni">
            <div aria-hidden="true" class="px-2.5 pb-1 pt-2 text-xs font-medium text-(--fg3)">Azioni</div>
            <button
              v-for="azione in azioniFiltrate"
              :key="azione.id"
              :id="`${idRisultati}-azione-${azione.id}`"
              role="option"
              tabindex="-1"
              :aria-selected="idSelezionato === `${idRisultati}-azione-${azione.id}`"
              type="button"
              :disabled="azione.disabilitata"
              class="risultato flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-left text-sm text-(--fg) transition-colors disabled:cursor-not-allowed disabled:opacity-40"
              @click="selezionaAzione(azione)"
            >
              <span class="flex h-6 w-6 items-center justify-center rounded-md bg-(--mint) text-(--green-d)">
                <component :is="azione.icona" :size="14" />
              </span>
              {{ azione.etichetta }}
            </button>
          </div>

          <div v-if="pazientiRisultati.length > 0" role="group" aria-label="Pazienti" class="mt-1">
            <div aria-hidden="true" class="px-2.5 pb-1 pt-2 text-xs font-medium text-(--fg3)">Pazienti</div>
            <button
              v-for="paziente in pazientiRisultati"
              :key="paziente.id"
              :id="`${idRisultati}-paziente-${paziente.id}`"
              role="option"
              tabindex="-1"
              :aria-selected="idSelezionato === `${idRisultati}-paziente-${paziente.id}`"
              type="button"
              class="risultato flex w-full items-center gap-2.5 rounded-lg px-2.5 py-2 text-left text-sm text-(--fg) transition-colors"
              @click="selezionaPaziente(paziente)"
            >
              <span
                class="flex h-6 w-6 shrink-0 items-center justify-center rounded-full text-[10px] font-bold"
                :class="classiAvatarPaziente(paziente)"
              >
                {{ inizialiPaziente(paziente.nome, paziente.cognome) }}
              </span>
              <span class="min-w-0">
                <span class="block truncate font-medium">{{ paziente.nome }} {{ paziente.cognome }}</span>
                <span v-if="paziente.email" class="block truncate text-xs text-(--fg3)">{{ paziente.email }}</span>
              </span>
            </button>
            <button
              v-if="pazientiTotale > pazientiRisultati.length"
              :id="`${idRisultati}-tutti`"
              role="option"
              tabindex="-1"
              :aria-selected="idSelezionato === `${idRisultati}-tutti`"
              type="button"
              class="risultato w-full rounded-lg px-2.5 py-2 text-left text-xs font-semibold text-(--green) transition-colors"
              @click="vediTuttiRisultati"
            >
              Vedi tutti i risultati ({{ pazientiTotale }})
            </button>
          </div>

        </div>
        <div role="status">
          <p v-if="ricercaInCorso" class="px-6 pb-6 pt-2 text-center text-xs text-(--fg3)">Ricerca pazienti in corso…</p>
          <p v-else-if="erroreRicerca" class="px-6 pt-2 text-center text-xs text-(--danger)">
            Non è stato possibile cercare i pazienti.
          </p>
          <p v-else-if="nessunRisultato" class="flex items-center justify-center gap-2 px-6 pb-6 pt-2 text-center text-xs text-(--fg3)">
            <SearchX :size="16" class="shrink-0" aria-hidden="true" />
            <span>Nessun risultato per «{{ testo.trim() }}»</span>
          </p>
        </div>
        <div v-if="erroreRicerca" class="flex justify-center px-6 pb-4 pt-2">
          <Button type="button" variant="neutral" size="sm" @click="riprovaRicerca"><RotateCcw /> Riprova</Button>
        </div>
      </FocusScope>
    </div>
  </Teleport>
</template>

<style scoped>
.risultato:not(:disabled) {
  cursor: pointer;
}

.risultato[aria-selected='true'] {
  background-color: var(--mint);
  box-shadow: inset 0 0 0 1px var(--sage);
}
</style>
