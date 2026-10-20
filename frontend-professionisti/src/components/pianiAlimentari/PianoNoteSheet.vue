<script setup lang="ts">
import { ref, watch } from 'vue'
import { toast } from 'vue-sonner'
import { aggiungiNota, eliminaNota, type NotaPiano } from '@/api/pianiAlimentari'
import { Button } from '@/components/ui/button'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import { DatePicker } from '@/components/ui/date-picker'
import { Sheet, SheetContent, SheetHeader, SheetTitle, SheetDescription } from '@/components/ui/sheet'
import {
  AlertDialog,
  AlertDialogContent,
  AlertDialogHeader,
  AlertDialogTitle,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogCancel,
  AlertDialogAction,
} from '@/components/ui/alert-dialog'
import { Trash2 } from '@lucide/vue'
import { formattaDataItalianaConMese } from '@/utils/data'

const props = defineProps<{
  open: boolean
  pianoId: string
  nomePiano: string
  note: NotaPiano[]
  /** Vero solo con piano attivo: abilita il modulo e l'eliminazione. Con piano concluso le note sono solo consultabili. */
  modificabile: boolean
}>()

const emit = defineEmits<{
  'update:open': [valore: boolean]
  aggiunta: [nota: NotaPiano]
  eliminata: [notaId: string]
}>()

function oggiIso(): string {
  const adesso = new Date()
  const mese = String(adesso.getMonth() + 1).padStart(2, '0')
  const giorno = String(adesso.getDate()).padStart(2, '0')
  return `${adesso.getFullYear()}-${mese}-${giorno}`
}

const data = ref(oggiIso())
const testo = ref('')
const salvataggioInCorso = ref(false)
const notaDaEliminare = ref<NotaPiano | null>(null)

// A ogni apertura il modulo riparte pulito, con la data di oggi.
watch(() => props.open, (aperto) => {
  if (aperto) {
    data.value = oggiIso()
    testo.value = ''
  }
})

// All'apertura il focus va sul testo della nota, non sul primo campo (la data). Senza modulo resta il comportamento di default.
function focalizzaTesto(evento: Event) {
  const campo = document.getElementById('nota-testo')
  if (!campo) return
  evento.preventDefault()
  campo.focus()
}

async function aggiungi() {
  if (!testo.value.trim() || !data.value || salvataggioInCorso.value) return
  salvataggioInCorso.value = true
  try {
    const nota = await aggiungiNota(props.pianoId, { data: data.value, testo: testo.value.trim() })
    emit('aggiunta', nota)
    testo.value = ''
    toast.success('Nota aggiunta.')
  } catch {
    toast.error('Non è stato possibile aggiungere la nota.')
  } finally {
    salvataggioInCorso.value = false
  }
}

async function confermaElimina() {
  const nota = notaDaEliminare.value
  if (!nota) return
  try {
    await eliminaNota(props.pianoId, nota.id)
    emit('eliminata', nota.id)
    toast.success('Nota eliminata.')
  } catch {
    toast.error('Non è stato possibile eliminare la nota.')
  } finally {
    notaDaEliminare.value = null
  }
}
</script>

<template>
  <Sheet :open="open" @update:open="(v) => emit('update:open', v)">
    <SheetContent
      side="right" class="w-full sm:max-w-100 overflow-y-auto" data-test="pannello-note"
      @open-auto-focus="focalizzaTesto"
    >
      <SheetHeader class="border-b border-(--div) pr-12">
        <SheetTitle class="font-heading text-xl italic">Note di aggiornamento</SheetTitle>
        <SheetDescription>{{ nomePiano }}</SheetDescription>
      </SheetHeader>

      <div class="flex flex-col gap-5 px-4 pb-6">
        <form v-if="modificabile" class="flex flex-col gap-3" data-test="modulo-nota" @submit.prevent="aggiungi">
          <div class="flex flex-col gap-1.5">
            <Label class="text-[10px] font-bold uppercase tracking-wide text-(--fg4)" for="nota-data">Data</Label>
            <DatePicker id="nota-data" digitabile v-model="data" />
          </div>
          <div class="flex flex-col gap-1.5">
            <Label class="text-[10px] font-bold uppercase tracking-wide text-(--fg4)" for="nota-testo">Nota</Label>
            <Textarea
              id="nota-testo" v-model="testo" data-test="nota-testo" rows="4" maxlength="5000"
              placeholder="Cosa è cambiato rispetto al piano…" class="resize-y text-sm"
            />
          </div>
          <Button type="submit" data-test="aggiungi-nota" :disabled="!testo.trim() || !data || salvataggioInCorso">
            Aggiungi nota
          </Button>
        </form>

        <p v-if="note.length === 0" data-test="nessuna-nota" class="py-6 text-center text-sm text-(--fg3)">
          Nessuna nota per questo piano.
        </p>

        <ul v-else class="flex flex-col gap-3">
          <li
            v-for="nota in note" :key="nota.id" data-test="nota"
            class="rounded-xl border border-(--bd) bg-(--surf) p-3.5"
          >
            <div class="mb-1.5 flex items-start justify-between gap-2">
              <p class="text-xs font-bold text-(--fg)">{{ formattaDataItalianaConMese(nota.data) }}</p>
              <Button
                v-if="modificabile" type="button" variant="destructive-ghost" size="icon-xs"
                data-test="elimina-nota" aria-label="Elimina nota" title="Elimina nota"
                @click="notaDaEliminare = nota"
              >
                <Trash2 :size="13" />
              </Button>
            </div>
            <p class="whitespace-pre-wrap text-sm text-(--fg2)">{{ nota.testo }}</p>
          </li>
        </ul>
      </div>
    </SheetContent>
  </Sheet>

  <AlertDialog :open="notaDaEliminare !== null" @update:open="(v) => { if (!v) notaDaEliminare = null }">
    <AlertDialogContent>
      <AlertDialogHeader>
        <AlertDialogTitle>Eliminare questa nota?</AlertDialogTitle>
        <AlertDialogDescription>L'operazione non è reversibile.</AlertDialogDescription>
      </AlertDialogHeader>
      <AlertDialogFooter>
        <AlertDialogCancel variant="neutral">Annulla</AlertDialogCancel>
        <AlertDialogAction data-test="conferma-elimina-nota" class="bg-(--danger) hover:bg-(--danger)/80" @click="confermaElimina">
          Elimina
        </AlertDialogAction>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
</template>
