<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppShell from '@/components/AppShell.vue'
import { lista, invita, type Paziente } from '@/api/pazienti'

const pazienti = ref<Paziente[]>([])
const ricerca = ref('')
const caricamento = ref(true)
const errore = ref(false)
const invitoInCorsoId = ref<string | null>(null)
const erroreInvito = ref(false)

const pazientiFiltrati = computed(() => {
  const termine = ricerca.value.trim().toLowerCase()
  if (!termine) return pazienti.value
  return pazienti.value.filter((p) =>
    `${p.nome} ${p.cognome} ${p.email}`.toLowerCase().includes(termine),
  )
})

async function carica() {
  caricamento.value = true
  errore.value = false
  try {
    pazienti.value = await lista()
  } catch {
    errore.value = true
  } finally {
    caricamento.value = false
  }
}

async function onInvita(paziente: Paziente) {
  invitoInCorsoId.value = paziente.id
  erroreInvito.value = false
  try {
    await invita(paziente.id)
    paziente.statoAccount = 'INVITATO'
  } catch {
    erroreInvito.value = true
  } finally {
    invitoInCorsoId.value = null
  }
}

function etichettaAzione(paziente: Paziente) {
  if (paziente.statoAccount === 'MAI_INVITATO') return 'Invita'
  if (paziente.statoAccount === 'INVITATO') return 'Reinvia invito'
  return null
}

onMounted(carica)
</script>

<template>
  <AppShell>
    <div class="mb-6 flex items-center justify-between">
      <h1 class="text-3xl italic" style="font-family: Fraunces, serif; color: var(--fg)">Pazienti</h1>
      <router-link
        to="/pazienti/nuovo"
        class="rounded-lg px-4 py-2 text-sm font-bold text-white"
        style="background: var(--green)"
      >
        + Nuovo paziente
      </router-link>
    </div>

    <input
      v-model="ricerca"
      type="search"
      placeholder="Cerca per nome, cognome o email"
      class="mb-4 w-full max-w-sm rounded-lg border px-3 py-2 text-sm"
      style="border-color: var(--bd2); background: var(--surf)"
    />

    <p v-if="erroreInvito" style="color: var(--danger)" class="mb-4 text-sm">Non è stato possibile inviare l'invito.</p>

    <p v-if="errore" style="color: var(--danger)">Non è stato possibile caricare i pazienti.</p>
    <p v-else-if="caricamento" style="color: var(--fg3)">Caricamento…</p>
    <p v-else-if="pazienti.length === 0" style="color: var(--fg3)">Nessun paziente, per ora.</p>
    <p v-else-if="pazientiFiltrati.length === 0" style="color: var(--fg3)">Nessun paziente con questi criteri di ricerca.</p>

    <table v-else class="w-full text-left text-sm">
      <thead>
        <tr style="color: var(--fg3)">
          <th class="pb-2">Nome</th>
          <th class="pb-2">Email</th>
          <th class="pb-2">Stato</th>
          <th class="pb-2"></th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="paziente in pazientiFiltrati" :key="paziente.id" class="border-t" style="border-color: var(--div)">
          <td class="py-2">
            <router-link :to="`/pazienti/${paziente.id}`" class="font-medium" style="color: var(--fg)">
              {{ paziente.nome }} {{ paziente.cognome }}
            </router-link>
          </td>
          <td class="py-2" style="color: var(--fg2)">{{ paziente.email }}</td>
          <td class="py-2">{{ paziente.statoAccount }}</td>
          <td class="py-2 text-right">
            <button
              v-if="etichettaAzione(paziente)"
              type="button"
              :disabled="invitoInCorsoId === paziente.id"
              class="text-xs font-semibold"
              style="color: var(--green)"
              @click="onInvita(paziente)"
            >
              {{ etichettaAzione(paziente) }}
            </button>
          </td>
        </tr>
      </tbody>
    </table>
  </AppShell>
</template>
