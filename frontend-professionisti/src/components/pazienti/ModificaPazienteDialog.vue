<script setup lang="ts">
import { nextTick, ref, watch, type Ref } from 'vue'
import type { AcceptableValue } from 'reka-ui'
import { toast } from 'vue-sonner'
import { aggiorna, type Paziente } from '@/api/pazienti'
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
import { Save } from '@lucide/vue'

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
const dataNascita = ref('')
const lavoro = ref('')
const tipoLavoro = ref<'' | 'SEDENTARIO' | 'POCO_ATTIVO' | 'ATTIVO' | 'MOLTO_ATTIVO'>('')
const note = ref('')

const inCorso = ref(false)
const errori = ref<Record<string, string>>({})

function inizializza() {
  nome.value = props.paziente.nome
  cognome.value = props.paziente.cognome
  codiceFiscale.value = props.paziente.codiceFiscale
  sesso.value = props.paziente.sesso
  email.value = props.paziente.email
  telefono.value = props.paziente.telefono ?? ''
  dataNascita.value = props.paziente.dataNascita ?? ''
  lavoro.value = props.paziente.lavoro ?? ''
  tipoLavoro.value = props.paziente.tipoLavoro ?? ''
  note.value = props.paziente.note ?? ''
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
const onLavoroInput = conFiltro(lavoro, capitalizzaPrimaLettera)

function onDataNascitaChange(valore: string) {
  dataNascita.value = valore
  pulisciErroreSeCorretto('dataNascita', erroreDataNascita, valore)
}

function onSessoChange(valore: AcceptableValue) {
  sesso.value = valore as string
  pulisciErroreSeCorretto('sesso', erroreSesso, valore as string)
}

function onTipoLavoroChange(valore: AcceptableValue) {
  tipoLavoro.value = valore === VALORE_SELEZIONA ? '' : (valore as typeof tipoLavoro.value)
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
  assegna('dataNascita', erroreDataNascita(dataNascita.value))
  assegna('sesso', erroreSesso(sesso.value))

  errori.value = nuoviErrori

  return Object.keys(nuoviErrori).length === 0
}

async function onSubmit() {
  if (!validaCampi()) return

  inCorso.value = true
  try {
    const aggiornato = await aggiorna(props.paziente.id, {
      nome: nome.value,
      cognome: cognome.value,
      codiceFiscale: codiceFiscale.value,
      email: email.value,
      telefono: telefono.value || undefined,
      dataNascita: dataNascita.value,
      sesso: sesso.value as 'M' | 'F' | 'ALTRO',
      lavoro: lavoro.value || undefined,
      tipoLavoro: tipoLavoro.value || undefined,
      note: note.value || undefined,
    })
    toast.success('Dati anagrafici aggiornati con successo.')
    emit('aggiornato', aggiornato)
    emit('update:open', false)
  } catch {
    toast.error('Non è stato possibile aggiornare i dati anagrafici. Controlla i dati e riprova.')
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

      <form class="space-y-5" @submit.prevent="onSubmit">
        <div class="grid gap-5 sm:grid-cols-2">
          <div class="flex flex-col gap-1.5">
            <Label for="modifica-nome" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Nome*</Label>
            <Input id="modifica-nome" :model-value="nome" @update:model-value="onNomeInput" type="text" :aria-invalid="!!errori.nome" placeholder="Es. Mario" />
            <p v-if="errori.nome" class="text-xs font-medium text-(--danger)">{{ errori.nome }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="modifica-cognome" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Cognome*</Label>
            <Input id="modifica-cognome" :model-value="cognome" @update:model-value="onCognomeInput" type="text" :aria-invalid="!!errori.cognome" placeholder="Es. Rossi" />
            <p v-if="errori.cognome" class="text-xs font-medium text-(--danger)">{{ errori.cognome }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="modifica-codice-fiscale" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Codice fiscale*</Label>
            <Input id="modifica-codice-fiscale" :model-value="codiceFiscale" @update:model-value="onCodiceFiscaleInput" type="text" :aria-invalid="!!errori.codiceFiscale" placeholder="Es. RSSMRA80A01H501U" />
            <p v-if="errori.codiceFiscale" class="text-xs font-medium text-(--danger)">{{ errori.codiceFiscale }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="modifica-sesso" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Sesso*</Label>
            <Select :model-value="sesso" @update:model-value="onSessoChange">
              <SelectTrigger id="modifica-sesso" class="w-full" :aria-invalid="!!errori.sesso">
                <SelectValue placeholder="Seleziona" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="M">Maschio</SelectItem>
                <SelectItem value="F">Femmina</SelectItem>
                <SelectItem value="ALTRO">Altro</SelectItem>
              </SelectContent>
            </Select>
            <p v-if="errori.sesso" class="text-xs font-medium text-(--danger)">{{ errori.sesso }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="modifica-data-nascita" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Data di nascita*</Label>
            <DatePicker id="modifica-data-nascita" :model-value="dataNascita" @update:model-value="onDataNascitaChange" />
            <p v-if="errori.dataNascita" class="text-xs font-medium text-(--danger)">{{ errori.dataNascita }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="modifica-email" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Email*</Label>
            <Input id="modifica-email" :model-value="email" @update:model-value="onEmailInput" type="text" :aria-invalid="!!errori.email" placeholder="Es. mariorossi@gmail.com" />
            <p v-if="errori.email" class="text-xs font-medium text-(--danger)">{{ errori.email }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="modifica-telefono" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Telefono</Label>
            <Input id="modifica-telefono" :model-value="telefono" @update:model-value="onTelefonoInput" type="text" inputmode="numeric" :aria-invalid="!!errori.telefono" placeholder="Es. 3325676543" />
            <p v-if="errori.telefono" class="text-xs font-medium text-(--danger)">{{ errori.telefono }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="modifica-lavoro" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Lavoro</Label>
            <Input id="modifica-lavoro" :model-value="lavoro" @update:model-value="onLavoroInput" type="text" placeholder="Es. Impiegato" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="modifica-tipo-lavoro" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Tipo lavoro</Label>
            <Select :model-value="tipoLavoro || VALORE_SELEZIONA" @update:model-value="onTipoLavoroChange">
              <SelectTrigger id="modifica-tipo-lavoro" class="w-full">
                <SelectValue placeholder="Seleziona" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem :value="VALORE_SELEZIONA">Seleziona</SelectItem>
                <SelectItem value="SEDENTARIO">Sedentario</SelectItem>
                <SelectItem value="POCO_ATTIVO">Poco attivo</SelectItem>
                <SelectItem value="ATTIVO">Attivo</SelectItem>
                <SelectItem value="MOLTO_ATTIVO">Molto attivo</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        <div class="flex flex-col gap-1.5">
          <Label for="modifica-note" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Note</Label>
          <Textarea id="modifica-note" v-model="note" maxlength="500" placeholder="Allergie, patologie, indicazioni utili…" />
          <p class="text-right text-xs text-(--fg3)">{{ note.length }}/500</p>
        </div>

        <DialogFooter>
          <Button type="submit" :disabled="inCorso" class="hover:bg-primary/80">
            <Save :size="16" />
            {{ inCorso ? 'Salvataggio…' : 'Salva modifiche' }}
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  </Dialog>
</template>
