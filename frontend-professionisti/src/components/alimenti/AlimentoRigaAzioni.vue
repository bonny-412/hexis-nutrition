<script setup lang="ts">
import { ref } from 'vue'
import type { Alimento } from '@/api/alimenti'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
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
import { Eye, MoreHorizontal, Pencil, Trash2 } from '@lucide/vue'

const props = defineProps<{
  alimento: Alimento
}>()

const emit = defineEmits<{
  apri: [alimento: Alimento]
  elimina: [alimento: Alimento]
}>()

const confermaAperta = ref(false)

function apriConferma() {
  confermaAperta.value = true
}

function confermaEliminazione() {
  emit('elimina', props.alimento)
  confermaAperta.value = false
}
</script>

<template>
  <div class="flex items-center justify-end gap-1">
    <DropdownMenu>
      <DropdownMenuTrigger as-child>
        <Button type="button" variant="ghost" size="icon" aria-label="Altre opzioni" title="Altre opzioni">
          <MoreHorizontal :size="15" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" class="w-52">
        <DropdownMenuItem class="cursor-pointer" @click="emit('apri', alimento)">
          <component :is="alimento.bda ? Eye : Pencil" />
          {{ alimento.bda ? 'Visualizza' : 'Modifica' }}
        </DropdownMenuItem>
        <template v-if="!alimento.bda">
          <DropdownMenuSeparator />
          <DropdownMenuItem
            variant="destructive"
            class="text-(--danger) focus:text-(--danger) cursor-pointer"
            @click="apriConferma"
          >
            <Trash2 />
            Elimina
          </DropdownMenuItem>
        </template>
      </DropdownMenuContent>
    </DropdownMenu>

    <AlertDialog v-model:open="confermaAperta">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>Eliminare {{ alimento.nome }}?</AlertDialogTitle>
          <AlertDialogDescription>
            L'alimento verrà rimosso definitivamente dal tuo catalogo personalizzato.
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel variant="neutral">Annulla</AlertDialogCancel>
          <AlertDialogAction class="bg-(--danger) hover:bg-(--danger)/80" @click="confermaEliminazione">
            Conferma
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>
