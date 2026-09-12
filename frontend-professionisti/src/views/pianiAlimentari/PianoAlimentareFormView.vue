<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import AppShell from '@/components/AppShell.vue'
import SelezionaPazienteCombobox from '@/components/pazienti/SelezionaPazienteCombobox.vue'
import AggiungiAlimentoDialog from '@/components/pianiAlimentari/AggiungiAlimentoDialog.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
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
import { dettaglio as dettaglioPaziente, type Paziente } from '@/api/pazienti'
import { ETICHETTE_OBIETTIVO } from '@/utils/visita'
import {
  crea, dettaglio, aggiorna, attiva as attivaApi, elimina as eliminaApi, cerca as cercaPiani,
  type PianoAlimentare, type ModalitaPiano, type GiornoSettimana, type TipoPasto,
} from '@/api/pianiAlimentari'
import { filtraDecimaleItaliano, numeroItalianoOpzionale } from '@/utils/validators'

const route = useRoute()
const router = useRouter()

const pianoIdRoute = route.params.id as string | undefined
const pazienteIdQuery = route.query.pazienteId as string | undefined
const modalitaCreazione = computed(() => !pianoIdRoute)

const caricamento = ref(!modalitaCreazione.value)
const erroreCaricamento = ref<string | null>(null)

// --- Flusso di creazione (nessun piano esiste ancora) ---
const pazienteSelezionato = ref<Paziente | null>(null)
const modalitaScelta = ref<ModalitaPiano>('PASTI')
const nomeCreazione = ref('Nuovo piano')
const creazioneInCorso = ref(false)

async function caricaPazienteQuery(id: string) {
  try {
    pazienteSelezionato.value = await dettaglioPaziente(id)
  } catch {
    // Nessun blocco: l'utente può comunque selezionare un paziente dal combobox.
  }
}

// Nome suggerito alla creazione: "{Obiettivo ultima visita} · fase {n}", contando i piani
// già esistenti del paziente con lo stesso obiettivo (dedotto dal prefisso del nome, dato
// che il piano non memorizza un proprio campo obiettivo separato).
watch(pazienteSelezionato, async (paziente) => {
  if (!paziente || !modalitaCreazione.value) return
  const obiettivo = paziente.obiettivoUltimaVisita
  if (!obiettivo) return
  const etichetta = ETICHETTE_OBIETTIVO[obiettivo]
  try {
    const pagina = await cercaPiani({ pazienteId: paziente.id, dimensione: 100 })
    const conteggio = pagina.contenuto.filter((p) => p.nome.startsWith(`${etichetta} · fase `)).length
    nomeCreazione.value = `${etichetta} · fase ${conteggio + 1}`
  } catch {
    nomeCreazione.value = `${etichetta} · fase 1`
  }
})

async function confermaCreazione() {
  if (!pazienteSelezionato.value || !nomeCreazione.value.trim()) return
  creazioneInCorso.value = true
  try {
    const creato = await crea({
      pazienteId: pazienteSelezionato.value.id,
      nome: nomeCreazione.value.trim(),
      modalita: modalitaScelta.value,
    })
    router.replace(`/piani-alimentari/${creato.id}`)
    applicaPiano(creato)
  } catch {
    toast.error('Non è stato possibile creare il piano. Riprova.')
  } finally {
    creazioneInCorso.value = false
  }
}

// --- Piano caricato/creato: stato locale modificabile ---
const piano = ref<PianoAlimentare | null>(null)
const nome = ref('')
const obiettivoKcalInput = ref('')
const dataFineInput = ref('')

interface RigaLocale {
  clientId: string
  alimentoId: string | null
  nome: string
  kcal100g: number
  proteine100g: number
  carboidrati100g: number
  grassi100g: number
  zuccheri100g: number | null
  grammi: number
}

interface PastoLocale {
  clientId: string
  giornoSettimana: GiornoSettimana
  nome: string
  tipo: TipoPasto
  nota: string | null
  righe: RigaLocale[]
}

const pastiLocali = ref<PastoLocale[]>([])

interface GiornoMacroLocale {
  giornoSettimana: GiornoSettimana
  kcalTarget: string
  proteineTarget: string
  carboidratiTarget: string
  grassiTarget: string
}
const giorniMacroLocali = ref<GiornoMacroLocale[]>([])

const GIORNI: { chiave: GiornoSettimana; etichetta: string }[] = [
  { chiave: 'LUNEDI', etichetta: 'Lunedì' },
  { chiave: 'MARTEDI', etichetta: 'Martedì' },
  { chiave: 'MERCOLEDI', etichetta: 'Mercoledì' },
  { chiave: 'GIOVEDI', etichetta: 'Giovedì' },
  { chiave: 'VENERDI', etichetta: 'Venerdì' },
  { chiave: 'SABATO', etichetta: 'Sabato' },
  { chiave: 'DOMENICA', etichetta: 'Domenica' },
]
const giornoSelezionato = ref<GiornoSettimana>('LUNEDI')

function applicaGiorniMacroDaRisultato(risultato: PianoAlimentare) {
  giorniMacroLocali.value = GIORNI.map((g) => {
    const esistente = risultato.giorniMacroTarget.find((t) => t.giornoSettimana === g.chiave)
    return {
      giornoSettimana: g.chiave,
      kcalTarget: esistente?.kcalTarget != null ? String(esistente.kcalTarget).replace('.', ',') : '',
      proteineTarget: esistente?.proteineTarget != null ? String(esistente.proteineTarget).replace('.', ',') : '',
      carboidratiTarget: esistente?.carboidratiTarget != null ? String(esistente.carboidratiTarget).replace('.', ',') : '',
      grassiTarget: esistente?.grassiTarget != null ? String(esistente.grassiTarget).replace('.', ',') : '',
    }
  })
}

interface EsempioLocale {
  clientId: string
  tipoPasto: TipoPasto
  nome: string
  righe: RigaLocale[]
}
const esempiLocali = ref<EsempioLocale[]>([])

const CATEGORIE_ESEMPI: { tipo: TipoPasto; etichetta: string }[] = [
  { tipo: 'COLAZIONE', etichetta: 'Colazione' },
  { tipo: 'SPUNTINO_MATTINA', etichetta: 'Spuntino mattina' },
  { tipo: 'PRANZO', etichetta: 'Pranzo' },
  { tipo: 'SPUNTINO_POMERIGGIO', etichetta: 'Spuntino pomeriggio' },
  { tipo: 'CENA', etichetta: 'Cena' },
]

function esempiPerCategoria(tipo: TipoPasto) {
  return esempiLocali.value.filter((e) => e.tipoPasto === tipo)
}

function aggiungiEsempio(tipo: TipoPasto, etichetta: string) {
  const numero = esempiPerCategoria(tipo).length + 1
  esempiLocali.value.push({ clientId: crypto.randomUUID(), tipoPasto: tipo, nome: `${etichetta} ${numero}`, righe: [] })
}
function rimuoviEsempio(clientId: string) {
  esempiLocali.value = esempiLocali.value.filter((e) => e.clientId !== clientId)
}
function rimuoviRigaEsempio(esempioClientId: string, rigaClientId: string) {
  const esempio = esempiLocali.value.find((e) => e.clientId === esempioClientId)
  if (!esempio) return
  esempio.righe = esempio.righe.filter((r) => r.clientId !== rigaClientId)
}

function applicaEsempiDaRisultato(risultato: PianoAlimentare) {
  esempiLocali.value = risultato.esempi.map((e) => ({
    clientId: e.id,
    tipoPasto: e.tipoPasto,
    nome: e.nome,
    righe: e.righe.map((r) => ({ clientId: r.id, ...r })),
  }))
}

function applicaPiano(risultato: PianoAlimentare) {
  piano.value = risultato
  nome.value = risultato.nome
  obiettivoKcalInput.value = risultato.obiettivoKcal !== null ? String(risultato.obiettivoKcal).replace('.', ',') : ''
  dataFineInput.value = risultato.dataFine ?? ''
  pastiLocali.value = risultato.pasti.map((p) => ({
    clientId: p.id,
    giornoSettimana: p.giornoSettimana,
    nome: p.nome,
    tipo: p.tipo,
    nota: p.nota,
    righe: p.righe.map((r) => ({ clientId: r.id, ...r })),
  }))
  applicaGiorniMacroDaRisultato(risultato)
  applicaEsempiDaRisultato(risultato)
}

async function caricaPiano(id: string) {
  caricamento.value = true
  try {
    applicaPiano(await dettaglio(id))
  } catch {
    erroreCaricamento.value = 'Non è stato possibile caricare il piano.'
  } finally {
    caricamento.value = false
  }
}

onMounted(() => {
  if (pianoIdRoute) {
    caricaPiano(pianoIdRoute)
  } else if (pazienteIdQuery) {
    caricaPazienteQuery(pazienteIdQuery)
  }
})

const pastiDelGiorno = computed(() =>
  pastiLocali.value.filter((p) => p.giornoSettimana === giornoSelezionato.value),
)

const macroDelGiornoSelezionato = computed(() =>
  giorniMacroLocali.value.find((g) => g.giornoSettimana === giornoSelezionato.value),
)

function applicaMacroATuttiIGiorni() {
  const corrente = macroDelGiornoSelezionato.value
  if (!corrente) return
  giorniMacroLocali.value = giorniMacroLocali.value.map((g) => ({ ...corrente, giornoSettimana: g.giornoSettimana }))
}

function macroRiga(riga: RigaLocale) {
  const rapporto = riga.grammi / 100
  return {
    kcal: riga.kcal100g * rapporto,
    proteine: riga.proteine100g * rapporto,
    carboidrati: riga.carboidrati100g * rapporto,
    grassi: riga.grassi100g * rapporto,
    zuccheri: (riga.zuccheri100g ?? 0) * rapporto,
  }
}
function kcalPasto(pasto: PastoLocale) {
  return Math.round(pasto.righe.reduce((somma, r) => somma + macroRiga(r).kcal, 0))
}
function kcalGiorno(giorno: GiornoSettimana) {
  return Math.round(
    pastiLocali.value.filter((p) => p.giornoSettimana === giorno)
      .reduce((somma, p) => somma + kcalPasto(p), 0),
  )
}

const totaliGiornoSelezionato = computed(() => {
  const righe = pastiDelGiorno.value.flatMap((p) => p.righe)
  return righe.reduce(
    (somma, r) => {
      const m = macroRiga(r)
      return {
        kcal: somma.kcal + m.kcal,
        proteine: somma.proteine + m.proteine,
        carboidrati: somma.carboidrati + m.carboidrati,
        grassi: somma.grassi + m.grassi,
        zuccheri: somma.zuccheri + m.zuccheri,
      }
    },
    { kcal: 0, proteine: 0, carboidrati: 0, grassi: 0, zuccheri: 0 },
  )
})

function aggiungiPasto() {
  pastiLocali.value.push({
    clientId: crypto.randomUUID(),
    giornoSettimana: giornoSelezionato.value,
    nome: 'Pasto extra',
    tipo: 'ALTRO',
    nota: null,
    righe: [],
  })
}
function rimuoviPasto(clientId: string) {
  pastiLocali.value = pastiLocali.value.filter((p) => p.clientId !== clientId)
}

const addModalAperto = ref(false)
const pastoTargetClientId = ref<string | null>(null)
// L'"apertura" del modal aggiungi-alimento ora deve sapere se il bersaglio è un pasto o un esempio.
const esempioTargetClientId = ref<string | null>(null)

function apriAggiungiAlimento(pastoClientId: string) {
  esempioTargetClientId.value = null
  pastoTargetClientId.value = pastoClientId
  addModalAperto.value = true
}
function apriAggiungiAlimentoEsempio(esempioClientId: string) {
  pastoTargetClientId.value = null
  esempioTargetClientId.value = esempioClientId
  addModalAperto.value = true
}
function onAlimentoAggiunto(riga: Omit<RigaLocale, 'clientId'>) {
  if (esempioTargetClientId.value) {
    const esempio = esempiLocali.value.find((e) => e.clientId === esempioTargetClientId.value)
    esempio?.righe.push({ clientId: crypto.randomUUID(), ...riga })
    return
  }
  const pasto = pastiLocali.value.find((p) => p.clientId === pastoTargetClientId.value)
  pasto?.righe.push({ clientId: crypto.randomUUID(), ...riga })
}
function rimuoviRiga(pastoClientId: string, rigaClientId: string) {
  const pasto = pastiLocali.value.find((p) => p.clientId === pastoClientId)
  if (!pasto) return
  pasto.righe = pasto.righe.filter((r) => r.clientId !== rigaClientId)
}
// Buffer di visualizzazione per il campo grammi: tenere il valore digitato come stringa
// (con virgola) finché l'utente scrive, invece di rimbalzare sul numero parsato a ogni
// tasto — altrimenti non si riesce mai a digitare la virgola decimale (es. "62,5").
const grammiInputPerRiga = ref<Record<string, string>>({})

function grammiVisualizzati(riga: RigaLocale) {
  return grammiInputPerRiga.value[riga.clientId] ?? String(riga.grammi).replace('.', ',')
}
function onGrammiInput(pastoClientId: string, rigaClientId: string, valore: string | number) {
  const filtrato = filtraDecimaleItaliano(String(valore))
  grammiInputPerRiga.value[rigaClientId] = filtrato
  const pasto = pastiLocali.value.find((p) => p.clientId === pastoClientId)
  const riga = pasto?.righe.find((r) => r.clientId === rigaClientId)
  const numero = numeroItalianoOpzionale(filtrato)
  if (riga && numero !== undefined && Number.isFinite(numero)) riga.grammi = numero
}

const salvataggioInCorso = ref(false)

async function salva(): Promise<boolean> {
  if (!piano.value) return false
  salvataggioInCorso.value = true
  try {
    const aggiornato = await aggiorna(piano.value.id, {
      nome: nome.value.trim(),
      dataFine: dataFineInput.value || null,
      obiettivoKcal: numeroItalianoOpzionale(obiettivoKcalInput.value) ?? null,
      pasti: piano.value.modalita === 'PASTI'
        ? pastiLocali.value.map((p) => ({
            giornoSettimana: p.giornoSettimana,
            nome: p.nome,
            tipo: p.tipo,
            nota: p.nota,
            righe: p.righe.map(({ clientId: _clientId, ...resto }) => resto),
          }))
        : null,
      giorniMacroTarget: piano.value.modalita === 'MACRO'
        ? giorniMacroLocali.value.map((g) => ({
            giornoSettimana: g.giornoSettimana,
            kcalTarget: numeroItalianoOpzionale(g.kcalTarget) ?? null,
            proteineTarget: numeroItalianoOpzionale(g.proteineTarget) ?? null,
            carboidratiTarget: numeroItalianoOpzionale(g.carboidratiTarget) ?? null,
            grassiTarget: numeroItalianoOpzionale(g.grassiTarget) ?? null,
          }))
        : null,
      esempi: piano.value.modalita === 'ESEMPI'
        ? esempiLocali.value.map((e) => ({
            tipoPasto: e.tipoPasto,
            nome: e.nome,
            righe: e.righe.map(({ clientId: _clientId, ...resto }) => resto),
          }))
        : null,
    })
    applicaPiano(aggiornato)
    toast.success('Piano salvato.')
    return true
  } catch {
    toast.error('Non è stato possibile salvare il piano.')
    return false
  } finally {
    salvataggioInCorso.value = false
  }
}

const attivazioneInCorso = ref(false)
async function attivaPiano() {
  if (!piano.value) return
  attivazioneInCorso.value = true
  try {
    const salvataggioRiuscito = await salva()
    if (!salvataggioRiuscito) return
    await attivaApi(piano.value.id)
    applicaPiano(await dettaglio(piano.value.id))
    toast.success('Piano attivato.')
  } catch {
    toast.error('Non è stato possibile attivare il piano.')
  } finally {
    attivazioneInCorso.value = false
  }
}

const confermaEliminaAperta = ref(false)
const eliminazioneInCorso = ref(false)
async function confermaElimina() {
  if (!piano.value) return
  eliminazioneInCorso.value = true
  try {
    await eliminaApi(piano.value.id)
    toast.success('Piano eliminato.')
    router.push('/piani-alimentari')
  } catch {
    toast.error('Non è stato possibile eliminare il piano.')
  } finally {
    eliminazioneInCorso.value = false
    confermaEliminaAperta.value = false
  }
}

function stampaPdf() {
  window.print()
}
</script>

<template>
  <AppShell>
    <div v-if="modalitaCreazione && !piano" class="mx-auto flex max-w-lg flex-col gap-4 p-6">
      <h1 class="font-heading text-2xl italic text-(--fg)">Nuovo piano alimentare</h1>

      <SelezionaPazienteCombobox v-model="pazienteSelezionato" />

      <div class="flex gap-2 rounded-xl border border-(--bd2) bg-(--soft) p-1">
        <button
          v-for="opzione in (['PASTI', 'MACRO', 'ESEMPI'] as ModalitaPiano[])"
          :key="opzione"
          class="flex-1 rounded-lg px-3 py-2 text-sm font-bold"
          :class="modalitaScelta === opzione ? 'bg-(--surf) text-(--green)' : 'text-(--fg3)'"
          @click="modalitaScelta = opzione"
        >
          {{ opzione === 'PASTI' ? 'Piano con pasti' : opzione === 'MACRO' ? 'Solo target macro' : 'Esempi intercambiabili' }}
        </button>
      </div>

      <Input v-model="nomeCreazione" placeholder="Nome del piano" />

      <Button :disabled="creazioneInCorso || !pazienteSelezionato" @click="confermaCreazione">
        Crea piano
      </Button>
    </div>

    <div v-else-if="caricamento" class="p-6 text-sm text-(--fg3)">Caricamento…</div>
    <div v-else-if="erroreCaricamento" class="p-6 text-sm text-(--danger)">{{ erroreCaricamento }}</div>

    <div v-else-if="piano" class="flex flex-col gap-4 p-6">
      <div class="flex items-end justify-between gap-4 print:hidden">
        <div>
          <Input v-model="nome" class="font-heading border-0 p-0 text-2xl italic" />
          <p class="text-sm text-(--fg2)">{{ piano.pazienteNomeCompleto }}</p>
        </div>
        <div class="flex gap-2">
          <Button variant="outline" @click="stampaPdf">Stampa PDF</Button>
          <Button v-if="piano.stato === 'BOZZA'" data-test="elimina-piano" variant="outline"
                  @click="confermaEliminaAperta = true">Elimina</Button>
          <Button v-if="piano.stato === 'BOZZA'" data-test="attiva-piano" :disabled="attivazioneInCorso"
                  @click="attivaPiano">Attiva piano</Button>
          <Button :disabled="salvataggioInCorso" @click="salva">Salva</Button>
        </div>
      </div>

      <div class="flex items-center justify-center gap-6 rounded-2xl border border-(--sage-l) bg-(--mint) p-4 print:hidden">
        <div>
          <p class="text-xs font-bold uppercase text-(--green)">Obiettivo giornaliero</p>
          <Input
            type="text" inputmode="decimal" class="w-24 font-heading text-base font-semibold"
            :model-value="obiettivoKcalInput"
            @update:model-value="(v) => (obiettivoKcalInput = filtraDecimaleItaliano(String(v)))"
          />
          <p v-if="piano.obiettivoKcalSuggerito !== null" class="text-xs text-(--green)">
            Suggerito: {{ piano.obiettivoKcalSuggerito }} kcal
            ({{ piano.formulaBmrUsata === 'KATCH_MCARDLE' ? 'Katch-McArdle' : 'Mifflin-St Jeor' }})
          </p>
          <p v-else class="text-xs text-(--fg3)">
            Nessun suggerimento automatico disponibile per l'obiettivo di questa visita: imposta il valore a mano.
          </p>
          <p v-if="piano.sottoSogliaSicurezza" class="text-xs text-(--danger)">
            Il valore suggerito è sotto la soglia minima indicativa: valutare con attenzione.
          </p>
        </div>
        <div>
          <p class="text-xs font-bold uppercase text-(--green)">Fine piano</p>
          <input type="date" v-model="dataFineInput" class="rounded-md border border-(--sage) bg-(--surf) px-2 py-1 text-sm" />
        </div>
      </div>

      <div v-if="piano.modalita === 'ESEMPI'" class="flex flex-col gap-4">
        <div class="grid grid-cols-[repeat(auto-fit,minmax(240px,1fr))] gap-4">
          <div v-for="categoria in CATEGORIE_ESEMPI" :key="categoria.tipo" class="rounded-2xl border border-(--bd) bg-(--surf)">
            <div class="bg-(--soft) px-3 py-2">
              <p class="font-heading text-sm font-semibold">{{ categoria.etichetta }}</p>
            </div>
            <div class="flex flex-col gap-2 p-2">
              <div
                v-for="esempio in esempiPerCategoria(categoria.tipo)" :key="esempio.clientId"
                data-test="card-esempio"
                class="rounded-xl border border-(--bd2) p-2"
              >
                <div class="mb-1 flex items-center justify-between">
                  <Input v-model="esempio.nome" class="border-0 bg-transparent p-0 text-xs font-bold" />
                  <button class="text-(--fg4)" @click="rimuoviEsempio(esempio.clientId)">✕</button>
                </div>
                <div v-for="riga in esempio.righe" :key="riga.clientId" class="flex items-center justify-between gap-1 text-xs">
                  <span class="min-w-0 flex-1 truncate">{{ riga.nome }}</span>
                  <span>{{ riga.grammi }}g</span>
                  <button class="text-(--fg4)" @click="rimuoviRigaEsempio(esempio.clientId, riga.clientId)">✕</button>
                </div>
                <button class="mt-1 text-xs font-bold text-(--fg3)" @click="apriAggiungiAlimentoEsempio(esempio.clientId)">
                  + Aggiungi alimento
                </button>
              </div>
              <button
                :data-test="`nuovo-esempio-${categoria.tipo}`"
                class="rounded-lg border border-dashed border-(--bd2) py-2 text-xs font-bold text-(--fg3)"
                @click="aggiungiEsempio(categoria.tipo, categoria.etichetta)"
              >
                + Nuovo esempio
              </button>
            </div>
          </div>
        </div>
      </div>

      <div v-if="piano.modalita === 'MACRO'" class="grid grid-cols-[180px_1fr] gap-4">
        <div class="rounded-2xl border border-(--bd) bg-(--surf)">
          <button
            v-for="giorno in GIORNI" :key="giorno.chiave" data-test="giorno-tab"
            class="flex w-full items-center justify-between border-b border-(--div2) px-3 py-3 text-left"
            :class="giornoSelezionato === giorno.chiave ? 'bg-(--mint) text-(--green)' : 'text-(--fg2)'"
            @click="giornoSelezionato = giorno.chiave"
          >
            <span class="text-sm font-bold">{{ giorno.etichetta }}</span>
          </button>
        </div>

        <div v-if="macroDelGiornoSelezionato" class="rounded-2xl border border-(--bd) bg-(--surf) p-4">
          <div class="mb-3 flex items-center justify-between">
            <p class="font-heading text-base font-semibold">Target macro · {{ GIORNI.find(g => g.chiave === giornoSelezionato)?.etichetta }}</p>
            <button data-test="applica-a-tutti" class="text-xs font-bold text-(--green)" @click="applicaMacroATuttiIGiorni">
              Applica a tutti i giorni
            </button>
          </div>
          <div class="grid grid-cols-4 gap-3">
            <label class="flex flex-col gap-1">
              <span class="text-xs font-bold uppercase text-(--fg3)">Kcal</span>
              <Input
                data-test="macro-target-kcal" type="text" inputmode="decimal"
                :model-value="macroDelGiornoSelezionato.kcalTarget"
                @update:model-value="(v) => (macroDelGiornoSelezionato!.kcalTarget = filtraDecimaleItaliano(String(v)))"
              />
            </label>
            <label class="flex flex-col gap-1">
              <span class="text-xs font-bold uppercase text-(--fg3)">Proteine (g)</span>
              <Input
                type="text" inputmode="decimal"
                :model-value="macroDelGiornoSelezionato.proteineTarget"
                @update:model-value="(v) => (macroDelGiornoSelezionato!.proteineTarget = filtraDecimaleItaliano(String(v)))"
              />
            </label>
            <label class="flex flex-col gap-1">
              <span class="text-xs font-bold uppercase text-(--fg3)">Carboidrati (g)</span>
              <Input
                type="text" inputmode="decimal"
                :model-value="macroDelGiornoSelezionato.carboidratiTarget"
                @update:model-value="(v) => (macroDelGiornoSelezionato!.carboidratiTarget = filtraDecimaleItaliano(String(v)))"
              />
            </label>
            <label class="flex flex-col gap-1">
              <span class="text-xs font-bold uppercase text-(--fg3)">Grassi (g)</span>
              <Input
                type="text" inputmode="decimal"
                :model-value="macroDelGiornoSelezionato.grassiTarget"
                @update:model-value="(v) => (macroDelGiornoSelezionato!.grassiTarget = filtraDecimaleItaliano(String(v)))"
              />
            </label>
          </div>
        </div>
      </div>

      <div v-if="piano.modalita === 'PASTI'" class="grid grid-cols-[180px_1fr] gap-4">
        <div class="rounded-2xl border border-(--bd) bg-(--surf)">
          <button
            v-for="giorno in GIORNI" :key="giorno.chiave" data-test="giorno-tab"
            class="flex w-full items-center justify-between border-b border-(--div2) px-3 py-3 text-left"
            :class="giornoSelezionato === giorno.chiave ? 'bg-(--mint) text-(--green)' : 'text-(--fg2)'"
            @click="giornoSelezionato = giorno.chiave"
          >
            <span class="text-sm font-bold">{{ giorno.etichetta }}</span>
            <span class="text-xs">{{ kcalGiorno(giorno.chiave) || '—' }}</span>
          </button>
        </div>

        <div class="flex flex-col gap-3">
          <div class="grid grid-cols-2 gap-2 sm:grid-cols-5" data-test="macro-card-giorno">
            <div class="rounded-xl border border-(--bd) bg-(--surf) p-2">
              <p class="text-[10px] font-bold uppercase text-(--fg3)">Kcal</p>
              <p class="font-heading text-sm font-semibold text-(--green)">{{ Math.round(totaliGiornoSelezionato.kcal) }}</p>
            </div>
            <div class="rounded-xl border border-(--bd) bg-(--surf) p-2">
              <p class="text-[10px] font-bold uppercase text-(--fg3)">Proteine</p>
              <p class="font-heading text-sm font-semibold">{{ Math.round(totaliGiornoSelezionato.proteine) }} g</p>
            </div>
            <div class="rounded-xl border border-(--bd) bg-(--surf) p-2">
              <p class="text-[10px] font-bold uppercase text-(--fg3)">Carboidrati</p>
              <p class="font-heading text-sm font-semibold">{{ Math.round(totaliGiornoSelezionato.carboidrati) }} g</p>
            </div>
            <div class="rounded-xl border border-(--bd) bg-(--surf) p-2">
              <p class="text-[10px] font-bold uppercase text-(--fg3)">Grassi</p>
              <p class="font-heading text-sm font-semibold">{{ Math.round(totaliGiornoSelezionato.grassi) }} g</p>
            </div>
            <div class="rounded-xl border border-(--bd) bg-(--surf) p-2">
              <p class="text-[10px] font-bold uppercase text-(--fg3)">Zuccheri</p>
              <p class="font-heading text-sm font-semibold">{{ Math.round(totaliGiornoSelezionato.zuccheri) }} g</p>
            </div>
          </div>

          <div
            v-for="pasto in pastiDelGiorno" :key="pasto.clientId"
            data-test="card-pasto"
            class="rounded-2xl border border-(--bd) bg-(--surf)"
          >
            <div class="flex items-center justify-between gap-3 bg-(--soft) px-4 py-3">
              <Input
                v-model="pasto.nome"
                class="border-0 bg-transparent p-0 font-heading text-sm font-semibold text-(--fg)"
              />
              <div class="flex items-center gap-3">
                <span class="text-xs text-(--fg3)">{{ kcalPasto(pasto) }} kcal</span>
                <button class="text-(--fg4)" @click="rimuoviPasto(pasto.clientId)">Elimina</button>
              </div>
            </div>

            <table v-if="pasto.righe.length" class="w-full">
              <tbody>
                <tr v-for="riga in pasto.righe" :key="riga.clientId" class="border-t border-(--div2)">
                  <td class="p-2 text-sm font-semibold">{{ riga.nome }}</td>
                  <td class="p-2 text-right">
                    <Input
                      type="text" inputmode="decimal" class="w-16 text-right"
                      :model-value="grammiVisualizzati(riga)"
                      @update:model-value="(v) => onGrammiInput(pasto.clientId, riga.clientId, v)"
                    />
                  </td>
                  <td class="p-2 text-right text-sm font-bold">{{ Math.round(macroRiga(riga).kcal) }}</td>
                  <td class="p-2 text-center">
                    <button class="text-(--fg4)" @click="rimuoviRiga(pasto.clientId, riga.clientId)">✕</button>
                  </td>
                </tr>
              </tbody>
            </table>

            <div class="flex items-center gap-3 p-3">
              <button
                data-test="apri-aggiungi-alimento"
                class="rounded-lg border border-dashed border-(--bd2) px-3 py-2 text-xs font-bold text-(--fg3)"
                @click="apriAggiungiAlimento(pasto.clientId)"
              >
                + Aggiungi alimento
              </button>
            </div>

            <div class="flex items-start gap-2 border-t border-(--div2) px-4 py-2">
              <span class="mt-1.5 whitespace-nowrap text-[10px] text-(--fg4)">Nota:</span>
              <Textarea
                :model-value="pasto.nota ?? ''"
                @update:model-value="(v) => (pasto.nota = String(v).trim() ? String(v) : null)"
                rows="1"
                placeholder="Cottura, preferenze, sostituti…"
                class="min-h-0 resize-y border-0 bg-transparent p-0 text-xs italic text-(--fg2) shadow-none focus-visible:ring-0"
              />
            </div>
          </div>

          <button
            data-test="aggiungi-pasto"
            class="rounded-xl border border-dashed border-(--bd2) bg-(--surf) px-4 py-3 text-sm font-bold text-(--fg2)"
            @click="aggiungiPasto"
          >
            + Aggiungi pasto
          </button>
        </div>
      </div>
    </div>

    <AggiungiAlimentoDialog v-model:open="addModalAperto" @aggiunto="onAlimentoAggiunto" />

    <AlertDialog v-model:open="confermaEliminaAperta">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Eliminare questo piano?</AlertDialogTitle>
          <AlertDialogDescription>L'operazione non è reversibile.</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel>Annulla</AlertDialogCancel>
          <AlertDialogAction data-test="conferma-elimina-piano" variant="destructive" @click="confermaElimina">
            Elimina
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </AppShell>
</template>

<style scoped>
@media print {
  :deep(.flex.shrink-0.flex-col.justify-between),
  :deep(header) {
    display: none !important;
  }
}
</style>
