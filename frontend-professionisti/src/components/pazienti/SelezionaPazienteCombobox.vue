<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { cerca, type Paziente } from '@/api/pazienti'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Popover, PopoverContent, PopoverAnchor } from '@/components/ui/popover'
import { X } from '@lucide/vue'
import { classiAvatarPaziente, inizialiPaziente } from '@/utils/paziente'

defineProps<{ modelValue: Paziente | null; id?: string; ariaInvalid?: boolean; ariaDescribedby?: string }>()
const emit = defineEmits<{ 'update:modelValue': [paziente: Paziente | null] }>()

const testoRicerca = ref('')
const risultati = ref<Paziente[]>([])
const ricercaInCorso = ref(false)
const erroreRicerca = ref(false)
const popoverAperto = ref(false)
// Con il campo vuoto si propongono i pazienti visti più di recente: di solito si cerca uno di loro.
const recenti = ref<Paziente[]>([])
const mostraRecenti = ref(false)

async function apriRecenti() {
  if (testoRicerca.value.trim()) return
  try {
    if (recenti.value.length === 0) {
      const pagina = await cerca({ ordinaPer: 'dataUltimaVisita', direzione: 'desc', dimensione: 20, archiviato: false })
      // In PostgreSQL l'ordine decrescente mette prima i pazienti senza visite: si scartano qui.
      recenti.value = pagina.contenuto.filter((p) => p.dataUltimaVisita).slice(0, 6)
    }
    if (recenti.value.length > 0 && !testoRicerca.value.trim()) {
      mostraRecenti.value = true
      popoverAperto.value = true
    }
  } catch {
    // senza suggerimenti si cerca comunque scrivendo
  }
}

let debounceHandle: ReturnType<typeof setTimeout> | undefined

async function eseguiRicerca(testo: string) {
  if (!testo.trim()) {
    risultati.value = []
    popoverAperto.value = false
    return
  }
  mostraRecenti.value = false
  ricercaInCorso.value = true
  erroreRicerca.value = false
  try {
    const pagina = await cerca({ ricerca: testo.trim(), dimensione: 8 })
    risultati.value = pagina.contenuto
  } catch {
    erroreRicerca.value = true
    risultati.value = []
  } finally {
    ricercaInCorso.value = false
    popoverAperto.value = true
  }
}

watch(testoRicerca, (valore) => {
  clearTimeout(debounceHandle)
  debounceHandle = setTimeout(() => eseguiRicerca(valore), 300)
})

onUnmounted(() => clearTimeout(debounceHandle))

function onInput(valore: string | number) {
  testoRicerca.value = String(valore)
}

function seleziona(paziente: Paziente) {
  emit('update:modelValue', paziente)
  popoverAperto.value = false
  testoRicerca.value = ''
  risultati.value = []
}

function cambiaSelezione() {
  emit('update:modelValue', null)
  testoRicerca.value = ''
}
</script>

<template>
  <div>
    <div
      v-if="modelValue"
      class="flex items-center justify-between gap-3 rounded-xl border border-(--bd) bg-(--surf) px-4 py-3"
    >
      <div>
        <p class="font-heading text-lg italic text-(--fg)">{{ modelValue.nome }} {{ modelValue.cognome }}</p>
        <p class="text-xs text-(--fg3)">{{ modelValue.email }}</p>
      </div>
      <Button type="button" variant="neutral" size="sm" @click="cambiaSelezione">
        <X :size="14" />
        Cambia paziente
      </Button>
    </div>

    <Popover v-else v-model:open="popoverAperto">
      <PopoverAnchor as-child>
        <Input
          :id="id"
          :aria-invalid="ariaInvalid"
          :aria-describedby="ariaDescribedby"
          aria-label="Cerca paziente per nome, email o codice fiscale"
          data-test="input-cerca-paziente"
          class="bg-(--surf)"
          :model-value="testoRicerca"
          @update:model-value="onInput"
          type="text"
          autocomplete="off" placeholder="Cerca paziente per nome, email o codice fiscale…"
          @focus="apriRecenti"
          @click="apriRecenti"
        />
      </PopoverAnchor>
      <PopoverContent class="w-(--reka-popper-anchor-width) p-1" @open-auto-focus.prevent>
        <p v-if="mostraRecenti" class="px-3 pb-1 pt-2 text-xs font-medium text-(--fg3)">Visite più recenti</p>
        <p v-else-if="ricercaInCorso" class="px-3 py-2 text-xs text-(--fg3)">Ricerca in corso…</p>
        <p v-else-if="erroreRicerca" class="px-3 py-2 text-xs text-(--danger)">Ricerca non riuscita, riprova.</p>
        <p v-else-if="risultati.length === 0" class="px-3 py-2 text-xs text-(--fg3)">Nessun paziente trovato.</p>
        <button
          v-for="paziente in (mostraRecenti ? recenti : risultati)"
          :key="paziente.id"
          type="button"
          data-test="risultato-paziente"
          class="flex w-full items-center gap-2.5 rounded-lg px-3 py-2 text-left hover:bg-(--hover)"
          @click="seleziona(paziente)"
        >
          <span
            class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full text-xs font-semibold"
            :class="classiAvatarPaziente(paziente)"
          >
            {{ inizialiPaziente(paziente.nome, paziente.cognome) }}
          </span>
          <span class="flex flex-col">
            <span class="text-sm font-semibold text-(--fg)">{{ paziente.nome }} {{ paziente.cognome }}</span>
            <span class="text-xs text-(--fg3)">{{ paziente.email }}</span>
          </span>
        </button>
      </PopoverContent>
    </Popover>
  </div>
</template>
