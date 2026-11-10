<script setup lang="ts">
import { computed, nextTick, ref, type Ref } from 'vue'
import type { AcceptableValue } from 'reka-ui'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Checkbox } from '@/components/ui/checkbox'
import { Textarea } from '@/components/ui/textarea'
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { DatePicker } from '@/components/ui/date-picker'
import { Ruler, ScissorsLineDashed } from '@lucide/vue'
import type { CreaVisitaRequest, Visita } from '@/api/pazienti'
import { calcolaBmi, formattaNumero } from '@/utils/visita'
import PlicometriaForm from './PlicometriaForm.vue'

import {
  numeroItaliano,
  numeroItalianoOpzionale,
  filtraSoloCifre,
  filtraDecimaleItaliano,
  erroreAltezza,
  errorePeso,
  erroreCirconferenza,
} from '@/utils/validators'

const props = defineProps<{
  sesso: string
  datiIniziali?: Visita | null
  altezzaSuggeritaCm?: number | null
  obiettivoSuggerito?: Visita['obiettivo'] | null
  parteDominanteSuggerita?: Visita['parteDominante']
}>()

function valoreIniziale(valore: number | null | undefined): string {
  return valore !== null && valore !== undefined ? formattaNumero(valore, 2) : ''
}

const dataVisita = ref(props.datiIniziali?.dataVisita ?? new Date().toISOString().slice(0, 10))
const dataVisitaIniziale = dataVisita.value
const altezzaCm = ref(
  props.datiIniziali
    ? String(props.datiIniziali.altezzaCm)
    : props.altezzaSuggeritaCm != null
      ? String(props.altezzaSuggeritaCm)
      : '',
)
const pesoKg = ref(valoreIniziale(props.datiIniziali?.pesoKg))
const circonferenzaVita = ref(valoreIniziale(props.datiIniziali?.circonferenze.vitaCm))
const circonferenzaFianchi = ref(valoreIniziale(props.datiIniziali?.circonferenze.fianchiCm))
const circonferenzaAddome = ref(valoreIniziale(props.datiIniziali?.circonferenze.addomeCm))
const circonferenzaBraccioRilassato = ref(valoreIniziale(props.datiIniziali?.circonferenze.braccioRilassatoCm))
const circonferenzaCoscia = ref(valoreIniziale(props.datiIniziali?.circonferenze.cosciaCm))
const circonferenzaPolpaccio = ref(valoreIniziale(props.datiIniziali?.circonferenze.polpaccioCm))
const circonferenzaTorace = ref(valoreIniziale(props.datiIniziali?.circonferenze.toraceCm))
const circonferenzaBraccioContratto = ref(valoreIniziale(props.datiIniziali?.circonferenze.braccioContrattoCm))
const circonferenzaBraccioRilassatoDominante = ref(valoreIniziale(props.datiIniziali?.circonferenze.braccioRilassatoDominanteCm))
const circonferenzaBraccioContrattoDominante = ref(valoreIniziale(props.datiIniziali?.circonferenze.braccioContrattoDominanteCm))
const circonferenzaCosciaDominante = ref(valoreIniziale(props.datiIniziali?.circonferenze.cosciaDominanteCm))
const circonferenzaPolpaccioDominante = ref(valoreIniziale(props.datiIniziali?.circonferenze.polpaccioDominanteCm))

// La parte dominante si precompila dall'ultima visita (o dalla visita in modifica); altrimenti resta vuota.
const parteDominanteIniziale: '' | 'DESTRA' | 'SINISTRA' =
  (props.datiIniziali ? props.datiIniziali.parteDominante : props.parteDominanteSuggerita) ?? ''
const parteDominante = ref<'' | 'DESTRA' | 'SINISTRA'>(parteDominanteIniziale)
// Le misure sulla parte dominante sono facoltative: il blocco compare solo se il professionista lo chiede.
const conParteDominante = ref(
  [
    circonferenzaBraccioRilassatoDominante.value,
    circonferenzaBraccioContrattoDominante.value,
    circonferenzaCosciaDominante.value,
    circonferenzaPolpaccioDominante.value,
  ].some((v) => v !== ''),
)
const note = ref(props.datiIniziali?.note ?? '')
const obiettivo = ref<Visita['obiettivo']>(
  props.datiIniziali?.obiettivo ?? props.obiettivoSuggerito ?? 'MANTENIMENTO',
)

const VALORE_SELEZIONA = '__seleziona__'

function onParteDominanteChange(valore: AcceptableValue) {
  parteDominante.value = valore === VALORE_SELEZIONA ? '' : (valore as 'DESTRA' | 'SINISTRA')
}

function onConParteDominanteChange(valore: boolean | 'indeterminate') {
  conParteDominante.value = valore === true
  if (valore !== true) {
    circonferenzaBraccioRilassatoDominante.value = ''
    circonferenzaBraccioContrattoDominante.value = ''
    circonferenzaCosciaDominante.value = ''
    circonferenzaPolpaccioDominante.value = ''
  }
}

function onObiettivoChange(valore: AcceptableValue) {
  obiettivo.value = valore as typeof obiettivo.value
}

const errori = ref<Record<string, string>>({})
const accordionAperto = ref('')
const accordionPlicometriaAperto = ref('')
const plicometriaForm = ref<InstanceType<typeof PlicometriaForm>>()

// --- GESTIONE FILTRI SU INPUT (LOGICA VUE UI) ---
const MARCATORE_INVISIBILE = '​'

function pulisciErroreSeCorretto(chiave: string, valida: (valore: string) => string | undefined, valore: string) {
  if (errori.value[chiave] && !valida(valore)) {
    const nuovi = { ...errori.value }
    delete nuovi[chiave]
    errori.value = nuovi
  }
}

function conFiltro(
  rif: Ref<string>,
  filtro: (valore: string) => string,
  chiave?: string,
  valida?: (valore: string) => string | undefined,
) {
  return async (valore: string | number) => {
    const filtrato = filtro(String(valore))
    if (filtrato === rif.value) {
      rif.value = `${filtrato}${MARCATORE_INVISIBILE}`
      await nextTick()
    }
    rif.value = filtrato
    if (chiave && valida) pulisciErroreSeCorretto(chiave, valida, filtrato)
  }
}

const onAltezzaInput = conFiltro(altezzaCm, (v) => filtraSoloCifre(v, 3), 'altezzaCm', erroreAltezza)
const onPesoInput = conFiltro(pesoKg, filtraDecimaleItaliano, 'pesoKg', errorePeso)

// --- BMI (calcolato live da altezza e peso) ---
const bmi = computed(() => {
  if (erroreAltezza(altezzaCm.value) || errorePeso(pesoKg.value)) return null
  return calcolaBmi(numeroItaliano(altezzaCm.value), numeroItaliano(pesoKg.value))
})

const creaHandlerCirconferenza = (rif: Ref<string>, chiave: string) =>
  conFiltro(rif, filtraDecimaleItaliano, chiave, erroreCirconferenza)

const onCirconferenzaVitaInput = creaHandlerCirconferenza(circonferenzaVita, 'circonferenzaVita')
const onCirconferenzaFianchiInput = creaHandlerCirconferenza(circonferenzaFianchi, 'circonferenzaFianchi')
const onCirconferenzaAddomeInput = creaHandlerCirconferenza(circonferenzaAddome, 'circonferenzaAddome')
const onCirconferenzaBraccioRilassatoInput = creaHandlerCirconferenza(circonferenzaBraccioRilassato, 'circonferenzaBraccioRilassato')
const onCirconferenzaCosciaInput = creaHandlerCirconferenza(circonferenzaCoscia, 'circonferenzaCoscia')
const onCirconferenzaPolpaccioInput = creaHandlerCirconferenza(circonferenzaPolpaccio, 'circonferenzaPolpaccio')
const onCirconferenzaToraceInput = creaHandlerCirconferenza(circonferenzaTorace, 'circonferenzaTorace')
const onCirconferenzaBraccioContrattoInput = creaHandlerCirconferenza(circonferenzaBraccioContratto, 'circonferenzaBraccioContratto')
const onCirconferenzaBraccioRilassatoDominanteInput = creaHandlerCirconferenza(circonferenzaBraccioRilassatoDominante, 'circonferenzaBraccioRilassatoDominante')
const onCirconferenzaBraccioContrattoDominanteInput = creaHandlerCirconferenza(circonferenzaBraccioContrattoDominante, 'circonferenzaBraccioContrattoDominante')
const onCirconferenzaCosciaDominanteInput = creaHandlerCirconferenza(circonferenzaCosciaDominante, 'circonferenzaCosciaDominante')
const onCirconferenzaPolpaccioDominanteInput = creaHandlerCirconferenza(circonferenzaPolpaccioDominante, 'circonferenzaPolpaccioDominante')

// --- VALIDAZIONE ---
function valida(): boolean {
  const nuoviErrori: Record<string, string> = {}

  const assegna = (chiave: string, messaggio: string | undefined) => {
    if (messaggio) nuoviErrori[chiave] = messaggio
  }

  assegna('altezzaCm', erroreAltezza(altezzaCm.value))
  assegna('pesoKg', errorePeso(pesoKg.value))

  const circonferenze: Array<[string, string]> = [
    ['circonferenzaVita', circonferenzaVita.value],
    ['circonferenzaFianchi', circonferenzaFianchi.value],
    ['circonferenzaAddome', circonferenzaAddome.value],
    ['circonferenzaBraccioRilassato', circonferenzaBraccioRilassato.value],
    ['circonferenzaCoscia', circonferenzaCoscia.value],
    ['circonferenzaPolpaccio', circonferenzaPolpaccio.value],
    ['circonferenzaTorace', circonferenzaTorace.value],
    ['circonferenzaBraccioContratto', circonferenzaBraccioContratto.value],
    ['circonferenzaBraccioRilassatoDominante', circonferenzaBraccioRilassatoDominante.value],
    ['circonferenzaBraccioContrattoDominante', circonferenzaBraccioContrattoDominante.value],
    ['circonferenzaCosciaDominante', circonferenzaCosciaDominante.value],
    ['circonferenzaPolpaccioDominante', circonferenzaPolpaccioDominante.value],
  ]
  for (const [chiave, valore] of circonferenze) {
    assegna(chiave, erroreCirconferenza(valore))
  }

  errori.value = nuoviErrori

  if (circonferenze.some(([chiave]) => nuoviErrori[chiave])) {
    accordionAperto.value = 'circonferenze'
  }

  const plicometriaValida = plicometriaForm.value?.valida() ?? true
  if (!plicometriaValida) {
    accordionPlicometriaAperto.value = 'plicometria'
  }

  return Object.keys(nuoviErrori).length === 0 && plicometriaValida
}

function ottieniDati(): CreaVisitaRequest {
  return {
    dataVisita: dataVisita.value || undefined,
    altezzaCm: numeroItaliano(altezzaCm.value),
    pesoKg: numeroItaliano(pesoKg.value),
    circonferenzaVitaCm: numeroItalianoOpzionale(circonferenzaVita.value),
    circonferenzaFianchiCm: numeroItalianoOpzionale(circonferenzaFianchi.value),
    circonferenzaAddomeCm: numeroItalianoOpzionale(circonferenzaAddome.value),
    circonferenzaBraccioRilassatoCm: numeroItalianoOpzionale(circonferenzaBraccioRilassato.value),
    circonferenzaCosciaCm: numeroItalianoOpzionale(circonferenzaCoscia.value),
    circonferenzaPolpaccioCm: numeroItalianoOpzionale(circonferenzaPolpaccio.value),
    circonferenzaToraceCm: numeroItalianoOpzionale(circonferenzaTorace.value),
    circonferenzaBraccioContrattoCm: numeroItalianoOpzionale(circonferenzaBraccioContratto.value),
    parteDominante: parteDominante.value || undefined,
    circonferenzaBraccioRilassatoDominanteCm: numeroItalianoOpzionale(circonferenzaBraccioRilassatoDominante.value),
    circonferenzaBraccioContrattoDominanteCm: numeroItalianoOpzionale(circonferenzaBraccioContrattoDominante.value),
    circonferenzaCosciaDominanteCm: numeroItalianoOpzionale(circonferenzaCosciaDominante.value),
    circonferenzaPolpaccioDominanteCm: numeroItalianoOpzionale(circonferenzaPolpaccioDominante.value),
    note: note.value || undefined,
    obiettivo: obiettivo.value,
    plicometria: plicometriaForm.value?.ottieniDati(),
  }
}

function valorizzato(): boolean {
  return (
    dataVisita.value !== dataVisitaIniziale ||
    altezzaCm.value !== '' ||
    pesoKg.value !== '' ||
    circonferenzaVita.value !== '' ||
    circonferenzaFianchi.value !== '' ||
    circonferenzaAddome.value !== '' ||
    circonferenzaBraccioRilassato.value !== '' ||
    circonferenzaCoscia.value !== '' ||
    circonferenzaPolpaccio.value !== '' ||
    circonferenzaTorace.value !== '' ||
    circonferenzaBraccioContratto.value !== '' ||
    parteDominante.value !== parteDominanteIniziale ||
    circonferenzaBraccioRilassatoDominante.value !== '' ||
    circonferenzaBraccioContrattoDominante.value !== '' ||
    circonferenzaCosciaDominante.value !== '' ||
    circonferenzaPolpaccioDominante.value !== '' ||
    note.value !== '' ||
    obiettivo.value !== 'MANTENIMENTO' ||
    (plicometriaForm.value?.valorizzato() ?? false)
  )
}

// Modificato rispetto ai valori con cui il form è stato creato (compresi i valori suggeriti dall'ultima visita):
// a differenza di valorizzato, che confronta con "vuoto", non scatta se l'utente non ha toccato nulla.
const firma = () => JSON.stringify([
  dataVisita.value, altezzaCm.value, pesoKg.value,
  circonferenzaVita.value, circonferenzaFianchi.value, circonferenzaAddome.value,
  circonferenzaBraccioRilassato.value, circonferenzaCoscia.value, circonferenzaPolpaccio.value,
  circonferenzaTorace.value, circonferenzaBraccioContratto.value,
  circonferenzaBraccioRilassatoDominante.value, circonferenzaBraccioContrattoDominante.value,
  circonferenzaCosciaDominante.value, circonferenzaPolpaccioDominante.value,
  parteDominante.value, conParteDominante.value, note.value, obiettivo.value,
])
const firmaIniziale = firma()
function modificato(): boolean {
  return firma() !== firmaIniziale || (plicometriaForm.value?.modificato() ?? false)
}

defineExpose({
  valida,
  ottieniDati,
  valorizzato,
  modificato,
})
</script>

<template>
  <div>
    <h2 class="font-heading text-xl italic text-(--fg)">Dati della visita</h2>

    <div class="mt-5 grid gap-5 sm:grid-cols-2">
      <div class="flex flex-col gap-1.5">
        <Label for="obiettivo" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Obiettivo</Label>
        <Select :model-value="obiettivo" @update:model-value="onObiettivoChange">
          <SelectTrigger id="obiettivo" class="w-full">
            <SelectValue placeholder="Seleziona" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="DIMAGRIMENTO">Dimagrimento</SelectItem>
            <SelectItem value="AUMENTO_PESO">Aumento peso</SelectItem>
            <SelectItem value="IPERTROFIA">Ipertrofia</SelectItem>
            <SelectItem value="RICOMPOSIZIONE">Ricomposizione</SelectItem>
            <SelectItem value="MANTENIMENTO">Mantenimento</SelectItem>
            <SelectItem value="PREPARAZIONE_SPORTIVA">Preparazione sportiva</SelectItem>
            <SelectItem value="EDUCATIVO">Educativo</SelectItem>
            <SelectItem value="PATOLOGIA_CLINICA">Patologia clinica</SelectItem>
            <SelectItem value="GRAVIDANZA_ALLATTAMENTO">Gravidanza/Allattamento</SelectItem>
          </SelectContent>
        </Select>
      </div>

      <div class="flex flex-col gap-1.5">
        <Label for="data-visita" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Data visita</Label>
        <DatePicker id="data-visita" v-model="dataVisita" />
      </div>

      <div class="flex flex-col gap-1.5">
        <Label for="altezza" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Altezza (cm)*</Label>
        <Input id="altezza" :model-value="altezzaCm" @update:model-value="onAltezzaInput" type="text" inputmode="numeric" :aria-invalid="!!errori.altezzaCm" placeholder="Es. 178" />
        <p v-if="errori.altezzaCm" class="text-xs font-medium text-(--danger)">{{ errori.altezzaCm }}</p>
      </div>

      <div class="flex flex-col gap-1.5">
        <Label for="peso" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Peso (kg)*</Label>
        <Input id="peso" :model-value="pesoKg" @update:model-value="onPesoInput" type="text" inputmode="decimal" :aria-invalid="!!errori.pesoKg" placeholder="Es. 78,50" />
        <p v-if="errori.pesoKg" class="text-xs font-medium text-(--danger)">{{ errori.pesoKg }}</p>
      </div>
    </div>

    <div class="mt-5 flex flex-col gap-1.5">
      <Label for="bmi" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">BMI</Label>
      <div class="flex items-center gap-2.5">
        <Input id="bmi" :model-value="bmi !== null ? formattaNumero(bmi, 1) : ''" type="text" disabled placeholder="Compila altezza e peso" class="sm:max-w-40" />
      </div>
    </div>

    <div class="mt-5 flex flex-col gap-1.5">
      <Label for="note-visita" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Note</Label>
      <Textarea id="note-visita" v-model="note" maxlength="500" placeholder="Note operative, piccoli obiettivi fino alla prossima visita..." />
      <p class="text-right text-xs text-(--fg3)">{{ note.length }}/500</p>
    </div>

    <div class="mt-6 border-t border-(--bd) pt-5">
      <h3 class="text-sm font-bold uppercase tracking-wide text-(--fg3)">Misurazione BIA</h3>
      <p class="mt-1.5 text-sm text-(--fg3)">Sarà disponibile a breve.</p>
    </div>

    <Accordion v-model="accordionPlicometriaAperto" type="single" collapsible class="mt-6">
      <AccordionItem value="plicometria" class="overflow-hidden rounded-xl border border-(--bd)">
        <AccordionTrigger class="group px-4 py-3.5 hover:no-underline sm:px-5">
          <div class="flex items-center gap-3">
            <div class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-(--mint) text-(--green)">
              <ScissorsLineDashed :size="18" />
            </div>

            <div class="flex flex-col items-start">
              <span class="text-sm font-bold text-(--fg)">Plicometria</span>
              <span class="mt-0.5 text-xs text-(--fg3)">Stima massa grassa da pliche cutanee</span>
            </div>
          </div>
        </AccordionTrigger>
        <AccordionContent>
          <div class="mx-2 border-t-2 border-t-(--bd)"></div>
          <div class="px-6 py-4">
            <PlicometriaForm ref="plicometriaForm" :sesso="props.sesso" :dati-iniziali="props.datiIniziali?.plicometria" />
          </div>
        </AccordionContent>
      </AccordionItem>
    </Accordion>

    <Accordion v-model="accordionAperto" type="single" collapsible class="mt-6">
      <AccordionItem value="circonferenze" class="overflow-hidden rounded-xl border border-(--bd)">
        <AccordionTrigger class="group px-4 py-3.5 hover:no-underline sm:px-5">
          <div class="flex items-center gap-3">
            <div class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-(--mint) text-(--green)">
              <Ruler :size="18" />
            </div>

            <div class="flex flex-col items-start">
              <span class="text-sm font-bold text-(--fg)">Circonferenze</span>
              <span class="mt-0.5 text-xs text-(--fg3)">Misure corporee in cm</span>
            </div>
          </div>
        </AccordionTrigger>
        <AccordionContent>
          <div class="mx-2 border-t-2 border-t-(--bd)"></div>
          <div class="grid gap-5 py-4 px-6 sm:grid-cols-2">
            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-vita" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza vita</Label>
              <Input id="circonferenza-vita" :model-value="circonferenzaVita" @update:model-value="onCirconferenzaVitaInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaVita" />
              <p v-if="errori.circonferenzaVita" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaVita }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-fianchi" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza fianchi</Label>
              <Input id="circonferenza-fianchi" :model-value="circonferenzaFianchi" @update:model-value="onCirconferenzaFianchiInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaFianchi" />
              <p v-if="errori.circonferenzaFianchi" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaFianchi }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-addome" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza addome</Label>
              <Input id="circonferenza-addome" :model-value="circonferenzaAddome" @update:model-value="onCirconferenzaAddomeInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaAddome" />
              <p v-if="errori.circonferenzaAddome" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaAddome }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-torace" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza torace</Label>
              <Input id="circonferenza-torace" :model-value="circonferenzaTorace" @update:model-value="onCirconferenzaToraceInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaTorace" />
              <p v-if="errori.circonferenzaTorace" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaTorace }}</p>
            </div>

            <div class="flex flex-col gap-1.5 sm:col-span-2">
              <Label for="parte-dominante" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Parte dominante</Label>
              <Select :model-value="parteDominante || VALORE_SELEZIONA" @update:model-value="onParteDominanteChange">
                <SelectTrigger id="parte-dominante" class="w-full sm:w-1/2">
                  <SelectValue placeholder="Seleziona" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem :value="VALORE_SELEZIONA">Seleziona</SelectItem>
                  <SelectItem value="DESTRA">Destra</SelectItem>
                  <SelectItem value="SINISTRA">Sinistra</SelectItem>
                </SelectContent>
              </Select>
            </div>

            <h3 class="text-sm font-bold text-(--fg) sm:col-span-2">Parte non dominante</h3>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-braccio-rilassato" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Braccio rilassato</Label>
              <Input id="circonferenza-braccio-rilassato" :model-value="circonferenzaBraccioRilassato" @update:model-value="onCirconferenzaBraccioRilassatoInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaBraccioRilassato" />
              <p v-if="errori.circonferenzaBraccioRilassato" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaBraccioRilassato }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-braccio-contratto" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Braccio contratto</Label>
              <Input id="circonferenza-braccio-contratto" :model-value="circonferenzaBraccioContratto" @update:model-value="onCirconferenzaBraccioContrattoInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaBraccioContratto" />
              <p v-if="errori.circonferenzaBraccioContratto" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaBraccioContratto }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-coscia" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza coscia</Label>
              <Input id="circonferenza-coscia" :model-value="circonferenzaCoscia" @update:model-value="onCirconferenzaCosciaInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaCoscia" />
              <p v-if="errori.circonferenzaCoscia" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaCoscia }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-polpaccio" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza polpaccio</Label>
              <Input id="circonferenza-polpaccio" :model-value="circonferenzaPolpaccio" @update:model-value="onCirconferenzaPolpaccioInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaPolpaccio" />
              <p v-if="errori.circonferenzaPolpaccio" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaPolpaccio }}</p>
            </div>

            <label class="flex items-center gap-2 text-sm text-(--fg2) sm:col-span-2">
              <Checkbox id="con-parte-dominante" :model-value="conParteDominante" @update:model-value="onConParteDominanteChange" />
              Aggiungi anche le circonferenze della parte dominante
            </label>

            <template v-if="conParteDominante">
              <h3 class="text-sm font-bold text-(--fg) sm:col-span-2">Parte dominante</h3>

              <div class="flex flex-col gap-1.5">
                <Label for="circonferenza-braccio-rilassato-dominante" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Braccio rilassato</Label>
                <Input id="circonferenza-braccio-rilassato-dominante" :model-value="circonferenzaBraccioRilassatoDominante" @update:model-value="onCirconferenzaBraccioRilassatoDominanteInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaBraccioRilassatoDominante" />
                <p v-if="errori.circonferenzaBraccioRilassatoDominante" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaBraccioRilassatoDominante }}</p>
              </div>

              <div class="flex flex-col gap-1.5">
                <Label for="circonferenza-braccio-contratto-dominante" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Braccio contratto</Label>
                <Input id="circonferenza-braccio-contratto-dominante" :model-value="circonferenzaBraccioContrattoDominante" @update:model-value="onCirconferenzaBraccioContrattoDominanteInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaBraccioContrattoDominante" />
                <p v-if="errori.circonferenzaBraccioContrattoDominante" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaBraccioContrattoDominante }}</p>
              </div>

              <div class="flex flex-col gap-1.5">
                <Label for="circonferenza-coscia-dominante" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza coscia</Label>
                <Input id="circonferenza-coscia-dominante" :model-value="circonferenzaCosciaDominante" @update:model-value="onCirconferenzaCosciaDominanteInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaCosciaDominante" />
                <p v-if="errori.circonferenzaCosciaDominante" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaCosciaDominante }}</p>
              </div>

              <div class="flex flex-col gap-1.5">
                <Label for="circonferenza-polpaccio-dominante" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza polpaccio</Label>
                <Input id="circonferenza-polpaccio-dominante" :model-value="circonferenzaPolpaccioDominante" @update:model-value="onCirconferenzaPolpaccioDominanteInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaPolpaccioDominante" />
                <p v-if="errori.circonferenzaPolpaccioDominante" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaPolpaccioDominante }}</p>
              </div>
            </template>
          </div>
        </AccordionContent>
      </AccordionItem>
    </Accordion>
  </div>
</template>
