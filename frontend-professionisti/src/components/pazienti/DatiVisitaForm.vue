<script setup lang="ts">
import { nextTick, ref, type Ref } from 'vue'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
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
import { Percent, Ruler } from '@lucide/vue'
import type { CreaVisitaRequest } from '@/api/pazienti'
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

const props = defineProps<{ sesso: string }>()

const dataVisita = ref(new Date().toISOString().slice(0, 10))
const altezzaCm = ref('')
const pesoKg = ref('')
const circonferenzaVita = ref('')
const circonferenzaFianchi = ref('')
const circonferenzaAddome = ref('')
const circonferenzaBraccioRilassato = ref('')
const circonferenzaCoscia = ref('')
const circonferenzaPolpaccio = ref('')
const circonferenzaCollo = ref('')
const circonferenzaTorace = ref('')
const circonferenzaBraccioContratto = ref('')
const circonferenzaAvambraccio = ref('')
const circonferenzaCaviglia = ref('')
const protocolloVita = ref<'' | 'OMS' | 'OMBELICALE' | 'ALTRO'>('')

const VALORE_SELEZIONA = '__seleziona__'

function onProtocolloVitaChange(valore: string) {
  protocolloVita.value = valore === VALORE_SELEZIONA ? '' : (valore as typeof protocolloVita.value)
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

const creaHandlerCirconferenza = (rif: Ref<string>, chiave: string) =>
  conFiltro(rif, filtraDecimaleItaliano, chiave, erroreCirconferenza)

const onCirconferenzaVitaInput = creaHandlerCirconferenza(circonferenzaVita, 'circonferenzaVita')
const onCirconferenzaFianchiInput = creaHandlerCirconferenza(circonferenzaFianchi, 'circonferenzaFianchi')
const onCirconferenzaAddomeInput = creaHandlerCirconferenza(circonferenzaAddome, 'circonferenzaAddome')
const onCirconferenzaBraccioRilassatoInput = creaHandlerCirconferenza(circonferenzaBraccioRilassato, 'circonferenzaBraccioRilassato')
const onCirconferenzaCosciaInput = creaHandlerCirconferenza(circonferenzaCoscia, 'circonferenzaCoscia')
const onCirconferenzaPolpaccioInput = creaHandlerCirconferenza(circonferenzaPolpaccio, 'circonferenzaPolpaccio')
const onCirconferenzaColloInput = creaHandlerCirconferenza(circonferenzaCollo, 'circonferenzaCollo')
const onCirconferenzaToraceInput = creaHandlerCirconferenza(circonferenzaTorace, 'circonferenzaTorace')
const onCirconferenzaBraccioContrattoInput = creaHandlerCirconferenza(circonferenzaBraccioContratto, 'circonferenzaBraccioContratto')
const onCirconferenzaAvambraccioInput = creaHandlerCirconferenza(circonferenzaAvambraccio, 'circonferenzaAvambraccio')
const onCirconferenzaCavigliaInput = creaHandlerCirconferenza(circonferenzaCaviglia, 'circonferenzaCaviglia')

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
    ['circonferenzaCollo', circonferenzaCollo.value],
    ['circonferenzaTorace', circonferenzaTorace.value],
    ['circonferenzaBraccioContratto', circonferenzaBraccioContratto.value],
    ['circonferenzaAvambraccio', circonferenzaAvambraccio.value],
    ['circonferenzaCaviglia', circonferenzaCaviglia.value],
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
    circonferenzaColloCm: numeroItalianoOpzionale(circonferenzaCollo.value),
    circonferenzaToraceCm: numeroItalianoOpzionale(circonferenzaTorace.value),
    circonferenzaBraccioContrattoCm: numeroItalianoOpzionale(circonferenzaBraccioContratto.value),
    circonferenzaAvambraccioCm: numeroItalianoOpzionale(circonferenzaAvambraccio.value),
    circonferenzaCavigliaCm: numeroItalianoOpzionale(circonferenzaCaviglia.value),
    protocolloVita: protocolloVita.value || undefined,
    plicometria: plicometriaForm.value?.ottieniDati(),
  }
}

defineExpose({
  valida,
  ottieniDati,
})
</script>

<template>
  <div>
    <h2 class="font-heading text-xl italic text-(--fg)">Dati della visita</h2>

    <div class="mt-5 grid gap-5 sm:grid-cols-2">
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

    <div class="mt-6 border-t border-(--bd) pt-5">
      <h3 class="text-sm font-bold uppercase tracking-wide text-(--fg3)">Misurazione BIA</h3>
      <p class="mt-1.5 text-sm text-(--fg3)">Sarà disponibile a breve.</p>
    </div>

    <Accordion v-model="accordionPlicometriaAperto" type="single" collapsible class="mt-6">
      <AccordionItem value="plicometria" class="overflow-hidden rounded-xl border border-(--bd)">
        <AccordionTrigger class="group px-4 py-3.5 hover:no-underline sm:px-5">
          <div class="flex items-center gap-3">
            <div class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-(--mint) text-(--green)">
              <Percent :size="18" />
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
            <PlicometriaForm ref="plicometriaForm" :sesso="props.sesso" />
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
              <Label for="protocollo-vita" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Protocollo vita</Label>
              <Select :model-value="protocolloVita || VALORE_SELEZIONA" @update:model-value="onProtocolloVitaChange">
                <SelectTrigger id="protocollo-vita" class="w-full">
                  <SelectValue placeholder="Seleziona" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem :value="VALORE_SELEZIONA">Seleziona</SelectItem>
                  <SelectItem value="OMS">OMS</SelectItem>
                  <SelectItem value="OMBELICALE">Ombelicale</SelectItem>
                  <SelectItem value="ALTRO">Altro</SelectItem>
                </SelectContent>
              </Select>
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
              <Label for="circonferenza-braccio-rilassato" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Braccio rilassato</Label>
              <Input id="circonferenza-braccio-rilassato" :model-value="circonferenzaBraccioRilassato" @update:model-value="onCirconferenzaBraccioRilassatoInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaBraccioRilassato" />
              <p v-if="errori.circonferenzaBraccioRilassato" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaBraccioRilassato }}</p>
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

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-collo" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza collo</Label>
              <Input id="circonferenza-collo" :model-value="circonferenzaCollo" @update:model-value="onCirconferenzaColloInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaCollo" />
              <p v-if="errori.circonferenzaCollo" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaCollo }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-torace" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza torace</Label>
              <Input id="circonferenza-torace" :model-value="circonferenzaTorace" @update:model-value="onCirconferenzaToraceInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaTorace" />
              <p v-if="errori.circonferenzaTorace" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaTorace }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-braccio-contratto" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Braccio contratto</Label>
              <Input id="circonferenza-braccio-contratto" :model-value="circonferenzaBraccioContratto" @update:model-value="onCirconferenzaBraccioContrattoInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaBraccioContratto" />
              <p v-if="errori.circonferenzaBraccioContratto" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaBraccioContratto }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-avambraccio" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza avambraccio</Label>
              <Input id="circonferenza-avambraccio" :model-value="circonferenzaAvambraccio" @update:model-value="onCirconferenzaAvambraccioInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaAvambraccio" />
              <p v-if="errori.circonferenzaAvambraccio" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaAvambraccio }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="circonferenza-caviglia" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza caviglia</Label>
              <Input id="circonferenza-caviglia" :model-value="circonferenzaCaviglia" @update:model-value="onCirconferenzaCavigliaInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaCaviglia" />
              <p v-if="errori.circonferenzaCaviglia" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaCaviglia }}</p>
            </div>
          </div>
        </AccordionContent>
      </AccordionItem>
    </Accordion>
  </div>
</template>
