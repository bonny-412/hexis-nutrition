<script setup lang="ts">
import { scorriAlPrimoErrore } from '@/utils/form'
import { computed, nextTick, ref, type Ref } from 'vue'
import type { AcceptableValue } from 'reka-ui'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import AppShell from '@/components/AppShell.vue'
import DatiVisitaForm from '@/components/pazienti/DatiVisitaForm.vue'
import BarraAzioniForm from '@/components/form/BarraAzioniForm.vue'
import { crea } from '@/api/pazienti'
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
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction,
} from '@/components/ui/alert-dialog'
import { ArrowLeft } from '@lucide/vue'

// Import di tutti i filtri, regex e validatori dal file esterno
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
import { calcolaEta } from '@/utils/data'

// --- STATO DEL FORM ---
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

const eta = computed(() => calcolaEta(dataNascita.value))

const datiVisitaForm = ref<InstanceType<typeof DatiVisitaForm>>()

const inCorso = ref(false)
const errori = ref<Record<string, string>>({})
const mostraConfermaAnnulla = ref(false)

const router = useRouter()

const VALORE_SELEZIONA = '__seleziona__'

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

// Handlers specifici
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

// --- ANNULLA ---
function formValorizzata(): boolean {
  return (
    nome.value !== '' ||
    cognome.value !== '' ||
    codiceFiscale.value !== '' ||
    sesso.value !== '' ||
    email.value !== '' ||
    telefono.value !== '' ||
    cellulare.value !== '' ||
    dataNascita.value !== '' ||
    lavoro.value !== '' ||
    medicoRiferimento.value !== '' ||
    stileDiVita.value !== '' ||
    note.value !== '' ||
    trattamenti.value !== '' ||
    (datiVisitaForm.value?.valorizzato() ?? false)
  )
}

function tornaIndietro() {
  if (router.options.history.state.back) {
    router.back()
  } else {
    router.push('/pazienti')
  }
}

function onAnnullaClick() {
  if (formValorizzata()) {
    mostraConfermaAnnulla.value = true
  } else {
    tornaIndietro()
  }
}

function confermaAnnulla() {
  mostraConfermaAnnulla.value = false
  tornaIndietro()
}

// --- VALIDAZIONE E SUBMIT ---
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
  const campiValidi = validaCampi()
  const visitaValida = datiVisitaForm.value?.valida() ?? false
  if (!campiValidi || !visitaValida) {
    scorriAlPrimoErrore()
    return
  }

  inCorso.value = true
  try {
    const paziente = await crea({
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
      visita: datiVisitaForm.value!.ottieniDati(),
    })
    toast.success('Paziente creato.')
    router.push(`/pazienti/${paziente.id}`)
  } catch {
    toast.error('Non è stato possibile creare il paziente. Controlla i dati e riprova.')
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <AppShell>
    <div class="mb-6">
      <router-link to="/pazienti" class="group inline-flex items-center gap-1.5 text-xs font-semibold text-(--fg3) transition-colors hover:text-(--green)">
        <ArrowLeft :size="14" aria-hidden="true" class="transition-transform duration-150 motion-safe:group-hover:-translate-x-0.5" />
        <span>Torna alla lista pazienti</span>
      </router-link>
      <h1 class="font-heading text-3xl italic text-(--fg)">Nuovo paziente</h1>
      <p class="mt-1 text-sm text-(--fg3)">
        Inserisci le informazioni personali e i dati della prima visita per registrare una nuova scheda clinica.
      </p>
    </div>

    <form class="space-y-6" @submit.prevent="onSubmit">
      <div class="rounded-2xl border border-(--bd) bg-(--surf) p-6 sm:p-8">
        <div class="w-full flex items-center">
          <!-- <div class="flex size-8 shrink-0 items-center justify-center  text-(--green)">
            <User :size="18" />
          </div> -->
          <h2 class="font-heading text-xl italic text-(--fg)">Dati anagrafici</h2>
        </div>

        <div class="mt-5 grid gap-5 sm:grid-cols-2">
          <div class="flex flex-col gap-1.5">
            <Label for="nome" class="text-[13px] font-medium text-(--fg2)">Nome*</Label>
            <Input id="nome" :model-value="nome" @update:model-value="onNomeInput" type="text" :aria-invalid="!!errori.nome" placeholder="Es. Mario" />
            <p v-if="errori.nome" class="text-xs font-medium text-(--danger)">{{ errori.nome }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="cognome" class="text-[13px] font-medium text-(--fg2)">Cognome*</Label>
            <Input id="cognome" :model-value="cognome" @update:model-value="onCognomeInput" type="text" :aria-invalid="!!errori.cognome" placeholder="Es. Rossi" />
            <p v-if="errori.cognome" class="text-xs font-medium text-(--danger)">{{ errori.cognome }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="codice-fiscale" class="text-[13px] font-medium text-(--fg2)">Codice fiscale*</Label>
            <Input id="codice-fiscale" :model-value="codiceFiscale" @update:model-value="onCodiceFiscaleInput" type="text" :aria-invalid="!!errori.codiceFiscale" placeholder="Es. RSSMRA80A01H501U" />
            <p v-if="errori.codiceFiscale" class="text-xs font-medium text-(--danger)">{{ errori.codiceFiscale }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="sesso" class="text-[13px] font-medium text-(--fg2)">Sesso*</Label>
            <Select :model-value="sesso" @update:model-value="onSessoChange">
              <SelectTrigger id="sesso" class="w-full" :aria-invalid="!!errori.sesso">
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
            <Label for="data-nascita" class="text-[13px] font-medium text-(--fg2)">Data di nascita*</Label>
            <DatePicker id="data-nascita" :model-value="dataNascita" @update:model-value="onDataNascitaChange" />
            <p v-if="errori.dataNascita" class="text-xs font-medium text-(--danger)">{{ errori.dataNascita }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="eta" class="text-[13px] font-medium text-(--fg2)">Età</Label>
            <Input id="eta" :model-value="eta ?? ''" type="text" disabled placeholder="—" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="email" class="text-[13px] font-medium text-(--fg2)">Email*</Label>
            <Input id="email" :model-value="email" @update:model-value="onEmailInput" type="text" :aria-invalid="!!errori.email" placeholder="Es. mariorossi@gmail.com" />
            <p v-if="errori.email" class="text-xs font-medium text-(--danger)">{{ errori.email }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="telefono" class="text-[13px] font-medium text-(--fg2)">Telefono</Label>
            <Input id="telefono" :model-value="telefono" @update:model-value="onTelefonoInput" type="text" inputmode="numeric" :aria-invalid="!!errori.telefono" placeholder="Es. 0212345678" />
            <p v-if="errori.telefono" class="text-xs font-medium text-(--danger)">{{ errori.telefono }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="cellulare" class="text-[13px] font-medium text-(--fg2)">Cellulare</Label>
            <Input id="cellulare" :model-value="cellulare" @update:model-value="onCellulareInput" type="text" inputmode="numeric" :aria-invalid="!!errori.cellulare" placeholder="Es. 3325676543" />
            <p v-if="errori.cellulare" class="text-xs font-medium text-(--danger)">{{ errori.cellulare }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="lavoro" class="text-[13px] font-medium text-(--fg2)">Lavoro</Label>
            <Input id="lavoro" :model-value="lavoro" @update:model-value="onLavoroInput" type="text" placeholder="Es. Impiegato" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="medico-riferimento" class="text-[13px] font-medium text-(--fg2)">Medico di riferimento</Label>
            <Input id="medico-riferimento" :model-value="medicoRiferimento" @update:model-value="onMedicoRiferimentoInput" type="text" placeholder="Es. Dott. Mario Bianchi" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="stile-di-vita" class="text-[13px] font-medium text-(--fg2)">Stile di vita</Label>
            <Select :model-value="stileDiVita || VALORE_SELEZIONA" @update:model-value="onStileDiVitaChange">
              <SelectTrigger id="stile-di-vita" class="w-full">
                <SelectValue placeholder="Seleziona" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem :value="VALORE_SELEZIONA">Seleziona</SelectItem>
                <SelectItem value="SEDENTARIO">Sedentario</SelectItem>
                <SelectItem value="POCO_ATTIVO">Poco attivo</SelectItem>
                <SelectItem value="ATTIVO">Attivo</SelectItem>
                <SelectItem value="MOLTO_ATTIVO">Molto attivo</SelectItem>
                <SelectItem value="ESTREMAMENTE_ATTIVO">Estremamente attivo</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </div>

        <div class="mt-5 flex flex-col gap-1.5">
          <Label for="note" class="text-[13px] font-medium text-(--fg2)">Storia paziente</Label>
          <Textarea id="note" v-model="note" maxlength="2000" placeholder="Es. vive da solo, fuma, beve, è autonomo, allergie, patologie…" />
          <p class="text-right text-xs text-(--fg3)">{{ note.length }}/2000</p>
        </div>

        <div class="mt-5 flex flex-col gap-1.5">
          <Label for="trattamenti" class="text-[13px] font-medium text-(--fg2)">Trattamenti/Terapie</Label>
          <Textarea id="trattamenti" v-model="trattamenti" maxlength="2000" placeholder="Es. terapie farmacologiche in corso, integratori, trattamenti…" />
          <p class="text-right text-xs text-(--fg3)">{{ trattamenti.length }}/2000</p>
        </div>
      </div>

      <div class="rounded-2xl border border-(--bd) bg-(--surf) p-6 sm:p-8">
        <DatiVisitaForm ref="datiVisitaForm" :sesso="sesso" />
      </div>

      <BarraAzioniForm
        :modificato="formValorizzata()"
        :in-corso="inCorso"
        etichetta-salva="Salva paziente"
        @annulla="onAnnullaClick"
      />
    </form>

    <AlertDialog v-model:open="mostraConfermaAnnulla">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Uscire senza salvare?</AlertDialogTitle>
          <AlertDialogDescription>
            Hai inserito dei dati che non sono stati salvati. Se esci ora andranno persi.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel data-test="conferma-annulla-continua">Continua a compilare</AlertDialogCancel>
          <AlertDialogAction data-test="conferma-annulla-esci" @click="confermaAnnulla">Esci senza salvare</AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </AppShell>
</template>
