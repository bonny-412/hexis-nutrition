<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { CalendarPlus, Lock, Video } from '@lucide/vue'
import { Button } from '@/components/ui/button'
import type { Appuntamento } from '@/api/agenda'
import AppuntamentoMenu, { type AzioneAppuntamento } from '@/components/agenda/AppuntamentoMenu.vue'
import { festivitaDelGiorno } from '@/utils/festivita'
import { eventiDelGiorno } from '@/utils/blocchi'
import {
  giorniDellaSettimana,
  impaginaSovrapposizioni,
  minutiDaOra,
  oggiIso,
  ORA_ORIGINE_MIN,
  posizioneEvento,
  PX_PER_ORA,
  stileCorsia,
} from '@/utils/agenda'
import { barrato, classiEvento, eVisitaAttiva, etichettaTipo, nomePaziente, orarioBlocco, titolo } from '@/utils/agendaStile'

const props = defineProps<{
  riferimento: string
  appuntamenti: Appuntamento[]
  /** Filtro Visite/Promemoria attivo: "libera" sarebbe falso, gli impegni ci sono ma sono nascosti. */
  filtroAttivo?: boolean
}>()
const emit = defineEmits<{ azione: [appuntamento: Appuntamento, azione: AzioneAppuntamento]; dettaglio: [appuntamento: Appuntamento]; nuovo: [data: string] }>()

const NOMI_GIORNI = ['lun', 'mar', 'mer', 'gio', 'ven', 'sab', 'dom']
const ORA_FINE_MIN = 20 * 60
const ORE = Array.from({ length: 13 }, (_, i) => 7 + i)
/** Durata minima (minuti) con cui un evento resta visibile e cliccabile in griglia (anche i promemoria, senza durata). */
const DURATA_MINIMA = 30

// "Adesso" e "oggi" si aggiornano ogni minuto.
const oggi = ref(oggiIso())
const adessoMin = ref(0)
function aggiornaAdesso() {
  const d = new Date()
  oggi.value = oggiIso()
  adessoMin.value = d.getHours() * 60 + d.getMinutes()
}
aggiornaAdesso()
let timer: ReturnType<typeof setInterval> | undefined
onMounted(() => {
  timer = setInterval(aggiornaAdesso, 60_000)
})
onUnmounted(() => {
  if (timer !== undefined) clearInterval(timer)
})

function oraDaMinuti(min: number): string {
  return `${String(Math.floor(min / 60)).padStart(2, '0')}:${String(min % 60).padStart(2, '0')}`
}

const colonne = computed(() =>
  giorniDellaSettimana(props.riferimento).map((iso, i) => {
    const delGiorno = eventiDelGiorno(props.appuntamenti, iso)
    // Eventi in griglia, ritagliati nella fascia 07:00-20:00 (mai fuori dalla griglia).
    const inGriglia = delGiorno
      .filter((a) => a.oraInizio !== null)
      .map((a) => {
        const inizioMin = Math.min(Math.max(minutiDaOra(a.oraInizio as string), ORA_ORIGINE_MIN), ORA_FINE_MIN - 15)
        const durata = a.durataMinuti ?? DURATA_MINIMA
        const fineMin = Math.min(Math.max(minutiDaOra(a.oraInizio as string) + durata, inizioMin + 15), ORA_FINE_MIN)
        const pos = posizioneEvento(oraDaMinuti(inizioMin), fineMin - inizioMin)
        return { a, inizioMin, fineMin, ...pos }
      })
    return {
      iso,
      nome: NOMI_GIORNI[i],
      domenica: i === 6,
      numero: Number(iso.slice(8)),
      festa: festivitaDelGiorno(iso),
      eOggi: iso === oggi.value,
      visite: delGiorno.filter(eVisitaAttiva).length,
      senzaOra: delGiorno.filter((a) => a.oraInizio === null),
      vuota: delGiorno.length === 0,
      eventi: impaginaSovrapposizioni(inGriglia),
    }
  }),
)

/** Settimana senza nessun impegno: un solo messaggio in alto invece di una scritta per colonna. */
const settimanaVuota = computed(() => colonne.value.every((c) => c.vuota))
/** I festivi della settimana si dicono nel messaggio: "libera" da sola farebbe pensare a sette giorni lavorativi. */
const festiviSettimana = computed(() => colonne.value.filter((c) => c.festa).length)
/** "Nuovo appuntamento" propone oggi se la settimana lo contiene, altrimenti il primo giorno non festivo. */
const dataNuovo = computed(() => (colonne.value.find((c) => c.eOggi) ?? colonne.value.find((c) => !c.festa) ?? colonne.value[0]).iso)

const lineaAdessoPx = computed(() =>
  adessoMin.value >= ORA_ORIGINE_MIN && adessoMin.value < ORA_FINE_MIN
    ? ((adessoMin.value - ORA_ORIGINE_MIN) * PX_PER_ORA) / 60
    : null,
)

function testoEvento(a: Appuntamento): string {
  if (a.tipo === 'BLOCCO') return `${titolo(a)} · ${orarioBlocco(a)} · Bloccato`
  const base = `${a.oraInizio ?? ''} ${titolo(a)} · ${etichettaTipo(a)}`
  const paz = a.tipo === 'PROMEMORIA' ? nomePaziente(a) : ''
  return paz ? `${base} · ${paz}` : base
}
</script>

<template>
  <section class="overflow-x-auto rounded-2xl border border-(--bd) bg-(--surf)" data-test="agenda-settimana">
    <div
      v-if="settimanaVuota"
      class="sticky left-0 flex flex-wrap items-center justify-between gap-3 border-b border-(--bd) bg-(--soft) px-5 py-3"
      data-test="settimana-vuota"
    >
      <p v-if="filtroAttivo" class="text-sm text-(--fg3)">Nessun impegno di questo tipo in questi sette giorni.</p>
      <template v-else>
        <p class="text-sm text-(--fg3)">
          <span class="font-heading text-base italic text-(--fg)">Settimana libera.</span> Nessun impegno in questi sette giorni<template v-if="festiviSettimana === 1">, uno è festivo</template><template v-else-if="festiviSettimana > 1">, {{ festiviSettimana }} sono festivi</template>.
        </p>
        <Button size="sm" @click="emit('nuovo', dataNuovo)"><CalendarPlus :size="14" /> Nuovo appuntamento</Button>
      </template>
    </div>
    <div class="min-w-[880px]">
      <!-- Intestazione -->
      <div class="grid grid-cols-[48px_repeat(7,minmax(0,1fr))] border-b border-(--bd)">
        <div />
        <div
          v-for="c in colonne"
          :key="c.iso"
          class="flex min-w-0 flex-col gap-1 border-l border-(--bd) px-2 py-2.5"
          :class="c.eOggi && (!c.festa || c.domenica) ? 'bg-(--mint)' : c.festa ? 'bg-(--warn-bg)' : c.domenica ? 'bg-(--agenda-domenica)' : ''"
          :data-test="`settimana-intestazione-${c.iso}`"
        >
          <div class="flex items-center gap-2">
            <span class="text-xs font-semibold" :class="c.eOggi ? 'text-(--green-d)' : 'text-(--fg3)'">
              {{ c.nome }}
            </span>
            <span
              class="flex size-7 items-center justify-center rounded-full font-heading text-base"
              :class="c.eOggi ? 'bg-(--green) text-(--on-green)' : 'text-(--fg)'"
            >
              {{ c.numero }}
            </span>
            <span v-if="c.visite > 0" class="ml-auto text-[11px] font-semibold text-(--fg3)" :data-test="`settimana-visite-${c.iso}`">
              {{ c.visite }} {{ c.visite === 1 ? 'visita' : 'visite' }}
            </span>
          </div>
          <div v-if="c.festa" class="truncate text-[10.5px] font-semibold text-(--warn-fg)" :title="c.festa">{{ c.festa }}</div>
          <ul v-if="c.senzaOra.length > 0" class="flex flex-col gap-1" :data-test="`settimana-senza-ora-${c.iso}`">
            <li
              v-for="a in c.senzaOra"
              :key="a.id"
              class="group flex cursor-pointer items-center gap-1 rounded-md border-l-[3px] py-0.5 pl-1.5 pr-0.5"
              :class="classiEvento(a)"
              :data-test="`agenda-evento-${a.id}`"
              @click="emit('dettaglio', a)"
            >
              <span class="min-w-0 flex-1 truncate text-[11px] font-semibold text-(--fg)" :class="{ 'line-through': barrato(a) }" :title="titolo(a)">
                {{ titolo(a) }}
              </span>
              <span @click.stop>
                <AppuntamentoMenu :appuntamento="a" @azione="(azione) => emit('azione', a, azione)" />
              </span>
            </li>
          </ul>
        </div>
      </div>

      <!-- Griglia oraria -->
      <div class="grid grid-cols-[48px_repeat(7,minmax(0,1fr))]">
        <div>
          <div v-for="h in ORE" :key="h" class="h-[58px] pr-2 pt-0.5 text-right text-[10.5px] font-semibold text-(--fg3)">
            {{ String(h).padStart(2, '0') }}:00
          </div>
        </div>
        <div
          v-for="c in colonne"
          :key="c.iso"
          class="relative min-w-0 border-l border-(--bd)"
          :class="c.eOggi && (!c.festa || c.domenica) ? 'bg-(--mint)/40' : c.festa ? 'bg-(--warn-bg)' : c.domenica ? 'bg-(--agenda-domenica)' : ''"
          :data-test="`settimana-colonna-${c.iso}`"
        >
          <div v-for="h in ORE" :key="h" class="h-[58px] border-t border-(--div)" />

          <!-- :style solo per top/height/left/width: valori dinamici da posizioneEvento e dalle corsie (eccezione approvata); vale anche per la linea dell'ora corrente. -->
          <div
            v-for="e in c.eventi"
            :key="e.a.id"
            class="group absolute cursor-pointer overflow-hidden rounded-md border-l-[3px] px-1.5 py-0.5 ring-[1.5px] ring-(--surf)"
            :class="classiEvento(e.a)"
            :style="{
              top: `${e.topPx}px`,
              height: `${e.altezzaPx}px`,
              ...stileCorsia(e.corsia, e.corsie),
            }"
            :title="testoEvento(e.a)"
            :data-test="`agenda-evento-${e.a.id}`"
            @click="emit('dettaglio', e.a)"
          >
            <template v-if="e.a.tipo === 'BLOCCO'">
              <div class="flex items-center gap-1 pr-5 text-[10.5px] font-bold leading-tight text-(--fg)">
                <Lock :size="11" class="flex-none text-(--fg3)" aria-hidden="true" />
                <span class="truncate">{{ titolo(e.a) }}</span>
              </div>
              <div class="truncate text-[10px] font-medium text-(--fg3)">{{ orarioBlocco(e.a) }}</div>
            </template>
            <div v-else class="flex items-center gap-1 pr-5 text-[10.5px] font-bold leading-tight text-(--fg)" :class="{ 'line-through': barrato(e.a) }">
              <Video v-if="e.a.tipo === 'VIDEOCONSULTO'" :size="12" class="flex-none" />
              <span class="truncate">{{ e.a.oraInizio }} {{ titolo(e.a) }}</span>
            </div>
            <div v-if="e.a.stato === 'ANNULLATA'" class="truncate text-xs font-medium text-(--danger)">Annullata</div>
            <div class="absolute right-0 top-0 opacity-0 transition-opacity focus-within:opacity-100 group-hover:opacity-100" @click.stop>
              <AppuntamentoMenu :appuntamento="e.a" @azione="(azione) => emit('azione', e.a, azione)" />
            </div>
          </div>

          <div
            v-if="c.eOggi && lineaAdessoPx !== null"
            class="pointer-events-none absolute inset-x-0 z-10 h-0.5 bg-(--danger)"
            :style="{ top: `${lineaAdessoPx}px` }"
            data-test="settimana-linea-adesso"
          />
        </div>
      </div>
    </div>
  </section>
</template>
