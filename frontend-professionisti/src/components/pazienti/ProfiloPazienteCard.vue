<script setup lang="ts">
import { computed, type Component } from 'vue'
import type { Paziente } from '@/api/pazienti'
import { calcolaEta, formattaDataItalianaEstesa } from '@/utils/data'
import { Activity, Briefcase, Cake, Fingerprint, Mail, Pencil, Phone, Pill, Plus, Smartphone, Stethoscope, StickyNote, User } from '@lucide/vue'
import { Button } from '@/components/ui/button'

// Stessa logica delle card Circonferenze e Plicometria: si mostrano solo i dati presenti,
// quelli mancanti stanno in una riga in fondo invece che in una colonna di trattini.
const props = defineProps<{ paziente: Paziente }>()
const emit = defineEmits<{ modifica: [] }>()

const ETICHETTE_SESSO: Record<Paziente['sesso'], string> = { M: 'Maschio', F: 'Femmina', ALTRO: 'Altro' }

const ETICHETTE_STILE_DI_VITA: Record<NonNullable<Paziente['stileDiVita']>, string> = {
  SEDENTARIO: 'Sedentario',
  POCO_ATTIVO: 'Poco attivo',
  ATTIVO: 'Attivo',
  MOLTO_ATTIVO: 'Molto attivo',
  ESTREMAMENTE_ATTIVO: 'Estremamente attivo',
}

interface Campo {
  chiave: string
  etichetta: string
  icona: Component
  valore: string | null
  /** Collegamento del valore (mailto:, tel:), se ha senso cliccarlo. */
  href?: string
  nota?: string
  mono?: boolean
}

const eta = computed(() => (props.paziente.dataNascita ? calcolaEta(props.paziente.dataNascita) : null))

const campi = computed<Campo[]>(() => {
  const p = props.paziente
  return [
    { chiave: 'cf', etichetta: 'Codice fiscale', icona: Fingerprint, valore: p.codiceFiscale?.toUpperCase() ?? null, mono: true },
    {
      chiave: 'nascita',
      etichetta: 'Data di nascita',
      icona: Cake,
      valore: p.dataNascita ? formattaDataItalianaEstesa(p.dataNascita) : null,
      nota: eta.value !== null ? `${eta.value} anni` : undefined,
    },
    { chiave: 'sesso', etichetta: 'Sesso', icona: User, valore: ETICHETTE_SESSO[p.sesso] ?? p.sesso },
    { chiave: 'email', etichetta: 'Email', icona: Mail, valore: p.email, href: `mailto:${p.email}` },
    { chiave: 'telefono', etichetta: 'Telefono', icona: Phone, valore: p.telefono, href: p.telefono ? `tel:${p.telefono}` : undefined },
    { chiave: 'cellulare', etichetta: 'Cellulare', icona: Smartphone, valore: p.cellulare, href: p.cellulare ? `tel:${p.cellulare}` : undefined },
    { chiave: 'lavoro', etichetta: 'Occupazione', icona: Briefcase, valore: p.lavoro },
    { chiave: 'attivita', etichetta: 'Livello di attività', icona: Activity, valore: p.stileDiVita ? ETICHETTE_STILE_DI_VITA[p.stileDiVita] : null },
    { chiave: 'medico', etichetta: 'Medico di riferimento', icona: Stethoscope, valore: p.medicoRiferimento },
  ]
})

const campiPresenti = computed(() => campi.value.filter((c) => c.valore))

const campiMancanti = computed(() => campi.value.filter((c) => !c.valore).map((c) => c.etichetta.toLowerCase()))

function elenco(voci: string[]): string {
  return voci.length > 1 ? `${voci.slice(0, -1).join(', ')} e ${voci.at(-1)}` : voci[0]
}

const testi = computed(() => [
  {
    chiave: 'storia',
    titolo: 'Storia del paziente',
    icona: StickyNote,
    testo: props.paziente.note,
    vuoto: 'Nessuna storia inserita per questo paziente.',
  },
  {
    chiave: 'terapie',
    titolo: 'Terapie in corso',
    icona: Pill,
    testo: props.paziente.trattamenti,
    vuoto: 'Nessun trattamento o terapia inserito per questo paziente.',
  },
])
</script>

<template>
  <aside class="rounded-2xl border border-(--bd) bg-(--surf) p-6" data-test="card-profilo">
    <div class="mb-5 flex items-start justify-between gap-3">
      <div>
        <h3 class="font-heading text-lg italic text-(--fg)">Profilo</h3>
        <p class="text-xs text-(--fg3)">Dati anagrafici e contatti</p>
      </div>
      <Button
        v-if="!paziente.archiviato"
        type="button"
        variant="ghost-primary"
        size="sm"
        data-test="profilo-modifica"
        @click="emit('modifica')"
      >
        <Pencil /> Modifica
      </Button>
    </div>

    <dl class="space-y-3.5 text-sm">
      <div v-for="campo in campiPresenti" :key="campo.chiave" class="flex items-start gap-3">
        <component :is="campo.icona" :size="16" class="mt-0.5 shrink-0 text-(--fg4)" aria-hidden="true" />
        <div class="min-w-0 flex-1">
          <dt class="text-xs text-(--fg3)">{{ campo.etichetta }}</dt>
          <dd class="truncate font-medium text-(--fg)" :class="{ 'font-mono tracking-wide': campo.mono }">
            <a
              v-if="campo.href"
              :href="campo.href"
              class="underline-offset-4 hover:text-(--green) hover:underline"
            >{{ campo.valore }}</a>
            <template v-else>{{ campo.valore }}</template>
            <span v-if="campo.nota" class="ml-1 text-xs font-normal text-(--fg3)">{{ campo.nota }}</span>
          </dd>
        </div>
      </div>
    </dl>

    <p
      v-if="campiMancanti.length"
      class="mt-4 border-t border-dashed border-(--bd2) pt-2.5 text-xs text-(--fg3)"
      data-test="profilo-non-indicati"
    >
      Non indicati: {{ elenco(campiMancanti) }}
    </p>

    <div class="mt-5 space-y-3">
      <section
        v-for="blocco in testi"
        :key="blocco.chiave"
        :data-test="`profilo-${blocco.chiave}`"
        class="rounded-xl bg-(--soft) p-3.5"
      >
        <h4 class="flex items-center gap-2 text-xs font-medium text-(--fg2)">
          <component :is="blocco.icona" :size="14" class="text-(--fg4)" aria-hidden="true" />
          {{ blocco.titolo }}
        </h4>
        <p v-if="blocco.testo" class="mt-1.5 whitespace-pre-wrap text-sm text-(--fg2)">{{ blocco.testo }}</p>
        <div v-else class="mt-1 flex flex-wrap items-center justify-between gap-x-3 gap-y-1">
          <p class="text-xs text-(--fg3)">{{ blocco.vuoto }}</p>
          <Button
            v-if="!paziente.archiviato"
            type="button"
            variant="link"
            size="xs"
            class="h-auto px-0"
            @click="emit('modifica')"
          >
            <Plus /> Aggiungi
          </Button>
        </div>
      </section>
    </div>
  </aside>
</template>
