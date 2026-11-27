<script setup lang="ts">
import { computed, ref } from 'vue'
import { TabsContent, TabsList, TabsRoot, TabsTrigger } from 'reka-ui'
import { toast } from 'vue-sonner'
import { invita, type Paziente } from '@/api/pazienti'
import type { PianoAlimentareRigaLista } from '@/api/pianiAlimentari'
import { Button } from '@/components/ui/button'
import StatoVuoto from '@/components/StatoVuoto.vue'
import { CircleCheck, ChevronRight, Copy, Mail, Pencil } from '@lucide/vue'
import { aggiungiGiorni } from '@/utils/agenda'
import { formattaDataItalianaConMese } from '@/utils/data'
import { AREA_PAZIENTE_ATTIVA } from '@/config/funzioni'

/** Una lista per gruppo (null se il caricamento è fallito) con il totale reale, per "Vedi tutti". */
export interface GruppoDaFare<T> {
  voci: T[] | null
  totale: number
}

const props = defineProps<{
  inScadenza: GruppoDaFare<PianoAlimentareRigaLista>
  bozze: GruppoDaFare<PianoAlimentareRigaLista>
  maiInvitati: GruppoDaFare<Paziente>
  oggi: string
  fineFinestraScadenze: string
}>()

const emit = defineEmits<{ invitato: [id: string] }>()

function quandoScade(dataFine: string | null): string {
  if (!dataFine) return ''
  if (dataFine === props.oggi) return 'Scade oggi'
  if (dataFine === aggiungiGiorni(props.oggi, 1)) return 'Scade domani'
  return `Scade il ${formattaDataItalianaConMese(dataFine)}`
}

// Senza area paziente (decisione 0012) gli inviti non esistono: la linguetta e il suo gruppo spariscono.
const categorie = computed(() => [
  { id: 'scadenze', nome: 'Scadenze', gruppo: props.inScadenza },
  { id: 'bozze', nome: 'Bozze', gruppo: props.bozze },
  ...(AREA_PAZIENTE_ATTIVA ? [{ id: 'inviti', nome: 'Inviti', gruppo: props.maiInvitati }] : []),
])
const gruppi = computed(() => categorie.value.map(({ gruppo }) => gruppo))

const caricamentoFallito = computed(() => gruppi.value.some((g) => g.voci === null))
const tuttoFallito = computed(() => gruppi.value.every((g) => g.voci === null))
const nienteInSospeso = computed(() => !caricamentoFallito.value && gruppi.value.every((g) => g.voci!.length === 0))
const categoriaIniziale = computed(() => categorie.value.find(({ gruppo }) => gruppo.voci?.length)?.id ?? 'scadenze')

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
      :testo="AREA_PAZIENTE_ATTIVA ? 'Nessun piano in scadenza o in bozza, tutti i pazienti sono stati invitati.' : 'Nessun piano in scadenza o in bozza.'"
    />

    <TabsRoot v-else :default-value="categoriaIniziale" class="px-2 pb-3 pt-2">
      <TabsList aria-label="Categorie Da fare" class="mx-2 flex flex-nowrap gap-1 overflow-x-auto rounded-xl border border-(--bd) bg-(--soft) p-1">
        <TabsTrigger
          v-for="{ id, nome, gruppo } in categorie"
          :key="id"
          :value="id"
          class="tab-attivita flex grow shrink-0 cursor-pointer items-center justify-center gap-1 whitespace-nowrap rounded-lg px-1.5 py-2 text-xs font-semibold text-(--fg3) focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-(--green-d)"
        >
          {{ nome }}
          <span v-if="gruppo.voci !== null" class="conteggio shrink-0 rounded-full bg-(--surf) px-1.5 tabular-nums">{{ gruppo.totale }}</span>
          <span v-else class="sr-only">Caricamento non riuscito</span>
        </TabsTrigger>
      </TabsList>

      <TabsContent value="scadenze" data-test="da-fare-scadenza" class="pt-2 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-(--green-d)">
        <p v-if="inScadenza.voci === null" class="px-3 py-4 text-sm text-(--fg3)">Non è stato possibile caricare i piani in scadenza.</p>
        <p v-else-if="!inScadenza.voci.length" class="px-3 py-4 text-sm text-(--fg3)">Nessun piano in scadenza.</p>
        <ul v-else>
          <li v-for="p in inScadenza.voci" :key="p.id" class="flex items-center gap-3 px-5 py-2">
            <router-link :to="`/piani-alimentari/${p.id}`" class="min-w-0 flex-1 group">
              <p class="truncate text-sm font-semibold text-(--fg) group-hover:text-(--green)">{{ p.pazienteNomeCompleto }}</p>
              <p class="truncate text-xs text-(--fg3)">{{ quandoScade(p.dataFine) }}</p>
            </router-link>
            <Button as-child size="sm" variant="neutral" class="shrink-0">
              <router-link :to="`/piani-alimentari/nuovo?duplicaDa=${p.id}`" data-test="da-fare-rinnova">
                <Copy :size="14" /> Duplica piano
              </router-link>
            </Button>
          </li>
        </ul>
        <router-link
          v-if="inScadenza.voci && inScadenza.totale > inScadenza.voci.length"
          :to="{ name: 'piani-alimentari', query: { stato: 'ATTIVO', dataFineDa: oggi, dataFineA: fineFinestraScadenze } }"
          class="group mx-5 inline-flex items-center gap-0.5 text-xs font-semibold text-(--green) hover:text-(--green-d)"
        >Vedi tutti <ChevronRight :size="14" aria-hidden="true" class="transition-transform duration-150 motion-safe:group-hover:translate-x-0.5" /></router-link>
      </TabsContent>

      <TabsContent value="bozze" data-test="da-fare-bozze" class="pt-2 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-(--green-d)">
        <p v-if="bozze.voci === null" class="px-3 py-4 text-sm text-(--fg3)">Non è stato possibile caricare le bozze.</p>
        <p v-else-if="!bozze.voci.length" class="px-3 py-4 text-sm text-(--fg3)">Nessun piano in bozza.</p>
        <ul v-else>
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
          v-if="bozze.voci && bozze.totale > bozze.voci.length"
          :to="{ name: 'piani-alimentari', query: { stato: 'BOZZA' } }"
          class="group mx-5 inline-flex items-center gap-0.5 text-xs font-semibold text-(--green) hover:text-(--green-d)"
        >Vedi tutti <ChevronRight :size="14" aria-hidden="true" class="transition-transform duration-150 motion-safe:group-hover:translate-x-0.5" /></router-link>
      </TabsContent>

      <TabsContent v-if="AREA_PAZIENTE_ATTIVA" value="inviti" data-test="da-fare-inviti" class="pt-2 focus-visible:outline-2 focus-visible:-outline-offset-2 focus-visible:outline-(--green-d)">
        <p v-if="maiInvitati.voci === null" class="px-3 py-4 text-sm text-(--fg3)">Non è stato possibile caricare i pazienti da invitare.</p>
        <p v-else-if="!maiInvitati.voci.length" class="px-3 py-4 text-sm text-(--fg3)">Tutti i pazienti sono stati invitati.</p>
        <ul v-else>
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
          v-if="maiInvitati.voci && maiInvitati.totale > maiInvitati.voci.length"
          :to="{ name: 'pazienti', query: { statoAccount: 'MAI_INVITATO' } }"
          class="group mx-5 inline-flex items-center gap-0.5 text-xs font-semibold text-(--green) hover:text-(--green-d)"
        >Vedi tutti <ChevronRight :size="14" aria-hidden="true" class="transition-transform duration-150 motion-safe:group-hover:translate-x-0.5" /></router-link>
      </TabsContent>

      <p v-if="caricamentoFallito" class="px-5 pt-3 text-xs text-(--fg3)">Alcune attività non sono state caricate.</p>
    </TabsRoot>
  </section>
</template>

<style scoped>
.tab-attivita[aria-selected='true'] {
  background-color: var(--green);
  color: var(--on-green);
}

.tab-attivita[aria-selected='true'] .conteggio {
  color: var(--green-d);
}

.tab-attivita[aria-selected='true']:focus-visible {
  outline-color: var(--on-green);
}

@media (hover: hover) {
  .tab-attivita[aria-selected='false']:hover {
    background-color: var(--hover);
    color: var(--fg);
  }
}
</style>
