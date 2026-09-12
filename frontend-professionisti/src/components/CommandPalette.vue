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
  { id: 'nuovo-piano-alimentare', etichetta: 'Nuovo piano alimentare', icona: FileText, href: '/piani-alimentari/nuovo' },
  { id: 'nuovo-alimento', etichetta: 'Nuovo alimento', icona: Apple, href: '/alimenti?azione=nuovo' },
]

const router = useRouter()

const aperta = ref(false)
const testo = ref('')
const inputRef = ref<InstanceType<typeof Input> | null>(null)

const pazientiRisultati = ref<Paziente[]>([])
const pazientiTotale = ref(0)
const ricercaInCorso = ref(false)

let debounceHandle: ReturnType<typeof setTimeout> | undefined

const testoNormalizzato = computed(() => testo.value.trim().toLowerCase())

const azioniFiltrate = computed(() =>
  AZIONI.filter((azione) => azione.etichetta.toLowerCase().includes(testoNormalizzato.value)),
)

const nessunRisultato = computed(
  () =>
    testoNormalizzato.value !== '' &&
    !ricercaInCorso.value &&
    azioniFiltrate.value.length === 0 &&
    pazientiRisultati.value.length === 0,
)

watch(testo, (valore) => {
  clearTimeout(debounceHandle)
  const query = valore.trim()
  if (query === '') {
    pazientiRisultati.value = []
    pazientiTotale.value = 0
    ricercaInCorso.value = false
    return
  }
  ricercaInCorso.value = true
  debounceHandle = setTimeout(async () => {
    try {
      const risultato = await cerca({ ricerca: query, dimensione: 5, archiviato: false })
      pazientiRisultati.value = risultato.contenuto
      pazientiTotale.value = risultato.totaleElementi
    } catch {
      pazientiRisultati.value = []
      pazientiTotale.value = 0
    } finally {
      ricercaInCorso.value = false
    }
  }, 300)
})

function apri() {
  aperta.value = true
  testo.value = ''
  pazientiRisultati.value = []
  pazientiTotale.value = 0
  ricercaInCorso.value = false
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
    aria-label="Cerca ovunque"
    data-test="command-palette-trigger"
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
      class="fixed inset-0 z-[60] flex items-start justify-center bg-black/30 px-4 pt-24 backdrop-blur-[2px]"
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
            class="flex-1 border-0 bg-transparent shadow-none focus-visible:ring-0"
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
