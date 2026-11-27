<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import type { AcceptableValue } from 'reka-ui'
import { toast } from 'vue-sonner'
import { CalendarClock, Check, LogOut, Pencil, Save, X } from '@lucide/vue'
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
import { formattaDataBreve } from '@/utils/data'
import { trattoNelGiorno } from '@/utils/blocchi'
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
const MAX_GIORNI_BLOCCO = 62
const REGEX_DATA = /^\d{4}-\d{2}-\d{2}$/

const tipo = ref<TipoAppuntamento>('VISITA_STUDIO')
const paziente = ref<Paziente | null>(null)
const data = ref('')
const ora = ref('')
const dataFine = ref('')
const oraFine = ref('')
const giornataIntera = ref(false)
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
  JSON.stringify([tipo.value, paziente.value?.id ?? null, data.value, ora.value, dataFine.value, oraFine.value, giornataIntera.value, durata.value, testo.value, link.value, notifica.value, anticipo.value]),
)
const modificato = computed(() => firmaBase.value !== null && firmaCorrente.value !== firmaBase.value)

function fissaBase() {
  firmaBase.value = firmaCorrente.value
}

function oraDaMinuti(minuti: number): string {
  return `${String(Math.floor(minuti / 60)).padStart(2, '0')}:${String(minuti % 60).padStart(2, '0')}`
}

// Numero di giorni dall'epoca per una data ISO valida, altrimenti null.
function giorniDa(iso: string): number | null {
  if (!REGEX_DATA.test(iso)) return null
  const [anno, mese, giorno] = iso.split('-').map(Number)
  const t = Date.UTC(anno, mese - 1, giorno)
  return Number.isNaN(t) ? null : Math.round(t / 86400000)
}

function dataDaGiorni(giorni: number): string {
  return new Date(giorni * 86400000).toISOString().slice(0, 10)
}

let sequenza = 0
// Durante l'inizializzazione i watcher di coerenza tra inizio e fine non devono toccare il form.
let inizializzando = false
const pazienteInCaricamento = ref(false)

async function inizializza() {
  const token = ++sequenza
  inizializzando = true
  const a = props.appuntamento ?? null
  tipo.value = a ? a.tipo : props.tipoIniziale
  data.value = a ? a.data : (props.dataIniziale ?? '')
  ora.value = a?.oraInizio ?? ''
  dataFine.value = a ? (a.dataFine ?? a.data) : data.value
  oraFine.value = a
    ? (a.oraFine ?? '')
    : REGEX_ORA.test(ora.value) ? oraDaMinuti(Math.min(minutiDaOra(ora.value) + 60, FINE_MIN)) : ''
  giornataIntera.value = a?.giornataIntera ?? false
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
  void nextTick(() => { inizializzando = false })

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
  if (eBlocco.value && giornataIntera.value) return undefined
  const obbligatoria = !ePromemoria.value
  const valore = ora.value.trim()
  if (!valore) return obbligatoria ? "L'ora di inizio è obbligatoria." : undefined
  if (!REGEX_ORA.test(valore)) return 'Inserisci un orario valido (es. 09:30).'
  const inizio = minutiDaOra(valore)
  if (inizio < INIZIO_MIN) return "L'orario deve essere compreso tra le 07:00 e le 20:00."
  if (ePromemoria.value || eBlocco.value) {
    return inizio >= FINE_MIN ? "L'orario deve essere compreso tra le 07:00 e le 20:00." : undefined
  }
  if (inizio + Number(durata.value) > FINE_MIN) return "L'appuntamento deve terminare entro le 20:00."
  return undefined
}

function erroreDataFine(): string | undefined {
  if (!eBlocco.value) return undefined
  if (!dataFine.value) return 'La data di fine è obbligatoria.'
  const inizio = giorniDa(data.value)
  const fine = giorniDa(dataFine.value)
  if (inizio === null || fine === null) return undefined
  if (fine < inizio) return "La fine deve seguire l'inizio."
  if (fine - inizio > MAX_GIORNI_BLOCCO) return `Un blocco può durare al massimo ${MAX_GIORNI_BLOCCO} giorni.`
  return undefined
}

function erroreOraFine(): string | undefined {
  if (!eBlocco.value || giornataIntera.value) return undefined
  const valore = oraFine.value.trim()
  if (!valore) return 'Ora di inizio e di fine sono obbligatorie.'
  if (!REGEX_ORA.test(valore)) return 'Inserisci un orario valido (es. 09:30).'
  const fineMin = minutiDaOra(valore)
  if (fineMin <= INIZIO_MIN || fineMin > FINE_MIN) return "L'orario deve essere compreso tra le 07:00 e le 20:00."
  const inizio = giorniDa(data.value)
  const fine = giorniDa(dataFine.value)
  if (inizio === null || fine === null || fine < inizio || !REGEX_ORA.test(ora.value.trim())) return undefined
  if (inizio * 1440 + minutiDaOra(ora.value.trim()) >= fine * 1440 + fineMin) return "La fine deve seguire l'inizio."
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
    case 'dataFine':
      return erroreDataFine()
    case 'oraFine':
      return erroreOraFine()
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

const CHIAVI = ['paziente', 'data', 'ora', 'dataFine', 'oraFine', 'testo', 'link']

// Gli errori spariscono non appena il campo viene corretto.
watch([tipo, paziente, data, ora, dataFine, oraFine, giornataIntera, durata, testo, link], () => {
  if (Object.keys(errori.value).length === 0) return
  const nuovi = { ...errori.value }
  for (const chiave of Object.keys(nuovi)) {
    if (!erroreCampo(chiave)) delete nuovi[chiave]
  }
  errori.value = nuovi
})

// Mantiene coerenti inizio e fine di un blocco quando l'utente cambia data o ora di inizio.
watch([data, ora], ([nuovaData, nuovaOra], [vecchiaData, vecchiaOra]) => {
  if (!eBlocco.value || inizializzando) return
  const orario = (o: string) => REGEX_ORA.test(o.trim())
  // Prima ora di inizio inserita: la fine proposta è un'ora dopo (al massimo 20:00), se è lo stesso giorno.
  if (!giornataIntera.value && orario(nuovaOra) && !oraFine.value.trim() && dataFine.value === nuovaData) {
    oraFine.value = oraDaMinuti(Math.min(minutiDaOra(nuovaOra.trim()) + 60, FINE_MIN))
    return
  }
  const giornoNuovo = giorniDa(nuovaData)
  const giornoFine = giorniDa(dataFine.value)
  if (giornoNuovo === null || giornoFine === null) return
  const giornoVecchio = giorniDa(vecchiaData)
  const conOrari = !giornataIntera.value && orario(nuovaOra) && orario(vecchiaOra) && orario(oraFine.value)
  if (!conOrari) {
    // Senza orari (o con "Giornate intere") scorrono solo le date: se l'inizio supera la fine, la fine lo segue.
    if (giornoNuovo <= giornoFine) return
    const spostata = giornoVecchio === null ? giornoNuovo : giornoFine + (giornoNuovo - giornoVecchio)
    dataFine.value = dataDaGiorni(Math.max(spostata, giornoNuovo))
    return
  }
  if (giornoVecchio === null) return
  const nuovo = giornoNuovo * 1440 + minutiDaOra(nuovaOra.trim())
  const vecchio = giornoVecchio * 1440 + minutiDaOra(vecchiaOra.trim())
  const fine = giornoFine * 1440 + minutiDaOra(oraFine.value.trim())
  if (nuovo <= fine) return
  const spostata = fine + (nuovo - vecchio)
  dataFine.value = dataDaGiorni(Math.floor(spostata / 1440))
  oraFine.value = oraDaMinuti(Math.min(spostata % 1440, FINE_MIN))
})

// Togliendo "Giornate intere" da un blocco senza orari si propone l'intera fascia 07:00-20:00.
watch(giornataIntera, (attiva) => {
  if (attiva || !eBlocco.value || inizializzando) return
  if (!ora.value.trim() && !oraFine.value.trim()) {
    ora.value = oraDaMinuti(INIZIO_MIN)
    oraFine.value = oraDaMinuti(FINE_MIN)
  }
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
      oraInizio: giornataIntera.value ? null : ora.value,
      dataFine: dataFine.value,
      oraFine: giornataIntera.value ? null : oraFine.value,
      giornataIntera: giornataIntera.value,
      durataMinuti: null,
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
  const appuntamento = props.appuntamento
  inCorso.value = true
  try {
    const salvato = appuntamento
      ? await aggiorna(appuntamento.id, richiesta, conferma)
      : await crea(richiesta, conferma)
    const evento = richiesta.tipo === 'PROMEMORIA' ? 'Promemoria' : 'Appuntamento'
    toast.success(`${evento} ${appuntamento ? 'aggiornato' : 'creato'}.`)
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

function onOpenAutoFocus(evento: Event) {
  if (!ePromemoria.value || !(evento.target instanceof HTMLElement)) return
  const titoloDialog = evento.target.querySelector<HTMLElement>('[data-test="appuntamento-titolo"]')
  if (!titoloDialog) return
  evento.preventDefault()
  titoloDialog.focus()
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
  const prefisso = eBlocco.value ? `${formattaDataBreve(c.data)} · ` : ''
  if (c.tipo === 'BLOCCO' && !eBlocco.value) {
    // Un blocco può durare più giorni: si mostra il tratto del giorno della visita, non l'orario del primo giorno.
    const tratto = trattoNelGiorno(c, data.value)
    if (tratto) return `${tratto.inizio}–${tratto.fine} · ${descrizione}`
  }
  if (!c.oraInizio) return `${prefisso}${descrizione}`
  const fine = c.durataMinuti ? minutiDaOra(c.oraInizio) + c.durataMinuti : null
  const fineTesto = fine === null ? '' : `–${oraDaMinuti(fine)}`
  return `${prefisso}${c.oraInizio}${fineTesto} · ${descrizione}`
}

const titoloSovrapposizione = computed(() => {
  if (!eBlocco.value) return 'Sovrapposizione'
  const n = conflitti.value.length
  return `Questo blocco copre ${n} ${n === 1 ? 'visita' : 'visite'}`
})
</script>

<template>
  <Dialog :open="aperto" @update:open="onDialogOpen">
    <DialogContent class="sm:max-w-lg" data-test="appuntamento-dialog" @open-auto-focus="onOpenAutoFocus">
      <DialogHeader>
        <DialogTitle :tabindex="ePromemoria ? -1 : undefined" class="font-heading text-xl italic outline-none" data-test="appuntamento-titolo">{{ titolo }}</DialogTitle>
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
            <Label for="appuntamento-paziente" class="text-[13px] font-medium text-(--fg2)">
              Paziente{{ eVisita ? '*' : ' (facoltativo)' }}
            </Label>
            <SelezionaPazienteCombobox id="appuntamento-paziente" v-model="paziente" :aria-invalid="!!errori.paziente" :aria-describedby="errori.paziente ? 'appuntamento-errore-paziente' : undefined" />
            <p v-if="errori.paziente" id="appuntamento-errore-paziente" class="text-xs font-medium text-(--danger)">{{ errori.paziente }}</p>
          </div>

          <template v-if="eBlocco">
            <div class="flex flex-col gap-1.5">
              <Label for="appuntamento-data" class="text-[13px] font-medium text-(--fg2)">Inizio*</Label>
              <div class="grid gap-3 sm:grid-cols-2">
                <div class="flex flex-col gap-1.5">
                  <DatePicker id="appuntamento-data" v-model="data" digitabile class="w-full" aria-label="Data di inizio" :aria-invalid="!!errori.data" :aria-describedby="errori.data ? 'appuntamento-errore-data' : undefined" />
                  <p v-if="errori.data" id="appuntamento-errore-data" class="text-xs font-medium text-(--danger)">{{ errori.data }}</p>
                </div>
                <div v-if="!giornataIntera" class="flex flex-col gap-1.5">
                  <TimePicker
                    id="appuntamento-ora"
                    v-model="ora"
                    min="07:00"
                    max="20:00"
                    aria-label="Ora di inizio"
                    :aria-invalid="!!errori.ora" :aria-describedby="errori.ora ? 'appuntamento-errore-ora' : undefined"
                    data-test="appuntamento-ora"
                  />
                  <p v-if="errori.ora" id="appuntamento-errore-ora" class="text-xs font-medium text-(--danger)">{{ errori.ora }}</p>
                </div>
              </div>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="appuntamento-data-fine" class="text-[13px] font-medium text-(--fg2)">Fine*</Label>
              <div class="grid gap-3 sm:grid-cols-2">
                <div class="flex flex-col gap-1.5">
                  <DatePicker id="appuntamento-data-fine" v-model="dataFine" digitabile class="w-full" aria-label="Data di fine" :aria-invalid="!!errori.dataFine" :aria-describedby="errori.dataFine ? 'appuntamento-errore-data-fine' : undefined" />
                  <p v-if="errori.dataFine" id="appuntamento-errore-data-fine" class="text-xs font-medium text-(--danger)">{{ errori.dataFine }}</p>
                </div>
                <div v-if="!giornataIntera" class="flex flex-col gap-1.5">
                  <TimePicker
                    id="appuntamento-ora-fine"
                    v-model="oraFine"
                    min="07:00"
                    max="20:00"
                    aria-label="Ora di fine"
                    :aria-invalid="!!errori.oraFine" :aria-describedby="errori.oraFine ? 'appuntamento-errore-ora-fine' : undefined"
                    data-test="appuntamento-ora-fine"
                  />
                  <p v-if="errori.oraFine" id="appuntamento-errore-ora-fine" class="text-xs font-medium text-(--danger)">{{ errori.oraFine }}</p>
                </div>
              </div>
            </div>

            <div class="flex items-center gap-2.5">
              <Checkbox id="appuntamento-giornata-intera" v-model="giornataIntera" data-test="appuntamento-giornata-intera" />
              <Label for="appuntamento-giornata-intera" class="cursor-pointer text-sm font-medium text-(--fg)">Giornate intere</Label>
            </div>
          </template>

          <template v-else>
            <div class="grid gap-3 sm:grid-cols-2">
              <div class="flex flex-col gap-1.5">
                <Label for="appuntamento-data" class="text-[13px] font-medium text-(--fg2)">Data*</Label>
                <DatePicker id="appuntamento-data" v-model="data" digitabile class="w-full" :aria-invalid="!!errori.data" :aria-describedby="errori.data ? 'appuntamento-errore-data' : undefined" />
                <p v-if="errori.data" id="appuntamento-errore-data" class="text-xs font-medium text-(--danger)">{{ errori.data }}</p>
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
                  :aria-invalid="!!errori.ora" :aria-describedby="errori.ora ? 'appuntamento-errore-ora' : undefined"
                  data-test="appuntamento-ora"
                />
                <p id="appuntamento-errore-ora" v-if="errori.ora" class="text-xs font-medium text-(--danger)">{{ errori.ora }}</p>
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
          </template>

          <div class="flex flex-col gap-1.5">
            <Label for="appuntamento-testo" class="text-[13px] font-medium text-(--fg2)">
              {{ ePromemoria ? 'Testo*' : eBlocco ? 'Motivo (facoltativo)' : 'Note brevi (facoltative)' }}
            </Label>
            <Textarea
              id="appuntamento-testo"
              v-model="testo"
              rows="3"
              :aria-invalid="!!errori.testo" :aria-describedby="errori.testo ? 'appuntamento-errore-testo' : undefined"
              :placeholder="ePromemoria ? 'Es. Richiamare per i risultati delle analisi' : eBlocco ? 'Es. Pausa pranzo' : 'Es. Controllo mensile'"
              data-test="appuntamento-testo"
            />
            <p id="appuntamento-errore-testo" v-if="errori.testo" class="text-xs font-medium text-(--danger)">{{ errori.testo }}</p>
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
              :aria-invalid="!!errori.link" :aria-describedby="errori.link ? 'appuntamento-errore-link' : undefined"
              placeholder="https://"
              data-test="appuntamento-link"
            />
            <p id="appuntamento-errore-link" v-if="errori.link" class="text-xs font-medium text-(--danger)">{{ errori.link }}</p>
          </div>
        </div>

        <DialogFooter class="flex items-center gap-2.5">
          <Button type="button" variant="neutral" :disabled="inCorso" data-test="appuntamento-annulla" @click="richiediChiusura">
            <X />
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
        <AlertDialogCancel data-test="uscita-continua"><Pencil /> Continua a modificare</AlertDialogCancel>
        <AlertDialogAction data-test="uscita-esci" @click="esciSenzaSalvare">
          <LogOut />
          Esci senza salvare
        </AlertDialogAction>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>

  <AlertDialog v-model:open="sovrapposizioneAperta">
    <AlertDialogContent data-test="sovrapposizione-dialog">
      <AlertDialogHeader>
        <AlertDialogTitle>{{ titoloSovrapposizione }}</AlertDialogTitle>
        <AlertDialogDescription>
          L'orario scelto si sovrappone a {{ conflitti.length === 1 ? 'un altro impegno' : 'altri impegni' }}.
        </AlertDialogDescription>
      </AlertDialogHeader>
      <ul v-if="conflitti.length > 0" class="space-y-1 text-sm text-(--fg2)">
        <li v-for="c in conflitti" :key="c.id">{{ etichettaConflitto(c) }}</li>
      </ul>
      <AlertDialogFooter>
        <AlertDialogCancel data-test="sovrapposizione-cambia"><CalendarClock /> Cambia orario</AlertDialogCancel>
        <AlertDialogAction data-test="sovrapposizione-conferma" @click="confermaComunque">
          <Check />
          Conferma comunque
        </AlertDialogAction>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
</template>
