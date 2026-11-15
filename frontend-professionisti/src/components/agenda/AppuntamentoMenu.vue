<script lang="ts">
export type AzioneAppuntamento =
  | 'AVVIA_VISITA'
  | 'MODIFICA'
  | 'ANNULLA'
  | 'APRI_VIDEO'
  | 'APRI_CARTELLA'
  | 'FATTO'
  | 'DA_FARE'
  | 'SPOSTA_DOMANI'
  | 'ELIMINA'
</script>

<script setup lang="ts">
import { computed } from 'vue'
import {
  CalendarClock,
  CalendarPlus,
  Check,
  FolderOpen,
  MoreHorizontal,
  Pencil,
  Play,
  RotateCcw,
  Trash2,
  Video,
  XCircle,
} from '@lucide/vue'
import type { Appuntamento } from '@/api/agenda'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'

const props = defineProps<{ appuntamento: Appuntamento }>()
const emit = defineEmits<{ azione: [azione: AzioneAppuntamento] }>()

const eVisita = computed(() => props.appuntamento.tipo === 'VISITA_STUDIO' || props.appuntamento.tipo === 'VIDEOCONSULTO')
const eProgrammata = computed(() => eVisita.value && props.appuntamento.stato === 'PROGRAMMATA')
const eChiusa = computed(() => eVisita.value && !eProgrammata.value)
const ePromemoria = computed(() => props.appuntamento.tipo === 'PROMEMORIA')
const eBlocco = computed(() => props.appuntamento.tipo === 'BLOCCO')
</script>

<template>
  <DropdownMenu>
    <DropdownMenuTrigger as-child>
      <Button type="button" variant="ghost" size="icon-sm" aria-label="Azioni appuntamento" title="Azioni" data-test="appuntamento-menu">
        <MoreHorizontal :size="15" />
      </Button>
    </DropdownMenuTrigger>
    <DropdownMenuContent align="end" class="w-56">
      <template v-if="eProgrammata">
        <DropdownMenuItem class="cursor-pointer" data-test="azione-avvia-visita" @click="emit('azione', 'AVVIA_VISITA')">
          <Play /> Avvia visita
        </DropdownMenuItem>
        <DropdownMenuItem class="cursor-pointer" data-test="azione-modifica" @click="emit('azione', 'MODIFICA')">
          <CalendarClock /> Sposta / Modifica
        </DropdownMenuItem>
        <DropdownMenuItem v-if="appuntamento.linkVideo" class="cursor-pointer" data-test="azione-apri-video" @click="emit('azione', 'APRI_VIDEO')">
          <Video /> Apri videoconsulto
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem
          variant="destructive"
          class="cursor-pointer text-(--danger) focus:text-(--danger)"
          data-test="azione-annulla"
          @click="emit('azione', 'ANNULLA')"
        >
          <XCircle /> Annulla visita
        </DropdownMenuItem>
      </template>

      <template v-else-if="eChiusa">
        <DropdownMenuItem class="cursor-pointer" data-test="azione-apri-cartella" @click="emit('azione', 'APRI_CARTELLA')">
          <FolderOpen /> Apri cartella paziente
        </DropdownMenuItem>
      </template>

      <template v-else-if="ePromemoria">
        <DropdownMenuItem
          v-if="!appuntamento.fatto"
          class="cursor-pointer"
          data-test="azione-fatto"
          @click="emit('azione', 'FATTO')"
        >
          <Check /> Segna come fatto
        </DropdownMenuItem>
        <DropdownMenuItem v-else class="cursor-pointer" data-test="azione-da-fare" @click="emit('azione', 'DA_FARE')">
          <RotateCcw /> Da fare
        </DropdownMenuItem>
        <DropdownMenuItem class="cursor-pointer" data-test="azione-sposta-domani" @click="emit('azione', 'SPOSTA_DOMANI')">
          <CalendarPlus /> Sposta a domani
        </DropdownMenuItem>
        <DropdownMenuItem class="cursor-pointer" data-test="azione-modifica" @click="emit('azione', 'MODIFICA')">
          <Pencil /> Modifica
        </DropdownMenuItem>
        <DropdownMenuSeparator />
        <DropdownMenuItem
          variant="destructive"
          class="cursor-pointer text-(--danger) focus:text-(--danger)"
          data-test="azione-elimina"
          @click="emit('azione', 'ELIMINA')"
        >
          <Trash2 /> Elimina promemoria
        </DropdownMenuItem>
      </template>

      <template v-else-if="eBlocco">
        <DropdownMenuItem class="cursor-pointer" data-test="azione-modifica" @click="emit('azione', 'MODIFICA')">
          <Pencil /> Modifica
        </DropdownMenuItem>
        <DropdownMenuItem
          variant="destructive"
          class="cursor-pointer text-(--danger) focus:text-(--danger)"
          data-test="azione-elimina"
          @click="emit('azione', 'ELIMINA')"
        >
          <Trash2 /> Elimina
        </DropdownMenuItem>
      </template>
    </DropdownMenuContent>
  </DropdownMenu>
</template>
