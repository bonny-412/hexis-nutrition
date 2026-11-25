<script setup lang="ts">
import { computed } from 'vue'
import { Video } from '@lucide/vue'
import type { Appuntamento } from '@/api/agenda'
import { Checkbox } from '@/components/ui/checkbox'
import AppuntamentoMenu, { type AzioneAppuntamento } from '@/components/agenda/AppuntamentoMenu.vue'
import { festivitaDelGiorno } from '@/utils/festivita'
import { minutiDaOra } from '@/utils/agenda'
import { barrato, classiEvento, etichettaTipo, oraFine, sottotitolo, titolo } from '@/utils/agendaStile'

const props = defineProps<{ giorno: string; appuntamenti: Appuntamento[] }>()
const emit = defineEmits<{ azione: [appuntamento: Appuntamento, azione: AzioneAppuntamento]; dettaglio: [appuntamento: Appuntamento] }>()

const festa = computed(() => festivitaDelGiorno(props.giorno))

// Impegni con orario, in ordine; i promemoria senza ora vanno nel pannello "Da fare oggi".
const delGiorno = computed(() => props.appuntamenti.filter((a) => a.data === props.giorno))
const impegni = computed(() =>
  delGiorno.value
    .filter((a) => a.oraInizio !== null)
    .sort((a, b) => minutiDaOra(a.oraInizio as string) - minutiDaOra(b.oraInizio as string)),
)
const daFare = computed(() => delGiorno.value.filter((a) => a.tipo === 'PROMEMORIA' && a.oraInizio === null))
</script>

<template>
  <section
    class="overflow-hidden rounded-2xl border border-(--bd)"
    :class="festa ? 'bg-(--warn-bg)' : 'bg-(--surf)'"
    data-test="agenda-giorno"
  >
    <h3 v-if="festa" class="border-b border-(--bd) px-5 py-3 font-heading text-lg italic text-(--warn-fg)" data-test="agenda-festa">
      {{ festa }}
    </h3>

    <div v-if="delGiorno.length === 0" class="p-14 text-center text-(--fg3)" data-test="agenda-giornata-libera">
      Giornata libera
    </div>

    <div v-else class="grid gap-5 p-4 lg:grid-cols-[1fr_280px]">
      <ul class="flex flex-col gap-2" data-test="agenda-impegni">
        <li v-if="impegni.length === 0" class="py-6 text-center text-sm text-(--fg3)">Nessun impegno con orario.</li>
        <li
          v-for="a in impegni"
          :key="a.id"
          class="flex cursor-pointer items-center gap-3 rounded-lg border-l-4 px-3 py-2.5"
          :class="classiEvento(a)"
          :data-test="`agenda-evento-${a.id}`"
          @click="emit('dettaglio', a)"
        >
          <div class="w-20 flex-none text-xs text-(--fg2)">
            <div class="text-sm font-bold text-(--fg)">{{ a.oraInizio }}</div>
            <div v-if="oraFine(a)">{{ oraFine(a) }} · {{ a.durataMinuti }} min</div>
          </div>
          <div class="min-w-0 flex-1" :class="{ 'line-through': barrato(a) }">
            <div class="truncate text-sm font-semibold text-(--fg)">{{ titolo(a) }}</div>
            <div v-if="sottotitolo(a)" class="truncate text-xs text-(--fg3)">{{ sottotitolo(a) }}</div>
          </div>
          <span class="flex flex-none items-center gap-1 text-[11px] font-bold uppercase tracking-wide text-(--fg3)">
            <Video v-if="a.tipo === 'VIDEOCONSULTO'" :size="14" />
            {{ etichettaTipo(a) }}
          </span>
          <span @click.stop>
            <AppuntamentoMenu :appuntamento="a" @azione="(azione) => emit('azione', a, azione)" />
          </span>
        </li>
      </ul>

      <aside class="rounded-xl border border-(--bd) bg-(--soft) p-3.5" data-test="agenda-da-fare">
        <h4 class="mb-2.5 font-heading text-base italic text-(--fg)">Da fare oggi</h4>
        <p v-if="daFare.length === 0" class="text-xs text-(--fg3)">Nessun promemoria senza orario.</p>
        <ul v-else class="flex flex-col gap-2">
          <li v-for="a in daFare" :key="a.id" class="flex cursor-pointer items-center gap-2.5" :data-test="`agenda-evento-${a.id}`" @click="emit('dettaglio', a)">
            <span class="flex" @click.stop>
              <Checkbox
                :model-value="a.fatto"
                :aria-label="`Fatto: ${titolo(a)}`"
                @update:model-value="emit('azione', a, a.fatto ? 'DA_FARE' : 'FATTO')"
              />
            </span>
            <span class="min-w-0 flex-1 truncate text-sm text-(--fg)" :class="{ 'text-(--fg3) line-through': a.fatto }">
              {{ titolo(a) }}
            </span>
            <span @click.stop>
              <AppuntamentoMenu :appuntamento="a" @azione="(azione) => emit('azione', a, azione)" />
            </span>
          </li>
        </ul>
      </aside>
    </div>
  </section>
</template>
