<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import { dettaglio, invita, type Paziente } from '@/api/pazienti'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'

const route = useRoute()
const paziente = ref<Paziente | null>(null)
const erroreCaricamento = ref<string | null>(null)
const invitoInCorso = ref(false)
const erroreInvito = ref(false)

async function carica() {
  try {
    paziente.value = await dettaglio(route.params.id as string)
  } catch (e) {
    if (e instanceof ApiError && e.status === 404) {
      erroreCaricamento.value = 'Paziente non trovato.'
    } else {
      erroreCaricamento.value = 'Non è stato possibile caricare il paziente.'
    }
  }
}

async function onInvita() {
  if (!paziente.value) return
  invitoInCorso.value = true
  erroreInvito.value = false
  try {
    await invita(paziente.value.id)
    paziente.value.statoAccount = 'INVITATO'
  } catch {
    erroreInvito.value = true
  } finally {
    invitoInCorso.value = false
  }
}

onMounted(carica)
</script>

<template>
  <AppShell>
    <p v-if="erroreCaricamento" class="text-[var(--danger)]">{{ erroreCaricamento }}</p>
    <div v-else-if="paziente">
      <h1 class="font-heading text-3xl italic text-[var(--fg)]">
        {{ paziente.nome }} {{ paziente.cognome }}
      </h1>
      <p class="text-[var(--fg2)]">{{ paziente.email }}</p>
      <p class="mt-1 flex items-center gap-1.5 text-[var(--fg3)]">Stato account: <Badge variant="secondary">{{ paziente.statoAccount }}</Badge></p>

      <p v-if="erroreInvito" class="mt-2 text-sm text-[var(--danger)]">Non è stato possibile inviare l'invito.</p>

      <Button
        v-if="paziente.statoAccount !== 'ATTIVO'"
        type="button"
        :disabled="invitoInCorso"
        class="mt-4"
        @click="onInvita"
      >
        {{ paziente.statoAccount === 'MAI_INVITATO' ? 'Invita' : 'Reinvia invito' }}
      </Button>
    </div>
  </AppShell>
</template>
