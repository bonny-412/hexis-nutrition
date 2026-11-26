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
import { Archive, ArchiveRestore, CalendarPlus, ClipboardPlus, FolderOpen, MoreHorizontal, Utensils, X } from '@lucide/vue'

const props = defineProps<{
  paziente: Paziente
  mostraArchiviati: boolean
  // Nelle schede tutta la scheda apre già la cartella: il pulsante sarebbe un doppione.
  senzaApriCartella?: boolean
}>()

const emit = defineEmits<{
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

</script>

<template>
  <div class="flex items-center justify-end gap-1">
    <Button v-if="!senzaApriCartella" as-child variant="ghost" size="icon" aria-label="Apri scheda paziente" title="Apri scheda paziente">
      <router-link :to="`/pazienti/${paziente.id}`">
        <FolderOpen :size="16" />
      </router-link>
    </Button>

    <DropdownMenu>
      <DropdownMenuTrigger as-child>
        <Button type="button" variant="ghost" size="icon" aria-label="Altre opzioni" title="Altre opzioni">
          <MoreHorizontal :size="16" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" class="w-52">
        <DropdownMenuItem v-if="!mostraArchiviati" as-child data-test="menu-nuova-visita" class="cursor-pointer">
          <router-link :to="`/pazienti/${paziente.id}/visite/nuova`">
            <ClipboardPlus />
            Nuova visita
          </router-link>
        </DropdownMenuItem>
        <DropdownMenuItem v-if="!mostraArchiviati" as-child data-test="menu-fissa-appuntamento" class="cursor-pointer">
          <router-link :to="`/agenda?nuovo=visita&pazienteId=${paziente.id}`">
            <CalendarPlus />
            Nuovo appuntamento
          </router-link>
        </DropdownMenuItem>
        <DropdownMenuItem v-if="!mostraArchiviati" as-child data-test="menu-nuovo-piano" class="cursor-pointer">
          <router-link :to="`/piani-alimentari/nuovo?pazienteId=${paziente.id}`">
            <Utensils />
            Nuovo piano
          </router-link>
        </DropdownMenuItem>
        <DropdownMenuSeparator v-if="!mostraArchiviati" />
        <DropdownMenuItem
          v-if="!mostraArchiviati"
          data-test="menu-archivia"
          variant="destructive"
          class="cursor-pointer"
          @click="apriConferma"
        >
          <Archive />
          Archivia paziente
        </DropdownMenuItem>
        <DropdownMenuItem v-else data-test="menu-de-archivia" class="cursor-pointer" @click="apriConferma">
          <ArchiveRestore />
          Ripristina paziente
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>

    <AlertDialog v-model:open="confermaAperta">
      <AlertDialogContent>
        <AlertDialogHeader>
          <AlertDialogTitle>
            {{ mostraArchiviati ? 'Ripristinare' : 'Archiviare' }} {{ paziente.nome }} {{ paziente.cognome }}?
          </AlertDialogTitle>
          <AlertDialogDescription>
            {{
              mostraArchiviati
                ? 'Il paziente tornerà visibile nella lista pazienti attivi.'
                : 'Il paziente uscirà dalla lista degli attivi. Le visite e i promemoria futuri collegati verranno eliminati. Potrai ripristinare il paziente in qualsiasi momento.'
            }}
          </AlertDialogDescription>
        </AlertDialogHeader>
        <AlertDialogFooter>
          <AlertDialogCancel data-test="conferma-annulla" variant="neutral"><X /> Annulla</AlertDialogCancel>
          <AlertDialogAction
            data-test="conferma-conferma"
            :variant="mostraArchiviati ? 'default' : 'destructive'"
            @click="confermaAzione"
          >
            <component :is="mostraArchiviati ? ArchiveRestore : Archive" />
            {{ mostraArchiviati ? 'Ripristina paziente' : 'Archivia paziente' }}
          </AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  </div>
</template>
