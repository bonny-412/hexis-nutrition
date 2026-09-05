<script setup lang="ts">
import { ref, computed, watch, nextTick, type Ref } from 'vue'
import { toast } from 'vue-sonner'
import { crea, aggiorna, elimina, duplica, type Alimento, type CreaAlimentoRequest } from '@/api/alimenti'
import {
  filtraDecimaleItaliano,
  numeroItaliano,
  numeroItalianoOpzionale,
  erroreNomeAlimento,
  erroreCategoriaAlimento,
  erroreNumeroDecimale,
  erroreNumeroDecimaleObbligatorio,
} from '@/utils/validators'
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
  id: string
  etichetta: string
  unita: string
  obbligatorio: boolean
}

const CAMPI_NUTRIENTI: CampoNutriente[] = [
  { chiave: 'kcal', id: 'alimento-kcal', etichetta: 'Kcal', unita: 'kcal', obbligatorio: true },
  { chiave: 'proteineG', id: 'alimento-proteine', etichetta: 'Proteine', unita: 'g', obbligatorio: true },
  { chiave: 'grassiG', id: 'alimento-grassi', etichetta: 'Grassi', unita: 'g', obbligatorio: true },
  { chiave: 'carboidratiG', id: 'alimento-carboidrati', etichetta: 'Carboidrati', unita: 'g', obbligatorio: true },
  { chiave: 'acquaG', id: 'alimento-acqua', etichetta: 'Acqua', unita: 'g', obbligatorio: false },
  { chiave: 'fibreG', id: 'alimento-fibre', etichetta: 'Fibre', unita: 'g', obbligatorio: false },
  { chiave: 'zuccheriG', id: 'alimento-zuccheri', etichetta: 'Zuccheri', unita: 'g', obbligatorio: false },
  { chiave: 'ferroMg', id: 'alimento-ferro', etichetta: 'Ferro', unita: 'mg', obbligatorio: false },
  { chiave: 'calcioMg', id: 'alimento-calcio', etichetta: 'Calcio', unita: 'mg', obbligatorio: false },
  { chiave: 'sodioMg', id: 'alimento-sodio', etichetta: 'Sodio', unita: 'mg', obbligatorio: false },
]

const alimentoCorrente = ref<Alimento | null>(null)
const nome = ref('')
const categoria = ref('')
const quantitaG = ref('100')
const valori = ref<Record<ChiaveNutriente, string>>({
  kcal: '', proteineG: '', grassiG: '', carboidratiG: '',
  acquaG: '', fibreG: '', zuccheriG: '', ferroMg: '', calcioMg: '', sodioMg: '',
})
const inCorso = ref(false)
const confermaEliminaAperta = ref(false)
const errori = ref<Record<string, string>>({})

const modo = computed<'create' | 'view' | 'edit'>(() => {
  if (!alimentoCorrente.value) return 'create'
  return alimentoCorrente.value.bda ? 'view' : 'edit'
})

const soloLettura = computed(() => modo.value === 'view')

function valoreOVuoto(valore: number | null | undefined): string {
  return valore === null || valore === undefined ? '' : String(valore).replace('.', ',')
}

function inizializza() {
  alimentoCorrente.value = props.alimento
  const a = props.alimento
  nome.value = a?.nome ?? ''
  categoria.value = a?.categoria ?? ''
  quantitaG.value = a ? valoreOVuoto(a.quantitaG) : '100'
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
  errori.value = {}
}

watch(() => props.open, (valore) => { if (valore) inizializza() })

// --- GESTIONE FILTRI SU INPUT (LOGICA VUE UI) ---
const MARCATORE_INVISIBILE = '​'

function pulisciErroreSeCorretto(chiave: string, valida: (valore: string) => string | undefined, valore: string) {
  if (errori.value[chiave] && !valida(valore)) {
    const nuovi = { ...errori.value }
    delete nuovi[chiave]
    errori.value = nuovi
  }
}

function conFiltroDecimale(rif: Ref<string>, chiave: string, valida: (valore: string) => string | undefined) {
  return async (valore: string | number) => {
    const filtrato = filtraDecimaleItaliano(String(valore))
    if (filtrato === rif.value) {
      rif.value = `${filtrato}${MARCATORE_INVISIBILE}`
      await nextTick()
    }
    rif.value = filtrato
    pulisciErroreSeCorretto(chiave, valida, filtrato)
  }
}

function onNomeInput(valore: string | number) {
  nome.value = String(valore)
  pulisciErroreSeCorretto('nome', erroreNomeAlimento, nome.value)
}

function onCategoriaInput(valore: string | number) {
  categoria.value = String(valore)
  pulisciErroreSeCorretto('categoria', erroreCategoriaAlimento, categoria.value)
}

const onQuantitaInput = conFiltroDecimale(quantitaG, 'quantitaG', erroreNumeroDecimaleObbligatorio)

function onCampoNutrienteInput(campo: CampoNutriente) {
  const valida = campo.obbligatorio ? erroreNumeroDecimaleObbligatorio : erroreNumeroDecimale
  return async (valore: string | number) => {
    const filtrato = filtraDecimaleItaliano(String(valore))
    if (filtrato === valori.value[campo.chiave]) {
      valori.value[campo.chiave] = `${filtrato}${MARCATORE_INVISIBILE}`
      await nextTick()
    }
    valori.value[campo.chiave] = filtrato
    pulisciErroreSeCorretto(campo.chiave, valida, filtrato)
  }
}

// --- VALIDAZIONE ---
function valida(): boolean {
  const nuoviErrori: Record<string, string> = {}
  const assegna = (chiave: string, messaggio: string | undefined) => { if (messaggio) nuoviErrori[chiave] = messaggio }

  assegna('nome', erroreNomeAlimento(nome.value))
  assegna('categoria', erroreCategoriaAlimento(categoria.value))
  assegna('quantitaG', erroreNumeroDecimaleObbligatorio(quantitaG.value))
  for (const campo of CAMPI_NUTRIENTI) {
    const validatore = campo.obbligatorio ? erroreNumeroDecimaleObbligatorio : erroreNumeroDecimale
    assegna(campo.chiave, validatore(valori.value[campo.chiave]))
  }

  errori.value = nuoviErrori
  return Object.keys(nuoviErrori).length === 0
}

function costruisciRichiesta(): CreaAlimentoRequest {
  return {
    nome: nome.value.trim(),
    categoria: categoria.value.trim(),
    quantitaG: numeroItaliano(quantitaG.value),
    kcal: numeroItaliano(valori.value.kcal),
    proteineG: numeroItaliano(valori.value.proteineG),
    grassiG: numeroItaliano(valori.value.grassiG),
    carboidratiG: numeroItaliano(valori.value.carboidratiG),
    acquaG: numeroItalianoOpzionale(valori.value.acquaG),
    fibreG: numeroItalianoOpzionale(valori.value.fibreG),
    zuccheriG: numeroItalianoOpzionale(valori.value.zuccheriG),
    ferroMg: numeroItalianoOpzionale(valori.value.ferroMg),
    calcioMg: numeroItalianoOpzionale(valori.value.calcioMg),
    sodioMg: numeroItalianoOpzionale(valori.value.sodioMg),
  }
}

async function onSubmit() {
  if (!valida()) return
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
    quantitaG.value = valoreOVuoto(copia.quantitaG)
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
    errori.value = {}
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

      <form class="flex min-h-0 flex-1 flex-col gap-4" @submit.prevent="onSubmit">
        <div class="min-h-0 flex-1 space-y-4 overflow-y-auto px-0.5 py-0.5">
          <div class="flex flex-col gap-1.5">
            <Label for="alimento-nome" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Nome alimento*</Label>
            <Input
              id="alimento-nome"
              :model-value="nome"
              @update:model-value="onNomeInput"
              type="text"
              :disabled="soloLettura"
              :aria-invalid="!!errori.nome"
              placeholder="Es. Petto di pollo, crudo"
            />
            <p v-if="errori.nome" class="text-xs font-medium text-(--danger)">{{ errori.nome }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="alimento-categoria" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Categoria*</Label>
            <Input
              id="alimento-categoria"
              :model-value="categoria"
              @update:model-value="onCategoriaInput"
              type="text"
              :disabled="soloLettura"
              :aria-invalid="!!errori.categoria"
              placeholder="Es. Carni"
            />
            <p v-if="errori.categoria" class="text-xs font-medium text-(--danger)">{{ errori.categoria }}</p>
          </div>

          <div class="flex flex-col gap-1.5">
            <Label for="alimento-quantita" class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Quantità di riferimento (g)*</Label>
            <Input
              id="alimento-quantita"
              :model-value="quantitaG"
              @update:model-value="onQuantitaInput"
              type="text"
              inputmode="decimal"
              :disabled="soloLettura"
              :aria-invalid="!!errori.quantitaG"
              placeholder="Es. 100"
            />
            <p v-if="errori.quantitaG" class="text-xs font-medium text-(--danger)">{{ errori.quantitaG }}</p>
          </div>

          <div>
            <div class="mb-2 text-xs font-bold uppercase tracking-wide text-(--fg3)">Valori nutrizionali · per {{ quantitaG || 100 }} g</div>
            <div class="grid gap-2.5 sm:grid-cols-2">
              <div v-for="campo in CAMPI_NUTRIENTI" :key="campo.chiave" class="flex flex-col gap-1.5">
                <Label :for="campo.id" class="text-xs text-(--fg2)">{{ campo.etichetta }}{{ campo.obbligatorio ? '*' : '' }}</Label>
                <div class="flex items-center gap-1.5 rounded-md border border-(--bd2) px-2" :class="{ 'border-(--danger)': errori[campo.chiave] }">
                  <Input
                    :id="campo.id"
                    :model-value="valori[campo.chiave]"
                    @update:model-value="onCampoNutrienteInput(campo)($event)"
                    type="text"
                    inputmode="decimal"
                    :disabled="soloLettura"
                    :aria-invalid="!!errori[campo.chiave]"
                    class="border-0 px-0 shadow-none focus-visible:ring-0"
                  />
                  <span class="text-xs text-(--fg4)">{{ campo.unita }}</span>
                </div>
                <p v-if="errori[campo.chiave]" class="text-xs font-medium text-(--danger)">{{ errori[campo.chiave] }}</p>
              </div>
            </div>
          </div>
        </div>

        <DialogFooter class="flex items-center gap-2.5">
          <Button v-if="modo === 'view'" type="button" variant="neutral" :disabled="inCorso" @click="onDuplica">
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
