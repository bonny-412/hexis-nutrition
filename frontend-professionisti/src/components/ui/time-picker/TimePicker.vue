<script setup lang="ts">
import { computed, nextTick, ref, watch, type HTMLAttributes } from 'vue'
import { Clock as ClockIcon } from '@lucide/vue'
import { Input } from '@/components/ui/input'
import { Popover, PopoverAnchor, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { cn } from '@/lib/utils'

defineOptions({ inheritAttrs: false })

const props = withDefaults(defineProps<{
  modelValue?: string
  id?: string
  class?: HTMLAttributes['class']
  /** Orario minimo e massimo selezionabili dall'elenco (`HH:mm`, estremi inclusi). Scrivendo a mano si può uscirne: la validazione spetta al form. */
  min?: string
  max?: string
  /** Passo dei minuti nell'elenco. */
  passoMinuti?: number
}>(), {
  modelValue: '',
  min: '00:00',
  max: '23:59',
  passoMinuti: 5,
})

const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()

const aperto = ref(false)
const ancora = ref<HTMLElement | null>(null)
const colonnaOre = ref<HTMLElement | null>(null)
const colonnaMinuti = ref<HTMLElement | null>(null)

function due(n: number): string {
  return String(n).padStart(2, '0')
}

function minutiDaOra(ora: string): number {
  const [h, m] = ora.split(':')
  return Number(h) * 60 + Number(m)
}

/** `HH:mm` valido dal testo, altrimenti stringa vuota (incompleto o inesistente, es. 25:00). */
function oraDaTesto(testo: string): string {
  const m = /^(\d{2}):(\d{2})$/.exec(testo)
  if (!m) return ''
  return Number(m[1]) <= 23 && Number(m[2]) <= 59 ? testo : ''
}

/** Tiene solo le cifre (max 4) e inserisce da solo i due punti: `0930` diventa `09:30`. */
function formattaTesto(grezzo: string): string {
  const cifre = grezzo.replace(/\D/g, '').slice(0, 4)
  return [cifre.slice(0, 2), cifre.slice(2)].filter(Boolean).join(':')
}

const testo = ref(props.modelValue)

watch(() => props.modelValue, (ora) => {
  if (ora !== oraDaTesto(testo.value)) testo.value = ora
})

const MARCATORE_INVISIBILE = '​'

/** Stesso workaround di `DatePicker.vue`: se il testo filtrato non cambia, Vue non riscrive il DOM e il carattere non valido resterebbe visibile. */
async function onTestoInput(valore: string | number) {
  const filtrato = formattaTesto(String(valore))
  if (filtrato === testo.value) {
    testo.value = `${filtrato}${MARCATORE_INVISIBILE}`
    await nextTick()
  }
  testo.value = filtrato
  const ora = oraDaTesto(filtrato)
  if (ora !== props.modelValue) emit('update:modelValue', ora)
}

const minMinuti = computed(() => minutiDaOra(props.min))
const maxMinuti = computed(() => minutiDaOra(props.max))

const ore = computed(() => {
  const lista: number[] = []
  for (let h = Math.floor(minMinuti.value / 60); h <= Math.floor(maxMinuti.value / 60); h++) lista.push(h)
  return lista
})

const minuti = computed(() => {
  const lista: number[] = []
  for (let m = 0; m < 60; m += props.passoMinuti) lista.push(m)
  return lista
})

const oraSelezionata = computed(() => (props.modelValue ? Number(props.modelValue.slice(0, 2)) : null))
const minutoSelezionato = computed(() => (props.modelValue ? Number(props.modelValue.slice(3, 5)) : null))

function fuoriIntervallo(h: number, m: number): boolean {
  const tot = h * 60 + m
  return tot < minMinuti.value || tot > maxMinuti.value
}

function scegliOra(h: number) {
  const m = minutoSelezionato.value ?? 0
  // Se con i minuti già scelti l'orario uscirebbe dall'intervallo, si ripiega sul primo minuto valido.
  const minutoValido = fuoriIntervallo(h, m) ? (minuti.value.find((x) => !fuoriIntervallo(h, x)) ?? 0) : m
  emit('update:modelValue', `${due(h)}:${due(minutoValido)}`)
}

function scegliMinuto(m: number) {
  const h = oraSelezionata.value ?? ore.value[0] ?? 0
  emit('update:modelValue', `${due(h)}:${due(m)}`)
  aperto.value = false
}

/** A ogni cambio di orario le voci selezionate restano visibili nelle liste scorrevoli. */
watch(() => props.modelValue, () => {
  nextTick(() => {
    for (const colonna of [colonnaOre.value, colonnaMinuti.value]) {
      colonna?.querySelector<HTMLElement>('[data-selezionato="true"]')?.scrollIntoView({ block: 'nearest' })
    }
  })
})

/** Il focus resta sul campo: aprendo l'elenco con un click si può continuare a scrivere. */
function evitaFocusSullaLista(evento: Event) {
  evento.preventDefault()
  nextTick(() => {
    for (const colonna of [colonnaOre.value, colonnaMinuti.value]) {
      colonna?.querySelector<HTMLElement>('[data-selezionato="true"]')?.scrollIntoView({ block: 'center' })
    }
  })
}

/** Un click sul campo (o sull'icona) mentre l'elenco è aperto non lo chiude: lo gestisce già il campo stesso. */
function nonChiudereSulCampo(evento: Event & { detail?: { originalEvent?: Event } }) {
  const bersaglio = (evento.detail?.originalEvent?.target ?? evento.target) as Node | null
  if (bersaglio && ancora.value?.contains(bersaglio)) evento.preventDefault()
}

const classiVoce =
  'flex h-8 w-full items-center justify-center rounded-md text-sm transition-colors hover:bg-accent hover:text-accent-foreground disabled:pointer-events-none disabled:opacity-40'
const classiVoceSelezionata = 'bg-primary text-primary-foreground hover:bg-primary hover:text-primary-foreground'
</script>

<template>
  <Popover v-model:open="aperto">
    <PopoverAnchor as-child>
      <div ref="ancora" class="relative w-full">
        <Input
          :id="id"
          v-bind="$attrs"
          type="text"
          inputmode="numeric"
          autocomplete="off"
          placeholder="hh:mm"
          maxlength="5"
          :model-value="testo"
          :class="cn('pr-9', props.class)"
          @click="aperto = true"
          @update:model-value="onTestoInput"
        />
        <PopoverTrigger as-child>
          <button
            type="button"
            aria-label="Apri la scelta dell'orario"
            title="Apri la scelta dell'orario"
            class="absolute right-1 top-1/2 flex h-6 w-6 -translate-y-1/2 items-center justify-center rounded-md text-(--fg3) transition-colors hover:text-(--fg)"
          >
            <ClockIcon :size="16" />
          </button>
        </PopoverTrigger>
      </div>
    </PopoverAnchor>
    <PopoverContent
      class="w-auto p-2"
      @open-auto-focus="evitaFocusSullaLista"
      @interact-outside="nonChiudereSulCampo"
    >
      <div class="flex gap-2">
        <div ref="colonnaOre" class="flex max-h-56 w-14 flex-col gap-0.5 overflow-y-auto pr-1" data-test="time-picker-ore">
          <button
            v-for="h in ore"
            :key="h"
            type="button"
            :data-selezionato="oraSelezionata === h"
            :class="cn(classiVoce, oraSelezionata === h && classiVoceSelezionata)"
            @click="scegliOra(h)"
          >
            {{ due(h) }}
          </button>
        </div>
        <div class="w-px bg-(--bd)" />
        <div ref="colonnaMinuti" class="flex max-h-56 w-14 flex-col gap-0.5 overflow-y-auto pr-1" data-test="time-picker-minuti">
          <button
            v-for="m in minuti"
            :key="m"
            type="button"
            :data-selezionato="minutoSelezionato === m"
            :disabled="oraSelezionata !== null && fuoriIntervallo(oraSelezionata, m)"
            :class="cn(classiVoce, minutoSelezionato === m && classiVoceSelezionata)"
            @click="scegliMinuto(m)"
          >
            {{ due(m) }}
          </button>
        </div>
      </div>
    </PopoverContent>
  </Popover>
</template>
