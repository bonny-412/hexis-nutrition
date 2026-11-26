<script setup lang="ts">
import { computed } from 'vue'
import type { Paziente, Visita } from '@/api/pazienti'
import type { PianoAlimentareRigaLista } from '@/api/pianiAlimentari'
import type { PianoConNote } from '@/utils/pianoAlimentare'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { X } from '@lucide/vue'
import { formattaDataItalianaConMese } from '@/utils/data'
import { ETICHETTE_OBIETTIVO, formattaNumero } from '@/utils/visita'
import PlicometriaCard from './PlicometriaCard.vue'
import CirconferenzeCard from './CirconferenzeCard.vue'

const props = defineProps<{
  visita: Visita
  /** Dal paziente arriva il livello di attività: non è registrato per singola visita. */
  stileDiVita: Paziente['stileDiVita']
  /** Piani in corso tra questa visita e la successiva, con le loro note; vuoto = la sezione note non compare. */
  piani: PianoConNote[]
  noteInCaricamento: boolean
}>()

defineEmits<{ chiudi: [] }>()

const ETICHETTE_STILE_DI_VITA: Record<NonNullable<Paziente['stileDiVita']>, string> = {
  SEDENTARIO: 'Sedentario',
  POCO_ATTIVO: 'Poco attivo',
  ATTIVO: 'Attivo',
  MOLTO_ATTIVO: 'Molto attivo',
  ESTREMAMENTE_ATTIVO: 'Estremamente attivo',
}

interface Riga { etichetta: string; valore: string }
interface Blocco { titolo: string; righe: Riga[] }

const blocchi = computed<Blocco[]>(() => {
  const v = props.visita
  const generali: Riga[] = [{ etichetta: 'Peso', valore: `${formattaNumero(v.pesoKg)} kg` }]
  if (v.bmi !== null) {
    generali.push({ etichetta: 'BMI', valore: formattaNumero(v.bmi, 1) })
  }
  if (props.stileDiVita) {
    generali.push({ etichetta: 'Livello di attività', valore: ETICHETTE_STILE_DI_VITA[props.stileDiVita] })
  }

  return [
    { titolo: 'Generali', righe: generali },
  ].filter((blocco) => blocco.righe.length > 0)
})

const haCirconferenze = computed(() => Object.values(props.visita.circonferenze).some((valore) => valore !== null))

// "5 set → 18 set 2026": l'anno si scrive una volta sola quando inizio e fine sono nello stesso anno.
function periodoPiano(piano: PianoAlimentareRigaLista): string {
  const inizio = piano.dataInizio
  const fine = piano.dataFine
  if (!fine) return `dal ${formattaDataItalianaConMese(inizio)}`
  const inizioTesto = inizio.slice(0, 4) === fine.slice(0, 4)
    ? formattaDataItalianaConMese(inizio).replace(/\s\d{4}$/, '')
    : formattaDataItalianaConMese(inizio)
  return `${inizioTesto} → ${formattaDataItalianaConMese(fine)}`
}
</script>

<template>
  <!-- Sotto i 1024px il pannello è un overlay: il velo scurisce la pagina (stesso stile del velo dei pannelli
    e dei menu) e un clic fuori lo chiude. Da 1024px in su il pannello è affiancato e il velo non c'è. -->
  <div
    data-test="velo-riepilogo"
    class="fixed inset-0 z-40 bg-black/10 supports-backdrop-filter:backdrop-blur-xs lg:hidden"
    @click="$emit('chiudi')"
  />
  <aside
    data-test="riepilogo-precedente"
    class="fixed inset-y-0 right-0 z-50 flex w-[min(380px,100%)] flex-col overflow-hidden border-l border-(--bd) bg-(--surf) shadow-xl lg:sticky lg:top-6 lg:z-auto lg:max-h-[calc(100vh-3rem)] lg:w-95 lg:flex-none lg:rounded-2xl lg:border lg:shadow-none"
  >
    <header class="flex flex-none items-start justify-between gap-3 border-b border-(--div) px-4 py-3.5">
      <div>
        <h2 class="font-heading font-semibold italic text-xl text-(--fg)">Riepilogo precedente</h2>
        <p class="text-xs text-(--fg3)">Sola lettura</p>
      </div>
      <Button type="button" variant="ghost" size="icon-sm" data-test="chiudi-riepilogo" aria-label="Chiudi" title="Chiudi" @click="$emit('chiudi')">
        <X :size="16" />
      </Button>
    </header>

    <div class="flex min-h-0 flex-1 flex-col gap-6 overflow-y-auto p-4">
      <section class="flex flex-col gap-3">
        <div class="flex flex-wrap items-center justify-between gap-2">
          <h3 class="text-sm font-bold text-(--fg)">Ultima visita</h3>
          <Badge variant="secondary" class="bg-(--mint) text-(--green-d)" data-test="badge-ultima-visita">
            {{ formattaDataItalianaConMese(visita.dataVisita) }} · {{ ETICHETTE_OBIETTIVO[visita.obiettivo] }}
          </Badge>
        </div>

        <div v-for="blocco in blocchi" :key="blocco.titolo" data-test="blocco-visita" class="rounded-xl bg-(--soft) p-3">
          <p class="mb-1.5 text-xs font-medium text-(--fg3)">{{ blocco.titolo }}</p>
          <dl class="flex flex-col gap-1">
            <div v-for="riga in blocco.righe" :key="riga.etichetta" class="flex items-baseline justify-between gap-3 text-xs">
              <dt class="text-(--fg3)">{{ riga.etichetta }}</dt>
              <dd class="text-right font-semibold text-(--fg)">{{ riga.valore }}</dd>
            </div>
          </dl>
        </div>

        <!-- Stesse card della scheda paziente; qui si mostrano solo se la visita ha quei dati. -->
        <PlicometriaCard v-if="visita.plicometria" :visita="visita" annidata />
        <CirconferenzeCard v-if="haCirconferenze" :visita="visita" annidata />

        <div v-if="visita.note" data-test="note-visita" class="rounded-xl border border-(--bd) p-3">
          <p class="mb-1 text-xs font-medium text-(--fg3)">Note della visita</p>
          <p class="whitespace-pre-wrap text-xs text-(--fg2)">{{ visita.note }}</p>
        </div>
      </section>

      <hr v-if="piani.length > 0" data-test="separatore-note-piano" class="border-0 border-t border-(--div)">

      <section v-if="piani.length > 0" data-test="sezione-note-piano" class="flex flex-col gap-4">
        <h3 class="text-sm font-bold text-(--fg)">{{ piani.length === 1 ? 'Note del piano' : 'Note dei piani' }}</h3>

        <div v-for="{ piano, note } in piani" :key="piano.id" data-test="gruppo-note-piano" class="flex flex-col gap-2.5">
          <p class="text-xs text-(--fg3)">{{ piano.nome }} · {{ periodoPiano(piano) }}</p>
          <p v-if="note.length === 0" data-test="nessuna-nota-piano" class="text-xs text-(--fg3)">Nessuna nota per questo piano.</p>
          <ul v-else class="flex flex-col gap-2.5">
            <li v-for="nota in note" :key="nota.id" data-test="nota-piano" class="rounded-xl border border-(--bd) p-3">
              <p class="mb-1 text-xs font-bold text-(--fg)">{{ formattaDataItalianaConMese(nota.data) }}</p>
              <p class="whitespace-pre-wrap text-xs text-(--fg2)">{{ nota.testo }}</p>
            </li>
          </ul>
        </div>
      </section>

      <p v-else-if="noteInCaricamento" class="text-xs text-(--fg3)">Caricamento note…</p>
    </div>
  </aside>
</template>
