<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { toast } from 'vue-sonner'
import { eliminaVisita, storicoVisite, type VisitaStorico } from '@/api/pazienti'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
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
import { ETICHETTE_CIRCONFERENZE, ETICHETTE_OBIETTIVO, categoriaBmi, formattaNumero } from '@/utils/visita'
import { formattaDataItalianaConMese } from '@/utils/data'
import { ChevronDown } from '@lucide/vue'
import type { Visita } from '@/api/pazienti'

const props = defineProps<{
  pazienteId: string
  archiviato: boolean
}>()

const emit = defineEmits<{ eliminata: [] }>()

// Lo storico si carica a blocchi dal server, dalla più recente: "Carica altre visite" accoda il
// blocco successivo a quelle già caricate (stile chat), senza mai sostituirle.
const BLOCCO_VISITE = 5
const MAX_DIMENSIONE_RICARICA = 100

const caricate = ref<VisitaStorico[]>([])
const totale = ref(0)
const caricamentoIniziale = ref(true)
const caricamentoAltre = ref(false)
const errore = ref(false)
const idAperto = ref<string | null>(null)

const visiteRimanenti = computed(() => Math.max(totale.value - caricate.value.length, 0))

async function caricaIniziale() {
  caricamentoIniziale.value = true
  errore.value = false
  try {
    const pagina = await storicoVisite(props.pazienteId, 0, BLOCCO_VISITE)
    caricate.value = pagina.contenuto
    totale.value = pagina.totaleElementi
    // Di default si apre la visita più recente; da lì in poi resta sotto il controllo dell'utente.
    idAperto.value = pagina.contenuto[0]?.visita.id ?? null
  } catch {
    errore.value = true
  } finally {
    caricamentoIniziale.value = false
  }
}
onMounted(caricaIniziale)

async function caricaAltre() {
  caricamentoAltre.value = true
  try {
    const pagina = await storicoVisite(props.pazienteId, Math.floor(caricate.value.length / BLOCCO_VISITE), BLOCCO_VISITE)
    caricate.value = [...caricate.value, ...pagina.contenuto]
    totale.value = pagina.totaleElementi
  } catch {
    toast.error('Non è stato possibile caricare altre visite.')
  } finally {
    caricamentoAltre.value = false
  }
}

// Dopo un'eliminazione si ricarica da capo la stessa quantità di visite già mostrata (ordinamento
// e posizioni cambiano), senza far tornare l'elenco a 5.
async function ricarica() {
  const dimensione = Math.min(Math.max(caricate.value.length, BLOCCO_VISITE), MAX_DIMENSIONE_RICARICA)
  try {
    const pagina = await storicoVisite(props.pazienteId, 0, dimensione)
    caricate.value = pagina.contenuto
    totale.value = pagina.totaleElementi
  } catch {
    errore.value = true
  }
}

function toggle(id: string) {
  idAperto.value = idAperto.value === id ? null : id
}

// `dialogEliminaAperto` è disaccoppiato da `visitaDaEliminare`: AlertDialogAction chiude
// la dialog internamente (è una DialogClose sotto il cofano) prima che `@click` giri,
// quindi se il target vivesse nello stesso stato di apertura verrebbe azzerato troppo presto.
const dialogEliminaAperto = ref(false)
const visitaDaEliminare = ref<Visita | null>(null)
const eliminazioneInCorso = ref(false)

function chiediConferma(visita: Visita) {
  visitaDaEliminare.value = visita
  dialogEliminaAperto.value = true
}

async function confermaEliminazione() {
  if (!visitaDaEliminare.value) return
  eliminazioneInCorso.value = true
  try {
    await eliminaVisita(props.pazienteId, visitaDaEliminare.value.id)
    toast.success('Visita eliminata.')
    visitaDaEliminare.value = null
    await ricarica()
    emit('eliminata')
  } catch {
    toast.error('Non è stato possibile eliminare la visita.')
  } finally {
    eliminazioneInCorso.value = false
  }
}

interface RigaStorico {
  visita: Visita
  rel: string
  deltaPeso: number | null
  deltaMg: number | null
  categoriaBmi: string | null
  circonferenze: Array<{ label: string; valore: number | null }>
  haCirconferenze: boolean
  recentissima: boolean
}

const righe = computed<RigaStorico[]>(() =>
  caricate.value.map(({ visita, posizione, deltaPesoKg, deltaPercentualeGrasso }) => {
    const circonferenze = (Object.keys(ETICHETTE_CIRCONFERENZE) as Array<keyof Visita['circonferenze']>).map((chiave) => ({
      label: ETICHETTE_CIRCONFERENZE[chiave],
      valore: visita.circonferenze[chiave],
    }))
    return {
      visita,
      rel: posizione === 0 ? 'Più recente' : posizione === 1 ? '1 visita fa' : posizione + ' visite fa',
      deltaPeso: deltaPesoKg !== null ? +deltaPesoKg.toFixed(2) : null,
      deltaMg: deltaPercentualeGrasso !== null ? +deltaPercentualeGrasso.toFixed(2) : null,
      categoriaBmi: categoriaBmi(visita.bmi),
      circonferenze,
      haCirconferenze: circonferenze.some((c) => c.valore !== null),
      recentissima: posizione === 0,
    }
  }),
)
</script>

<template>
  <div v-if="errore" class="text-xs font-medium text-(--danger)">
    Non è stato possibile caricare l'elenco delle visite.
  </div>

  <div v-else-if="caricamentoIniziale" class="space-y-3">
    <div v-for="n in 3" :key="n" data-test="storico-skeleton" class="h-16 animate-pulse rounded-2xl bg-(--hover)" />
  </div>

  <div v-else-if="totale === 0" class="rounded-2xl border border-(--bd) bg-(--surf) p-8 text-center text-sm text-(--fg3)">
    <h4 class="font-heading text-lg italic text-(--fg)">Nessuna visita registrata</h4>
    <p class="mx-auto mt-1.5 max-w-sm text-sm text-(--fg3)">
      Lo storico del paziente è vuoto. Registra la prima visita per iniziare a documentare il suo percorso.
    </p>
  </div>

  <div v-else class="relative lg:pl-2">
    <div class="absolute bottom-2 left-7.25 top-2 hidden w-0.5 bg-(--bd2) lg:block"></div>

    <div v-for="riga in righe" :key="riga.visita.id" class="relative mb-3.5 flex gap-4 last:mb-0">
      <div class="relative hidden w-11 shrink-0 justify-center pt-4 lg:flex">
        <span
          class="h-3.25 w-3.25 rounded-full"
          :style="{ background: riga.recentissima ? 'var(--green)' : 'var(--sage)', boxShadow: '0 0 0 4px var(--bg), 0 0 0 5px var(--bd2)' }"
        />
      </div>

      <div class="min-w-0 flex-1 overflow-hidden rounded-2xl border border-(--bd) bg-(--surf) shadow-sm transition-colors hover:border-(--green)">
        <button
          type="button"
          data-test="storico-riga"
          class="flex w-full flex-wrap items-center gap-4 px-5 py-4 text-left transition-colors hover:bg-(--soft)"
          @click="toggle(riga.visita.id)"
        >
          <div class="flex min-w-24 flex-col gap-0.5">
            <span class="text-sm font-bold text-(--fg)">{{ formattaDataItalianaConMese(riga.visita.dataVisita) }}</span>
            <span class="text-xs text-(--fg4)">{{ riga.rel }}</span>
          </div>

          <Badge class="bg-(--mint) text-(--green)">{{ ETICHETTE_OBIETTIVO[riga.visita.obiettivo] }}</Badge>

          <div class="flex flex-1 flex-wrap gap-6">
            <div class="flex flex-col gap-0.5">
              <span class="font-heading text-lg text-(--fg)">{{ formattaNumero(riga.visita.pesoKg) }} <span class="text-xs font-sans text-(--fg4)">kg</span></span>
              <span
                v-if="riga.deltaPeso !== null"
                class="text-xs font-bold"
                :class="riga.deltaPeso <= 0 ? 'text-(--green)' : 'text-(--danger)'"
              >
                {{ riga.deltaPeso > 0 ? '+' : '' }}{{ formattaNumero(riga.deltaPeso) }} kg
              </span>
              <span v-else class="text-xs text-(--fg4)">Prima visita</span>
            </div>

            <div v-if="riga.visita.bmi !== null" class="flex flex-col gap-0.5">
              <span class="font-heading text-lg text-(--fg)">{{ formattaNumero(riga.visita.bmi, 1) }} <span class="text-xs font-sans text-(--fg4)">BMI</span></span>
              <span class="text-xs text-(--fg4)">{{ riga.categoriaBmi }}</span>
            </div>

            <div v-if="riga.visita.plicometria" class="flex flex-col gap-0.5">
              <span class="font-heading text-lg text-(--fg)">{{ formattaNumero(riga.visita.plicometria.percentualeGrassoCorporeo) }}% <span class="text-xs font-sans text-(--fg4)">massa grassa</span></span>
              <span
                v-if="riga.deltaMg !== null"
                class="text-xs font-bold"
                :class="riga.deltaMg <= 0 ? 'text-(--green)' : 'text-(--danger)'"
              >
                {{ riga.deltaMg > 0 ? '+' : '' }}{{ formattaNumero(riga.deltaMg) }} pt
              </span>
            </div>
          </div>

          <ChevronDown :size="16" class="shrink-0 text-(--fg3) transition-transform" :class="{ 'rotate-180': idAperto === riga.visita.id }" />
        </button>

        <div v-if="idAperto === riga.visita.id" data-test="storico-dettaglio" class="border-t border-(--div) px-5 pb-5 pt-4">
          <div v-if="!archiviato" class="mb-4 flex justify-end gap-2">
            <Button as-child variant="outline" size="sm">
              <router-link :to="`/pazienti/${pazienteId}/visite/${riga.visita.id}/modifica`">Modifica visita</router-link>
            </Button>
            <Button
              type="button"
              variant="destructive-outline"
              size="sm"
              data-test="elimina-visita"
              class="text-(--danger)"
              @click="chiediConferma(riga.visita)"
            >
              Elimina visita
            </Button>
          </div>

          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-4">
            <div class="rounded-xl bg-(--soft) p-4">
              <h5 class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Generali</h5>
              <dl class="mt-2 space-y-1 text-sm">
                <div class="flex items-center justify-between"><dt class="text-(--fg2)">Peso</dt><dd class="font-semibold text-(--fg)">{{ formattaNumero(riga.visita.pesoKg) }} kg</dd></div>
                <div v-if="riga.visita.bmi !== null" class="flex items-center justify-between"><dt class="text-(--fg2)">BMI</dt><dd class="font-semibold text-(--fg)">{{ formattaNumero(riga.visita.bmi, 1) }}</dd></div>
              </dl>
              <p v-if="riga.visita.note" class="mt-2 border-t border-(--div2) pt-2 text-xs leading-relaxed text-(--fg3)">{{ riga.visita.note }}</p>
            </div>

            <div class="rounded-xl bg-(--soft) p-4">
              <h5 class="text-xs font-bold uppercase tracking-wide text-(--fg3)">BIA</h5>
              <p class="mt-2 text-xs text-(--fg4)">Dati non disponibili.</p>
            </div>

            <div class="rounded-xl bg-(--soft) p-4">
              <h5 class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Plicometria</h5>
              <dl v-if="riga.visita.plicometria" class="mt-2 space-y-1 text-sm">
                <div class="flex items-center justify-between"><dt class="text-(--fg2)">% Grasso</dt><dd class="font-semibold text-(--fg)">{{ formattaNumero(riga.visita.plicometria.percentualeGrassoCorporeo) }}%</dd></div>
                <div class="flex items-center justify-between"><dt class="text-(--fg2)">Massa grassa</dt><dd class="font-semibold text-(--fg)">{{ formattaNumero(riga.visita.plicometria.massaGrassaKg) }} kg</dd></div>
                <div class="flex items-center justify-between"><dt class="text-(--fg2)">Massa magra</dt><dd class="font-semibold text-(--fg)">{{ formattaNumero(riga.visita.plicometria.massaMagraKg) }} kg</dd></div>
              </dl>
              <p v-else class="mt-2 text-xs text-(--fg4)">Nessuna plicometria registrata.</p>
            </div>

            <div class="rounded-xl bg-(--soft) p-4">
              <h5 class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Circonferenze</h5>
              <dl v-if="riga.haCirconferenze" class="mt-2 space-y-1 text-sm">
                <div v-for="c in riga.circonferenze" :key="c.label" class="flex items-center justify-between">
                  <dt class="text-(--fg2)">{{ c.label }}</dt>
                  <dd class="font-semibold text-(--fg)">{{ c.valore !== null ? `${formattaNumero(c.valore)} cm` : '—' }}</dd>
                </div>
              </dl>
              <p v-else class="mt-2 text-xs text-(--fg4)">Nessuna circonferenza registrata.</p>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-if="visiteRimanenti > 0" class="relative mt-3.5 flex justify-center lg:pl-11">
      <Button type="button" variant="neutral" data-test="carica-altre-visite" :disabled="caricamentoAltre" @click="caricaAltre">
        {{ caricamentoAltre ? 'Caricamento…' : 'Carica altre visite' }}
        <span class="text-(--fg4)">({{ visiteRimanenti }} {{ visiteRimanenti === 1 ? 'rimanente' : 'rimanenti' }})</span>
      </Button>
    </div>
  </div>

  <AlertDialog v-model:open="dialogEliminaAperto">
    <AlertDialogContent>
      <AlertDialogHeader>
        <AlertDialogTitle>Eliminare la visita del {{ visitaDaEliminare ? formattaDataItalianaConMese(visitaDaEliminare.dataVisita) : '' }}?</AlertDialogTitle>
        <AlertDialogDescription>
          L'operazione è irreversibile: tutti i dati registrati per questa visita (misurazioni, plicometria, circonferenze) andranno persi.
        </AlertDialogDescription>
      </AlertDialogHeader>
      <AlertDialogFooter>
        <AlertDialogCancel data-test="elimina-visita-annulla" variant="neutral" :disabled="eliminazioneInCorso">Annulla</AlertDialogCancel>
        <AlertDialogAction
          data-test="elimina-visita-conferma"
          :disabled="eliminazioneInCorso"
          class="bg-(--danger) hover:bg-(--danger)/80"
          @click="confermaEliminazione"
        >
          {{ eliminazioneInCorso ? 'Eliminazione…' : 'Elimina' }}
        </AlertDialogAction>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
</template>
