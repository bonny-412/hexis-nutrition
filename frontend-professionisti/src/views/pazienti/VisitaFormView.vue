<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { toast } from 'vue-sonner'
import AppShell from '@/components/AppShell.vue'
import DatiVisitaForm from '@/components/pazienti/DatiVisitaForm.vue'
import SelezionaPazienteCombobox from '@/components/pazienti/SelezionaPazienteCombobox.vue'
import {
  dettaglio,
  dettaglioVisita,
  visite as caricaVisite,
  creaVisita,
  aggiornaVisita,
  type Paziente,
  type Visita,
} from '@/api/pazienti'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { ArrowLeft, Save } from '@lucide/vue'
import { formattaDataItalianaConMese } from '@/utils/data'
import { formattaNumero } from '@/utils/visita'

const route = useRoute()
const router = useRouter()

const pazienteIdRoute = route.params.id as string | undefined
const visitaId = route.params.visitaId as string | undefined
const modalitaModifica = computed(() => !!visitaId)

const paziente = ref<Paziente | null>(null)
const visitaEsistente = ref<Visita | null>(null)
const ultimaVisita = ref<Visita | null>(null)
const caricamento = ref(true)
const erroreCaricamento = ref<string | null>(null)
const inCorso = ref(false)

const datiVisitaForm = ref<InstanceType<typeof DatiVisitaForm>>()

async function caricaUltimaVisita(pazienteId: string) {
  try {
    const lista = await caricaVisite(pazienteId)
    ultimaVisita.value = lista.length > 0 ? lista[lista.length - 1] : null
  } catch {
    ultimaVisita.value = null
  }
}

async function caricaConPazienteNoto(pazienteId: string) {
  caricamento.value = true
  try {
    paziente.value = await dettaglio(pazienteId)
    if (visitaId) {
      visitaEsistente.value = await dettaglioVisita(pazienteId, visitaId)
    }
    await caricaUltimaVisita(pazienteId)
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      erroreCaricamento.value = modalitaModifica.value ? 'Visita non trovata.' : 'Paziente non trovato.'
    } else {
      erroreCaricamento.value = 'Non è stato possibile caricare i dati.'
    }
  } finally {
    caricamento.value = false
  }
}

async function onPazienteCambiato(selezionato: Paziente | null) {
  ultimaVisita.value = null
  if (!selezionato) {
    paziente.value = null
    return
  }
  // Attende il caricamento dell'ultima visita prima di mostrare il form, così
  // altezza/obiettivo suggeriti sono già disponibili al primo render di DatiVisitaForm.
  await caricaUltimaVisita(selezionato.id)
  paziente.value = selezionato
}

onMounted(() => {
  if (pazienteIdRoute) {
    caricaConPazienteNoto(pazienteIdRoute)
  } else {
    caricamento.value = false
  }
})

const sottotitolo = computed(() => {
  if (!paziente.value) return pazienteIdRoute ? '' : 'Seleziona il paziente per cui registrare la nuova visita.'
  const base = `${paziente.value.nome} ${paziente.value.cognome}`
  if (!ultimaVisita.value) return base
  return `${base} · ultima visita ${formattaDataItalianaConMese(ultimaVisita.value.dataVisita)} con peso ${formattaNumero(ultimaVisita.value.pesoKg)} kg`
})

/**
 * Da `/pazienti/visite/nuova` (link "Nuova visita" della dashboard) si torna alla dashboard.
 * Da `/pazienti/:id/visite/...` (creazione o modifica avviata dal dettaglio paziente) si torna lì.
 */
const linkIndietro = computed(() => (pazienteIdRoute ? `/pazienti/${pazienteIdRoute}` : '/'))
const testoLinkIndietro = computed(() => (pazienteIdRoute ? 'Torna al paziente' : 'Torna alla dashboard'))

function tornaIndietro() {
  if (paziente.value) {
    router.push(`/pazienti/${paziente.value.id}`)
  } else if (router.options.history.state.back) {
    router.back()
  } else {
    router.push('/')
  }
}

async function onSubmit() {
  if (!paziente.value) return
  const valido = datiVisitaForm.value?.valida() ?? false
  if (!valido) return

  inCorso.value = true
  try {
    const dati = datiVisitaForm.value!.ottieniDati()
    if (modalitaModifica.value && visitaId) {
      await aggiornaVisita(paziente.value.id, visitaId, dati)
      toast.success('Visita aggiornata con successo.')
    } else {
      await creaVisita(paziente.value.id, dati)
      toast.success('Visita registrata con successo.')
    }
    router.push(`/pazienti/${paziente.value.id}`)
  } catch {
    toast.error('Non è stato possibile salvare la visita. Controlla i dati e riprova.')
  } finally {
    inCorso.value = false
  }
}
</script>

<template>
  <AppShell>
    <div class="mb-6">
      <router-link
        :to="linkIndietro"
        data-test="link-indietro"
        class="inline-flex items-center gap-2 text-xs font-semibold text-(--fg3) transition-colors hover:text-(--green)"
      >
        <ArrowLeft :size="16" />
        <span>{{ testoLinkIndietro }}</span>
      </router-link>
      <h1 class="font-heading text-3xl italic text-(--fg)">{{ modalitaModifica ? 'Modifica visita' : 'Nuova visita' }}</h1>
      <p v-if="sottotitolo" class="mt-1 text-sm text-(--fg3)">{{ sottotitolo }}</p>
    </div>

    <div
      v-if="erroreCaricamento"
      class="flex flex-col items-center justify-center rounded-2xl border border-(--bd) bg-(--surf) p-12 text-center shadow-sm"
    >
      <p class="text-sm text-(--fg3)">{{ erroreCaricamento }}</p>
    </div>

    <div v-else-if="caricamento" class="space-y-6">
      <div class="h-64 animate-pulse rounded-2xl bg-(--hover)" />
    </div>

    <template v-else>
      <SelezionaPazienteCombobox
        v-if="!pazienteIdRoute"
        class="mb-6"
        :model-value="paziente"
        @update:model-value="onPazienteCambiato"
      />

      <form v-if="paziente" class="space-y-6" @submit.prevent="onSubmit">
        <div class="rounded-2xl border border-(--bd) bg-(--surf) p-6 shadow-sm sm:p-8">
          <DatiVisitaForm
            ref="datiVisitaForm"
            :sesso="paziente.sesso"
            :dati-iniziali="visitaEsistente"
            :altezza-suggerita-cm="ultimaVisita?.altezzaCm"
            :obiettivo-suggerito="ultimaVisita?.obiettivo"
          />
        </div>

        <div class="w-full flex justify-end items-center gap-2">
          <Button type="button" variant="neutral" :disabled="inCorso" @click="tornaIndietro">
            Annulla
          </Button>
          <Button type="submit" :disabled="inCorso" class="hover:bg-primary/80">
            <Save :size="16" />
            {{ inCorso ? 'Salvataggio…' : 'Salva visita' }}
          </Button>
        </div>
      </form>
    </template>
  </AppShell>
</template>
