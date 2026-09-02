<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import AppShell from '@/components/AppShell.vue'
import { lista, invita, type Paziente } from '@/api/pazienti'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { Plus } from '@lucide/vue'

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
      <h1 class="font-heading text-3xl italic text-[var(--fg)]">Pazienti</h1>
      <Button as-child size="lg" class="hover:bg-primary/80">
        <router-link to="/pazienti/nuovo"><Plus :size="16" /> Nuovo paziente</router-link>
      </Button>
    </div>

    <Input v-model="ricerca" type="search" placeholder="Cerca per nome, cognome o email" class="mb-4 w-full max-w-sm" />

    <p v-if="erroreInvito" class="mb-4 text-sm text-[var(--danger)]">Non è stato possibile inviare l'invito.</p>

    <p v-if="errore" class="text-[var(--danger)]">Non è stato possibile caricare i pazienti.</p>
    <p v-else-if="caricamento" class="text-[var(--fg3)]">Caricamento…</p>
    <p v-else-if="pazienti.length === 0" class="text-[var(--fg3)]">Nessun paziente, per ora.</p>
    <p v-else-if="pazientiFiltrati.length === 0" class="text-[var(--fg3)]">Nessun paziente con questi criteri di ricerca.</p>

    <Table v-else>
      <TableHeader>
        <TableRow>
          <TableHead>Nome</TableHead>
          <TableHead>Email</TableHead>
          <TableHead>Stato</TableHead>
          <TableHead></TableHead>
        </TableRow>
      </TableHeader>
      <TableBody>
        <TableRow v-for="paziente in pazientiFiltrati" :key="paziente.id">
          <TableCell>
            <router-link :to="`/pazienti/${paziente.id}`" class="font-medium text-[var(--fg)]">
              {{ paziente.nome }} {{ paziente.cognome }}
            </router-link>
          </TableCell>
          <TableCell class="text-[var(--fg2)]">{{ paziente.email }}</TableCell>
          <TableCell><Badge variant="secondary">{{ paziente.statoAccount }}</Badge></TableCell>
          <TableCell class="text-right">
            <Button
              v-if="etichettaAzione(paziente)"
              type="button"
              variant="link"
              size="sm"
              :disabled="invitoInCorsoId === paziente.id"
              @click="onInvita(paziente)"
            >
              {{ etichettaAzione(paziente) }}
            </Button>
          </TableCell>
        </TableRow>
      </TableBody>
    </Table>
  </AppShell>
</template>
