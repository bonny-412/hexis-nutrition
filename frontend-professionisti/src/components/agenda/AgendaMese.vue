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
  return celleDelMese(props.riferimento).map((iso) => {
    const eventi = props.appuntamenti
      .filter((a) => a.data === iso)
      // Prima gli eventi con orario in ordine, poi quelli senza.
      .sort((a, b) => (a.oraInizio === null ? 1 : 0) - (b.oraInizio === null ? 1 : 0) || minutiDaOra(a.oraInizio ?? '00:00') - minutiDaOra(b.oraInizio ?? '00:00'))
    return {
      iso,
      numero: Number(iso.slice(8)),
      fuoriMese: !iso.startsWith(mese),
      eOggi: iso === oggi,
      festa: festivitaDelGiorno(iso),
      visite: eventi.filter(eVisitaAttiva).length,
      mostrati: eventi.slice(0, MAX_EVENTI),
      altri: Math.max(eventi.length - MAX_EVENTI, 0),
    }
  })
})
</script>

<template>
  <section class="overflow-hidden rounded-2xl border border-(--bd) bg-(--surf)" data-test="agenda-mese">
    <div class="grid grid-cols-7 border-b border-(--bd)">
      <div v-for="nome in NOMI_GIORNI" :key="nome" class="px-2.5 py-2 text-[10.5px] font-bold uppercase tracking-widest text-(--fg4)">
        {{ nome }}
      </div>
    </div>
    <div class="grid grid-cols-7">
      <!-- La cella è un div (non un button): al suo interno gli eventi sono pulsanti, niente pulsanti annidati. -->
      <div
        v-for="c in celle"
        :key="c.iso"
        class="flex h-28 min-w-0 cursor-pointer flex-col gap-1 overflow-hidden border-b border-r border-(--div) p-1.5 text-left transition-colors hover:bg-(--soft)"
        :class="[c.festa ? 'bg-(--warn-bg)' : c.eOggi ? 'bg-(--mint)' : '', c.fuoriMese ? 'opacity-45' : '']"
        :data-test="`mese-cella-${c.iso}`"
        @click="emit('apriGiorno', c.iso)"
      >
        <div class="flex items-center gap-1.5">
          <button
            type="button"
            class="flex size-6 flex-none cursor-pointer items-center justify-center rounded-full text-xs font-bold focus-visible:outline-2 focus-visible:outline-(--green)"
            :class="c.eOggi ? 'bg-(--green) text-(--on-green)' : 'text-(--fg)'"
            :aria-label="`Apri il giorno ${c.iso}`"
            :data-test="`mese-giorno-${c.iso}`"
          >
            {{ c.numero }}
          </button>
          <span v-if="c.festa" class="min-w-0 flex-1 truncate text-[10px] font-semibold text-(--warn-fg)" :title="c.festa">{{ c.festa }}</span>
          <span v-if="c.visite > 0" class="ml-auto flex-none text-[10.5px] font-semibold text-(--fg3)">{{ c.visite }}</span>
        </div>
        <button
          v-for="a in c.mostrati"
          :key="a.id"
          type="button"
          class="flex w-full min-w-0 cursor-pointer items-center gap-1 rounded border-l-[3px] px-1 py-px text-left text-[10.5px] text-(--fg) focus-visible:outline-2 focus-visible:outline-(--green)"
          :class="[classiEvento(a), { 'line-through': barrato(a) }]"
          :data-test="`agenda-evento-${a.id}`"
          @click.stop="emit('dettaglio', a)"
        >
          <span v-if="a.oraInizio" class="flex-none font-bold">{{ a.oraInizio }}</span>
          <span class="truncate font-semibold">{{ titolo(a) }}</span>
        </button>
        <div v-if="c.altri > 0" class="px-1 text-[10.5px] font-bold text-(--fg3)">+{{ c.altri }} altri</div>
      </div>
    </div>
  </section>
</template>
