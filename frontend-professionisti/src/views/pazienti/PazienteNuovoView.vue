<script setup lang="ts">
import { computed, nextTick, ref, type Ref } from 'vue'
import type { AcceptableValue } from 'reka-ui'
import { useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import AppShell from '@/components/AppShell.vue'
import DatiVisitaForm from '@/components/pazienti/DatiVisitaForm.vue'
import { crea } from '@/api/pazienti'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { DatePicker } from '@/components/ui/date-picker'
import { ArrowLeft, Save } from '@lucide/vue'

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
const dataNascita = ref('')
const lavoro = ref('')
const tipoLavoro = ref<'' | 'SEDENTARIO' | 'POCO_ATTIVO' | 'ATTIVO' | 'MOLTO_ATTIVO'>('')

const eta = computed(() => calcolaEta(dataNascita.value))

const datiVisitaForm = ref<InstanceType<typeof DatiVisitaForm>>()

const inCorso = ref(false)
const errori = ref<Record<string, string>>({})

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
  assegna('dataNascita', erroreDataNascita(dataNascita.value))
  assegna('sesso', erroreSesso(sesso.value))

  errori.value = nuoviErrori

  return Object.keys(nuoviErrori).length === 0
}

async function onSubmit() {
  const campiValidi = validaCampi()
  const visitaValida = datiVisitaForm.value?.valida() ?? false
  if (!campiValidi || !visitaValida) return

  inCorso.value = true
  try {
    const paziente = await crea({
      nome: nome.value,
      cognome: cognome.value,
      codiceFiscale: codiceFiscale.value,
      email: email.value,
      telefono: telefono.value || undefined,
      dataNascita: dataNascita.value,
      sesso: sesso.value as 'M' | 'F' | 'ALTRO',
      lavoro: lavoro.value || undefined,
      tipoLavoro: tipoLavoro.value || undefined,
      visita: datiVisitaForm.value!.ottieniDati(),
    })
    toast.success('Paziente creato con successo.')
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
      <router-link to="/pazienti" class="inline-flex items-center gap-2 text-xs font-semibold text-(--fg3) transition-colors hover:text-(--green)">
        <ArrowLeft :size="16" />
        <span>Torna alla lista pazienti</span>
      </router-link>
      <h1 class="font-heading text-3xl italic text-(--fg)">Nuovo paziente</h1>
      <p class="mt-1 text-sm text-(--fg3)">
        Inserisci le informazioni personali e i dati della prima visita per registrare una nuova scheda clinica.
      </p>
    </div>

    <form class="space-y-6" @submit.prevent="onSubmit">
      <div class="rounded-2xl border border-(--bd) bg-(--surf) p-6 shadow-sm sm:p-8">
        <h2 class="font-heading text-xl italic text-(--fg)">Dati anagrafici</h2>

        <div class="mt-5 grid gap-5 sm:grid-cols-2">
          <div class="flex flex-col gap-1.5">
            <Label for="nome" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Nome*</Label>
            <Input id="nome" :model-value="nome" @update:model-value="onNomeInput" type="text" :aria-invalid="!!errori.nome" placeholder="Es. Mario" />
            <p v-if="errori.nome" class="text-xs font-medium text-(--danger)">{{ errori.nome }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="cognome" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Cognome*</Label>
            <Input id="cognome" :model-value="cognome" @update:model-value="onCognomeInput" type="text" :aria-invalid="!!errori.cognome" placeholder="Es. Rossi" />
            <p v-if="errori.cognome" class="text-xs font-medium text-(--danger)">{{ errori.cognome }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="codice-fiscale" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Codice fiscale*</Label>
            <Input id="codice-fiscale" :model-value="codiceFiscale" @update:model-value="onCodiceFiscaleInput" type="text" :aria-invalid="!!errori.codiceFiscale" placeholder="Es. RSSMRA80A01H501U" />
            <p v-if="errori.codiceFiscale" class="text-xs font-medium text-(--danger)">{{ errori.codiceFiscale }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="sesso" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Sesso*</Label>
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
            <Label for="data-nascita" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Data di nascita*</Label>
            <DatePicker id="data-nascita" :model-value="dataNascita" @update:model-value="onDataNascitaChange" />
            <p v-if="errori.dataNascita" class="text-xs font-medium text-(--danger)">{{ errori.dataNascita }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="eta" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Età</Label>
            <Input id="eta" :model-value="eta ?? ''" type="text" disabled placeholder="—" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="email" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Email*</Label>
            <Input id="email" :model-value="email" @update:model-value="onEmailInput" type="text" :aria-invalid="!!errori.email" placeholder="Es. mariorossi@gmail.com" />
            <p v-if="errori.email" class="text-xs font-medium text-(--danger)">{{ errori.email }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="telefono" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Telefono</Label>
            <Input id="telefono" :model-value="telefono" @update:model-value="onTelefonoInput" type="text" inputmode="numeric" :aria-invalid="!!errori.telefono" placeholder="Es. 3325676543" />
            <p v-if="errori.telefono" class="text-xs font-medium text-(--danger)">{{ errori.telefono }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="lavoro" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Lavoro</Label>
            <Input id="lavoro" :model-value="lavoro" @update:model-value="onLavoroInput" type="text" placeholder="Es. Impiegato" />
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="tipo-lavoro" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Tipo lavoro</Label>
            <Select :model-value="tipoLavoro || VALORE_SELEZIONA" @update:model-value="onTipoLavoroChange">
              <SelectTrigger id="tipo-lavoro" class="w-full">
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
      </div>

      <div class="rounded-2xl border border-(--bd) bg-(--surf) p-6 shadow-sm sm:p-8">
        <DatiVisitaForm ref="datiVisitaForm" :sesso="sesso" />
      </div>

      <div class="w-full flex justify-end">
        <Button type="submit" :disabled="inCorso" size="lg" class="hover:bg-primary/80 active:not-aria-[haspopup]:translate-y-0.5">
          <Save :size="16" />
          {{ inCorso ? 'Salvataggio…' : 'Salva paziente' }}
        </Button>
      </div>
    </form>
  </AppShell>
</template>
