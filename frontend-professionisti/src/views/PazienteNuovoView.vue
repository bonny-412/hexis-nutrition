<script setup lang="ts">
import { nextTick, ref, type Ref } from 'vue'
import { useRouter } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
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
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion'
import { DatePicker } from '@/components/ui/date-picker'
import { AlertCircle, ArrowLeft, Ruler, Save } from '@lucide/vue'

// Import di tutti i filtri, regex e validatori dal file esterno
import {
  numeroItaliano,
  numeroItalianoOpzionale,
  filtraNome,
  filtraSoloCifre,
  filtraDecimaleItaliano,
  filtraEmail,
  capitalizzaPrimaLettera,
  erroreNome,
  erroreCognome,
  erroreEmail,
  erroreTelefono,
  erroreAltezza,
  errorePeso,
  erroreCirconferenza,
} from '@/utils/validators'

// --- STATO DEL FORM ---
const nome = ref('')
const cognome = ref('')
const sesso = ref('')
const email = ref('')
const telefono = ref('')
const dataNascita = ref('')
const lavoro = ref('')
const tipoLavoro = ref<'' | 'SEDENTARIO' | 'POCO_ATTIVO' | 'ATTIVO' | 'MOLTO_ATTIVO'>('')

const dataVisita = ref(new Date().toISOString().slice(0, 10))
const altezzaCm = ref('')
const pesoKg = ref('')
const circonferenzaVita = ref('')
const circonferenzaOmbelico = ref('')
const circonferenzaFianchi = ref('')
const circonferenzaPetto = ref('')
const circonferenzaCosciaDx = ref('')
const circonferenzaCosciaSx = ref('')
const circonferenzaPolpaccioDx = ref('')
const circonferenzaPolpaccioSx = ref('')
const larghezzaSpalle = ref('')
const circonferenzaSpalle = ref('')
const circonferenzaBicipiteDx = ref('')
const circonferenzaBicipiteSx = ref('')

const inCorso = ref(false)
const errore = ref('')
const errori = ref<Record<string, string>>({})
const accordionAperto = ref('')

const router = useRouter()

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
const onEmailInput = conFiltro(email, filtraEmail, 'email', erroreEmail)
const onTelefonoInput = conFiltro(telefono, (v) => filtraSoloCifre(v, 10), 'telefono', erroreTelefono)
const onLavoroInput = conFiltro(lavoro, capitalizzaPrimaLettera)
const onAltezzaInput = conFiltro(altezzaCm, (v) => filtraSoloCifre(v, 3), 'altezzaCm', erroreAltezza)
const onPesoInput = conFiltro(pesoKg, filtraDecimaleItaliano, 'pesoKg', errorePeso)

// Helper compatto per le 12 circonferenze
const creaHandlerCirconferenza = (rif: Ref<string>, chiave: string) =>
  conFiltro(rif, filtraDecimaleItaliano, chiave, erroreCirconferenza)

const onCirconferenzaVitaInput = creaHandlerCirconferenza(circonferenzaVita, 'circonferenzaVita')
const onCirconferenzaOmbelicoInput = creaHandlerCirconferenza(circonferenzaOmbelico, 'circonferenzaOmbelico')
const onCirconferenzaFianchiInput = creaHandlerCirconferenza(circonferenzaFianchi, 'circonferenzaFianchi')
const onCirconferenzaPettoInput = creaHandlerCirconferenza(circonferenzaPetto, 'circonferenzaPetto')
const onCirconferenzaCosciaDxInput = creaHandlerCirconferenza(circonferenzaCosciaDx, 'circonferenzaCosciaDx')
const onCirconferenzaCosciaSxInput = creaHandlerCirconferenza(circonferenzaCosciaSx, 'circonferenzaCosciaSx')
const onCirconferenzaPolpaccioDxInput = creaHandlerCirconferenza(circonferenzaPolpaccioDx, 'circonferenzaPolpaccioDx')
const onCirconferenzaPolpaccioSxInput = creaHandlerCirconferenza(circonferenzaPolpaccioSx, 'circonferenzaPolpaccioSx')
const onLarghezzaSpalleInput = creaHandlerCirconferenza(larghezzaSpalle, 'larghezzaSpalle')
const onCirconferenzaSpalleInput = creaHandlerCirconferenza(circonferenzaSpalle, 'circonferenzaSpalle')
const onCirconferenzaBicipiteDxInput = creaHandlerCirconferenza(circonferenzaBicipiteDx, 'circonferenzaBicipiteDx')
const onCirconferenzaBicipiteSxInput = creaHandlerCirconferenza(circonferenzaBicipiteSx, 'circonferenzaBicipiteSx')

// --- VALIDAZIONE E SUBMIT ---
function validaCampi(): boolean {
  const nuoviErrori: Record<string, string> = {}

  const assegna = (chiave: string, messaggio: string | undefined) => {
    if (messaggio) nuoviErrori[chiave] = messaggio
  }

  assegna('nome', erroreNome(nome.value))
  assegna('cognome', erroreCognome(cognome.value))
  assegna('email', erroreEmail(email.value))
  assegna('telefono', erroreTelefono(telefono.value))
  assegna('altezzaCm', erroreAltezza(altezzaCm.value))
  assegna('pesoKg', errorePeso(pesoKg.value))

  const circonferenze: Array<[string, string]> = [
    ['circonferenzaVita', circonferenzaVita.value],
    ['circonferenzaOmbelico', circonferenzaOmbelico.value],
    ['circonferenzaFianchi', circonferenzaFianchi.value],
    ['circonferenzaPetto', circonferenzaPetto.value],
    ['circonferenzaCosciaDx', circonferenzaCosciaDx.value],
    ['circonferenzaCosciaSx', circonferenzaCosciaSx.value],
    ['circonferenzaPolpaccioDx', circonferenzaPolpaccioDx.value],
    ['circonferenzaPolpaccioSx', circonferenzaPolpaccioSx.value],
    ['larghezzaSpalle', larghezzaSpalle.value],
    ['circonferenzaSpalle', circonferenzaSpalle.value],
    ['circonferenzaBicipiteDx', circonferenzaBicipiteDx.value],
    ['circonferenzaBicipiteSx', circonferenzaBicipiteSx.value],
  ]
  for (const [chiave, valore] of circonferenze) {
    assegna(chiave, erroreCirconferenza(valore))
  }

  errori.value = nuoviErrori

  if (circonferenze.some(([chiave]) => nuoviErrori[chiave])) {
    accordionAperto.value = 'circonferenze'
  }

  return Object.keys(nuoviErrori).length === 0
}

async function onSubmit() {
  if (!validaCampi()) return

  inCorso.value = true
  errore.value = ''
  try {
    const paziente = await crea({
      nome: nome.value,
      cognome: cognome.value,
      email: email.value,
      telefono: telefono.value || undefined,
      dataNascita: dataNascita.value || undefined,
      sesso: sesso.value || undefined,
      lavoro: lavoro.value || undefined,
      tipoLavoro: tipoLavoro.value || undefined,
      visita: {
        dataVisita: dataVisita.value || undefined,
        altezzaCm: numeroItaliano(altezzaCm.value),
        pesoKg: numeroItaliano(pesoKg.value),
        circonferenzaVitaCm: numeroItalianoOpzionale(circonferenzaVita.value),
        circonferenzaOmbelicoCm: numeroItalianoOpzionale(circonferenzaOmbelico.value),
        circonferenzaFianchiCm: numeroItalianoOpzionale(circonferenzaFianchi.value),
        circonferenzaPettoCm: numeroItalianoOpzionale(circonferenzaPetto.value),
        circonferenzaCosciaDxCm: numeroItalianoOpzionale(circonferenzaCosciaDx.value),
        circonferenzaCosciaSxCm: numeroItalianoOpzionale(circonferenzaCosciaSx.value),
        circonferenzaPolpaccioDxCm: numeroItalianoOpzionale(circonferenzaPolpaccioDx.value),
        circonferenzaPolpaccioSxCm: numeroItalianoOpzionale(circonferenzaPolpaccioSx.value),
        larghezzaSpalleCm: numeroItalianoOpzionale(larghezzaSpalle.value),
        circonferenzaSpalleCm: numeroItalianoOpzionale(circonferenzaSpalle.value),
        circonferenzaBicipiteDxCm: numeroItalianoOpzionale(circonferenzaBicipiteDx.value),
        circonferenzaBicipiteSxCm: numeroItalianoOpzionale(circonferenzaBicipiteSx.value),
      },
    })
    router.push(`/pazienti/${paziente.id}`)
  } catch {
    errore.value = 'Non è stato possibile creare il paziente. Controlla i dati e riprova.'
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <AppShell>
    <div class="mb-6">
      <router-link to="/pazienti"
          class="inline-flex items-center gap-2 text-xs font-semibold text-(--fg3) transition-colors hover:text-(--green)"
        >
          <ArrowLeft :size="16" />
          <span>Torna alla lista pazienti</span>
        </router-link>
        <h1 class="font-heading text-3xl italic text-(--fg)">Nuovo paziente</h1>
        <p class="mt-1 text-sm text-(--fg3)">
          Inserisci le informazioni personali e i dati della prima visita per registrare una nuova scheda clinica.
        </p>
    </div>

    <div v-if="errore" class="mb-6 flex items-start gap-3 rounded-xl border border-(--danger)/20 bg-(--warn-bg) p-3.5 text-xs font-medium text-(--danger)">
      <AlertCircle :size="16" class="mt-0.5 shrink-0" />
      <span>{{ errore }}</span>
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
            <Label for="sesso" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Sesso</Label>
            <Select v-model="sesso">
              <SelectTrigger id="sesso" class="w-full">
                <SelectValue placeholder="Seleziona" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="M">Maschio</SelectItem>
                <SelectItem value="F">Femmina</SelectItem>
              </SelectContent>
            </Select>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="data-nascita" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Data di nascita</Label>
            <DatePicker id="data-nascita" v-model="dataNascita"  />
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
            <Select v-model="tipoLavoro">
              <SelectTrigger id="tipo-lavoro" class="w-full">
                <SelectValue placeholder="Seleziona" />
              </SelectTrigger>
              <SelectContent>
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
                  <Label for="circonferenza-ombelico" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza ombelico</Label>
                  <Input id="circonferenza-ombelico" :model-value="circonferenzaOmbelico" @update:model-value="onCirconferenzaOmbelicoInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaOmbelico" />
                  <p v-if="errori.circonferenzaOmbelico" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaOmbelico }}</p>
                </div>

                <div class="flex flex-col gap-1.5">
                  <Label for="circonferenza-fianchi" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza fianchi</Label>
                  <Input id="circonferenza-fianchi" :model-value="circonferenzaFianchi" @update:model-value="onCirconferenzaFianchiInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaFianchi" />
                  <p v-if="errori.circonferenzaFianchi" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaFianchi }}</p>
                </div>

                <div class="flex flex-col gap-1.5">
                  <Label for="circonferenza-petto" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza petto</Label>
                  <Input id="circonferenza-petto" :model-value="circonferenzaPetto" @update:model-value="onCirconferenzaPettoInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaPetto" />
                  <p v-if="errori.circonferenzaPetto" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaPetto }}</p>
                </div>

                <div class="flex flex-col gap-1.5">
                  <Label for="circonferenza-coscia-dx" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza coscia dx</Label>
                  <Input id="circonferenza-coscia-dx" :model-value="circonferenzaCosciaDx" @update:model-value="onCirconferenzaCosciaDxInput" placeholder="Es. 78,50" type="text" inputmode="decimal" :aria-invalid="!!errori.circonferenzaCosciaDx" />
                  <p v-if="errori.circonferenzaCosciaDx" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaCosciaDx }}</p>
                </div>

                <div class="flex flex-col gap-1.5">
                  <Label for="circonferenza-coscia-sx" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza coscia sx</Label>
                  <Input id="circonferenza-coscia-sx" :model-value="circonferenzaCosciaSx" @update:model-value="onCirconferenzaCosciaSxInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaCosciaSx" />
                  <p v-if="errori.circonferenzaCosciaSx" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaCosciaSx }}</p>
                </div>

                <div class="flex flex-col gap-1.5">
                  <Label for="circonferenza-polpaccio-dx" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza polpaccio dx</Label>
                  <Input id="circonferenza-polpaccio-dx" :model-value="circonferenzaPolpaccioDx" @update:model-value="onCirconferenzaPolpaccioDxInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaPolpaccioDx" />
                  <p v-if="errori.circonferenzaPolpaccioDx" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaPolpaccioDx }}</p>
                </div>

                <div class="flex flex-col gap-1.5">
                  <Label for="circonferenza-polpaccio-sx" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza polpaccio sx</Label>
                  <Input id="circonferenza-polpaccio-sx" :model-value="circonferenzaPolpaccioSx" @update:model-value="onCirconferenzaPolpaccioSxInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaPolpaccioSx" />
                  <p v-if="errori.circonferenzaPolpaccioSx" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaPolpaccioSx }}</p>
                </div>

                <div class="flex flex-col gap-1.5">
                  <Label for="larghezza-spalle" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Larghezza spalle</Label>
                  <Input id="larghezza-spalle" :model-value="larghezzaSpalle" @update:model-value="onLarghezzaSpalleInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.larghezzaSpalle" />
                  <p v-if="errori.larghezzaSpalle" class="text-xs font-medium text-(--danger)">{{ errori.larghezzaSpalle }}</p>
                </div>

                <div class="flex flex-col gap-1.5">
                  <Label for="circonferenza-spalle" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza spalle</Label>
                  <Input id="circonferenza-spalle" :model-value="circonferenzaSpalle" @update:model-value="onCirconferenzaSpalleInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaSpalle" />
                  <p v-if="errori.circonferenzaSpalle" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaSpalle }}</p>
                </div>

                <div class="flex flex-col gap-1.5">
                  <Label for="circonferenza-bicipite-dx" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza bicipite dx</Label>
                  <Input id="circonferenza-bicipite-dx" :model-value="circonferenzaBicipiteDx" @update:model-value="onCirconferenzaBicipiteDxInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaBicipiteDx" />
                  <p v-if="errori.circonferenzaBicipiteDx" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaBicipiteDx }}</p>
                </div>

                <div class="flex flex-col gap-1.5">
                  <Label for="circonferenza-bicipite-sx" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenza bicipite sx</Label>
                  <Input id="circonferenza-bicipite-sx" :model-value="circonferenzaBicipiteSx" @update:model-value="onCirconferenzaBicipiteSxInput" type="text" placeholder="Es. 78,50" inputmode="decimal" :aria-invalid="!!errori.circonferenzaBicipiteSx" />
                  <p v-if="errori.circonferenzaBicipiteSx" class="text-xs font-medium text-(--danger)">{{ errori.circonferenzaBicipiteSx }}</p>
                </div>
              </div>
            </AccordionContent>
          </AccordionItem>
        </Accordion>
      </div>

      <div class="w-full flex justify-end">
        <Button type="submit" :disabled="inCorso" size="lg" class="hover:bg-primary/80">
          <Save :size="16" />
          {{ inCorso ? 'Salvataggio…' : 'Salva paziente' }}
        </Button>
      </div>
    </form>
  </AppShell>
</template>
