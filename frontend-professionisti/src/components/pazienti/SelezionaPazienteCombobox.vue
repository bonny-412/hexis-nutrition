<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { cerca, type Paziente } from '@/api/pazienti'
import { Input } from '@/components/ui/input'
import { Button } from '@/components/ui/button'
import { Popover, PopoverContent, PopoverAnchor } from '@/components/ui/popover'
import { X } from '@lucide/vue'

const props = defineProps<{ modelValue: Paziente | null }>()
const emit = defineEmits<{ 'update:modelValue': [paziente: Paziente | null] }>()

const testoRicerca = ref('')
const risultati = ref<Paziente[]>([])
const ricercaInCorso = ref(false)
const erroreRicerca = ref(false)
const popoverAperto = ref(false)

let debounceHandle: ReturnType<typeof setTimeout> | undefined

async function eseguiRicerca(testo: string) {
  if (!testo.trim()) {
    risultati.value = []
    popoverAperto.value = false
    return
  }
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
      <Button type="button" variant="outline" size="sm" @click="cambiaSelezione">
        <X :size="14" />
        Cambia paziente
      </Button>
    </div>

    <Popover v-else v-model:open="popoverAperto">
      <PopoverAnchor as-child>
        <Input
          data-test="input-cerca-paziente"
          :model-value="testoRicerca"
          @update:model-value="onInput"
          type="text"
          placeholder="Cerca paziente per nome, email o codice fiscale…"
        />
      </PopoverAnchor>
      <PopoverContent class="w-80 p-1" @open-auto-focus.prevent>
        <p v-if="ricercaInCorso" class="px-3 py-2 text-xs text-(--fg3)">Ricerca in corso…</p>
        <p v-else-if="erroreRicerca" class="px-3 py-2 text-xs text-(--danger)">Ricerca non riuscita, riprova.</p>
        <p v-else-if="risultati.length === 0" class="px-3 py-2 text-xs text-(--fg3)">Nessun paziente trovato.</p>
        <button
          v-for="paziente in risultati"
          :key="paziente.id"
          type="button"
          data-test="risultato-paziente"
          class="flex w-full flex-col items-start rounded-lg px-3 py-2 text-left hover:bg-(--hover)"
          @click="seleziona(paziente)"
        >
          <span class="text-sm font-semibold text-(--fg)">{{ paziente.nome }} {{ paziente.cognome }}</span>
          <span class="text-xs text-(--fg3)">{{ paziente.email }}</span>
        </button>
      </PopoverContent>
    </Popover>
  </div>
</template>
