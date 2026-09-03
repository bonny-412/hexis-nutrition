<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { toast } from 'vue-sonner'
import AppShell from '@/components/AppShell.vue'
import { dettaglio, invita, visite as caricaVisite, type Paziente, type Visita } from '@/api/pazienti'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import type { AcceptableValue } from 'reka-ui'
import PazienteTabPanoramica from '@/components/pazienti/PazienteTabPanoramica.vue'
import PazienteTabStoricoMisurazioni from '@/components/pazienti/PazienteTabStoricoMisurazioni.vue'
import PazienteTabConfrontoVisite from '@/components/pazienti/PazienteTabConfrontoVisite.vue'
import ModificaPazienteDialog from '@/components/pazienti/ModificaPazienteDialog.vue'
import { prepareAndamento } from '@/utils/andamento'
import { formattaDataItalianaConMese } from '@/utils/data'
import { ETICHETTE_OBIETTIVO, formattaNumero } from '@/utils/visita'
import {
  ArrowLeft,
  ArrowUp,
  ArrowDown,
  Pencil,
  AlertCircle,
  Plus,
  Scale,
  Ruler,
  Percent,
  Dumbbell,
  CalendarX2,
} from '@lucide/vue'

const ETICHETTE_STATO_ACCOUNT: Record<Paziente['statoAccount'], string> = {
  MAI_INVITATO: 'Mai invitato',
  INVITATO: 'Invitato',
  ATTIVO: 'Attivo',
}

const CLASSI_STATO_ACCOUNT: Record<Paziente['statoAccount'], string> = {
  MAI_INVITATO: 'bg-(--hover) text-(--fg4) border border-(--fg4)/20',
  INVITATO: 'bg-(--warn-bg) text-(--warn-fg) border border-(--warn-fg)',
  ATTIVO: 'bg-(--mint) text-(--green) border border-(--green)',
}

const TAB_DEFS = [
  { id: 'panoramica', label: 'Panoramica' },
  { id: 'storico', label: 'Storico misurazioni' },
  { id: 'confronto', label: 'Confronto visite' },
  { id: 'piani', label: 'Piani alimentari' },
] as const

type TabId = (typeof TAB_DEFS)[number]['id']

const route = useRoute()

// Stato Paziente
const caricamento = ref(true)
const paziente = ref<Paziente | null>(null)
const erroreCaricamento = ref<string | null>(null)
const invitoInCorso = ref(false)
const mostraModificaAnagrafica = ref(false)

// Stato Visite e Andamento
const visite = ref<Visita[]>([])
const erroreVisite = ref(false)
const visiteInCaricamento = ref(true)

// Tab attivo nella sezione clinica
const tabAttivo = ref<TabId>('panoramica')

function onCambiaTab(valore: AcceptableValue) {
  tabAttivo.value = valore as TabId
}

const andamento = computed(() => prepareAndamento(visite.value))
const primaVisita = computed(() => (visite.value.length > 0 ? visite.value[0] : null))
const ultimaVisita = computed(() => (visite.value.length > 0 ? visite.value[visite.value.length - 1] : null))

async function carica() {
  caricamento.value = true
  try {
    paziente.value = await dettaglio(route.params.id as string)
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      erroreCaricamento.value = 'Paziente non trovato.'
    } else {
      erroreCaricamento.value = 'Non è stato possibile caricare il paziente.'
    }
  } finally {
    caricamento.value = false
  }
}

async function caricaAndamento() {
  try {
    visite.value = await caricaVisite(route.params.id as string)
  } catch {
    erroreVisite.value = true
  } finally {
    visiteInCaricamento.value = false
  }
}

function onPazienteAggiornato(aggiornato: Paziente) {
  paziente.value = aggiornato
}

async function onInvita() {
  if (!paziente.value) return
  invitoInCorso.value = true
  try {
    await invita(paziente.value.id)
    paziente.value.statoAccount = 'INVITATO'
    toast.success('Invito inviato con successo.')
  } catch {
    toast.error("Non è stato possibile inviare l'invito.")
  } finally {
    invitoInCorso.value = false
  }
}

onMounted(() => {
  carica()
  caricaAndamento()
})
</script>

<template>
  <AppShell>
    <!-- Link Torna Indietro -->
    <div>
      <router-link to="/pazienti"
        class="inline-flex items-center gap-2 text-xs font-semibold text-(--fg3) transition-colors hover:text-(--green)"
      >
        <ArrowLeft :size="16" />
        <span>Torna alla lista pazienti</span>
      </router-link>
    </div>

    <!-- Stato Errore Caricamento -->
    <div
      v-if="erroreCaricamento"
      class="flex flex-col items-center justify-center rounded-2xl border border-(--bd) bg-(--surf) p-12 text-center shadow-sm"
    >
      <AlertCircle :size="40" class="text-(--danger) mb-3" />
      <h2 class="font-heading text-xl italic text-(--fg)">Errore di caricamento</h2>
      <p class="mt-1 text-sm text-(--fg3)">{{ erroreCaricamento }}</p>
      <Button variant="outline" size="sm" class="mt-4" @click="carica">Riprova</Button>
    </div>

    <!-- Skeleton Loading -->
    <div v-else-if="caricamento" class="space-y-6">
      <div class="space-y-2">
        <div class="h-8 w-64 animate-pulse rounded bg-(--hover)" />
        <div class="h-4 w-96 animate-pulse rounded bg-(--hover)" />
      </div>
      <div class="h-40 animate-pulse rounded-2xl bg-(--hover)" />
      <div class="grid gap-6 md:grid-cols-2">
        <div class="h-48 animate-pulse rounded-2xl bg-(--hover)" />
        <div class="h-48 animate-pulse rounded-2xl bg-(--hover)" />
      </div>
      <div class="h-64 animate-pulse rounded-2xl bg-(--hover)" />
    </div>

    <!-- Contenuto Principale -->
    <div v-else-if="paziente" class="space-y-6 mt-4">
      <!-- 1. INTESTAZIONE PAZIENTE -->
      <div class="flex flex-col gap-4 sm:flex-row sm:flex-wrap sm:items-start sm:justify-between">
        <div class="flex items-start gap-4">
          <span
            class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl font-heading text-xl font-semibold select-none"
            :class="CLASSI_STATO_ACCOUNT[paziente.statoAccount]"
          >
            {{ paziente.nome[0] }}{{ paziente.cognome[0] }}
          </span>
          <div>
            <div class="flex flex-wrap items-center gap-2.5">
              <h1 class="font-heading text-2xl italic text-(--fg)">{{ paziente.nome }} {{ paziente.cognome }}</h1>
              <Badge v-if="paziente.statoAccount !== 'MAI_INVITATO'" :class="CLASSI_STATO_ACCOUNT[paziente.statoAccount]">
                {{ ETICHETTE_STATO_ACCOUNT[paziente.statoAccount] }}
              </Badge>
              <Button
                variant="ghost"
                size="icon-sm"
                title="Modifica dati anagrafici"
                aria-label="Modifica dati anagrafici"
                @click="mostraModificaAnagrafica = true"
              >
                <Pencil :size="14" />
              </Button>
              <button
                v-if="paziente.statoAccount !== 'ATTIVO'"
                type="button"
                :disabled="invitoInCorso"
                class="text-sm font-semibold text-(--green) underline-offset-4 hover:underline disabled:cursor-not-allowed disabled:opacity-50"
                @click="onInvita"
              >
                {{ paziente.statoAccount === 'MAI_INVITATO' ? 'Invita' : 'Reinvia invito' }}
              </button>
            </div>
            <div class="mt-1 text-sm text-(--fg3) flex w-full lg:flex-wrap lg:flex-row flex-col lg:items-center">
              <span>{{ paziente.email }}</span>
              <span class="hidden lg:flex mx-1"> · </span>
              <span v-if="ultimaVisita">Obiettivo attuale: {{ ETICHETTE_OBIETTIVO[ultimaVisita.obiettivo] }}</span>
              <span class="hidden lg:flex mx-1"> · </span>
              <span v-if="primaVisita">paziente dal {{ formattaDataItalianaConMese(primaVisita.dataVisita) }}</span>
            </div>
            <div class="mt-2.5 flex flex-wrap items-center gap-2">
              <span class="inline-flex items-center gap-1.5 rounded-full bg-(--warn-bg) px-2.5 py-1 text-xs font-bold text-(--warn-fg) border border-(--warn-fg)">
                <CalendarX2 :size="16" />
                Nessuna visita programmata
              </span>
            </div>
          </div>
        </div>

        <div class="flex flex-wrap items-center gap-2 ">
          <Button variant="outline" size="sm" disabled>
            <Plus :size="15" />
            <span>Nuovo piano</span>
          </Button>
          <Button as-child size="sm" class="hover:bg-primary/80">
            <router-link :to="`/pazienti/${paziente.id}/visite/nuova`">
              <Plus :size="15" />
              <span>Nuova visita</span>
            </router-link>
          </Button>
        </div>
      </div>

      <!-- 2. STATISTICHE RAPIDE -->
      <p v-if="erroreVisite" class="text-xs font-medium text-(--danger)">
        Non è stato possibile caricare i dati clinici del paziente.
      </p>
      <div v-else class="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-5">
        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-4 shadow-sm">
          <div class="flex items-center gap-1.5">
            <Scale :size="14" class="text-primary" />
            <p class="text-xs font-bold uppercase tracking-wide text-(--fg4)">Peso attuale</p>
          </div>
          <div v-if="visiteInCaricamento" class="mt-2 h-6 w-16 animate-pulse rounded bg-(--hover)" />
          <template v-else>
            <p class="mt-1 text-xl font-heading text-(--fg)">
              {{ andamento.peso.ultimo !== null ? `${formattaNumero(andamento.peso.ultimo)} kg` : '—' }}
            </p>
            <p
              v-if="andamento.peso.delta !== null"
              class="flex items-center gap-0.5 text-xs font-medium"
              :class="andamento.peso.delta < 0 ? 'text-(--green)' : andamento.peso.delta > 0 ? 'text-(--danger)' : 'text-(--fg3)'"
            >
              <ArrowDown v-if="andamento.peso.delta < 0" :size="11" />
              <ArrowUp v-else-if="andamento.peso.delta > 0" :size="11" />
              {{ formattaNumero(Math.abs(andamento.peso.delta)) }} kg vs prec.
            </p>
          </template>
        </div>

        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-4 shadow-sm">
          <div class="flex items-center gap-1.5">
            <Ruler :size="14" class="text-primary" />
            <p class="text-xs font-bold uppercase tracking-wide text-(--fg4)">BMI</p>
          </div>
          <div v-if="visiteInCaricamento" class="mt-2 h-6 w-12 animate-pulse rounded bg-(--hover)" />
          <template v-else>
            <p class="mt-1 text-xl font-heading text-(--fg)">
              {{ andamento.bmi.ultimo !== null ? formattaNumero(andamento.bmi.ultimo) : '—' }}
            </p>
            <p
              v-if="andamento.bmi.delta !== null"
              class="flex items-center gap-0.5 text-xs font-medium"
              :class="andamento.bmi.delta < 0 ? 'text-(--green)' : andamento.bmi.delta > 0 ? 'text-(--danger)' : 'text-(--fg3)'"
            >
              <ArrowDown v-if="andamento.bmi.delta < 0" :size="11" />
              <ArrowUp v-else-if="andamento.bmi.delta > 0" :size="11" />
              {{ formattaNumero(Math.abs(andamento.bmi.delta)) }} vs prec.
            </p>
          </template>
        </div>

        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-4 shadow-sm">
          <div class="flex items-center gap-1.5">
            <Percent :size="14" class="text-primary" />
            <p class="text-xs font-bold uppercase tracking-wide text-(--fg4)">Massa grassa</p>
          </div>
          <div v-if="visiteInCaricamento" class="mt-2 h-6 w-12 animate-pulse rounded bg-(--hover)" />
          <template v-else>
            <p class="mt-1 text-xl font-heading text-(--fg)">
              {{ andamento.percentualeGrassoCorporeo.ultimo !== null ? `${formattaNumero(andamento.percentualeGrassoCorporeo.ultimo)}%` : '—' }}
            </p>
            <p
              v-if="andamento.percentualeGrassoCorporeo.delta !== null"
              class="flex items-center gap-0.5 text-xs font-medium"
              :class="andamento.percentualeGrassoCorporeo.delta < 0 ? 'text-(--green)' : andamento.percentualeGrassoCorporeo.delta > 0 ? 'text-(--danger)' : 'text-(--fg3)'"
            >
              <ArrowDown v-if="andamento.percentualeGrassoCorporeo.delta < 0" :size="11" />
              <ArrowUp v-else-if="andamento.percentualeGrassoCorporeo.delta > 0" :size="11" />
              {{ formattaNumero(Math.abs(andamento.percentualeGrassoCorporeo.delta)) }} pt vs prec.
            </p>
          </template>
        </div>

        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-4 shadow-sm">
          <div class="flex items-center gap-1.5">
            <Dumbbell :size="14" class="text-primary" />
            <p class="text-xs font-bold uppercase tracking-wide text-(--fg4)">Massa magra</p>
          </div>
          <div v-if="visiteInCaricamento" class="mt-2 h-6 w-16 animate-pulse rounded bg-(--hover)" />
          <template v-else>
            <p class="mt-1 text-xl font-heading text-(--fg)">
              {{ andamento.massaMagra.ultimo !== null ? `${formattaNumero(andamento.massaMagra.ultimo)} kg` : '—' }}
            </p>
            <p
              v-if="andamento.massaMagra.delta !== null"
              class="flex items-center gap-0.5 text-xs font-medium"
              :class="andamento.massaMagra.delta < 0 ? 'text-(--danger)' : andamento.massaMagra.delta > 0 ? 'text-(--green)' : 'text-(--fg3)'"
            >
              <ArrowDown v-if="andamento.massaMagra.delta < 0" :size="11" />
              <ArrowUp v-else-if="andamento.massaMagra.delta > 0" :size="11" />
              {{ formattaNumero(Math.abs(andamento.massaMagra.delta)) }} kg vs prec.
            </p>
          </template>
        </div>

        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-4 opacity-50 shadow-sm">
          <p class="text-xs font-bold uppercase tracking-wide text-(--fg4)">Aderenza piano</p>
          <p class="mt-1 text-sm italic text-(--fg4)">Presto disponibile</p>
        </div>
      </div>

      <!-- 3. SEZIONE CLINICA A TAB -->
      <div>
        <!-- Selettore per schermi piccoli -->
        <Select :model-value="tabAttivo" @update:model-value="onCambiaTab">
          <SelectTrigger id="sezione-clinica-tab" class="w-full lg:hidden">
            <SelectValue />
          </SelectTrigger>
          <SelectContent>
            <SelectItem v-for="tab in TAB_DEFS" :key="tab.id" :value="tab.id">{{ tab.label }}</SelectItem>
          </SelectContent>
        </Select>

        <!-- Barra a tab per schermi grandi -->
        <div class="hidden gap-1 overflow-x-auto border-b border-(--bd) lg:flex">
          <button
            v-for="tab in TAB_DEFS"
            :key="tab.id"
            type="button"
            class="shrink-0 border-b-2 px-1 py-2.5 mr-5 text-sm font-bold transition-colors"
            :class="tabAttivo === tab.id ? 'border-(--green) text-(--green)' : 'border-transparent text-(--fg3) hover:text-(--fg2)'"
            @click="tabAttivo = tab.id"
          >
            {{ tab.label }}
          </button>
        </div>

        <div class="mt-5">
          <PazienteTabPanoramica
            v-if="tabAttivo === 'panoramica'"
            :paziente="paziente"
            :visite-in-caricamento="visiteInCaricamento"
            :errore-visite="erroreVisite"
            :visite="visite"
            :andamento="andamento"
          />

          <PazienteTabStoricoMisurazioni
            v-else-if="tabAttivo === 'storico'"
            :paziente-id="paziente.id"
            :visite-in-caricamento="visiteInCaricamento"
            :errore-visite="erroreVisite"
            :visite="visite"
          />

          <PazienteTabConfrontoVisite v-else-if="tabAttivo === 'confronto'" :visite="visite" />

          <div
            v-else-if="tabAttivo === 'piani'"
            class="rounded-2xl border border-(--bd) bg-(--surf) p-10 text-center shadow-sm"
          >
            <h4 class="font-heading text-lg italic text-(--fg)">Nessun piano collegato</h4>
            <p class="mx-auto mt-1.5 max-w-sm text-sm text-(--fg3)">
              La sezione piani alimentari per paziente sarà disponibile a breve.
            </p>
          </div>
        </div>
      </div>
    </div>
  </AppShell>

  <ModificaPazienteDialog
    v-if="paziente"
    v-model:open="mostraModificaAnagrafica"
    :paziente="paziente"
    @aggiornato="onPazienteAggiornato"
  />
</template>
