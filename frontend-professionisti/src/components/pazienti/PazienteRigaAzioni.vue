<script setup lang="ts">
import { ref } from 'vue'
import type { Paziente } from '@/api/pazienti'
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
import { ClipboardPlus, FolderOpen, MoreHorizontal, Trash2 } from '@lucide/vue'

const props = defineProps<{
  paziente: Paziente
  mostraArchiviati: boolean
}>()

const emit = defineEmits<{
  invita: [paziente: Paziente]
  archivia: [paziente: Paziente]
  deArchivia: [paziente: Paziente]
}>()

const confermaAperta = ref(false)

function apriConferma() {
  confermaAperta.value = true
}

function confermaAzione() {
  if (props.mostraArchiviati) {
    emit('deArchivia', props.paziente)
  } else {
    emit('archivia', props.paziente)
  }
  confermaAperta.value = false
}

function etichettaAzione(paziente: Paziente) {
  if (paziente.statoAccount === 'MAI_INVITATO') return 'Invita'
  if (paziente.statoAccount === 'INVITATO') return 'Reinvia invito'
  return null
}
</script>

<template>
  <div class="flex items-center justify-end gap-1">
    <Button as-child variant="ghost" size="icon" aria-label="Apri cartella" title="Apri cartella">
      <router-link :to="`/pazienti/${paziente.id}`">
        <FolderOpen :size="15" />
      </router-link>
    </Button>

    <Button
      v-if="!mostraArchiviati && etichettaAzione(paziente)"
      type="button"
      variant="ghost"
      @click="emit('invita', paziente)"
    >
      {{ etichettaAzione(paziente) }}
    </Button>

    <DropdownMenu>
      <DropdownMenuTrigger as-child>
        <Button type="button" variant="ghost" size="icon" aria-label="Altre opzioni" title="Altre opzioni">
          <MoreHorizontal :size="15" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" class="w-52">
        <DropdownMenuItem as-child data-test="menu-nuova-visita" class="cursor-pointer">
          <router-link :to="`/pazienti/${paziente.id}/visite/nuova`">
            <ClipboardPlus />
            Nuova visita
          </router-link>
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem
          v-if="!mostraArchiviati"
          data-test="menu-archivia"
          variant="destructive"
          class="text-(--danger) focus:text-(--danger) cursor-pointer"
          @click="apriConferma"
        >
          <Trash2 />
          Archivia paziente
        </DropdownMenuItem>
        <DropdownMenuItem v-else data-test="menu-de-archivia" @click="apriConferma">
          De-archivia paziente
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>

    <AlertDialog v-model:open="confermaAperta">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            {{ mostraArchiviati ? 'De-archiviare' : 'Archiviare' }} {{ paziente.nome }} {{ paziente.cognome }}?
          </AlertDialogTitle>
          <AlertDialogDescription>
            {{
              mostraArchiviati
                ? 'Il paziente tornerà visibile nella lista pazienti attivi.'
                : 'Il paziente non comparirà più nella lista pazienti attivi. Potrai de-archiviarlo in qualsiasi momento.'
            }}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel data-test="conferma-annulla">Annulla</AlertDialogCancel>
          <AlertDialogAction data-test="conferma-conferma" @click="confermaAzione" class="hover:bg-primary/80">Conferma</AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>
