<script setup lang="ts">
import { computed } from 'vue'
import type { Appuntamento } from '@/api/agenda'
import { giorniDellaSettimana } from '@/utils/agenda'
import { eVisitaAttiva } from '@/utils/agendaStile'
import { festivitaDelGiorno } from '@/utils/festivita'

// Colpo d'occhio sulla settimana corrente: visite e promemoria da fare.
// Ogni giorno apre l'Agenda su quella data (`?data=`).
const props = defineProps<{
  oggi: string
  /** Appuntamenti nella finestra del riepilogo; `null` finché i dati non sono disponibili. */
  appuntamenti: Appuntamento[] | null
  errore: boolean
}>()

/** Oltre questo numero di pallini si mostra "+n": il giorno resta leggibile anche se è pieno. */
const MAX_PALLINI = 6

const GIORNI_BREVI = ['dom', 'lun', 'mar', 'mer', 'gio', 'ven', 'sab']

const giorni = computed(() =>
  giorniDellaSettimana(props.oggi).map((iso, indice) => {
    const eventi = props.appuntamenti?.filter((a) => a.data === iso) ?? []
    const visite = eventi.filter(eVisitaAttiva).length
    const promemoria = eventi.filter((a) => a.tipo === 'PROMEMORIA' && !a.fatto).length
    const palliniVisite = Math.min(visite, MAX_PALLINI - (promemoria > 0 ? 1 : 0))
    const palliniPromemoria = Math.min(promemoria, MAX_PALLINI - palliniVisite)
    const giornoSettimana = (indice + 1) % 7
    const oggi = iso === props.oggi
    return {
      iso,
      oggi,
      etichetta: oggi ? 'oggi' : GIORNI_BREVI[giornoSettimana],
      numero: Number(iso.slice(8, 10)),
      visite,
      promemoria,
      palliniVisite,
      palliniPromemoria,
      altri: visite + promemoria - palliniVisite - palliniPromemoria,
      festa: festivitaDelGiorno(iso),
      domenica: giornoSettimana === 0,
      weekend: giornoSettimana === 0 || giornoSettimana === 6,
    }
  }),
)

function descrizione(g: (typeof giorni.value)[number]): string {
  const giorno = `${g.oggi ? 'Oggi' : g.etichetta} ${g.numero}${g.festa ? `, ${g.festa}` : ''}`
  if (props.appuntamenti === null) return giorno
  const visite = g.visite === 0 ? 'nessuna visita' : g.visite === 1 ? '1 visita' : `${g.visite} visite`
  const promemoria = g.promemoria > 0 ? `, ${g.promemoria} promemoria da fare` : ''
  return `${giorno}: ${visite}${promemoria}`
}
</script>

<template>
  <nav
    aria-label="Impegni della settimana corrente, da lunedì a domenica"
    class="grid grid-cols-7 overflow-clip rounded-2xl border border-(--bd) bg-(--surf)"
    data-test="dashboard-settimana"
  >
    <router-link
      v-for="g in giorni"
      :key="g.iso"
      :to="{ name: 'agenda', query: { data: g.iso } }"
      :aria-label="descrizione(g)"
      :title="g.festa ?? undefined"
      class="group flex min-w-0 flex-col items-center gap-1 border-l border-(--div) px-1 py-3 transition-colors first:border-l-0 sm:items-start sm:px-4"
      :class="g.oggi ? 'bg-(--mint)' : g.domenica && !g.festa ? 'bg-(--agenda-domenica) hover:brightness-95 dark:hover:brightness-110' : 'hover:bg-(--soft)'"
      :data-test="`dashboard-giorno-${g.iso}`"
    >
      <span
        class="text-xs"
        :class="g.oggi ? 'font-semibold text-(--green-d)' : g.festa ? 'text-(--warn-fg)' : 'text-(--fg3)'"
      >{{ g.etichetta }}</span>
      <span
        class="font-heading text-2xl leading-none tabular-nums"
        :class="g.oggi ? 'text-(--green-d)' : g.weekend || g.festa ? 'text-(--fg3)' : 'text-(--fg)'"
      >{{ g.numero }}</span>
      <span v-if="appuntamenti !== null" class="mt-1 flex min-h-2 flex-wrap items-center justify-center gap-0.5 sm:justify-start sm:gap-1" aria-hidden="true">
        <template v-if="g.visite || g.promemoria">
          <span
            v-for="n in g.palliniVisite"
            :key="`visita-${n}`"
            class="size-1.5 rounded-full bg-(--green)"
          />
          <span
            v-for="n in g.palliniPromemoria"
            :key="`promemoria-${n}`"
            class="size-1.5 rounded-full bg-(--warn-fg)"
          />
          <span v-if="g.altri" class="text-[10px] font-semibold text-(--fg3)">+{{ g.altri }}</span>
        </template>
        <span v-else class="h-px w-3 bg-(--bd2)" />
      </span>
      <span v-if="appuntamenti !== null" class="hidden text-[11px] text-(--fg3) sm:flex sm:flex-wrap sm:gap-x-1.5">
        <span v-if="g.visite">{{ g.visite === 1 ? '1 visita' : `${g.visite} visite` }}</span>
        <span v-if="g.promemoria">{{ g.promemoria }} promemoria</span>
        <span v-if="g.festa">Festivo</span>
        <span v-else-if="!g.visite && !g.promemoria">Libero</span>
      </span>
    </router-link>
    <p v-if="appuntamenti === null" class="col-span-full border-t border-(--div) px-3 py-2 text-xs text-(--fg3)" role="status">
      {{ errore ? 'Riepilogo agenda non disponibile.' : 'Caricamento del riepilogo agenda…' }}
    </p>
  </nav>
</template>
