<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { AcceptableValue } from 'reka-ui'
import { toast } from 'vue-sonner'
import { Save } from '@lucide/vue'
import {
  aggiorna,
  crea,
  SovrapposizioneError,
  type Appuntamento,
  type AppuntamentoRichiesta,
  type TipoAppuntamento,
} from '@/api/agenda'
import { dettaglio, type Paziente } from '@/api/pazienti'
import { ANTICIPI_NOTIFICA, ANTICIPO_NOTIFICA_DEFAULT, minutiDaOra } from '@/utils/agenda'
import { Checkbox } from '@/components/ui/checkbox'
import { Button } from '@/components/ui/button'
import { DatePicker } from '@/components/ui/date-picker'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { TimePicker } from '@/components/ui/time-picker'
import { Label } from '@/components/ui/label'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import SelezionaPazienteCombobox from '@/components/pazienti/SelezionaPazienteCombobox.vue'

const props = defineProps<{
  aperto: boolean
  tipoIniziale: TipoAppuntamento
  appuntamento?: Appuntamento | null
  pazienteIdIniziale?: string | null
  dataIniziale?: string
}>()

const emit = defineEmits<{
  'update:aperto': [valore: boolean]
  salvato: [appuntamento: Appuntamento]
}>()

const DURATE = [15, 30, 45, 60, 90, 120]
const INIZIO_MIN = 7 * 60
const FINE_MIN = 20 * 60
const REGEX_ORA = /^([01]\d|2[0-3]):[0-5]\d$/
const REGEX_LINK = /^https?:\/\/\S+$/
const MAX_TESTO = 500

const tipo = ref<TipoAppuntamento>('VISITA_STUDIO')
const paziente = ref<Paziente | null>(null)
const data = ref('')
const ora = ref('')
const durata = ref('30')
const testo = ref('')
const link = ref('')
const notifica = ref(true)
const anticipo = ref(String(ANTICIPO_NOTIFICA_DEFAULT))
const errori = ref<Record<string, string>>({})
const inCorso = ref(false)

const confermaUscitaAperta = ref(false)
const sovrapposizioneAperta = ref(false)
const conflitti = ref<Appuntamento[]>([])
const richiestaInAttesa = ref<AppuntamentoRichiesta | null>(null)
const firmaBase = ref<string | null>(null)

const eVisita = computed(() => tipo.value === 'VISITA_STUDIO' || tipo.value === 'VIDEOCONSULTO')
const ePromemoria = computed(() => tipo.value === 'PROMEMORIA')
const eBlocco = computed(() => tipo.value === 'BLOCCO')
const inModifica = computed(() => !!props.appuntamento)
// La notifica ha senso solo per visite e per promemoria con un orario valido: per gli altri la sezione non compare.
const sezioneNotifica = computed(() => eVisita.value || (ePromemoria.value && REGEX_ORA.test(ora.value.trim())))

const titolo = computed(() => {
  if (ePromemoria.value) return inModifica.value ? 'Modifica promemoria' : 'Nuovo promemoria'
  if (eBlocco.value) return inModifica.value ? 'Modifica fascia bloccata' : 'Blocca fascia oraria'
  return inModifica.value ? 'Modifica appuntamento' : 'Nuovo appuntamento'
})

const opzioniDurata = computed(() => {
  const corrente = Number(durata.value)
  const valori = DURATE.includes(corrente) || !corrente ? DURATE : [...DURATE, corrente].sort((a, b) => a - b)
  return valori.map((v) => ({ valore: String(v), etichetta: `${v} minuti` }))
})

const firmaCorrente = computed(() =>
  JSON.stringify([tipo.value, paziente.value?.id ?? null, data.value, ora.value, durata.value, testo.value, link.value, notifica.value, anticipo.value]),
)
const modificato = computed(() => firmaBase.value !== null && firmaCorrente.value !== firmaBase.value)

function fissaBase() {
  firmaBase.value = firmaCorrente.value
}

let sequenza = 0
const pazienteInCaricamento = ref(false)

async function inizializza() {
  const token = ++sequenza
  const a = props.appuntamento ?? null
  tipo.value = a ? a.tipo : props.tipoIniziale
  data.value = a ? a.data : (props.dataIniziale ?? '')
  ora.value = a?.oraInizio ?? ''
  durata.value = String(a?.durataMinuti ?? (tipo.value === 'BLOCCO' ? 60 : 30))
  testo.value = a?.testo ?? ''
  link.value = a?.linkVideo ?? ''
  notifica.value = a?.notifica ?? true
  anticipo.value = String(a?.anticipoNotificaMinuti ?? ANTICIPO_NOTIFICA_DEFAULT)
  paziente.value = null
  errori.value = {}
  inCorso.value = false
  firmaBase.value = null
  pazienteInCaricamento.value = false

  const pazienteId = a ? a.pazienteId : (props.pazienteIdIniziale ?? null)
  if (!pazienteId) {
    fissaBase()
    return
  }
  pazienteInCaricamento.value = true
  try {
    const trovato = await dettaglio(pazienteId)
    if (token !== sequenza) return
    // In creazione un paziente archiviato non è selezionabile (come nel combobox).
    if (a || !trovato.archiviato) paziente.value = trovato
  } catch {
    if (token !== sequenza) return
    paziente.value = null
  }
  pazienteInCaricamento.value = false
  fissaBase()
}

watch(() => props.aperto, (valore) => { if (valore) void inizializza() }, { immediate: true })

function erroreOra(): string | undefined {
  const obbligatoria = !ePromemoria.value
  const valore = ora.value.trim()
  if (!valore) return obbligatoria ? "L'ora di inizio è obbligatoria." : undefined
  if (!REGEX_ORA.test(valore)) return 'Inserisci un orario valido (es. 09:30).'
  const inizio = minutiDaOra(valore)
  if (inizio < INIZIO_MIN) return "L'orario deve essere compreso tra le 07:00 e le 20:00."
  if (ePromemoria.value) {
    return inizio >= FINE_MIN ? "L'orario deve essere compreso tra le 07:00 e le 20:00." : undefined
  }
  if (inizio + Number(durata.value) > FINE_MIN) return "L'appuntamento deve terminare entro le 20:00."
  return undefined
}

function erroreCampo(chiave: string): string | undefined {
  switch (chiave) {
    case 'paziente':
      return eVisita.value && !paziente.value ? 'Seleziona un paziente.' : undefined
    case 'data':
      return data.value ? undefined : 'La data è obbligatoria.'
    case 'ora':
      return erroreOra()
    case 'testo':
      if (ePromemoria.value && !testo.value.trim()) return 'Il testo del promemoria è obbligatorio.'
      if (testo.value.length > MAX_TESTO) return `Massimo ${MAX_TESTO} caratteri.`
      return undefined
    case 'link': {
      const valore = link.value.trim()
      if (tipo.value !== 'VIDEOCONSULTO' || !valore) return undefined
      if (!REGEX_LINK.test(valore)) return 'Inserisci un link valido che inizi con http:// o https://.'
      if (valore.length > MAX_TESTO) return `Massimo ${MAX_TESTO} caratteri.`
      return undefined
    }
    default:
      return undefined
  }
}

const CHIAVI = ['paziente', 'data', 'ora', 'testo', 'link']

// Gli errori spariscono non appena il campo viene corretto.
watch([tipo, paziente, data, ora, durata, testo, link], () => {
  if (Object.keys(errori.value).length === 0) return
  const nuovi = { ...errori.value }
  for (const chiave of Object.keys(nuovi)) {
    if (!erroreCampo(chiave)) delete nuovi[chiave]
  }
  errori.value = nuovi
})

function valida(): boolean {
  const nuovi: Record<string, string> = {}
  for (const chiave of CHIAVI) {
    const messaggio = erroreCampo(chiave)
    if (messaggio) nuovi[chiave] = messaggio
  }
  errori.value = nuovi
  return Object.keys(nuovi).length === 0
}

function costruisciRichiesta(): AppuntamentoRichiesta {
  const richiesta = costruisciRichiestaBase()
  // Senza sezione notifica (blocchi, promemoria senza ora) i campi non si inviano: il backend li imposta a false.
  if (sezioneNotifica.value) {
    richiesta.notifica = notifica.value
    richiesta.anticipoNotificaMinuti = Number(anticipo.value)
  }
  return richiesta
}

function costruisciRichiestaBase(): AppuntamentoRichiesta {
  const testoPulito = testo.value.trim() || null
  if (eVisita.value) {
    const linkPulito = link.value.trim()
    return {
      tipo: tipo.value,
      pazienteId: paziente.value?.id ?? null,
      data: data.value,
      oraInizio: ora.value,
      durataMinuti: Number(durata.value),
      testo: testoPulito,
      // Il link esiste solo per i videoconsulti, mai stringa vuota (il backend la rifiuta).
      linkVideo: tipo.value === 'VIDEOCONSULTO' && linkPulito ? linkPulito : null,
    }
  }
  if (eBlocco.value) {
    return {
      tipo: 'BLOCCO',
      pazienteId: null,
      data: data.value,
      oraInizio: ora.value,
      durataMinuti: Number(durata.value),
      testo: testoPulito,
      linkVideo: null,
    }
  }
  return {
    tipo: 'PROMEMORIA',
    pazienteId: paziente.value?.id ?? null,
    data: data.value,
    oraInizio: ora.value.trim() || null,
    durataMinuti: null,
    testo: testoPulito,
    linkVideo: null,
  }
}

async function invia(richiesta: AppuntamentoRichiesta, conferma: boolean) {
  inCorso.value = true
  try {
    const salvato = props.appuntamento
      ? await aggiorna(props.appuntamento.id, richiesta, conferma)
      : await crea(richiesta, conferma)
    toast.success(props.appuntamento ? 'Appuntamento aggiornato.' : 'Appuntamento creato.')
    emit('salvato', salvato)
    emit('update:aperto', false)
  } catch (errore) {
    if (errore instanceof SovrapposizioneError) {
      conflitti.value = errore.conflitti
      richiestaInAttesa.value = richiesta
      sovrapposizioneAperta.value = true
    } else {
      toast.error("Non è stato possibile salvare l'appuntamento.")
    }
  } finally {
    inCorso.value = false
  }
}

async function onSubmit() {
  if (inCorso.value || pazienteInCaricamento.value || !valida()) return
  await invia(costruisciRichiesta(), false)
}

async function confermaComunque() {
  const richiesta = richiestaInAttesa.value
  richiestaInAttesa.value = null
  if (richiesta) await invia(richiesta, true)
}

function richiediChiusura() {
  if (inCorso.value) return
  if (modificato.value) confermaUscitaAperta.value = true
  else emit('update:aperto', false)
}

function onDialogOpen(valore: boolean) {
  if (valore) emit('update:aperto', true)
  else richiediChiusura()
}

function esciSenzaSalvare() {
  emit('update:aperto', false)
}

function onTipoChange(valore: AcceptableValue) {
  tipo.value = String(valore) as TipoAppuntamento
}

function onDurataChange(valore: AcceptableValue) {
  durata.value = String(valore)
}

function etichettaConflitto(c: Appuntamento): string {
  const nome = [c.pazienteNome, c.pazienteCognome].filter(Boolean).join(' ')
  const descrizione = nome || c.testo || (c.tipo === 'BLOCCO' ? 'Fascia bloccata' : 'Appuntamento')
  if (!c.oraInizio) return descrizione
  const fine = c.durataMinuti ? minutiDaOra(c.oraInizio) + c.durataMinuti : null
  const fineTesto = fine === null ? '' : `–${String(Math.floor(fine / 60)).padStart(2, '0')}:${String(fine % 60).padStart(2, '0')}`
  return `${c.oraInizio}${fineTesto} · ${descrizione}`
}
</script>

<template>
  <Dialog :open="aperto" @update:open="onDialogOpen">
    <DialogContent class="sm:max-w-lg" data-test="appuntamento-dialog">
      <DialogHeader>
        <DialogTitle class="font-heading text-xl italic">{{ titolo }}</DialogTitle>
        <DialogDescription class="sr-only">Compila i campi e salva per aggiornare l'agenda.</DialogDescription>
      </DialogHeader>

      <form class="flex min-h-0 flex-1 flex-col gap-4" novalidate @submit.prevent="onSubmit">
        <div class="min-h-0 flex-1 space-y-4 overflow-y-auto px-0.5 py-0.5">
          <div v-if="eVisita" class="flex flex-col gap-1.5">
            <Label for="appuntamento-tipo" class="text-[13px] font-medium text-(--fg2)">Tipo</Label>
            <Select :model-value="tipo" @update:model-value="onTipoChange">
              <SelectTrigger id="appuntamento-tipo" class="w-full" data-test="appuntamento-tipo">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="VISITA_STUDIO">Visita in studio</SelectItem>
                <SelectItem value="VIDEOCONSULTO">Videoconsulto</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div v-if="eVisita || ePromemoria" class="flex flex-col gap-1.5">
            <Label class="text-[13px] font-medium text-(--fg2)">
              Paziente{{ eVisita ? '*' : ' (facoltativo)' }}
            </Label>
            <SelezionaPazienteCombobox v-model="paziente" />
            <p v-if="errori.paziente" class="text-xs font-medium text-(--danger)">{{ errori.paziente }}</p>
          </div>

          <div class="grid gap-3 sm:grid-cols-2">
            <div class="flex flex-col gap-1.5">
              <Label for="appuntamento-data" class="text-[13px] font-medium text-(--fg2)">Data*</Label>
              <DatePicker id="appuntamento-data" v-model="data" digitabile class="w-full" />
              <p v-if="errori.data" class="text-xs font-medium text-(--danger)">{{ errori.data }}</p>
            </div>
            <div class="flex flex-col gap-1.5">
              <Label for="appuntamento-ora" class="text-[13px] font-medium text-(--fg2)">
                Ora{{ ePromemoria ? ' (facoltativa)' : '*' }}
              </Label>
              <TimePicker
                id="appuntamento-ora"
                v-model="ora"
                min="07:00"
                max="20:00"
                :aria-invalid="!!errori.ora"
                data-test="appuntamento-ora"
              />
              <p v-if="errori.ora" class="text-xs font-medium text-(--danger)">{{ errori.ora }}</p>
            </div>
          </div>

          <div v-if="!ePromemoria" class="flex flex-col gap-1.5">
            <Label for="appuntamento-durata" class="text-[13px] font-medium text-(--fg2)">Durata</Label>
            <Select :model-value="durata" @update:model-value="onDurataChange">
              <SelectTrigger id="appuntamento-durata" class="w-full" data-test="appuntamento-durata">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                <SelectItem v-for="o in opzioniDurata" :key="o.valore" :value="o.valore">{{ o.etichetta }}</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="appuntamento-testo" class="text-[13px] font-medium text-(--fg2)">
              {{ ePromemoria ? 'Testo*' : eBlocco ? 'Motivo (facoltativo)' : 'Note brevi (facoltative)' }}
            </Label>
            <Textarea
              id="appuntamento-testo"
              v-model="testo"
              rows="3"
              :aria-invalid="!!errori.testo"
              :placeholder="ePromemoria ? 'Es. Richiamare per i risultati delle analisi' : eBlocco ? 'Es. Pausa pranzo' : 'Es. Controllo mensile'"
              data-test="appuntamento-testo"
            />
            <p v-if="errori.testo" class="text-xs font-medium text-(--danger)">{{ errori.testo }}</p>
          </div>

          <div v-if="sezioneNotifica" class="flex flex-col gap-3 rounded-lg border border-(--bd) p-3" data-test="appuntamento-notifica-sezione">
            <div class="flex items-center gap-2.5">
              <Checkbox id="appuntamento-notifica" v-model="notifica" data-test="appuntamento-notifica" />
              <Label for="appuntamento-notifica" class="cursor-pointer text-sm font-medium text-(--fg)">Ricevi notifica</Label>
            </div>
            <div v-if="notifica" class="flex flex-col gap-1.5">
              <Label for="appuntamento-anticipo" class="text-[13px] font-medium text-(--fg2)">Anticipo</Label>
              <Select v-model="anticipo">
                <SelectTrigger id="appuntamento-anticipo" class="w-full" data-test="appuntamento-anticipo">
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem v-for="o in ANTICIPI_NOTIFICA" :key="o.valore" :value="String(o.valore)">{{ o.etichetta }}</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div v-if="tipo === 'VIDEOCONSULTO'" class="flex flex-col gap-1.5">
            <Label for="appuntamento-link" class="text-[13px] font-medium text-(--fg2)">Link del videoconsulto (facoltativo)</Label>
            <Input
              id="appuntamento-link"
              v-model="link"
              type="text"
              inputmode="url"
              :aria-invalid="!!errori.link"
              placeholder="https://"
              data-test="appuntamento-link"
            />
            <p v-if="errori.link" class="text-xs font-medium text-(--danger)">{{ errori.link }}</p>
          </div>
        </div>

        <DialogFooter class="flex items-center gap-2.5">
          <Button type="button" variant="neutral" :disabled="inCorso" data-test="appuntamento-annulla" @click="richiediChiusura">
            Annulla
          </Button>
          <Button type="submit" :disabled="inCorso || pazienteInCaricamento" data-test="appuntamento-salva">
            <Save :size="16" />
            {{ inCorso ? 'Salvataggio…' : 'Salva' }}
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  </Dialog>

  <AlertDialog v-model:open="confermaUscitaAperta">
    <AlertDialogContent>
      <AlertDialogHeader>
        <AlertDialogTitle>Uscire senza salvare?</AlertDialogTitle>
        <AlertDialogDescription>Hai delle modifiche non salvate. Se esci ora andranno perse.</AlertDialogDescription>
      </AlertDialogHeader>
      <AlertDialogFooter>
        <AlertDialogCancel data-test="uscita-continua">Continua a modificare</AlertDialogCancel>
        <AlertDialogAction data-test="uscita-esci" @click="esciSenzaSalvare">
          Esci senza salvare
        </AlertDialogAction>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>

  <AlertDialog v-model:open="sovrapposizioneAperta">
    <AlertDialogContent data-test="sovrapposizione-dialog">
      <AlertDialogHeader>
        <AlertDialogTitle>Sovrapposizione</AlertDialogTitle>
        <AlertDialogDescription>
          L'orario scelto si sovrappone a {{ conflitti.length === 1 ? 'un altro impegno' : 'altri impegni' }}.
        </AlertDialogDescription>
      </AlertDialogHeader>
      <ul v-if="conflitti.length > 0" class="space-y-1 text-sm text-(--fg2)">
        <li v-for="c in conflitti" :key="c.id">{{ etichettaConflitto(c) }}</li>
      </ul>
      <AlertDialogFooter>
        <AlertDialogCancel data-test="sovrapposizione-cambia">Cambia orario</AlertDialogCancel>
        <AlertDialogAction data-test="sovrapposizione-conferma" @click="confermaComunque">
          Conferma comunque
        </AlertDialogAction>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
</template>
