<script setup lang="ts">
import { computed } from 'vue'
import type { Appuntamento } from '@/api/agenda'
import { festivitaDelGiorno } from '@/utils/festivita'
import { celleDelMese, minutiDaOra, oggiIso } from '@/utils/agenda'
import { barrato, classiEvento, eVisitaAttiva, titolo } from '@/utils/agendaStile'

const props = defineProps<{ riferimento: string; appuntamenti: Appuntamento[] }>()
const emit = defineEmits<{ apriGiorno: [iso: string]; dettaglio: [appuntamento: Appuntamento] }>()

const NOMI_GIORNI = ['Lun', 'Mar', 'Mer', 'Gio', 'Ven', 'Sab', 'Dom']
const MAX_EVENTI = 2

const celle = computed(() => {
  const oggi = oggiIso()
  const mese = props.riferimento.slice(0, 7)
  return celleDelMese(props.riferimento).map((iso, indice) => {
    const eventi = props.appuntamenti
      .filter((a) => a.data === iso)
      // Prima gli eventi con orario in ordine, poi quelli senza.
      .sort((a, b) => (a.oraInizio === null ? 1 : 0) - (b.oraInizio === null ? 1 : 0) || minutiDaOra(a.oraInizio ?? '00:00') - minutiDaOra(b.oraInizio ?? '00:00'))
    return {
      iso,
      numero: Number(iso.slice(8)),
      fuoriMese: !iso.startsWith(mese),
      eOggi: iso === oggi,
      domenica: indice % 7 === 6,
      festa: festivitaDelGiorno(iso),
      visite: eventi.filter(eVisitaAttiva).length,
      promemoria: eventi.filter((a) => a.tipo === 'PROMEMORIA' && !a.fatto).length,
      blocchi: eventi.filter((a) => a.tipo === 'BLOCCO').length,
      mostrati: eventi.slice(0, MAX_EVENTI),
      altri: Math.max(eventi.length - MAX_EVENTI, 0),
    }
  })
})
</script>

<template>
  <section class="overflow-hidden rounded-2xl border border-(--bd) bg-(--surf)" data-test="agenda-mese">
    <div class="grid grid-cols-7 border-b border-(--bd)">
      <div v-for="nome in NOMI_GIORNI" :key="nome" class="px-2.5 py-2 text-[10.5px] font-bold uppercase tracking-widest text-(--fg3) last:bg-(--agenda-domenica)">
        {{ nome }}
      </div>
    </div>
    <div class="grid grid-cols-7">
      <!-- La cella è un div (non un button): al suo interno gli eventi sono pulsanti, niente pulsanti annidati. -->
      <div
        v-for="c in celle"
        :key="c.iso"
        class="flex min-h-16 min-w-0 cursor-pointer flex-col gap-1 overflow-hidden border-b border-r border-(--div) p-1 md:h-28 md:p-1.5"
        :class="[c.eOggi && (!c.festa || c.domenica) ? 'bg-(--mint)' : c.festa ? 'bg-(--warn-bg) hover:bg-(--soft)' : c.domenica ? 'bg-(--agenda-domenica) hover:brightness-95 dark:hover:brightness-110' : 'hover:bg-(--soft)', c.fuoriMese ? 'opacity-45' : '']"
        :data-test="`mese-cella-${c.iso}`"
        @click="emit('apriGiorno', c.iso)"
      >
        <div class="flex items-center justify-center gap-1.5 md:justify-start">
          <button
            type="button"
            class="flex size-6 flex-none cursor-pointer items-center justify-center rounded-full text-xs font-bold focus-visible:outline-2 focus-visible:outline-(--green)"
            :class="c.eOggi ? 'bg-(--green) text-(--on-green)' : 'text-(--fg)'"
            :aria-label="`Apri il giorno ${c.iso}${c.festa ? `, ${c.festa}` : ''}${c.visite ? `, ${c.visite} ${c.visite === 1 ? 'visita' : 'visite'}` : ''}${c.promemoria ? `, ${c.promemoria} promemoria da fare` : ''}${c.blocchi ? `, ${c.blocchi} ${c.blocchi === 1 ? 'fascia bloccata' : 'fasce bloccate'}` : ''}`"
            :data-test="`mese-giorno-${c.iso}`"
          >
            {{ c.numero }}
          </button>
          <span v-if="c.festa" class="hidden min-w-0 flex-1 truncate text-[10px] font-semibold text-(--warn-fg) md:block" :title="c.festa">{{ c.festa }}</span>
          <!-- Con la parola: un numero da solo nell'angolo non diceva cosa contava. -->
          <span v-if="c.visite > 0" class="ml-auto hidden flex-none text-[10.5px] font-semibold text-(--fg3) md:block">{{ c.visite }} {{ c.visite === 1 ? 'visita' : 'visite' }}</span>
        </div>
        <div class="flex flex-wrap justify-center gap-x-1 text-[10px] font-semibold tabular-nums md:hidden" aria-hidden="true">
          <span v-if="c.visite" class="inline-flex items-center gap-0.5 text-(--green-d)"><span class="size-1.5 rounded-full bg-(--green)" />{{ c.visite }}</span>
          <span v-if="c.promemoria" class="inline-flex items-center gap-0.5 text-(--warn-fg)"><span class="size-1.5 rounded-full bg-(--warn-fg)" />{{ c.promemoria }}</span>
          <span v-if="c.blocchi" class="inline-flex items-center gap-0.5 text-(--fg3)"><span class="size-1.5 rounded-full bg-(--fg3)" />{{ c.blocchi }}</span>
        </div>
        <button
          v-for="a in c.mostrati"
          :key="a.id"
          type="button"
          class="hidden w-full min-w-0 cursor-pointer items-center gap-1 rounded border-l-[3px] px-1 py-px text-left text-[10.5px] text-(--fg) focus-visible:outline-2 focus-visible:outline-(--green) md:flex"
          :class="[classiEvento(a), { 'line-through': barrato(a) }]"
          :data-test="`agenda-evento-${a.id}`"
          @click.stop="emit('dettaglio', a)"
        >
          <span v-if="a.oraInizio" class="flex-none font-bold">{{ a.oraInizio }}</span>
          <span class="truncate font-semibold">{{ titolo(a) }}</span>
        </button>
        <div v-if="c.altri > 0" class="hidden px-1 text-[10.5px] font-bold text-(--fg3) md:block">+{{ c.altri }} altri</div>
      </div>
    </div>
  </section>
</template>
