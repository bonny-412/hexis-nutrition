<script setup lang="ts">
import { LIMITI_CAMPI } from '@/config/limitiCampi'
import ContatoreCaratteri from '@/components/form/ContatoreCaratteri.vue'
import { nextTick, ref, watch, type Ref } from 'vue'
import type { AcceptableValue } from 'reka-ui'
import { toast } from 'vue-sonner'
import { aggiorna, type Paziente } from '@/api/pazienti'
import { messaggioErroreSalvataggio } from '@/utils/erroreSalvataggio'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { DatePicker } from '@/components/ui/date-picker'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog'
import { Save, X } from '@lucide/vue'

import {
  filtraNome,
  filtraSoloCifre,
  filtraEmail,
  filtraCodiceFiscale,
  capitalizzaPrimaLettera,
  erroreNome,
  erroreCognome,
  erroreEmail,
  erroreTelefono,
  erroreCellulare,
  erroreDataNascita,
  erroreSesso,
  erroreCodiceFiscale,
} from '@/utils/validators'

const props = defineProps<{
  open: boolean
  paziente: Paziente
}>()

const emit = defineEmits<{
  'update:open': [valore: boolean]
  aggiornato: [paziente: Paziente]
}>()

const VALORE_SELEZIONA = '__seleziona__'
const MARCATORE_INVISIBILE = '​'

const nome = ref('')
const cognome = ref('')
const codiceFiscale = ref('')
const sesso = ref('')
const email = ref('')
const telefono = ref('')
const cellulare = ref('')
const dataNascita = ref('')
const lavoro = ref('')
const medicoRiferimento = ref('')
const stileDiVita = ref<'' | 'SEDENTARIO' | 'POCO_ATTIVO' | 'ATTIVO' | 'MOLTO_ATTIVO' | 'ESTREMAMENTE_ATTIVO'>('')
const note = ref('')
const trattamenti = ref('')

const inCorso = ref(false)
const errori = ref<Record<string, string>>({})

function inizializza() {
  nome.value = props.paziente.nome
  cognome.value = props.paziente.cognome
  codiceFiscale.value = props.paziente.codiceFiscale
  sesso.value = props.paziente.sesso
  email.value = props.paziente.email
  telefono.value = props.paziente.telefono ?? ''
  cellulare.value = props.paziente.cellulare ?? ''
  dataNascita.value = props.paziente.dataNascita ?? ''
  lavoro.value = props.paziente.lavoro ?? ''
  medicoRiferimento.value = props.paziente.medicoRiferimento ?? ''
  stileDiVita.value = props.paziente.stileDiVita ?? ''
  note.value = props.paziente.note ?? ''
  trattamenti.value = props.paziente.trattamenti ?? ''
  errori.value = {}
}

watch(
  () => props.open,
  (valore) => {
    if (valore) inizializza()
  },
)

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

const onNomeInput = conFiltro(nome, filtraNome, 'nome', erroreNome)
const onCognomeInput = conFiltro(cognome, filtraNome, 'cognome', erroreCognome)
const onCodiceFiscaleInput = conFiltro(codiceFiscale, filtraCodiceFiscale, 'codiceFiscale', erroreCodiceFiscale)
const onEmailInput = conFiltro(email, filtraEmail, 'email', erroreEmail)
const onTelefonoInput = conFiltro(telefono, (v) => filtraSoloCifre(v, 10), 'telefono', erroreTelefono)
const onCellulareInput = conFiltro(cellulare, (v) => filtraSoloCifre(v, 10), 'cellulare', erroreCellulare)
const onLavoroInput = conFiltro(lavoro, capitalizzaPrimaLettera)
const onMedicoRiferimentoInput = conFiltro(medicoRiferimento, capitalizzaPrimaLettera)

function onDataNascitaChange(valore: string) {
  dataNascita.value = valore
  pulisciErroreSeCorretto('dataNascita', erroreDataNascita, valore)
}

function onSessoChange(valore: AcceptableValue) {
  sesso.value = valore as string
  pulisciErroreSeCorretto('sesso', erroreSesso, valore as string)
}

function onStileDiVitaChange(valore: AcceptableValue) {
  stileDiVita.value = valore === VALORE_SELEZIONA ? '' : (valore as typeof stileDiVita.value)
}

function validaCampi(): boolean {
  const nuoviErrori: Record<string, string> = {}

  const assegna = (chiave: string, messaggio: string | undefined) => {
    if (messaggio) nuoviErrori[chiave] = messaggio
  }

  assegna('nome', erroreNome(nome.value))
  assegna('cognome', erroreCognome(cognome.value))
  assegna('codiceFiscale', erroreCodiceFiscale(codiceFiscale.value))
  assegna('email', erroreEmail(email.value))
  assegna('telefono', erroreTelefono(telefono.value))
  assegna('cellulare', erroreCellulare(cellulare.value))
  assegna('dataNascita', erroreDataNascita(dataNascita.value))
  assegna('sesso', erroreSesso(sesso.value))

  errori.value = nuoviErrori

  return Object.keys(nuoviErrori).length === 0
}

async function onSubmit() {
  if (inCorso.value || !validaCampi()) return

  inCorso.value = true
  try {
    const aggiornato = await aggiorna(props.paziente.id, {
      nome: nome.value,
      cognome: cognome.value,
      codiceFiscale: codiceFiscale.value,
      email: email.value,
      telefono: telefono.value || undefined,
      cellulare: cellulare.value || undefined,
      dataNascita: dataNascita.value,
      sesso: sesso.value as 'M' | 'F' | 'ALTRO',
      lavoro: lavoro.value || undefined,
      medicoRiferimento: medicoRiferimento.value || undefined,
      stileDiVita: stileDiVita.value || undefined,
      note: note.value || undefined,
      trattamenti: trattamenti.value || undefined,
    })
    toast.success('Dati anagrafici aggiornati.')
    emit('aggiornato', aggiornato)
    emit('update:open', false)
  } catch (errore) {
    toast.error(messaggioErroreSalvataggio(errore, 'Non è stato possibile aggiornare i dati anagrafici. Controlla i dati e riprova.'))
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <Dialog :open="open" @update:open="(valore) => emit('update:open', valore)">
    <DialogContent class="sm:max-w-2xl">
      <DialogHeader>
        <DialogTitle>Modifica dati anagrafici</DialogTitle>
      </DialogHeader>

      <form class="flex min-h-0 flex-1 flex-col gap-4" @submit.prevent="onSubmit">
        <div class="min-h-0 flex-1 space-y-5 overflow-y-auto px-0.5 py-0.5">
          <div class="grid gap-5 sm:grid-cols-2">
            <div class="flex flex-col gap-1.5">
              <Label for="modifica-nome" class="text-[13px] font-medium text-(--fg2)">Nome*</Label>
              <Input id="modifica-nome" :model-value="nome" @update:model-value="onNomeInput" type="text" :aria-invalid="!!errori.nome" :aria-describedby="errori.nome ? 'modifica-paziente-errore-nome' : undefined" placeholder="Es. Mario" :maxlength="LIMITI_CAMPI.nomePersona" />
              <p id="modifica-paziente-errore-nome" v-if="errori.nome" class="text-xs font-medium text-(--danger)">{{ errori.nome }}</p>
              <ContatoreCaratteri :valore="nome" :max="LIMITI_CAMPI.nomePersona" />
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="modifica-cognome" class="text-[13px] font-medium text-(--fg2)">Cognome*</Label>
              <Input id="modifica-cognome" :model-value="cognome" @update:model-value="onCognomeInput" type="text" :aria-invalid="!!errori.cognome" :aria-describedby="errori.cognome ? 'modifica-paziente-errore-cognome' : undefined" placeholder="Es. Rossi" :maxlength="LIMITI_CAMPI.nomePersona" />
              <p id="modifica-paziente-errore-cognome" v-if="errori.cognome" class="text-xs font-medium text-(--danger)">{{ errori.cognome }}</p>
              <ContatoreCaratteri :valore="cognome" :max="LIMITI_CAMPI.nomePersona" />
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="modifica-codice-fiscale" class="text-[13px] font-medium text-(--fg2)">Codice fiscale*</Label>
              <Input id="modifica-codice-fiscale" :model-value="codiceFiscale" @update:model-value="onCodiceFiscaleInput" type="text" :aria-invalid="!!errori.codiceFiscale" :aria-describedby="errori.codiceFiscale ? 'modifica-paziente-errore-codiceFiscale' : undefined" placeholder="Es. RSSMRA80A01H501U" />
              <p id="modifica-paziente-errore-codiceFiscale" v-if="errori.codiceFiscale" class="text-xs font-medium text-(--danger)">{{ errori.codiceFiscale }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="modifica-sesso" class="text-[13px] font-medium text-(--fg2)">Sesso*</Label>
              <Select :model-value="sesso" @update:model-value="onSessoChange">
                <SelectTrigger id="modifica-sesso" class="w-full" :aria-invalid="!!errori.sesso" :aria-describedby="errori.sesso ? 'modifica-paziente-errore-sesso' : undefined">
                  <SelectValue placeholder="Seleziona" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="M">Maschio</SelectItem>
                  <SelectItem value="F">Femmina</SelectItem>
                  <SelectItem value="ALTRO">Altro</SelectItem>
                </SelectContent>
              </Select>
              <p id="modifica-paziente-errore-sesso" v-if="errori.sesso" class="text-xs font-medium text-(--danger)">{{ errori.sesso }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="modifica-data-nascita" class="text-[13px] font-medium text-(--fg2)">Data di nascita*</Label>
              <DatePicker id="modifica-data-nascita" :model-value="dataNascita" :aria-invalid="!!errori.dataNascita" :aria-describedby="errori.dataNascita ? 'modifica-paziente-errore-data-nascita' : undefined" @update:model-value="onDataNascitaChange" />
              <p v-if="errori.dataNascita" id="modifica-paziente-errore-data-nascita" class="text-xs font-medium text-(--danger)">{{ errori.dataNascita }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="modifica-email" class="text-[13px] font-medium text-(--fg2)">Email*</Label>
              <Input id="modifica-email" :model-value="email" @update:model-value="onEmailInput" type="text" :aria-invalid="!!errori.email" :aria-describedby="errori.email ? 'modifica-paziente-errore-email' : undefined" placeholder="Es. mariorossi@gmail.com" :maxlength="LIMITI_CAMPI.email" />
              <p id="modifica-paziente-errore-email" v-if="errori.email" class="text-xs font-medium text-(--danger)">{{ errori.email }}</p>
              <ContatoreCaratteri :valore="email" :max="LIMITI_CAMPI.email" />
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="modifica-telefono" class="text-[13px] font-medium text-(--fg2)">Telefono</Label>
              <Input id="modifica-telefono" :model-value="telefono" @update:model-value="onTelefonoInput" type="text" inputmode="numeric" :aria-invalid="!!errori.telefono" :aria-describedby="errori.telefono ? 'modifica-paziente-errore-telefono' : undefined" placeholder="Es. 0212345678" />
              <p id="modifica-paziente-errore-telefono" v-if="errori.telefono" class="text-xs font-medium text-(--danger)">{{ errori.telefono }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="modifica-cellulare" class="text-[13px] font-medium text-(--fg2)">Cellulare</Label>
              <Input id="modifica-cellulare" :model-value="cellulare" @update:model-value="onCellulareInput" type="text" inputmode="numeric" :aria-invalid="!!errori.cellulare" :aria-describedby="errori.cellulare ? 'modifica-paziente-errore-cellulare' : undefined" placeholder="Es. 3325676543" />
              <p id="modifica-paziente-errore-cellulare" v-if="errori.cellulare" class="text-xs font-medium text-(--danger)">{{ errori.cellulare }}</p>
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="modifica-lavoro" class="text-[13px] font-medium text-(--fg2)">Lavoro</Label>
              <Input id="modifica-lavoro" :model-value="lavoro" @update:model-value="onLavoroInput" type="text" placeholder="Es. Impiegato" :maxlength="LIMITI_CAMPI.lavoro" />
              <ContatoreCaratteri :valore="lavoro" :max="LIMITI_CAMPI.lavoro" />
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="modifica-medico-riferimento" class="text-[13px] font-medium text-(--fg2)">Medico di riferimento</Label>
              <Input id="modifica-medico-riferimento" :model-value="medicoRiferimento" @update:model-value="onMedicoRiferimentoInput" type="text" placeholder="Es. Dott. Mario Bianchi" :maxlength="LIMITI_CAMPI.medicoRiferimento" />
              <ContatoreCaratteri :valore="medicoRiferimento" :max="LIMITI_CAMPI.medicoRiferimento" />
            </div>

            <div class="flex flex-col gap-1.5">
              <Label for="modifica-stile-di-vita" class="text-[13px] font-medium text-(--fg2)">Livello di attività</Label>
              <Select :model-value="stileDiVita || VALORE_SELEZIONA" @update:model-value="onStileDiVitaChange">
                <SelectTrigger id="modifica-stile-di-vita" class="w-full" :class="{ 'text-(--fg3)': !stileDiVita }">
                  <SelectValue placeholder="Seleziona" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem :value="VALORE_SELEZIONA">Non indicato</SelectItem>
                  <SelectItem value="SEDENTARIO">Sedentario</SelectItem>
                  <SelectItem value="POCO_ATTIVO">Poco attivo</SelectItem>
                  <SelectItem value="ATTIVO">Attivo</SelectItem>
                  <SelectItem value="MOLTO_ATTIVO">Molto attivo</SelectItem>
                  <SelectItem value="ESTREMAMENTE_ATTIVO">Estremamente attivo</SelectItem>
                </SelectContent>
              </Select>
            </div>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="modifica-note" class="text-[13px] font-medium text-(--fg2)">Storia del paziente</Label>
            <Textarea id="modifica-note" v-model="note" placeholder="Es. vive da solo, fuma, beve, è autonomo, allergie, patologie…" :maxlength="LIMITI_CAMPI.notePaziente" />
            <ContatoreCaratteri :valore="note" :max="LIMITI_CAMPI.notePaziente" sempre />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="modifica-trattamenti" class="text-[13px] font-medium text-(--fg2)">Terapie in corso</Label>
            <Textarea id="modifica-trattamenti" v-model="trattamenti" placeholder="Es. terapie farmacologiche in corso, integratori, trattamenti…" :maxlength="LIMITI_CAMPI.trattamenti" />
            <ContatoreCaratteri :valore="trattamenti" :max="LIMITI_CAMPI.trattamenti" sempre />
          </div>
        </div>

        <DialogFooter class="flex items-center gap-2.5">
          <Button type="button" variant="neutral" :disabled="inCorso" @click="emit('update:open', false)">
            <X />
            Annulla
          </Button>
          <Button type="submit" :disabled="inCorso">
            <Save :size="16" />
            {{ inCorso ? 'Salvataggio…' : 'Salva modifiche' }}
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  </Dialog>
</template>
