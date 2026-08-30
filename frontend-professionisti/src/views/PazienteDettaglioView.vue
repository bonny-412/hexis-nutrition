<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import AppShell from '@/components/AppShell.vue'
import { dettaglio, invita, type Paziente } from '@/api/pazienti'

const route = useRoute()
const paziente = ref<Paziente | null>(null)
const errore = ref(false)
const invitoInCorso = ref(false)

async function carica() {
  try {
    paziente.value = await dettaglio(route.params.id as string)
  } catch {
    errore.value = true
  }
}

async function onInvita() {
  if (!paziente.value) return
  invitoInCorso.value = true
  try {
    await invita(paziente.value.id)
    paziente.value.statoAccount = 'INVITATO'
  } finally {
    invitoInCorso.value = false
  }
}

onMounted(carica)
</script>

<template>
  <AppShell>
    <p v-if="errore" style="color: var(--danger)">Paziente non trovato.</p>
    <div v-else-if="paziente">
      <h1 class="text-3xl italic" style="font-family: Fraunces, serif; color: var(--fg)">
        {{ paziente.nome }} {{ paziente.cognome }}
      </h1>
      <p style="color: var(--fg2)">{{ paziente.email }}</p>
      <p style="color: var(--fg3)">Stato account: {{ paziente.statoAccount }}</p>

      <button
        v-if="paziente.statoAccount !== 'ATTIVO'"
        type="button"
        :disabled="invitoInCorso"
        class="mt-4 rounded-lg px-4 py-2.5 text-sm font-bold text-white disabled:opacity-70"
        style="background: var(--green)"
        @click="onInvita"
      >
        {{ paziente.statoAccount === 'MAI_INVITATO' ? 'Invita' : 'Reinvia invito' }}
      </button>
    </div>
  </AppShell>
</template>
