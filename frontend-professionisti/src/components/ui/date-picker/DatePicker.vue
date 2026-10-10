<script setup lang="ts">
import { computed, nextTick, ref, watch, type HTMLAttributes } from 'vue'
import { DateFormatter, getLocalTimeZone, parseDate, type DateValue } from '@internationalized/date'
import { Calendar as CalendarIcon } from '@lucide/vue'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import { Input } from '@/components/ui/input'
import { Popover, PopoverAnchor, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { cn } from '@/lib/utils'
import { formattaDataItalianaConMese } from '@/utils/data'

defineOptions({ inheritAttrs: false })

const props = withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  id?: string
  class?: HTMLAttributes['class']
  /** Se true la data si può anche scrivere (gg/mm/aaaa); il calendario resta disponibile dall'icona. */
  digitabile?: boolean
  /** Date selezionabili (ISO `AAAA-MM-GG`, estremi inclusi): fuori da questo intervallo il calendario le disabilita. */
  min?: string
  max?: string
}>(), {
  modelValue: '',
  placeholder: 'Seleziona una data',
  digitabile: false,
})

const emit = defineEmits<{ (e: 'update:modelValue', value: string): void }>()

const formatter = new DateFormatter('it-IT', { dateStyle: 'long' })

const valore = computed<DateValue | undefined>({
  get: () => (props.modelValue ? parseDate(props.modelValue) : undefined),
  set: (data) => emit('update:modelValue', data ? data.toString() : ''),
})

const valoreMin = computed<DateValue | undefined>(() => (props.min ? parseDate(props.min) : undefined))
const valoreMax = computed<DateValue | undefined>(() => (props.max ? parseDate(props.max) : undefined))

const aperto = ref(false)

/** Selezionare una data nel calendario chiude il popover: nessun secondo click per confermare. */
watch(() => props.modelValue, () => {
  aperto.value = false
})

const testoVisualizzato = computed(() =>
  valore.value ? formatter.format(valore.value.toDate(getLocalTimeZone())) : props.placeholder,
)

// --- Modalità digitabile ---

/** Data ISO (`AAAA-MM-GG`) dal testo `gg/mm/aaaa`; stringa vuota se incompleto o inesistente (es. 31/02). */
function isoDaTesto(testo: string): string {
  const m = /^(\d{2})\/(\d{2})\/(\d{4})$/.exec(testo)
  if (!m) return ''
  const [giorno, mese, anno] = [Number(m[1]), Number(m[2]), Number(m[3])]
  const controllo = new Date(Date.UTC(anno, mese - 1, giorno))
  const esiste = controllo.getUTCFullYear() === anno && controllo.getUTCMonth() === mese - 1 && controllo.getUTCDate() === giorno
  return esiste ? `${m[3]}-${m[2]}-${m[1]}` : ''
}

function testoDaIso(iso: string): string {
  const [anno, mese, giorno] = iso.split('-')
  return anno && mese && giorno ? `${giorno}/${mese}/${anno}` : ''
}

/** Tiene solo le cifre (max 8) e inserisce da solo le barre: `12032026` diventa `12/03/2026`. */
function formattaTesto(grezzo: string): string {
  const cifre = grezzo.replace(/\D/g, '').slice(0, 8)
  return [cifre.slice(0, 2), cifre.slice(2, 4), cifre.slice(4)].filter(Boolean).join('/')
}

const testo = ref(testoDaIso(props.modelValue))
const inFocus = ref(false)

/** A riposo una data valida si legge come `14 set 2026`; con il focus torna `gg/mm/aaaa`, il formato in cui si scrive. */
const testoMostrato = computed(() => {
  const iso = isoDaTesto(testo.value)
  return !inFocus.value && iso ? formattaDataItalianaConMese(iso) : testo.value
})

/** Aggiorna il testo solo per cambi esterni (calendario, reset): mentre si scrive il valore è '' finché la data non è completa. */
watch(() => props.modelValue, (iso) => {
  if (iso !== isoDaTesto(testo.value)) testo.value = testoDaIso(iso)
})

const MARCATORE_INVISIBILE = '​'

/**
 * Se il testo filtrato coincide con quello attuale, Vue non aggiorna il DOM dell'input: il carattere
 * non valido appena digitato resterebbe visibile. Un marcatore invisibile temporaneo forza il resync,
 * stesso workaround di PlicaInput.vue.
 */
async function onTestoInput(valore: string | number) {
  const filtrato = formattaTesto(String(valore))
  if (filtrato === testo.value) {
    testo.value = `${filtrato}${MARCATORE_INVISIBILE}`
    await nextTick()
  }
  testo.value = filtrato
  let iso = isoDaTesto(filtrato)
  // Una data scritta a mano fuori dall'intervallo ammesso non è valida, come se fosse incompleta.
  if ((props.min && iso < props.min) || (props.max && iso > props.max)) iso = ''
  if (iso !== props.modelValue) emit('update:modelValue', iso)
}
</script>

<template>
  <Popover v-model:open="aperto">
    <template v-if="digitabile">
      <PopoverAnchor as-child>
        <div class="relative w-full">
          <Input
            :id="id"
            type="text"
            inputmode="numeric"
            autocomplete="off"
            placeholder="gg/mm/aaaa"            maxlength="10"
            :model-value="testoMostrato"
            :class="cn('pr-9', props.class)"
            @focus="inFocus = true"
            @blur="inFocus = false"
            @update:model-value="onTestoInput"
          />
          <PopoverTrigger as-child>
            <button
              type="button"
              aria-label="Apri il calendario"
              title="Apri il calendario"
              class="absolute right-1 top-1/2 flex h-6 w-6 -translate-y-1/2 items-center justify-center rounded-md text-(--fg3) transition-colors hover:text-(--fg)"
            >
              <CalendarIcon :size="16" />
            </button>
          </PopoverTrigger>
        </div>
      </PopoverAnchor>
    </template>
    <PopoverTrigger v-else as-child>
      <Button
        :id="id"
        type="button"
        variant="outline"
        :class="cn(
          'w-full justify-start border-input bg-transparent px-2.5 py-1 text-left text-base font-normal hover:bg-transparent dark:bg-input/30 md:text-sm',
          !valore && 'text-muted-foreground',
          props.class,
        )"
      >
        <CalendarIcon :size="16" class="mr-2 shrink-0" />
        {{ testoVisualizzato }}
      </Button>
    </PopoverTrigger>
    <PopoverContent class="w-auto overflow-hidden p-0">
      <Calendar
        v-model="valore"
        :min-value="valoreMin"
        :max-value="valoreMax"
        locale="it"
        layout="month-and-year"
        initial-focus
      />
    </PopoverContent>
  </Popover>
</template>
