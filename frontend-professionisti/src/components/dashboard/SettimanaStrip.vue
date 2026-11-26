<script setup lang="ts">
import { computed } from 'vue'
import type { Appuntamento } from '@/api/agenda'
import { aggiungiGiorni } from '@/utils/agenda'
import { eVisitaAttiva } from '@/utils/agendaStile'
import { festivitaDelGiorno } from '@/utils/festivita'

// Colpo d'occhio sui prossimi 7 giorni: quante visite ha ogni giorno, un pallino per visita.
// Ogni giorno apre l'Agenda su quella data (`?data=`).
const props = defineProps<{
  oggi: string
  /** Appuntamenti da `oggi` in avanti; `null` se l'agenda non si è caricata. */
  appuntamenti: Appuntamento[] | null
}>()

/** Oltre questo numero di pallini si mostra "+n": il giorno resta leggibile anche se è pieno. */
const MAX_PALLINI = 6

const GIORNI_BREVI = ['dom', 'lun', 'mar', 'mer', 'gio', 'ven', 'sab']

const giorni = computed(() =>
  Array.from({ length: 7 }, (_, i) => {
    const iso = aggiungiGiorni(props.oggi, i)
    const visite = props.appuntamenti?.filter((a) => a.data === iso && eVisitaAttiva(a)).length ?? 0
    // Giorno della settimana dal calendario UTC della data ISO: niente fuso locale.
    const giornoSettimana = new Date(`${iso}T12:00:00Z`).getUTCDay()
    return {
      iso,
      oggi: i === 0,
      etichetta: i === 0 ? 'oggi' : GIORNI_BREVI[giornoSettimana],
      numero: Number(iso.slice(8, 10)),
      visite,
      festa: festivitaDelGiorno(iso),
      weekend: giornoSettimana === 0 || giornoSettimana === 6,
    }
  }),
)

function descrizione(g: (typeof giorni.value)[number]): string {
  const visite = g.visite === 0 ? 'nessuna visita' : g.visite === 1 ? '1 visita' : `${g.visite} visite`
  return `${g.oggi ? 'Oggi' : g.etichetta} ${g.numero}: ${visite}${g.festa ? `, ${g.festa}` : ''}`
}
</script>

<template>
  <nav
    aria-label="Visite dei prossimi 7 giorni"
    class="grid grid-cols-7 overflow-clip rounded-2xl border border-(--bd) bg-(--surf)"
    data-test="dashboard-settimana"
  >
    <router-link
      v-for="g in giorni"
      :key="g.iso"
      :to="{ name: 'agenda', query: { data: g.iso } }"
      :aria-label="descrizione(g)"
      :title="g.festa ?? undefined"
      class="group flex min-w-0 flex-col items-center gap-1 border-l border-(--div) px-1 py-3 transition-colors first:border-l-0 hover:bg-(--soft) sm:items-start sm:px-4"
      :class="g.oggi ? 'bg-(--mint) hover:bg-(--mint)' : ''"
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
      <!-- Un pallino per visita: il ritmo della settimana si legge senza contare. -->
      <span class="mt-1 flex h-2 items-center gap-1" aria-hidden="true">
        <template v-if="g.visite > 0">
          <span
            v-for="n in Math.min(g.visite, MAX_PALLINI)"
            :key="n"
            class="size-1.5 rounded-full bg-(--green)"
          />
          <span v-if="g.visite > MAX_PALLINI" class="text-[10px] font-semibold text-(--green-d)">+{{ g.visite - MAX_PALLINI }}</span>
        </template>
        <span v-else class="h-px w-3 bg-(--bd2)" />
      </span>
      <span class="hidden text-[11px] text-(--fg3) sm:block">
        {{ g.festa ? 'Festivo' : g.visite === 0 ? 'Libero' : g.visite === 1 ? '1 visita' : `${g.visite} visite` }}
      </span>
    </router-link>
  </nav>
</template>
