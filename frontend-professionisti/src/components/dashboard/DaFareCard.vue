<script setup lang="ts">
import { computed, ref } from 'vue'
import { toast } from 'vue-sonner'
import { invita, type Paziente } from '@/api/pazienti'
import type { PianoAlimentareRigaLista } from '@/api/pianiAlimentari'
import { Button } from '@/components/ui/button'
import StatoVuoto from '@/components/StatoVuoto.vue'
import { CircleCheck, ChevronRight, Mail, Pencil, RefreshCw } from '@lucide/vue'
import { aggiungiGiorni, oggiIso } from '@/utils/agenda'
import { formattaDataItalianaConMese } from '@/utils/data'

/** Una lista per gruppo (null se il caricamento è fallito) con il totale reale, per "Vedi tutti". */
export interface GruppoDaFare<T> {
  voci: T[] | null
  totale: number
}

const props = defineProps<{
  inScadenza: GruppoDaFare<PianoAlimentareRigaLista>
  bozze: GruppoDaFare<PianoAlimentareRigaLista>
  maiInvitati: GruppoDaFare<Paziente>
}>()

const emit = defineEmits<{ invitato: [id: string] }>()

const oggi = oggiIso()

function quandoScade(dataFine: string | null): string {
  if (!dataFine) return ''
  if (dataFine === oggi) return 'Scade oggi'
  if (dataFine === aggiungiGiorni(oggi, 1)) return 'Scade domani'
  return `Scade il ${formattaDataItalianaConMese(dataFine)}`
}

const caricamentoFallito = computed(() => [props.inScadenza, props.bozze, props.maiInvitati].some((g) => g.voci === null))
const tuttoFallito = computed(() => [props.inScadenza, props.bozze, props.maiInvitati].every((g) => g.voci === null))
const nienteInSospeso = computed(
  () => !caricamentoFallito.value && [props.inScadenza, props.bozze, props.maiInvitati].every((g) => g.voci!.length === 0),
)

/** Pazienti per cui l'invio dell'invito è in corso: il pulsante resta disabilitato fino alla risposta. */
const invioInCorso = ref(new Set<string>())

async function onInvita(p: Paziente) {
  if (invioInCorso.value.has(p.id)) return
  invioInCorso.value.add(p.id)
  try {
    await invita(p.id)
    toast.success('Invito inviato.')
    emit('invitato', p.id)
  } catch {
    toast.error("Non è stato possibile inviare l'invito.")
  } finally {
    invioInCorso.value.delete(p.id)
  }
}
</script>

<template>
  <section class="rounded-2xl border border-(--bd) bg-(--surf)" data-test="dashboard-da-fare">
    <header class="px-5 pt-4 pb-1">
      <h2 class="font-heading text-lg italic text-(--fg)">Da fare</h2>
    </header>

    <p v-if="tuttoFallito" class="px-5 pb-5 text-sm text-(--fg3)">Non è stato possibile caricare le attività.</p>

    <StatoVuoto
      v-else-if="nienteInSospeso"
      compatto
      :icona="CircleCheck"
      titolo="Niente in sospeso"
      testo="Nessun piano in scadenza o in bozza, tutti i pazienti sono stati invitati."
    />

    <div v-else class="pb-2">
      <!-- 1. Piani attivi che scadono entro una settimana -->
      <div v-if="inScadenza.voci?.length" data-test="da-fare-scadenza">
        <h3 class="flex items-center justify-between px-5 pt-3 pb-1 text-xs font-semibold text-(--fg3)">
          Piani in scadenza
          <span class="tabular-nums">{{ inScadenza.totale }}</span>
        </h3>
        <ul>
          <li v-for="p in inScadenza.voci" :key="p.id" class="flex items-center gap-3 px-5 py-2">
            <router-link :to="`/piani-alimentari/${p.id}`" class="min-w-0 flex-1 group">
              <p class="truncate text-sm font-semibold text-(--fg) group-hover:text-(--green)">{{ p.pazienteNomeCompleto }}</p>
              <p class="truncate text-xs text-(--fg3)">{{ quandoScade(p.dataFine) }}</p>
            </router-link>
            <Button as-child size="sm" variant="neutral" class="shrink-0">
              <router-link :to="`/piani-alimentari/nuovo?duplicaDa=${p.id}`" data-test="da-fare-rinnova">
                <RefreshCw :size="14" /> Rinnova
              </router-link>
            </Button>
          </li>
        </ul>
        <router-link
          v-if="inScadenza.totale > inScadenza.voci.length"
          to="/piani-alimentari"
          class="group mx-5 inline-flex items-center gap-0.5 text-xs font-semibold text-(--green) hover:text-(--green-d)"
        >Vedi tutti <ChevronRight :size="14" aria-hidden="true" class="transition-transform duration-150 motion-safe:group-hover:translate-x-0.5" /></router-link>
      </div>

      <!-- 2. Piani in bozza -->
      <div v-if="bozze.voci?.length" data-test="da-fare-bozze">
        <h3 class="flex items-center justify-between px-5 pt-3 pb-1 text-xs font-semibold text-(--fg3)">
          Piani in bozza
          <span class="tabular-nums">{{ bozze.totale }}</span>
        </h3>
        <ul>
          <li v-for="p in bozze.voci" :key="p.id" class="flex items-center gap-3 px-5 py-2">
            <div class="min-w-0 flex-1">
              <p class="truncate text-sm font-semibold text-(--fg)">{{ p.pazienteNomeCompleto }}</p>
              <p class="truncate text-xs text-(--fg3)">{{ p.nome }}</p>
            </div>
            <Button as-child size="sm" variant="neutral" class="shrink-0">
              <router-link :to="`/piani-alimentari/${p.id}`" data-test="da-fare-completa">
                <Pencil :size="14" /> Completa
              </router-link>
            </Button>
          </li>
        </ul>
        <router-link
          v-if="bozze.totale > bozze.voci.length"
          to="/piani-alimentari"
          class="group mx-5 inline-flex items-center gap-0.5 text-xs font-semibold text-(--green) hover:text-(--green-d)"
        >Vedi tutti <ChevronRight :size="14" aria-hidden="true" class="transition-transform duration-150 motion-safe:group-hover:translate-x-0.5" /></router-link>
      </div>

      <!-- 3. Pazienti mai invitati all'app -->
      <div v-if="maiInvitati.voci?.length" data-test="da-fare-inviti">
        <h3 class="flex items-center justify-between px-5 pt-3 pb-1 text-xs font-semibold text-(--fg3)">
          Mai invitati
          <span class="tabular-nums">{{ maiInvitati.totale }}</span>
        </h3>
        <ul>
          <li v-for="p in maiInvitati.voci" :key="p.id" class="flex items-center gap-3 px-5 py-2">
            <router-link :to="`/pazienti/${p.id}`" class="min-w-0 flex-1 group">
              <p class="truncate text-sm font-semibold text-(--fg) group-hover:text-(--green)">{{ p.nome }} {{ p.cognome }}</p>
              <p class="truncate text-xs text-(--fg3)">{{ p.email }}</p>
            </router-link>
            <Button
              size="sm"
              variant="neutral"
              class="shrink-0"
              data-test="da-fare-invita"
              :disabled="invioInCorso.has(p.id)"
              @click="onInvita(p)"
            >
              <Mail :size="14" /> {{ invioInCorso.has(p.id) ? 'Invio…' : 'Invita' }}
            </Button>
          </li>
        </ul>
        <router-link
          v-if="maiInvitati.totale > maiInvitati.voci.length"
          to="/pazienti"
          class="group mx-5 inline-flex items-center gap-0.5 text-xs font-semibold text-(--green) hover:text-(--green-d)"
        >Vedi tutti <ChevronRight :size="14" aria-hidden="true" class="transition-transform duration-150 motion-safe:group-hover:translate-x-0.5" /></router-link>
      </div>

      <p v-if="caricamentoFallito" class="px-5 pt-3 text-xs text-(--fg3)">Alcune attività non sono state caricate.</p>
    </div>
  </section>
</template>
