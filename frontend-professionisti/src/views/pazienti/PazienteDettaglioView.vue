<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { toast } from 'vue-sonner'
import AppShell from '@/components/AppShell.vue'
import { dettaglio, invita, type Paziente } from '@/api/pazienti'
import { ApiError } from '@/api/client'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'

const route = useRoute()
const paziente = ref<Paziente | null>(null)
const erroreCaricamento = ref<string | null>(null)
const invitoInCorso = ref(false)

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
  try {
    await invita(paziente.value.id)
    paziente.value.statoAccount = 'INVITATO'
    toast.success('Invito inviato.')
  } catch {
    toast.error('Non è stato possibile inviare l\'invito.')
  } finally {
    invitoInCorso.value = false
  }
}

onMounted(carica)
</script>

<template>
  <AppShell>
    <p v-if="erroreCaricamento" class="text-(--danger)">{{ erroreCaricamento }}</p>
    <div v-else-if="paziente">
      <h1 class="font-heading text-3xl italic text-(--fg)">
        {{ paziente.nome }} {{ paziente.cognome }}
      </h1>
      <p class="text-(--fg2)">{{ paziente.email }}</p>
      <p class="text-(--fg3)">Codice fiscale: {{ paziente.codiceFiscale }}</p>
      <p class="mt-1 flex items-center gap-1.5 text-(--fg3)">Stato account: <Badge variant="secondary">{{ paziente.statoAccount }}</Badge></p>

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
