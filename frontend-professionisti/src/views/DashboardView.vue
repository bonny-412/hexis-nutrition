<script setup lang="ts">
import { ref, onMounted } from 'vue'
import AppShell from '@/components/AppShell.vue'
import { lista } from '@/api/pazienti'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { Button } from '@/components/ui/button'
import { DropdownMenu, DropdownMenuTrigger, DropdownMenuContent, DropdownMenuItem } from '@/components/ui/dropdown-menu'
import { Plus, ChevronDown, UserPlus, CalendarPlus, FileText } from '@lucide/vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const pazientiAttivi = ref<number | null>(null)
const erroreCaricamento = ref(false)

const dataOggi = new Intl.DateTimeFormat('it-IT', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})
  .format(new Date())
  .replace(/^./, (c) => c.toUpperCase())

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
    <div class="flex items-start justify-between">
      <div>
        <h1 class="font-heading text-3xl italic text-(--fg)">Ciao, {{ auth.professionista?.nome }}</h1>
        <p class="mt-1 text-sm text-(--fg3)">{{ dataOggi }}</p>
      </div>

      <DropdownMenu>
        <DropdownMenuTrigger as-child>
          <Button class="group">
            <Plus :size="16" />
            Crea nuovo
            <ChevronDown :size="14" class="transition-transform duration-200 group-data-[state=open]:rotate-180" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" class="flex w-60 flex-col gap-0.5">
          <DropdownMenuItem as-child class="gap-2 px-2.5 py-1.5">
            <router-link to="/pazienti/nuovo">
              <UserPlus :size="16" />
              Nuovo paziente
            </router-link>
          </DropdownMenuItem>
          <DropdownMenuItem disabled class="gap-2 px-2.5 py-1.5">
            <CalendarPlus :size="16" />
            Nuovo appuntamento
          </DropdownMenuItem>
          <DropdownMenuItem disabled class="gap-2 px-2.5 py-1.5">
            <FileText :size="16" />
            Nuovo piano alimentare
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </div>

    <div class="mt-6 grid grid-cols-2 gap-4 md:grid-cols-4">
      <Card>
        <CardHeader>
          <CardTitle class="text-xs font-bold uppercase tracking-wide text-(--fg3)">Pazienti attivi</CardTitle>
        </CardHeader>
        <CardContent class="text-3xl font-semibold text-(--fg)">
          {{ erroreCaricamento ? '—' : (pazientiAttivi ?? '…') }}
        </CardContent>
      </Card>

      <Card v-for="titolo in ['Visite oggi', 'Piani in scadenza', 'Messaggi non letti']" :key="titolo" class="opacity-60">
        <CardHeader>
          <CardTitle class="text-xs font-bold uppercase tracking-wide text-(--fg3)">{{ titolo }}</CardTitle>
        </CardHeader>
        <CardContent class="text-sm text-(--fg4)">Disponibile a breve</CardContent>
      </Card>
    </div>
  </AppShell>
</template>
