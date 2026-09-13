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
import { DatePicker } from '@/components/ui/date-picker'
import { NativeSelect, NativeSelectOption } from '@/components/ui/native-select'
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
import { dettaglio as dettaglioPaziente, visite, type Paziente, type Visita } from '@/api/pazienti'
import { ETICHETTE_OBIETTIVO } from '@/utils/visita'
import {
  crea, dettaglio, aggiorna, attiva as attivaApi, elimina as eliminaApi, cerca as cercaPiani,
  type PianoAlimentare, type ModalitaPiano, type GiornoSettimana, type TipoPasto,
} from '@/api/pianiAlimentari'
import { filtraDecimaleItaliano, numeroItalianoOpzionale, bloccaTastoNonNumerico } from '@/utils/validators'
import { formattaDataItalianaEstesa } from '@/utils/data'
import { ArrowLeft, MoreHorizontal, Trash2, Pencil } from '@lucide/vue'
import { DropdownMenu, DropdownMenuTrigger, DropdownMenuContent, DropdownMenuItem } from '@/components/ui/dropdown-menu'

function oggiIso(): string {
  const oggi = new Date()
  const anno = oggi.getFullYear()
  const mese = String(oggi.getMonth() + 1).padStart(2, '0')
  const giorno = String(oggi.getDate()).padStart(2, '0')
  return `${anno}-${mese}-${giorno}`
}

const ETICHETTA_MODALITA: Record<ModalitaPiano, string> = {
  PASTI: 'Piano con pasti',
  MACRO: 'Solo target macro',
  ESEMPI: 'Esempi intercambiabili',
}

const COLORE_TIPO_PASTO: Record<TipoPasto, string> = {
  COLAZIONE: 'var(--chart-3)',
  SPUNTINO_MATTINA: 'var(--chart-2)',
  PRANZO: 'var(--chart-1)',
  SPUNTINO_POMERIGGIO: 'var(--chart-3)',
  CENA: 'var(--chart-4)',
  ALTRO: 'var(--fg4)',
}

// Stima locale delle calorie giornaliere suggerite, prima ancora che il piano esista sul
// server: stessa formula di CalcolatoreTdee.java (Katch-McArdle se si conosce la massa magra
// dell'ultima visita, altrimenti Mifflin-St Jeor su peso/altezza/età). Una volta salvato il
// piano, il valore mostrato è invece quello calcolato dal backend (fonte di verità).
type FormulaBmr = 'KATCH_MCARDLE' | 'MIFFLIN_ST_JEOR'
const OBIETTIVI_ESCLUSI_SUGGERIMENTO: Visita['obiettivo'][] = ['PATOLOGIA_CLINICA', 'GRAVIDANZA_ALLATTAMENTO']
const FATTORI_ATTIVITA: Record<NonNullable<Paziente['stileDiVita']>, number> = {
  SEDENTARIO: 1.2, POCO_ATTIVO: 1.375, ATTIVO: 1.55, MOLTO_ATTIVO: 1.725, ESTREMAMENTE_ATTIVO: 1.9,
}
const FATTORE_ATTIVITA_DEFAULT = 1.375

function calcolaEtaAnni(dataNascita: string, dataVisita: string): number {
  const nascita = new Date(dataNascita)
  const visita = new Date(dataVisita)
  let eta = visita.getFullYear() - nascita.getFullYear()
  const compleannoNonAncoraRaggiunto =
    visita.getMonth() < nascita.getMonth() ||
    (visita.getMonth() === nascita.getMonth() && visita.getDate() < nascita.getDate())
  if (compleannoNonAncoraRaggiunto) eta -= 1
  return eta
}

function calcolaSuggerimentoTdee(paziente: Paziente, visita: Visita): { kcal: number; formula: FormulaBmr; sottoSoglia: boolean } | null {
  if (!paziente.dataNascita || OBIETTIVI_ESCLUSI_SUGGERIMENTO.includes(visita.obiettivo)) return null

  let bmr: number
  let formula: FormulaBmr
  if (visita.plicometria?.massaMagraKg != null) {
    bmr = 370 + 21.6 * visita.plicometria.massaMagraKg
    formula = 'KATCH_MCARDLE'
  } else {
    const eta = calcolaEtaAnni(paziente.dataNascita, visita.dataVisita)
    const correzioneSesso = paziente.sesso === 'M' ? 5 : paziente.sesso === 'F' ? -161 : -78
    bmr = 10 * visita.pesoKg + 6.25 * visita.altezzaCm - 5 * eta + correzioneSesso
    formula = 'MIFFLIN_ST_JEOR'
  }
  const fattoreAttivita = paziente.stileDiVita ? FATTORI_ATTIVITA[paziente.stileDiVita] : FATTORE_ATTIVITA_DEFAULT
  const kcal = Math.round(bmr * fattoreAttivita)
  const sogliaMinima = paziente.sesso === 'F' ? 1200 : 1500
  return { kcal, formula, sottoSoglia: kcal < sogliaMinima }
}

const route = useRoute()
const router = useRouter()

const pianoIdRoute = route.params.id as string | undefined
const pazienteIdQuery = route.query.pazienteId as string | undefined
const modalitaCreazione = computed(() => !pianoIdRoute)

const caricamento = ref(!modalitaCreazione.value)
const erroreCaricamento = ref<string | null>(null)

// --- Selezione paziente e modalità, prima ancora che il piano esista sul server:
// tutto (nome, modalità, pasti/target macro/esempi) resta locale finché non si preme
// "Salva", che crea il piano e vi applica subito il contenuto costruito in questa pagina. ---
const pazienteSelezionato = ref<Paziente | null>(null)
const modalitaScelta = ref<ModalitaPiano>('PASTI')
const modalitaTarget = ref<ModalitaPiano | null>(null)
const confermaCambioModalitaAperta = ref(false)

async function caricaPazienteQuery(id: string) {
  try {
    pazienteSelezionato.value = await dettaglioPaziente(id)
  } catch {
    // Nessun blocco: l'utente può comunque selezionare un paziente dal combobox.
  }
}

const piano = ref<PianoAlimentare | null>(null)
const nome = ref('Nuovo piano')
const obiettivoKcalInput = ref('')
const dataFineInput = ref('')
// Data di partenza: impostabile solo alla creazione (immutabile dopo, come modalità e paziente).
const dataInizioInput = ref(oggiIso())

// Un paziente scelto (o un piano già caricato) basta per mostrare il builder.
const pronto = computed(() => piano.value !== null || pazienteSelezionato.value !== null)
const modalitaAttiva = computed(() => piano.value?.modalita ?? modalitaScelta.value)
const nomePazienteVisualizzato = computed(() => {
  if (piano.value) return piano.value.pazienteNomeCompleto
  if (pazienteSelezionato.value) return `${pazienteSelezionato.value.nome} ${pazienteSelezionato.value.cognome}`
  return ''
})

const linkIndietro = computed(() => {
  if (piano.value) return '/piani-alimentari'
  return pazienteIdQuery ? `/pazienti/${pazienteIdQuery}` : '/piani-alimentari'
})
const testoLinkIndietro = computed(() => {
  if (piano.value) return 'Piani alimentari'
  return pazienteIdQuery ? 'Torna al paziente' : 'Torna ai piani alimentari'
})

// Ultima visita del paziente scelto: guida sia il nome suggerito ("{Obiettivo} · fase {n}")
// sia la stima TDEE prima che il piano esista sul server (vedi calcolaSuggerimentoTdee).
// Presa da GET /pazienti/{id}/visite e non da Paziente.obiettivoUltimaVisita: quest'ultimo è
// valorizzato solo dall'endpoint di ricerca/lista pazienti, non da GET /pazienti/{id} (usato
// per precompilare il paziente quando si apre questa pagina da "Nuovo piano" nella scheda
// paziente) — usarlo lasciava il titolo bloccato su "Nuovo piano" in quel solo percorso.
const ultimaVisitaSelezionato = ref<Visita | null>(null)
watch(pazienteSelezionato, async (paziente) => {
  ultimaVisitaSelezionato.value = null
  if (!paziente || piano.value) return
  let ultimaVisita: Visita | null = null
  try {
    const lista = await visite(paziente.id)
    ultimaVisita = lista.length > 0 ? lista[lista.length - 1] : null
  } catch {
    ultimaVisita = null
  }
  ultimaVisitaSelezionato.value = ultimaVisita
  if (!ultimaVisita) return

  const etichetta = ETICHETTE_OBIETTIVO[ultimaVisita.obiettivo]
  try {
    const pagina = await cercaPiani({ pazienteId: paziente.id, dimensione: 100 })
    const conteggio = pagina.contenuto.filter((p) => p.nome.startsWith(`${etichetta} · fase `)).length
    nome.value = `${etichetta} · fase ${conteggio + 1}`
  } catch {
    nome.value = `${etichetta} · fase 1`
  }
})

const suggerimentoObiettivo = computed(() => {
  if (piano.value) {
    if (piano.value.obiettivoKcalSuggerito === null || piano.value.formulaBmrUsata === null) return null
    return {
      kcal: piano.value.obiettivoKcalSuggerito,
      formula: piano.value.formulaBmrUsata,
      sottoSoglia: piano.value.sottoSogliaSicurezza,
    }
  }
  if (!pazienteSelezionato.value || !ultimaVisitaSelezionato.value) return null
  return calcolaSuggerimentoTdee(pazienteSelezionato.value, ultimaVisitaSelezionato.value)
})

interface RigaLocale {
  clientId: string
  alimentoId: string | null
  nome: string
  kcal100g: number
  proteine100g: number
  carboidrati100g: number
  grassi100g: number
  zuccheri100g: number | null
  fibre100g: number | null
  ferro100mg: number | null
  calcio100mg: number | null
  acqua100g: number | null
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

const PASTI_STANDARD: { tipo: TipoPasto; nome: string }[] = [
  { tipo: 'COLAZIONE', nome: 'Colazione' },
  { tipo: 'SPUNTINO_MATTINA', nome: 'Spuntino mattina' },
  { tipo: 'PRANZO', nome: 'Pranzo' },
  { tipo: 'SPUNTINO_POMERIGGIO', nome: 'Spuntino pomeriggio' },
  { tipo: 'CENA', nome: 'Cena' },
]

// Prima del primo salvataggio non esiste ancora un piano da cui leggere pasti/target/esempi:
// si parte con la stessa struttura template che PianoAlimentareService.creaBozza() genera lato
// server per una bozza appena creata (7 giorni × 5 pasti standard, 7 righe macro vuote, 5
// esempi standard) — altrimenti il primo salvataggio, sostituendo integralmente la struttura
// col contenuto (vuoto) di questa pagina, cancellerebbe il template che il server crea da solo.
pastiLocali.value = GIORNI.flatMap((g) =>
  PASTI_STANDARD.map((p) => ({
    clientId: crypto.randomUUID(), giornoSettimana: g.chiave, nome: p.nome, tipo: p.tipo, nota: null, righe: [],
  })),
)

giorniMacroLocali.value = GIORNI.map((g) => ({
  giornoSettimana: g.chiave, kcalTarget: '', proteineTarget: '', carboidratiTarget: '', grassiTarget: '',
}))

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

const CATEGORIE_ESEMPI: { tipo: TipoPasto; etichetta: string; colore: string }[] = [
  { tipo: 'COLAZIONE', etichetta: 'Colazione', colore: COLORE_TIPO_PASTO.COLAZIONE },
  { tipo: 'SPUNTINO_MATTINA', etichetta: 'Spuntino mattina', colore: COLORE_TIPO_PASTO.SPUNTINO_MATTINA },
  { tipo: 'PRANZO', etichetta: 'Pranzo', colore: COLORE_TIPO_PASTO.PRANZO },
  { tipo: 'SPUNTINO_POMERIGGIO', etichetta: 'Spuntino pomeriggio', colore: COLORE_TIPO_PASTO.SPUNTINO_POMERIGGIO },
  { tipo: 'CENA', etichetta: 'Cena', colore: COLORE_TIPO_PASTO.CENA },
]

// Stesso motivo del template dei pasti: un esempio "segnaposto" per categoria, come genera
// PianoAlimentareService.creaBozza() per una bozza ESEMPI appena creata.
esempiLocali.value = CATEGORIE_ESEMPI.map((c) => ({
  clientId: crypto.randomUUID(), tipoPasto: c.tipo, nome: c.etichetta, righe: [],
}))

// Prima del salvataggio, cambiare modalità sostituisce la sezione visibile: i dati già
// inseriti nella sezione lasciata non vengono mai inviati al server (solo la modalità scelta
// al momento del salvataggio ha una struttura persistita, vedi salva()), quindi vanno persi.
// "Valorizzata" = contiene qualcosa oltre il template vuoto di default.
function sezioneValorizzata(modalita: ModalitaPiano): boolean {
  if (modalita === 'PASTI') {
    return pastiLocali.value.some((p) => p.righe.length > 0) || pastiLocali.value.length > PASTI_STANDARD.length * GIORNI.length
  }
  if (modalita === 'MACRO') {
    return giorniMacroLocali.value.some((g) => g.kcalTarget || g.proteineTarget || g.carboidratiTarget || g.grassiTarget)
  }
  return esempiLocali.value.some((e) => e.righe.length > 0) || esempiLocali.value.length > CATEGORIE_ESEMPI.length
}

function selezionaModalita(opzione: ModalitaPiano) {
  if (opzione === modalitaScelta.value) return
  if (sezioneValorizzata(modalitaScelta.value)) {
    modalitaTarget.value = opzione
    confermaCambioModalitaAperta.value = true
    return
  }
  modalitaScelta.value = opzione
}

function confermaCambioModalita() {
  if (modalitaTarget.value) modalitaScelta.value = modalitaTarget.value
  modalitaTarget.value = null
  confermaCambioModalitaAperta.value = false
}

function annullaCambioModalita() {
  modalitaTarget.value = null
}

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
    fibre: (riga.fibre100g ?? 0) * rapporto,
    ferro: (riga.ferro100mg ?? 0) * rapporto,
    calcio: (riga.calcio100mg ?? 0) * rapporto,
    acqua: (riga.acqua100g ?? 0) * rapporto,
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
        fibre: somma.fibre + m.fibre,
        ferro: somma.ferro + m.ferro,
        calcio: somma.calcio + m.calcio,
        acqua: somma.acqua + m.acqua,
      }
    },
    { kcal: 0, proteine: 0, carboidrati: 0, grassi: 0, zuccheri: 0, fibre: 0, ferro: 0, calcio: 0, acqua: 0 },
  )
})

function percentuale(valore: number, target: number) {
  return target > 0 ? Math.min(100, Math.round((valore / target) * 100)) : 0
}

// Ripartizione indicativa 25% proteine / 50% carboidrati / 25% grassi sull'obiettivo
// giornaliero, solo per dimensionare le barre di avanzamento delle macro card.
const macroCardsGiorno = computed(() => {
  const obiettivo = numeroItalianoOpzionale(obiettivoKcalInput.value) ?? 0
  const t = totaliGiornoSelezionato.value
  const targetProteine = (obiettivo * 0.25) / 4
  const targetCarboidrati = (obiettivo * 0.5) / 4
  const targetGrassi = (obiettivo * 0.25) / 9
  return [
    { label: 'Energia', val: Math.round(t.kcal), unit: 'kcal', pct: percentuale(t.kcal, obiettivo), barColor: 'var(--green)', valFg: 'var(--green)' },
    { label: 'Proteine', val: Math.round(t.proteine), unit: 'g', pct: percentuale(t.proteine, targetProteine), barColor: 'var(--danger)', valFg: 'var(--fg)' },
    { label: 'Carboidrati', val: Math.round(t.carboidrati), unit: 'g', pct: percentuale(t.carboidrati, targetCarboidrati), barColor: 'var(--warn-fg)', valFg: 'var(--fg)' },
    { label: 'Grassi', val: Math.round(t.grassi), unit: 'g', pct: percentuale(t.grassi, targetGrassi), barColor: 'var(--green-d)', valFg: 'var(--fg)' },
  ]
})

const macroCardsPiccole = computed(() => {
  const t = totaliGiornoSelezionato.value
  return [
    { label: 'Zuccheri', val: Math.round(t.zuccheri), unit: 'g' },
    { label: 'Fibra', val: Math.round(t.fibre), unit: 'g' },
    { label: 'Ferro', val: Math.round(t.ferro), unit: 'mg' },
    { label: 'Calcio', val: Math.round(t.calcio), unit: 'mg' },
    { label: 'Acqua', val: Math.round(t.acqua), unit: 'g' },
  ]
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

// Rimuovere un pasto/esempio con alimenti già inseriti richiede conferma (si perderebbero
// senza preavviso); uno vuoto si rimuove subito, non c'è nulla da perdere.
const rimozionePendente = ref<{ tipo: 'pasto' | 'esempio'; clientId: string } | null>(null)

function chiediRimuoviPasto(clientId: string) {
  const pasto = pastiLocali.value.find((p) => p.clientId === clientId)
  if (pasto && pasto.righe.length > 0) {
    rimozionePendente.value = { tipo: 'pasto', clientId }
  } else {
    rimuoviPasto(clientId)
  }
}

function chiediRimuoviEsempio(clientId: string) {
  const esempio = esempiLocali.value.find((e) => e.clientId === clientId)
  if (esempio && esempio.righe.length > 0) {
    rimozionePendente.value = { tipo: 'esempio', clientId }
  } else {
    rimuoviEsempio(clientId)
  }
}

function confermaRimozione() {
  if (!rimozionePendente.value) return
  if (rimozionePendente.value.tipo === 'pasto') rimuoviPasto(rimozionePendente.value.clientId)
  else rimuoviEsempio(rimozionePendente.value.clientId)
  rimozionePendente.value = null
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
  if (!piano.value && !pazienteSelezionato.value) return false
  salvataggioInCorso.value = true
  try {
    let id = piano.value?.id
    if (!id) {
      // Primo salvataggio: il piano non esiste ancora sul server, lo si crea ora
      // (non alla scelta di paziente/modalità) e gli si applica subito il resto sotto.
      const creato = await crea({
        pazienteId: pazienteSelezionato.value!.id,
        nome: nome.value.trim(),
        modalita: modalitaAttiva.value,
        dataInizio: dataInizioInput.value || undefined,
      })
      id = creato.id
      router.replace(`/piani-alimentari/${id}`)
    }
    const aggiornato = await aggiorna(id, {
      nome: nome.value.trim(),
      dataFine: dataFineInput.value || null,
      obiettivoKcal: numeroItalianoOpzionale(obiettivoKcalInput.value) ?? null,
      pasti: modalitaAttiva.value === 'PASTI'
        ? pastiLocali.value.map((p) => ({
            giornoSettimana: p.giornoSettimana,
            nome: p.nome,
            tipo: p.tipo,
            nota: p.nota,
            righe: p.righe.map(({ clientId: _clientId, ...resto }) => resto),
          }))
        : null,
      giorniMacroTarget: modalitaAttiva.value === 'MACRO'
        ? giorniMacroLocali.value.map((g) => ({
            giornoSettimana: g.giornoSettimana,
            kcalTarget: numeroItalianoOpzionale(g.kcalTarget) ?? null,
            proteineTarget: numeroItalianoOpzionale(g.proteineTarget) ?? null,
            carboidratiTarget: numeroItalianoOpzionale(g.carboidratiTarget) ?? null,
            grassiTarget: numeroItalianoOpzionale(g.grassiTarget) ?? null,
          }))
        : null,
      esempi: modalitaAttiva.value === 'ESEMPI'
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
    <div v-if="caricamento" class="p-6 text-sm text-(--fg3)">Caricamento…</div>
    <div v-else-if="erroreCaricamento" class="p-6 text-sm text-(--danger)">{{ erroreCaricamento }}</div>

    <div v-else class="flex flex-col gap-4 p-6">
      <router-link
        :to="linkIndietro"
        data-test="link-indietro"
        class="inline-flex w-fit items-center gap-2 text-xs font-semibold text-(--fg3) transition-colors hover:text-(--green) print:hidden"
      >
        <ArrowLeft :size="16" />
        <span>{{ testoLinkIndietro }}</span>
      </router-link>

      <div v-if="!pronto">
        <h1 class="font-heading text-3xl italic text-(--fg)">Nuovo piano alimentare</h1>
        <p class="mb-4 mt-1 text-sm text-(--fg3)">Seleziona il paziente per cui creare il nuovo piano.</p>
        <SelezionaPazienteCombobox v-model="pazienteSelezionato" />
      </div>

      <template v-else>
        <div class="flex items-end justify-between gap-4 print:hidden">
          <div>
            <div class="flex items-center gap-2">
              <Input v-model="nome" class="font-heading! text-3xl! italic! text-(--fg)! border-0 p-0" />
              <Pencil :size="16" class="shrink-0 text-(--fg4)" />
            </div>
            <p class="mt-1 text-sm text-(--fg3)">{{ nomePazienteVisualizzato }} · {{ ETICHETTA_MODALITA[modalitaAttiva] }}</p>
          </div>
          <div class="flex gap-2">
            <Button v-if="piano" variant="outline" @click="stampaPdf">Stampa PDF</Button>
            <Button v-if="piano?.stato === 'BOZZA'" data-test="elimina-piano" variant="outline"
                    @click="confermaEliminaAperta = true">Elimina</Button>
            <Button v-if="piano?.stato === 'BOZZA'" data-test="attiva-piano" :disabled="attivazioneInCorso"
                    @click="attivaPiano">Attiva piano</Button>
            <Button :disabled="salvataggioInCorso || !nome.trim()" @click="salva">
              {{ salvataggioInCorso ? 'Salvataggio…' : 'Salva' }}
            </Button>
          </div>
        </div>

        <SelezionaPazienteCombobox v-if="!piano" v-model="pazienteSelezionato" class="print:hidden" />

        <div v-if="!piano" class="flex w-fit gap-2 rounded-xl border border-(--bd2) bg-(--soft) p-1 print:hidden">
          <button
            v-for="opzione in (['PASTI', 'MACRO', 'ESEMPI'] as ModalitaPiano[])"
            :key="opzione"
            type="button"
            class="rounded-lg px-3 py-2 text-sm font-bold"
            :class="modalitaScelta === opzione ? 'bg-(--surf) text-(--green)' : 'text-(--fg3)'"
            @click="selezionaModalita(opzione)"
          >
            {{ ETICHETTA_MODALITA[opzione] }}
          </button>
        </div>

        <div class="flex flex-wrap items-center justify-center gap-6 rounded-2xl border border-(--sage) bg-(--mint) p-4 print:hidden">
          <div class="flex flex-col gap-1">
            <span class="flex items-center gap-1.5 text-[10px] font-bold uppercase tracking-wide text-(--green)">
              Obiettivo giornaliero
              <span
                title="Stima da BMR (Mifflin-St Jeor o Katch-McArdle) × livello di attività dell'ultima visita. Modificabile."
                class="flex h-3.5 w-3.5 cursor-help items-center justify-center rounded-full bg-(--sage)/35 text-[9px] font-bold text-(--green-d)"
              >i</span>
            </span>
            <span class="flex items-center gap-2">
              <Input
                data-test="obiettivo-kcal-input"
                type="text" inputmode="decimal" class="w-24 border-(--sage) bg-(--surf) font-heading text-base font-semibold"
                :model-value="obiettivoKcalInput"
                :placeholder="suggerimentoObiettivo ? String(suggerimentoObiettivo.kcal) : undefined"
                @keydown="bloccaTastoNonNumerico"
                @update:model-value="(v) => (obiettivoKcalInput = filtraDecimaleItaliano(String(v)))"
              />
              <span class="text-xs font-semibold text-(--green)">kcal/giorno</span>
            </span>
            <p v-if="suggerimentoObiettivo" class="text-xs text-(--green)">
              Suggerimento calcolato con la formula
              {{ suggerimentoObiettivo.formula === 'KATCH_MCARDLE' ? 'Katch-McArdle' : 'Mifflin-St Jeor' }}.
            </p>
            <p v-else-if="!piano && !ultimaVisitaSelezionato" class="text-xs text-(--fg3)">
              Il paziente non ha ancora una visita registrata: imposta il valore a mano.
            </p>
            <p v-else class="text-xs text-(--fg3)">
              Nessun suggerimento automatico disponibile per l'obiettivo di questa visita: imposta il valore a mano.
            </p>
            <p v-if="suggerimentoObiettivo?.sottoSoglia" class="text-xs text-(--danger)">
              Il valore suggerito è sotto la soglia minima indicativa: valutare con attenzione.
            </p>
          </div>
          <div class="hidden h-8 w-px bg-(--sage) sm:block" />
          <div class="flex flex-col gap-1">
            <span class="text-[10px] font-bold uppercase tracking-wide text-(--green)">Data di partenza</span>
            <DatePicker
              v-if="!piano" id="data-inizio" v-model="dataInizioInput"
              class="border-(--sage) dark:border-(--sage) bg-(--surf) dark:bg-(--surf) font-heading text-base font-semibold"
            />
            <span v-else class="font-heading text-sm font-semibold text-(--fg)">{{ formattaDataItalianaEstesa(piano.dataInizio) }}</span>
          </div>
          <div class="hidden h-8 w-px bg-(--sage) sm:block" />
          <div class="flex flex-col gap-1">
            <span class="text-[10px] font-bold uppercase tracking-wide text-(--green)">Fine piano</span>
            <DatePicker
              id="data-fine" v-model="dataFineInput"
              class="border-(--sage) dark:border-(--sage) bg-(--surf) dark:bg-(--surf) font-heading text-base font-semibold"
            />
          </div>
        </div>

        <div v-if="modalitaAttiva === 'ESEMPI'" class="flex flex-col gap-4">
        <div class="grid grid-cols-[repeat(auto-fit,minmax(240px,1fr))] gap-4">
          <div v-for="categoria in CATEGORIE_ESEMPI" :key="categoria.tipo" class="rounded-2xl border border-(--bd) bg-(--surf)">
            <div class="flex items-center gap-2 bg-(--soft) px-3 py-2">
              <span class="h-2 w-2 flex-none rounded-full" :style="{ background: categoria.colore }" />
              <p class="font-heading text-sm font-semibold">{{ categoria.etichetta }}</p>
            </div>
            <div class="flex flex-col gap-2 p-2">
              <div
                v-for="esempio in esempiPerCategoria(categoria.tipo)" :key="esempio.clientId"
                data-test="card-esempio"
                class="rounded-xl border border-(--bd2) p-2"
              >
                <div class="mb-1 flex items-center justify-between gap-2">
                  <div class="flex min-w-0 flex-1 items-center gap-1.5">
                    <Input v-model="esempio.nome" class="min-w-0 border-0 bg-transparent p-0 text-xs font-bold" />
                    <Pencil :size="11" class="shrink-0 text-(--fg4)" />
                  </div>
                  <button class="shrink-0 text-(--fg4)" @click="chiediRimuoviEsempio(esempio.clientId)">✕</button>
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

      <div v-if="modalitaAttiva === 'MACRO'" class="flex flex-col gap-4 sm:grid sm:grid-cols-[180px_1fr] sm:items-start">
        <NativeSelect v-model="giornoSelezionato" class="sm:hidden">
          <NativeSelectOption v-for="giorno in GIORNI" :key="giorno.chiave" :value="giorno.chiave">
            {{ giorno.etichetta }}
          </NativeSelectOption>
        </NativeSelect>

        <div class="hidden rounded-2xl border border-(--bd) bg-(--surf) sm:block">
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
                :model-value="macroDelGiornoSelezionato.kcalTarget" @keydown="bloccaTastoNonNumerico"
                @update:model-value="(v) => (macroDelGiornoSelezionato!.kcalTarget = filtraDecimaleItaliano(String(v)))"
              />
            </label>
            <label class="flex flex-col gap-1">
              <span class="text-xs font-bold uppercase text-(--fg3)">Proteine (g)</span>
              <Input
                type="text" inputmode="decimal"
                :model-value="macroDelGiornoSelezionato.proteineTarget" @keydown="bloccaTastoNonNumerico"
                @update:model-value="(v) => (macroDelGiornoSelezionato!.proteineTarget = filtraDecimaleItaliano(String(v)))"
              />
            </label>
            <label class="flex flex-col gap-1">
              <span class="text-xs font-bold uppercase text-(--fg3)">Carboidrati (g)</span>
              <Input
                type="text" inputmode="decimal"
                :model-value="macroDelGiornoSelezionato.carboidratiTarget" @keydown="bloccaTastoNonNumerico"
                @update:model-value="(v) => (macroDelGiornoSelezionato!.carboidratiTarget = filtraDecimaleItaliano(String(v)))"
              />
            </label>
            <label class="flex flex-col gap-1">
              <span class="text-xs font-bold uppercase text-(--fg3)">Grassi (g)</span>
              <Input
                type="text" inputmode="decimal"
                :model-value="macroDelGiornoSelezionato.grassiTarget" @keydown="bloccaTastoNonNumerico"
                @update:model-value="(v) => (macroDelGiornoSelezionato!.grassiTarget = filtraDecimaleItaliano(String(v)))"
              />
            </label>
          </div>
        </div>
      </div>

      <div v-if="modalitaAttiva === 'PASTI'" class="flex flex-col gap-4 sm:grid sm:grid-cols-[180px_1fr] sm:items-start">
        <NativeSelect v-model="giornoSelezionato" class="sm:hidden">
          <NativeSelectOption v-for="giorno in GIORNI" :key="giorno.chiave" :value="giorno.chiave">
            {{ giorno.etichetta }} — {{ kcalGiorno(giorno.chiave) || '—' }} kcal
          </NativeSelectOption>
        </NativeSelect>

        <div class="hidden rounded-2xl border border-(--bd) bg-(--surf) sm:block">
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
          <div class="grid grid-cols-2 gap-2 sm:grid-cols-4" data-test="macro-card-giorno">
            <div v-for="m in macroCardsGiorno" :key="m.label" class="rounded-xl border border-(--bd) bg-(--surf) p-3">
              <p class="text-[10px] font-bold uppercase tracking-wide text-(--fg3)">{{ m.label }}</p>
              <p class="font-heading text-lg font-semibold" :style="{ color: m.valFg }">
                {{ m.val }}<span class="ml-0.5 text-xs font-normal text-(--fg4)">{{ m.unit }}</span>
              </p>
              <div class="mt-2 h-1 overflow-hidden rounded-full bg-(--div)">
                <div class="h-full rounded-full" :style="{ width: m.pct + '%', background: m.barColor }" />
              </div>
            </div>
          </div>
          <div class="grid grid-cols-3 gap-2 sm:grid-cols-5">
            <div v-for="m in macroCardsPiccole" :key="m.label" class="rounded-xl border border-(--bd) bg-(--surf) p-3">
              <p class="text-[10px] font-bold uppercase tracking-wide text-(--fg3)">{{ m.label }}</p>
              <p class="font-heading text-base font-semibold text-(--fg)">
                {{ m.val }}<span class="ml-0.5 text-xs font-normal text-(--fg4)">{{ m.unit }}</span>
              </p>
            </div>
          </div>

          <div
            v-for="pasto in pastiDelGiorno" :key="pasto.clientId"
            data-test="card-pasto"
            class="rounded-2xl border border-(--bd) bg-(--surf)"
          >
            <div class="flex items-center justify-between gap-3 bg-(--soft) px-4 py-3">
              <div class="flex min-w-0 flex-1 items-center gap-2">
                <span class="h-2 w-2 flex-none rounded-full" :style="{ background: COLORE_TIPO_PASTO[pasto.tipo] }" />
                <Input
                  v-model="pasto.nome"
                  class="min-w-0 border-0 bg-transparent p-0 font-heading text-sm font-semibold text-(--fg)"
                />
                <Pencil :size="12" class="shrink-0 text-(--fg4)" />
              </div>
              <div class="flex items-center gap-3">
                <span class="text-xs text-(--fg3)">{{ kcalPasto(pasto) }} kcal</span>
                <DropdownMenu>
                  <DropdownMenuTrigger as-child>
                    <Button type="button" variant="ghost" size="icon" class="h-7 w-7" aria-label="Opzioni pasto" title="Opzioni pasto">
                      <MoreHorizontal :size="15" />
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent align="end">
                    <DropdownMenuItem
                      variant="destructive" class="cursor-pointer text-(--danger) focus:text-(--danger)"
                      @click="chiediRimuoviPasto(pasto.clientId)"
                    >
                      <Trash2 />
                      Elimina pasto
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
              </div>
            </div>

            <div v-if="pasto.righe.length" class="overflow-x-auto">
              <table class="w-full min-w-185">
                <thead>
                  <tr>
                    <th class="px-4 pb-1.5 pt-2 text-left text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Alimento</th>
                    <th class="w-20 px-2 pb-1.5 pt-2 text-right text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Qtà</th>
                    <th class="px-2 pb-1.5 pt-2 text-right text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Kcal</th>
                    <th class="px-2 pb-1.5 pt-2 text-right text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Prot.</th>
                    <th class="px-2 pb-1.5 pt-2 text-right text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Gras.</th>
                    <th class="px-2 pb-1.5 pt-2 text-right text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Carb.</th>
                    <th class="px-2 pb-1.5 pt-2 text-right text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Zucc.</th>
                    <th class="px-2 pb-1.5 pt-2 text-right text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Fibra</th>
                    <th class="px-2 pb-1.5 pt-2 text-right text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Ferro</th>
                    <th class="px-4 pb-1.5 pt-2 text-right text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Calcio</th>
                    <th class="w-8"></th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="riga in pasto.righe" :key="riga.clientId" class="border-t border-(--div2)">
                    <td class="p-2 pl-4 text-sm font-semibold">{{ riga.nome }}</td>
                    <td class="p-2 text-right">
                      <Input
                        type="text" inputmode="decimal" class="w-16 text-right"
                        :model-value="grammiVisualizzati(riga)" @keydown="bloccaTastoNonNumerico"
                        @update:model-value="(v) => onGrammiInput(pasto.clientId, riga.clientId, v)"
                      />
                    </td>
                    <td class="p-2 text-right text-sm font-bold">{{ Math.round(macroRiga(riga).kcal) }}</td>
                    <td class="p-2 text-right text-xs text-(--fg2)">{{ Math.round(macroRiga(riga).proteine) }}</td>
                    <td class="p-2 text-right text-xs text-(--fg2)">{{ Math.round(macroRiga(riga).grassi) }}</td>
                    <td class="p-2 text-right text-xs text-(--fg2)">{{ Math.round(macroRiga(riga).carboidrati) }}</td>
                    <td class="p-2 text-right text-xs text-(--fg2)">{{ Math.round(macroRiga(riga).zuccheri) }}</td>
                    <td class="p-2 text-right text-xs text-(--fg2)">{{ Math.round(macroRiga(riga).fibre) }}</td>
                    <td class="p-2 text-right text-xs text-(--fg2)">{{ Math.round(macroRiga(riga).ferro) }}</td>
                    <td class="p-2 pr-4 text-right text-xs text-(--fg2)">{{ Math.round(macroRiga(riga).calcio) }}</td>
                    <td class="p-2 text-center">
                      <button class="text-(--fg4)" @click="rimuoviRiga(pasto.clientId, riga.clientId)">✕</button>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>

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
      </template>
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

    <AlertDialog v-model:open="confermaCambioModalitaAperta">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Cambiare modalità del piano?</AlertDialogTitle>
          <AlertDialogDescription>
            I dati inseriti in "{{ ETICHETTA_MODALITA[modalitaScelta] }}" non sono collegati alle altre modalità e andranno persi. Continuare?
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="annullaCambioModalita">Annulla</AlertDialogCancel>
          <AlertDialogAction data-test="conferma-cambio-modalita" variant="destructive" @click="confermaCambioModalita">
            Cambia modalità
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>

    <AlertDialog :open="rimozionePendente !== null" @update:open="(v) => { if (!v) rimozionePendente = null }">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>{{ rimozionePendente?.tipo === 'pasto' ? 'Rimuovere questo pasto?' : 'Rimuovere questo esempio?' }}</AlertDialogTitle>
          <AlertDialogDescription>Gli alimenti già inseriti andranno persi. L'operazione non è reversibile.</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel @click="rimozionePendente = null">Annulla</AlertDialogCancel>
          <AlertDialogAction data-test="conferma-rimuovi-elemento" variant="destructive" @click="confermaRimozione">
            Rimuovi
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
