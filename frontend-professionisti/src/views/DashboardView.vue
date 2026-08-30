<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppShell from '@/components/AppShell.vue'
import { lista } from '@/api/pazienti'

const pazientiAttivi = ref<number | null>(null)
const erroreCaricamento = ref(false)

onMounted(async () => {
  try {
    const pazienti = await lista()
    pazientiAttivi.value = pazienti.filter((p) => p.statoAccount === 'ATTIVO').length
  } catch {
    erroreCaricamento.value = true
  }
})
</script>

<template>
  <AppShell>
    <h1 class="text-3xl italic" style="font-family: Fraunces, serif; color: var(--fg)">Bentornata</h1>

    <div class="mt-6 grid grid-cols-2 gap-4 md:grid-cols-4">
      <div class="rounded-xl border p-4" style="border-color: var(--bd2); background: var(--surf)">
        <div class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">Pazienti attivi</div>
        <div class="mt-2 text-3xl font-semibold" style="color: var(--fg)">
          {{ erroreCaricamento ? '—' : (pazientiAttivi ?? '…') }}
        </div>
      </div>

      <div
        v-for="titolo in ['Visite oggi', 'Piani in scadenza', 'Messaggi non letti']"
        :key="titolo"
        class="rounded-xl border p-4 opacity-60"
        style="border-color: var(--bd2); background: var(--surf)"
      >
        <div class="text-xs font-bold uppercase tracking-wide" style="color: var(--fg3)">{{ titolo }}</div>
        <div class="mt-2 text-sm" style="color: var(--fg4)">Disponibile a breve</div>
      </div>
    </div>
  </AppShell>
</template>
