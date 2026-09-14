<script setup lang="ts">
import { ref } from 'vue'
import type { PianoAlimentareRigaLista } from '@/api/pianiAlimentari'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
} from '@/components/ui/dropdown-menu'
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
import { CircleCheck, FolderOpen, MoreHorizontal, Trash2 } from '@lucide/vue'
import DropdownMenuSeparator from '../ui/dropdown-menu/DropdownMenuSeparator.vue'

const props = defineProps<{
  riga: PianoAlimentareRigaLista
}>()

const emit = defineEmits<{
  attiva: [riga: PianoAlimentareRigaLista]
  elimina: [riga: PianoAlimentareRigaLista]
}>()

const confermaEliminaAperta = ref(false)

function confermaElimina() {
  emit('elimina', props.riga)
  confermaEliminaAperta.value = false
}
</script>

<template>
  <div class="flex items-center justify-end gap-1">
    <Button as-child variant="ghost" size="icon" aria-label="Apri piano" title="Apri piano" @click.stop>
      <router-link :to="`/piani-alimentari/${riga.id}`">
        <FolderOpen :size="15" />
      </router-link>
    </Button>

    <DropdownMenu v-if="riga.stato === 'BOZZA'">
      <DropdownMenuTrigger as-child>
        <Button
          type="button" variant="ghost" size="icon" data-test="opzioni-piano"
          aria-label="Altre opzioni" title="Altre opzioni" @click.stop
        >
          <MoreHorizontal :size="15" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" class="w-52">
        <DropdownMenuItem data-test="attiva-piano" class="cursor-pointer" @click="emit('attiva', riga)">
          <CircleCheck />
          Attiva piano
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem
          data-test="elimina-piano"
          variant="destructive" class="cursor-pointer text-(--danger) focus:text-(--danger)"
          @click="confermaEliminaAperta = true"
        >
          <Trash2 />
          Elimina
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>

    <AlertDialog v-model:open="confermaEliminaAperta">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Eliminare questo piano?</AlertDialogTitle>
          <AlertDialogDescription>L'operazione non è reversibile.</AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel variant="neutral">Annulla</AlertDialogCancel>
          <AlertDialogAction data-test="conferma-elimina-piano" class="bg-(--danger) hover:bg-(--danger)/80" @click="confermaElimina">
            Elimina
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>
