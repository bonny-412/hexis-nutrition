<script setup lang="ts">
import { computed } from 'vue'
import { CalendarClock, CalendarPlus, Check, FolderOpen, Pencil, Play, RotateCcw, Trash2, Video, XCircle } from '@lucide/vue'
import type { Appuntamento } from '@/api/agenda'
import type { AzioneAppuntamento } from '@/components/agenda/AppuntamentoMenu.vue'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { etichettaTipo, nomePaziente, oraFine, titolo } from '@/utils/agendaStile'

const props = defineProps<{ aperto: boolean; appuntamento: Appuntamento | null }>()
const emit = defineEmits<{
  'update:aperto': [valore: boolean]
  azione: [appuntamento: Appuntamento, azione: AzioneAppuntamento]
}>()

const a = computed(() => props.appuntamento)
const eVisita = computed(() => a.value?.tipo === 'VISITA_STUDIO' || a.value?.tipo === 'VIDEOCONSULTO')
const eProgrammata = computed(() => eVisita.value && a.value?.stato === 'PROGRAMMATA')
const ePromemoria = computed(() => a.value?.tipo === 'PROMEMORIA')
const eBlocco = computed(() => a.value?.tipo === 'BLOCCO')

// Componenti locali: mai new Date('YYYY-MM-DD') (UTC).
const dataEstesa = computed(() => {
  if (!a.value) return ''
  const [anno, mese, giorno] = a.value.data.split('-').map(Number)
  const testo = new Date(anno, mese - 1, giorno).toLocaleDateString('it-IT', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' })
  return testo.charAt(0).toUpperCase() + testo.slice(1)
})

const orario = computed(() => {
  if (!a.value?.oraInizio) return 'Senza orario'
  const fine = oraFine(a.value)
  return fine ? `${a.value.oraInizio}–${fine} · ${a.value.durataMinuti} min` : a.value.oraInizio
})

const titoloDialog = computed(() => {
  if (!a.value) return ''
  if (ePromemoria.value) return 'Promemoria'
  if (eBlocco.value) return 'Fascia bloccata'
  return titolo(a.value)
})

const paziente = computed(() => (a.value ? nomePaziente(a.value) : ''))
const etichettaTesto = computed(() => (ePromemoria.value ? 'Testo' : eBlocco.value ? 'Motivo' : 'Note'))

// Il link è testo libero: si mostra come collegamento solo se http(s).
const linkSicuro = computed(() => {
  const link = a.value?.linkVideo
  if (!link) return null
  try {
    const url = new URL(link)
    return url.protocol === 'http:' || url.protocol === 'https:' ? url.href : null
  } catch {
    return null
  }
})

function esegui(azione: AzioneAppuntamento) {
  if (!a.value) return
  emit('azione', a.value, azione)
  emit('update:aperto', false)
}
</script>

<template>
  <Dialog :open="aperto" @update:open="emit('update:aperto', $event)">
    <DialogContent class="sm:max-w-lg" data-test="appuntamento-dettaglio-dialog">
      <template v-if="a">
        <DialogHeader>
          <!-- Promemoria e fasce bloccate hanno un titolo fisso: il loro testo compare solo nel corpo, non due volte. -->
          <p v-if="eVisita" class="text-xs font-semibold text-(--fg3)" data-test="dettaglio-tipo">{{ etichettaTipo(a) }}</p>
          <DialogTitle class="font-heading text-xl italic" data-test="dettaglio-titolo">{{ titoloDialog }}</DialogTitle>
          <DialogDescription class="sr-only">Dettaglio dell'impegno selezionato in agenda.</DialogDescription>
        </DialogHeader>

        <dl class="space-y-3 text-sm">
          <div>
            <dt class="text-[13px] font-medium text-(--fg2)">Data e ora</dt>
            <dd class="mt-0.5 text-(--fg)" data-test="dettaglio-data">{{ dataEstesa }} · {{ orario }}</dd>
          </div>
          <div v-if="paziente && !eVisita">
            <dt class="text-[13px] font-medium text-(--fg2)">Paziente</dt>
            <dd class="mt-0.5 text-(--fg)" data-test="dettaglio-paziente">{{ paziente }}</dd>
          </div>
          <div v-if="a.testo">
            <dt class="text-[13px] font-medium text-(--fg2)">{{ etichettaTesto }}</dt>
            <dd class="mt-0.5 whitespace-pre-wrap break-words text-(--fg)" :class="{ 'line-through': ePromemoria && a.fatto }" data-test="dettaglio-testo">{{ a.testo }}</dd>
          </div>
          <div v-if="a.tipo === 'VIDEOCONSULTO' && a.linkVideo">
            <dt class="text-[13px] font-medium text-(--fg2)">Link del videoconsulto</dt>
            <dd class="mt-0.5 break-all" data-test="dettaglio-link">
              <a v-if="linkSicuro" :href="linkSicuro" target="_blank" rel="noopener noreferrer" class="inline-flex items-center gap-1 text-(--green) underline">
                <Video :size="14" /> {{ a.linkVideo }}
              </a>
              <span v-else class="text-(--fg)">{{ a.linkVideo }}</span>
            </dd>
          </div>
          <div v-if="ePromemoria">
            <dt class="text-[13px] font-medium text-(--fg2)">Stato</dt>
            <dd class="mt-0.5 text-(--fg)" data-test="dettaglio-stato">{{ a.fatto ? 'Fatto' : 'Da fare' }}</dd>
          </div>
        </dl>

        <DialogFooter>
          <Button v-if="a.pazienteId" type="button" variant="neutral" data-test="dettaglio-apri-cartella" @click="esegui('APRI_CARTELLA')"><FolderOpen /> Apri scheda paziente</Button>

          <template v-if="eProgrammata">
            <Button type="button" variant="destructive-neutral" data-test="dettaglio-annulla" @click="esegui('ANNULLA')"><XCircle /> Annulla visita</Button>
            <Button v-if="a.linkVideo" type="button" variant="neutral" data-test="dettaglio-apri-video" @click="esegui('APRI_VIDEO')"><Video /> Apri videoconsulto</Button>
            <Button type="button" variant="neutral" data-test="dettaglio-modifica" @click="esegui('MODIFICA')"><CalendarClock /> Sposta / Modifica</Button>
            <Button type="button" data-test="dettaglio-avvia-visita" @click="esegui('AVVIA_VISITA')"><Play /> Avvia visita</Button>
          </template>

          <template v-else-if="ePromemoria">
            <Button type="button" variant="destructive-neutral" data-test="dettaglio-elimina" @click="esegui('ELIMINA')"><Trash2 /> Elimina</Button>
            <Button type="button" variant="neutral" data-test="dettaglio-sposta-domani" @click="esegui('SPOSTA_DOMANI')"><CalendarPlus /> Sposta a domani</Button>
            <Button type="button" variant="neutral" data-test="dettaglio-modifica" @click="esegui('MODIFICA')"><Pencil /> Modifica</Button>
            <Button v-if="!a.fatto" type="button" data-test="dettaglio-fatto" @click="esegui('FATTO')"><Check /> Segna come fatto</Button>
            <Button v-else type="button" data-test="dettaglio-da-fare" @click="esegui('DA_FARE')"><RotateCcw /> Da fare</Button>
          </template>

          <template v-else-if="eBlocco">
            <Button type="button" variant="destructive-neutral" data-test="dettaglio-elimina" @click="esegui('ELIMINA')"><Trash2 /> Elimina</Button>
            <Button type="button" data-test="dettaglio-modifica" @click="esegui('MODIFICA')"><Pencil /> Modifica</Button>
          </template>
        </DialogFooter>
      </template>
    </DialogContent>
  </Dialog>
</template>
