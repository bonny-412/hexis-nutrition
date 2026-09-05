<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { toast } from 'vue-sonner'
import { crea, aggiorna, elimina, duplica, type Alimento, type CreaAlimentoRequest } from '@/api/alimenti'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
} from '@/components/ui/dialog'
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
import { Copy, Save, Trash2 } from '@lucide/vue'

const props = defineProps<{
  open: boolean
  alimento: Alimento | null
}>()

const emit = defineEmits<{
  'update:open': [valore: boolean]
  salvato: []
  duplicato: []
}>()

type ChiaveNutriente =
  | 'kcal' | 'proteineG' | 'grassiG' | 'carboidratiG'
  | 'acquaG' | 'fibreG' | 'zuccheriG' | 'ferroMg' | 'calcioMg' | 'sodioMg'

interface CampoNutriente {
  chiave: ChiaveNutriente
  etichetta: string
  unita: string
  obbligatorio: boolean
}

const CAMPI_NUTRIENTI: CampoNutriente[] = [
  { chiave: 'kcal', etichetta: 'Kcal', unita: 'kcal', obbligatorio: true },
  { chiave: 'proteineG', etichetta: 'Proteine', unita: 'g', obbligatorio: true },
  { chiave: 'grassiG', etichetta: 'Grassi', unita: 'g', obbligatorio: true },
  { chiave: 'carboidratiG', etichetta: 'Carboidrati', unita: 'g', obbligatorio: true },
  { chiave: 'acquaG', etichetta: 'Acqua', unita: 'g', obbligatorio: false },
  { chiave: 'fibreG', etichetta: 'Fibre', unita: 'g', obbligatorio: false },
  { chiave: 'zuccheriG', etichetta: 'Zuccheri', unita: 'g', obbligatorio: false },
  { chiave: 'ferroMg', etichetta: 'Ferro', unita: 'mg', obbligatorio: false },
  { chiave: 'calcioMg', etichetta: 'Calcio', unita: 'mg', obbligatorio: false },
  { chiave: 'sodioMg', etichetta: 'Sodio', unita: 'mg', obbligatorio: false },
]

const alimentoCorrente = ref<Alimento | null>(null)
const nome = ref('')
const categoria = ref('')
const valori = ref<Record<ChiaveNutriente, string>>({
  kcal: '', proteineG: '', grassiG: '', carboidratiG: '',
  acquaG: '', fibreG: '', zuccheriG: '', ferroMg: '', calcioMg: '', sodioMg: '',
})
const inCorso = ref(false)
const confermaEliminaAperta = ref(false)

const modo = computed<'create' | 'view' | 'edit'>(() => {
  if (!alimentoCorrente.value) return 'create'
  return alimentoCorrente.value.bda ? 'view' : 'edit'
})

const soloLettura = computed(() => modo.value === 'view')

function valoreOVuoto(valore: number | null | undefined): string {
  return valore === null || valore === undefined ? '' : String(valore)
}

function inizializza() {
  alimentoCorrente.value = props.alimento
  const a = props.alimento
  nome.value = a?.nome ?? ''
  categoria.value = a?.categoria ?? ''
  valori.value = {
    kcal: valoreOVuoto(a?.kcal),
    proteineG: valoreOVuoto(a?.proteineG),
    grassiG: valoreOVuoto(a?.grassiG),
    carboidratiG: valoreOVuoto(a?.carboidratiG),
    acquaG: valoreOVuoto(a?.acquaG),
    fibreG: valoreOVuoto(a?.fibreG),
    zuccheriG: valoreOVuoto(a?.zuccheriG),
    ferroMg: valoreOVuoto(a?.ferroMg),
    calcioMg: valoreOVuoto(a?.calcioMg),
    sodioMg: valoreOVuoto(a?.sodioMg),
  }
}

watch(() => props.open, (valore) => { if (valore) inizializza() })

function numeroOUndefined(valore: string): number | undefined {
  return valore.trim() === '' ? undefined : Number(valore)
}

function costruisciRichiesta(): CreaAlimentoRequest {
  return {
    nome: nome.value,
    categoria: categoria.value,
    kcal: Number(valori.value.kcal) || 0,
    proteineG: Number(valori.value.proteineG) || 0,
    grassiG: Number(valori.value.grassiG) || 0,
    carboidratiG: Number(valori.value.carboidratiG) || 0,
    acquaG: numeroOUndefined(valori.value.acquaG),
    fibreG: numeroOUndefined(valori.value.fibreG),
    zuccheriG: numeroOUndefined(valori.value.zuccheriG),
    ferroMg: numeroOUndefined(valori.value.ferroMg),
    calcioMg: numeroOUndefined(valori.value.calcioMg),
    sodioMg: numeroOUndefined(valori.value.sodioMg),
  }
}

async function onSubmit() {
  inCorso.value = true
  try {
    if (modo.value === 'create') {
      await crea(costruisciRichiesta())
      toast.success('Alimento creato.')
    } else {
      await aggiorna(alimentoCorrente.value!.id, costruisciRichiesta())
      toast.success('Alimento aggiornato.')
    }
    emit('salvato')
    emit('update:open', false)
  } catch {
    toast.error("Non è stato possibile salvare l'alimento.")
  } finally {
    inCorso.value = false
  }
}

async function onDuplica() {
  if (!alimentoCorrente.value) return
  inCorso.value = true
  try {
    const copia = await duplica(alimentoCorrente.value.id)
    alimentoCorrente.value = copia
    nome.value = copia.nome
    categoria.value = copia.categoria
    valori.value = {
      kcal: valoreOVuoto(copia.kcal),
      proteineG: valoreOVuoto(copia.proteineG),
      grassiG: valoreOVuoto(copia.grassiG),
      carboidratiG: valoreOVuoto(copia.carboidratiG),
      acquaG: valoreOVuoto(copia.acquaG),
      fibreG: valoreOVuoto(copia.fibreG),
      zuccheriG: valoreOVuoto(copia.zuccheriG),
      ferroMg: valoreOVuoto(copia.ferroMg),
      calcioMg: valoreOVuoto(copia.calcioMg),
      sodioMg: valoreOVuoto(copia.sodioMg),
    }
    toast.success('Alimento duplicato come personalizzato.')
    emit('duplicato')
  } catch {
    toast.error("Non è stato possibile duplicare l'alimento.")
  } finally {
    inCorso.value = false
  }
}

async function onElimina() {
  if (!alimentoCorrente.value) return
  try {
    await elimina(alimentoCorrente.value.id)
    toast.success('Alimento eliminato.')
    emit('salvato')
    emit('update:open', false)
  } catch {
    toast.error("Non è stato possibile eliminare l'alimento.")
  } finally {
    confermaEliminaAperta.value = false
  }
}

const titolo = computed(() => (modo.value === 'create' ? 'Nuovo alimento' : nome.value))
</script>

<template>
  <Dialog :open="open" @update:open="(valore) => emit('update:open', valore)">
    <DialogContent class="sm:max-w-lg">
      <DialogHeader>
        <DialogTitle>{{ titolo }}</DialogTitle>
        <DialogDescription v-if="modo === 'view'">Alimento BDA · sola lettura</DialogDescription>
      </DialogHeader>

      <form class="space-y-4" @submit.prevent="onSubmit">
        <div class="flex flex-col gap-1.5">
          <Label for="alimento-nome" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Nome alimento</Label>
          <Input id="alimento-nome" v-model="nome" type="text" :disabled="soloLettura" placeholder="Es. Petto di pollo, crudo" />
        </div>

        <div class="flex flex-col gap-1.5">
          <Label for="alimento-categoria" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Categoria</Label>
          <Input id="alimento-categoria" v-model="categoria" type="text" :disabled="soloLettura" placeholder="Es. Carni" />
        </div>

        <div>
          <div class="mb-2 text-xs font-bold uppercase tracking-wide text-(--fg3)">Valori nutrizionali · per 100 g</div>
          <div class="grid grid-cols-2 gap-2.5">
            <div v-for="campo in CAMPI_NUTRIENTI" :key="campo.chiave" class="flex flex-col gap-1.5">
              <Label class="text-xs text-(--fg2)">{{ campo.etichetta }}{{ campo.obbligatorio ? '*' : '' }}</Label>
              <div class="flex items-center gap-1.5 rounded-md border border-(--bd2) px-2">
                <Input
                  v-model="valori[campo.chiave]"
                  type="number"
                  step="0.1"
                  :disabled="soloLettura"
                  class="border-0 px-0 shadow-none focus-visible:ring-0"
                />
                <span class="text-xs text-(--fg4)">{{ campo.unita }}</span>
              </div>
            </div>
          </div>
        </div>

        <DialogFooter class="flex items-center gap-2.5">
          <Button v-if="modo === 'view'" type="button" variant="outline" :disabled="inCorso" @click="onDuplica">
            <Copy :size="16" /> Duplica come personalizzato
          </Button>
          <Button
            v-if="modo === 'edit'"
            type="button"
            variant="ghost"
            class="mr-auto text-(--danger) hover:text-(--danger)"
            @click="confermaEliminaAperta = true"
          >
            <Trash2 :size="16" /> Elimina
          </Button>
          <Button v-if="modo !== 'view'" type="submit" :disabled="inCorso" class="hover:bg-primary/80">
            <Save :size="16" />
            {{ inCorso ? 'Salvataggio…' : 'Salva' }}
          </Button>
        </DialogFooter>
      </form>
    </DialogContent>
  </Dialog>

  <AlertDialog v-model:open="confermaEliminaAperta">
    <AlertDialogContent>
      <AlertDialogHeader>
        <AlertDialogTitle>Eliminare {{ nome }}?</AlertDialogTitle>
        <AlertDialogDescription>L'alimento verrà rimosso definitivamente dal tuo catalogo personalizzato.</AlertDialogDescription>
      </AlertDialogHeader>
      <AlertDialogFooter>
        <AlertDialogCancel variant="neutral">Annulla</AlertDialogCancel>
        <AlertDialogAction class="bg-(--danger) hover:bg-(--danger)/80" @click="onElimina">Conferma</AlertDialogAction>
      </AlertDialogFooter>
    </AlertDialogContent>
  </AlertDialog>
</template>
