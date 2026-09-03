<script setup lang="ts">
import { computed, ref, watch, type Ref } from 'vue'
import type { AcceptableValue } from 'reka-ui'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Label } from '@/components/ui/label'
import PlicaInput from './PlicaInput.vue'
import { numeroItalianoOpzionale, erroreCirconferenza } from '@/utils/validators'
import { formattaNumero } from '@/utils/visita'
import type { CreaPlicometriaRequest, Plicometria } from '@/api/pazienti'

const props = defineProps<{ sesso: string; datiIniziali?: Plicometria | null }>()

type Protocollo = '' | 'JACKSON_POLLOCK_3' | 'JACKSON_POLLOCK_7' | 'DURNIN_WOMERSLEY_4' | 'FAULKNER_4' | 'SLAUGHTER_PEDIATRICO' | 'EVANS_ATLETI'
type Campo = 'pettorale' | 'ascellare' | 'tricipitale' | 'bicipitale' | 'sottoscapolare' | 'soprailiaca' | 'addominale' | 'coscia' | 'polpaccio'

function valoreIniziale(valore: number | null | undefined): string {
  return valore !== null && valore !== undefined ? formattaNumero(valore, 2) : ''
}

const protocollo = ref<Protocollo>(props.datiIniziali?.protocollo ?? '')
const etniaAtleta = ref<'CAUCASICO' | 'AFROAMERICANO'>(props.datiIniziali?.etniaAtleta ?? 'CAUCASICO')

const plicaPettorale = ref(valoreIniziale(props.datiIniziali?.plicaPettoraleMm))
const plicaAscellare = ref(valoreIniziale(props.datiIniziali?.plicaAscellareMm))
const plicaTricipitale = ref(valoreIniziale(props.datiIniziali?.plicaTricipitaleMm))
const plicaBicipitale = ref(valoreIniziale(props.datiIniziali?.plicaBicipitaleMm))
const plicaSottoscapolare = ref(valoreIniziale(props.datiIniziali?.plicaSottoscapolareMm))
const plicaSoprailiaca = ref(valoreIniziale(props.datiIniziali?.plicaSoprailiacaMm))
const plicaAddominale = ref(valoreIniziale(props.datiIniziali?.plicaAddominaleMm))
const plicaCoscia = ref(valoreIniziale(props.datiIniziali?.plicaCosciaMm))
const plicaPolpaccio = ref(valoreIniziale(props.datiIniziali?.plicaPolpaccioMm))

const valoriPerCampo: Record<Campo, Ref<string>> = {
  pettorale: plicaPettorale,
  ascellare: plicaAscellare,
  tricipitale: plicaTricipitale,
  bicipitale: plicaBicipitale,
  sottoscapolare: plicaSottoscapolare,
  soprailiaca: plicaSoprailiaca,
  addominale: plicaAddominale,
  coscia: plicaCoscia,
  polpaccio: plicaPolpaccio,
}

const campiPerProtocollo: Record<Exclude<Protocollo, ''>, Campo[]> = {
  JACKSON_POLLOCK_3: [], // sesso-dipendente, gestito in campiVisibili
  JACKSON_POLLOCK_7: ['pettorale', 'ascellare', 'tricipitale', 'sottoscapolare', 'addominale', 'soprailiaca', 'coscia'],
  DURNIN_WOMERSLEY_4: ['bicipitale', 'tricipitale', 'sottoscapolare', 'soprailiaca'],
  FAULKNER_4: ['tricipitale', 'sottoscapolare', 'soprailiaca', 'addominale'],
  SLAUGHTER_PEDIATRICO: ['tricipitale', 'polpaccio'],
  EVANS_ATLETI: ['tricipitale', 'addominale', 'coscia'],
}

const etichette: Record<Campo, string> = {
  pettorale: 'Plica pettorale',
  ascellare: 'Plica ascellare media',
  tricipitale: 'Plica tricipitale',
  bicipitale: 'Plica bicipitale',
  sottoscapolare: 'Plica sottoscapolare',
  soprailiaca: 'Plica soprailiaca',
  addominale: 'Plica addominale',
  coscia: 'Plica coscia anteriore',
  polpaccio: 'Plica polpaccio mediale',
}

const VALORE_SELEZIONA = '__seleziona__'

function onProtocolloChange(valore: AcceptableValue) {
  protocollo.value = valore === VALORE_SELEZIONA ? '' : (valore as Protocollo)
}

const campiVisibili = computed<Campo[]>(() => {
  if (!protocollo.value) return []
  if (protocollo.value === 'JACKSON_POLLOCK_3') {
    return props.sesso === 'F' ? ['tricipitale', 'soprailiaca', 'coscia'] : ['pettorale', 'addominale', 'coscia']
  }
  return campiPerProtocollo[protocollo.value]
})

const disabilitato = computed(() => props.sesso === 'ALTRO')

watch([protocollo, () => props.sesso], () => {
  for (const campo of Object.keys(valoriPerCampo) as Campo[]) {
    valoriPerCampo[campo].value = ''
  }
})

const errori = ref<Record<string, string>>({})

function erroreCampo(campo: Campo, valore: string): string | undefined {
  if (!campiVisibili.value.includes(campo)) return undefined
  if (!valore.trim()) return 'Questa plica è obbligatoria per il protocollo scelto.'
  return erroreCirconferenza(valore)
}

for (const campo of Object.keys(valoriPerCampo) as Campo[]) {
  watch(valoriPerCampo[campo], (valore) => {
    if (!errori.value[campo]) return
    if (erroreCampo(campo, valore)) return
    const nuovi = { ...errori.value }
    delete nuovi[campo]
    errori.value = nuovi
  })
}

function valida(): boolean {
  if (disabilitato.value || !protocollo.value) {
    errori.value = {}
    return true
  }
  const nuoviErrori: Record<string, string> = {}
  for (const campo of campiVisibili.value) {
    const messaggio = erroreCampo(campo, valoriPerCampo[campo].value)
    if (messaggio) nuoviErrori[campo] = messaggio
  }
  errori.value = nuoviErrori
  return Object.keys(nuoviErrori).length === 0
}

function valorePerCampo(campo: Campo): number | undefined {
  if (!campiVisibili.value.includes(campo)) return undefined
  return numeroItalianoOpzionale(valoriPerCampo[campo].value)
}

function ottieniDati(): CreaPlicometriaRequest | undefined {
  if (disabilitato.value || !protocollo.value) return undefined
  return {
    protocollo: protocollo.value,
    etniaAtleta: protocollo.value === 'EVANS_ATLETI' ? etniaAtleta.value : undefined,
    plicaPettoraleMm: valorePerCampo('pettorale'),
    plicaAscellareMm: valorePerCampo('ascellare'),
    plicaTricipitaleMm: valorePerCampo('tricipitale'),
    plicaBicipitaleMm: valorePerCampo('bicipitale'),
    plicaSottoscapolareMm: valorePerCampo('sottoscapolare'),
    plicaSoprailiacaMm: valorePerCampo('soprailiaca'),
    plicaAddominaleMm: valorePerCampo('addominale'),
    plicaCosciaMm: valorePerCampo('coscia'),
    plicaPolpaccioMm: valorePerCampo('polpaccio'),
  }
}

function valorizzato(): boolean {
  return (
    protocollo.value !== '' ||
    etniaAtleta.value !== 'CAUCASICO' ||
    Object.values(valoriPerCampo).some((rif) => rif.value !== '')
  )
}

defineExpose({ valida, ottieniDati, valorizzato })
</script>

<template>
  <div>
    <p v-if="disabilitato" class="text-sm text-(--fg3)">
      Non disponibile per sesso "Altro": le equazioni plicometriche richiedono Maschio o Femmina.
    </p>

    <template v-else>
      <div class="grid gap-5 sm:grid-cols-2">
        <div class="flex flex-col gap-1.5">
          <Label for="protocollo-plico" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Protocollo</Label>
          <Select :model-value="protocollo || VALORE_SELEZIONA" @update:model-value="onProtocolloChange">
            <SelectTrigger id="protocollo-plico" class="w-full">
              <SelectValue placeholder="Seleziona" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem :value="VALORE_SELEZIONA">Seleziona</SelectItem>
              <SelectItem value="JACKSON_POLLOCK_3">Jackson-Pollock 3 pliche</SelectItem>
              <SelectItem value="JACKSON_POLLOCK_7">Jackson-Pollock 7 pliche</SelectItem>
              <SelectItem value="DURNIN_WOMERSLEY_4">Durnin-Womersley 4 pliche</SelectItem>
              <SelectItem value="FAULKNER_4">Faulkner 4 pliche</SelectItem>
              <SelectItem value="SLAUGHTER_PEDIATRICO">Slaughter (pediatrico)</SelectItem>
              <SelectItem value="EVANS_ATLETI">Evans (atleti)</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div v-if="protocollo === 'EVANS_ATLETI'" class="flex flex-col gap-1.5">
          <Label for="etnia-atleta" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Etnia</Label>
          <Select v-model="etniaAtleta">
            <SelectTrigger id="etnia-atleta" class="w-full">
              <SelectValue placeholder="Caucasico" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="CAUCASICO">Caucasico</SelectItem>
              <SelectItem value="AFROAMERICANO">Afroamericano</SelectItem>
            </SelectContent>
          </Select>
        </div>
      </div>

      <div v-if="campiVisibili.length" class="mt-5 grid gap-5 sm:grid-cols-2">
        <PlicaInput
          v-for="campo in campiVisibili"
          :key="campo"
          :id="`plica-${campo}`"
          :label="etichette[campo]"
          v-model="valoriPerCampo[campo].value"
          :errore="errori[campo]"
        />
      </div>
    </template>
  </div>
</template>
