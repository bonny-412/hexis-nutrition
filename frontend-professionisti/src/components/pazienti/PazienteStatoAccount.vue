<script setup lang="ts">
import { computed } from 'vue'
import { Loader2, MailCheck, MailX, UserCheck } from '@lucide/vue'
import type { Paziente } from '@/api/pazienti'
import { CLASSI_STATO_ACCOUNT } from '@/utils/paziente'

const props = defineProps<{
  paziente: Paziente
  hero?: boolean
  solaLettura?: boolean
  invitoInCorso?: boolean
}>()

const emit = defineEmits<{ invita: [] }>()

const stati = {
  MAI_INVITATO: { etichetta: 'Mai invitato', icona: MailX, azione: 'Invita' },
  INVITATO: { etichetta: 'Invitato', icona: MailCheck, azione: 'Reinvia invito' },
  ATTIVO: { etichetta: 'Attivo', icona: UserCheck, azione: null },
}

const classiHero = {
  MAI_INVITATO: 'bg-(--hero-pill) text-(--danger-fixed) ring-1 ring-(--hero-pill-ring)',
  INVITATO: CLASSI_STATO_ACCOUNT.INVITATO,
  ATTIVO: 'bg-(--hero-pill) text-(--hero-pill-fg) ring-1 ring-(--hero-pill-ring)',
}

const stato = computed(() => stati[props.paziente.statoAccount])
const puoInvitare = computed(() => !props.solaLettura && !props.paziente.archiviato && !!stato.value.azione)
</script>

<template>
  <span
    data-test="badge-stato-account"
    class="inline-flex w-fit items-center gap-1.5 rounded-full py-1 pl-2.5 text-xs font-bold"
    :class="[(hero ? classiHero : CLASSI_STATO_ACCOUNT)[paziente.statoAccount], puoInvitare ? 'pr-1' : 'pr-2.5']"
  >
    <span class="inline-flex items-center gap-1.5">
      <component :is="stato.icona" :size="hero ? 16 : 14" class="shrink-0" aria-hidden="true" />
      {{ stato.etichetta }}
    </span>
    <button
      v-if="puoInvitare"
      type="button"
      data-test="pulsante-invita"
      :aria-label="`${stato.azione}: ${paziente.nome} ${paziente.cognome}`"
      :aria-busy="invitoInCorso"
      :disabled="invitoInCorso"
      class="relative ml-0.5 inline-flex items-center gap-1 border-l border-current/25 px-2 underline-offset-2 transition-colors before:absolute before:-inset-x-1 before:-inset-y-3 before:content-[''] hover:underline focus-visible:rounded-full focus-visible:outline-2 focus-visible:outline-current disabled:cursor-not-allowed disabled:opacity-60"
      @click="emit('invita')"
    ><Loader2 v-if="invitoInCorso" :size="12" class="shrink-0 animate-spin" aria-hidden="true" />{{ stato.azione }}</button>
  </span>
</template>
