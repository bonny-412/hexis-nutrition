<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { BellRing, CalendarClock, StickyNote, Video } from '@lucide/vue'
import type { Notifica } from '@/api/notifiche'
import { Button } from '@/components/ui/button'
import { useNotificheStore } from '@/stores/notifiche'
import { attivaPush, statoPush, type StatoPush } from '@/utils/push'

const store = useNotificheStore()
const router = useRouter()

const push = ref<StatoPush>('attivo')
onMounted(async () => { push.value = await statoPush() })
async function attiva() { push.value = await attivaPush() }

function titolo(n: Notifica): string {
  if (n.tipo === 'PROMEMORIA') return n.testo || 'Promemoria'
  const nome = [n.pazienteNome, n.pazienteCognome].filter(Boolean).join(' ')
  return nome ? `${n.tipo === 'VIDEOCONSULTO' ? 'Videoconsulto' : 'Visita'} con ${nome}` : 'Visita'
}

function sottotitolo(n: Notifica): string {
  const paziente = n.tipo === 'PROMEMORIA' ? [n.pazienteNome, n.pazienteCognome].filter(Boolean).join(' ') : ''
  const ora = n.oraInizio ? `alle ${n.oraInizio}` : ''
  return [ora, paziente].filter(Boolean).join(' · ')
}

async function apri(n: Notifica) {
  await store.chiudiLista()
  await router.push({ name: 'agenda', query: { evento: n.appuntamentoId, data: n.data } })
}
</script>

<template>
  <div class="flex flex-col gap-2" data-test="notifiche-lista">
    <div class="px-1 pt-0.5 font-heading text-base italic text-(--fg)">Notifiche</div>

    <div v-if="push === 'da-attivare'" class="rounded-lg border border-(--bd) p-2.5" data-test="notifiche-push-invito">
      <p class="mb-2 text-xs text-(--fg3)">Ricevi un avviso anche quando Hexis è chiuso.</p>
      <Button type="button" size="sm" class="w-full" data-test="notifiche-push-attiva" @click="attiva">
        <BellRing :size="14" /> Attiva le notifiche su questo dispositivo
      </Button>
    </div>
    <p v-else-if="push === 'negato'" class="rounded-lg border border-(--bd) p-2.5 text-xs text-(--fg3)" data-test="notifiche-push-negato">
      Le notifiche sono bloccate per questo sito. Per riceverle anche a Hexis chiuso, riattivale dalle impostazioni del sito nel browser.
    </p>
    <p v-else-if="push === 'non-supportato'" class="rounded-lg border border-(--bd) p-2.5 text-xs text-(--fg3)" data-test="notifiche-push-non-supportato">
      Questo browser non supporta le notifiche push: vedrai gli avvisi solo qui, con Hexis aperto.
    </p>

    <p v-if="store.notifiche.length === 0" class="px-1 py-4 text-center text-xs text-(--fg3)" data-test="notifiche-vuote">
      Nessuna notifica
    </p>

    <ul v-else class="flex max-h-80 flex-col gap-1 overflow-y-auto">
      <li v-for="n in store.notifiche" :key="n.id">
        <button
          type="button"
          class="flex w-full cursor-pointer items-start gap-2.5 rounded-lg px-2 py-2 text-left transition-colors hover:bg-(--soft)"
          :class="n.letta ? '' : 'bg-(--mint)/60'"
          :data-test="`notifica-${n.id}`"
          @click="apri(n)"
        >
          <component :is="n.tipo === 'PROMEMORIA' ? StickyNote : n.tipo === 'VIDEOCONSULTO' ? Video : CalendarClock" :size="15" class="mt-0.5 flex-none text-(--fg3)" />
          <span class="min-w-0 flex-1">
            <span class="block truncate text-[13px] font-semibold text-(--fg)">{{ titolo(n) }}</span>
            <span v-if="sottotitolo(n)" class="block truncate text-xs text-(--fg3)">{{ sottotitolo(n) }}</span>
          </span>
          <span v-if="!n.letta" class="mt-1.5 size-2 flex-none rounded-full border border-(--green-d) bg-(--lime)" aria-label="Non letta" />
        </button>
      </li>
    </ul>
  </div>
</template>
