<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import type { AcceptableValue } from 'reka-ui'
import { toast } from 'vue-sonner'
import AppShell from '@/components/AppShell.vue'
import {
  cerca,
  invita,
  archivia,
  deArchivia,
  type Paziente,
  type PaginaPazienti,
  type CriteriRicercaPazienti,
} from '@/api/pazienti'
import PazienteRigaAzioni from '@/components/pazienti/PazienteRigaAzioni.vue'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Label } from '@/components/ui/label'
import { Checkbox } from '@/components/ui/checkbox'
import { DatePicker } from '@/components/ui/date-picker'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableHeader, TableBody, TableRow, TableHead, TableCell } from '@/components/ui/table'
import { ChevronRight, ArrowUp, ArrowDown, ArrowUpDown, UserPlus } from '@lucide/vue'
import { calcolaEta } from '@/utils/data'

type CampoOrdinamento = NonNullable<CriteriRicercaPazienti['ordinaPer']>

const chipStatoAccount: { valore: 'TUTTI' | Paziente['statoAccount']; etichetta: string }[] = [
  { valore: 'TUTTI', etichetta: 'Tutti' },
  { valore: 'MAI_INVITATO', etichetta: 'Mai invitato' },
  { valore: 'INVITATO', etichetta: 'Invitato' },
  { valore: 'ATTIVO', etichetta: 'Attivo' },
]

const ETICHETTE_STATO_ACCOUNT: Record<Paziente['statoAccount'], string> = {
  MAI_INVITATO: 'Mai invitato',
  INVITATO: 'Invitato',
  ATTIVO: 'Attivo',
}

const CLASSI_STATO_ACCOUNT: Record<Paziente['statoAccount'], string> = {
  MAI_INVITATO: 'bg-(--hover) text-(--fg4)',
  INVITATO: 'bg-(--warn-bg) text-(--warn-fg)',
  ATTIVO: 'bg-(--mint) text-(--green)',
}

const ricercaInput = ref('')
const ricercaEffettiva = ref('')
const statoAccount = ref<'TUTTI' | Paziente['statoAccount']>('TUTTI')
const sesso = ref<'TUTTI' | 'M' | 'F' | 'ALTRO'>('TUTTI')
const dataNascitaDa = ref('')
const dataNascitaA = ref('')
const mostraArchiviati = ref(false)
const filtriAvanzatiAperti = ref(false)
const pagina = ref(0)
const ordinaPer = ref<CampoOrdinamento | undefined>(undefined)
const direzione = ref<'asc' | 'desc'>('asc')

const paginaDati = ref<PaginaPazienti | null>(null)
const caricamentoIniziale = ref(true)
const aggiornamentoInCorso = ref(false)
const errore = ref(false)
const invitoInCorsoId = ref<string | null>(null)

let debounceHandle: ReturnType<typeof setTimeout> | undefined

watch(ricercaInput, (valore) => {
  clearTimeout(debounceHandle)
  debounceHandle = setTimeout(() => {
    ricercaEffettiva.value = valore
    pagina.value = 0
  }, 300)
})

onUnmounted(() => clearTimeout(debounceHandle))

const filtriAvanzatiAttivi = computed(() =>
  [sesso.value !== 'TUTTI', !!dataNascitaDa.value, !!dataNascitaA.value, mostraArchiviati.value].filter(Boolean).length,
)

const filtriAttivi = computed(() =>
  ricercaEffettiva.value.trim() !== '' || statoAccount.value !== 'TUTTI' || filtriAvanzatiAttivi.value > 0,
)

function criteriCorrenti(): CriteriRicercaPazienti {
  return {
    pagina: pagina.value,
    dimensione: 20,
    ordinaPer: ordinaPer.value,
    direzione: direzione.value,
    ricerca: ricercaEffettiva.value.trim() || undefined,
    statoAccount: statoAccount.value === 'TUTTI' ? undefined : statoAccount.value,
    sesso: sesso.value === 'TUTTI' ? undefined : sesso.value,
    dataNascitaDa: dataNascitaDa.value || undefined,
    dataNascitaA: dataNascitaA.value || undefined,
    archiviato: mostraArchiviati.value,
  }
}

async function carica() {
  if (paginaDati.value === null) {
    caricamentoIniziale.value = true
  } else {
    aggiornamentoInCorso.value = true
  }
  errore.value = false
  try {
    const risultato = await cerca(criteriCorrenti())
    if (risultato.contenuto.length === 0 && pagina.value > 0 && risultato.totaleElementi > 0) {
      pagina.value -= 1
      return
    }
    paginaDati.value = risultato
  } catch {
    errore.value = true
  } finally {
    caricamentoIniziale.value = false
    aggiornamentoInCorso.value = false
  }
}

watch(
  [ricercaEffettiva, statoAccount, sesso, dataNascitaDa, dataNascitaA, mostraArchiviati, pagina, ordinaPer, direzione],
  carica,
)

onMounted(carica)

function selezionaStato(valore: typeof statoAccount.value) {
  statoAccount.value = valore
  pagina.value = 0
}

function onSessoChange(valore: AcceptableValue) {
  sesso.value = valore as typeof sesso.value
  pagina.value = 0
}

function onDataNascitaDaChange(valore: string) {
  dataNascitaDa.value = valore
  pagina.value = 0
}

function onDataNascitaAChange(valore: string) {
  dataNascitaA.value = valore
  pagina.value = 0
}

function onMostraArchiviatiChange(valore: boolean | 'indeterminate') {
  mostraArchiviati.value = valore === true
  pagina.value = 0
}

function ordina(campo: CampoOrdinamento) {
  if (ordinaPer.value !== campo) {
    ordinaPer.value = campo
    direzione.value = 'asc'
  } else if (direzione.value === 'asc') {
    direzione.value = 'desc'
  } else {
    ordinaPer.value = undefined
    direzione.value = 'asc'
  }
  pagina.value = 0
}

function iconaOrdinamento(campo: CampoOrdinamento) {
  if (ordinaPer.value !== campo) return ArrowUpDown
  return direzione.value === 'asc' ? ArrowUp : ArrowDown
}

function pulisciFiltri() {
  clearTimeout(debounceHandle)
  ricercaInput.value = ''
  ricercaEffettiva.value = ''
  statoAccount.value = 'TUTTI'
  sesso.value = 'TUTTI'
  dataNascitaDa.value = ''
  dataNascitaA.value = ''
  mostraArchiviati.value = false
  pagina.value = 0
}

function paginaPrecedente() {
  if (pagina.value > 0) pagina.value -= 1
}

function paginaSuccessiva() {
  if (paginaDati.value && pagina.value < paginaDati.value.totalePagine - 1) pagina.value += 1
}

async function onInvita(paziente: Paziente) {
  invitoInCorsoId.value = paziente.id
  try {
    await invita(paziente.id)
    paziente.statoAccount = 'INVITATO'
    toast.success('Invito inviato.')
  } catch {
    toast.error('Non è stato possibile inviare l\'invito.')
  } finally {
    invitoInCorsoId.value = null
  }
}

async function onArchivia(paziente: Paziente) {
  try {
    await archivia(paziente.id)
    await carica()
    toast.success('Paziente archiviato.')
  } catch {
    toast.error('Non è stato possibile completare l\'operazione.')
  }
}

async function onDeArchivia(paziente: Paziente) {
  try {
    await deArchivia(paziente.id)
    await carica()
    toast.success('Paziente ripristinato.')
  } catch {
    toast.error('Non è stato possibile completare l\'operazione.')
  }
}

const conteggioTesto = computed(() => {
  if (!paginaDati.value) return ''
  const { totaleElementi, paginaCorrente, dimensionePagina, contenuto } = paginaDati.value
  if (totaleElementi === 0) return mostraArchiviati.value ? 'Nessun paziente archiviato' : 'Nessun paziente'
  const primo = paginaCorrente * dimensionePagina + 1
  const ultimo = paginaCorrente * dimensionePagina + contenuto.length
  return `Mostrati ${primo}-${ultimo} di ${totaleElementi} pazienti`
})
</script>

<template>
  <AppShell>
    <div class="mb-6 flex items-center justify-between">
      <div>
        <h1 class="font-heading text-3xl italic text-(--fg)">Pazienti</h1>
        <p class="mt-1 text-sm text-(--fg3)">Gestisci la lista dei pazienti, gli inviti e le cartelle cliniche.</p>
      </div>
      <Button as-child size="lg" class="hover:bg-primary/80">
        <router-link to="/pazienti/nuovo"><UserPlus :size="16" /> Nuovo paziente</router-link>
      </Button>
    </div>

    <section class="mb-3.5 rounded-2xl border border-(--bd) bg-(--surf) p-3.5">
      <div class="flex flex-wrap items-center gap-2.5">
        <Input
          v-model="ricercaInput"
          type="search"
          placeholder="Filtra per nome, email o codice fiscale…"
          class="min-w-70 flex-1"
        />
        <div class="flex flex-wrap gap-1.5">
          <button
            v-for="chip in chipStatoAccount"
            :key="chip.valore"
            type="button"
            class="rounded-full border px-3 py-1.5 text-xs font-bold transition-colors"
            :class="statoAccount === chip.valore
              ? 'border-(--sage) bg-(--mint) text-(--green)'
              : 'border-(--bd2) bg-(--surf) text-(--fg2) hover:border-(--sage)'"
            @click="selezionaStato(chip.valore)"
          >
            {{ chip.etichetta }}
          </button>
        </div>
      </div>

      <div class="my-3 h-px bg-(--div)" />

      <button
        type="button"
        class="flex items-center gap-2 text-xs font-bold text-(--fg2) hover:text-(--fg)"
        @click="filtriAvanzatiAperti = !filtriAvanzatiAperti"
      >
        <ChevronRight :size="14" class="transition-transform" :class="{ 'rotate-90': filtriAvanzatiAperti }" />
        Filtri avanzati
        <Badge :variant="filtriAvanzatiAttivi > 0 ? 'secondary' : 'outline'">
          {{ filtriAvanzatiAttivi > 0 ? `${filtriAvanzatiAttivi} attivi` : 'Nessuno attivo' }}
        </Badge>
      </button>

      <div v-if="filtriAvanzatiAperti" class="mt-3 grid grid-cols-[repeat(auto-fit,minmax(170px,1fr))] items-end gap-2.5">
        <div class="flex flex-col gap-1.5">
          <Label class="text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Sesso</Label>
          <Select :model-value="sesso" @update:model-value="onSessoChange">
            <SelectTrigger class="w-full"><SelectValue /></SelectTrigger>
            <SelectContent>
              <SelectItem value="TUTTI">Tutti</SelectItem>
              <SelectItem value="M">Maschio</SelectItem>
              <SelectItem value="F">Femmina</SelectItem>
              <SelectItem value="ALTRO">Altro</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div class="flex flex-col gap-1.5">
          <Label class="text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Data di nascita da</Label>
          <DatePicker id="data-nascita-da" :model-value="dataNascitaDa" @update:model-value="onDataNascitaDaChange" />
        </div>
        <div class="flex flex-col gap-1.5">
          <Label class="text-[10px] font-bold uppercase tracking-wide text-(--fg4)">Data di nascita a</Label>
          <DatePicker id="data-nascita-a" :model-value="dataNascitaA" @update:model-value="onDataNascitaAChange" />
        </div>
        <label class="flex items-center gap-2 pb-1.5">
          <Checkbox
            id="filtro-mostra-archiviati"
            :model-value="mostraArchiviati"
            @update:model-value="onMostraArchiviatiChange"
          />
          <span class="text-sm text-(--fg2)">Mostra pazienti archiviati</span>
        </label>
        <button
          type="button"
          class="rounded-lg border border-dashed border-(--dash) px-3 py-2 text-xs font-bold text-(--fg3) hover:border-(--fg4) hover:text-(--fg)"
          @click="pulisciFiltri"
        >
          Pulisci filtri
        </button>
      </div>
    </section>

    <section class="overflow-hidden rounded-2xl border border-(--bd) bg-(--surf)">
      <div v-if="errore" class="flex flex-col items-center gap-3 p-14 text-center">
        <p class="text-(--danger)">Non è stato possibile caricare i pazienti.</p>
        <Button type="button" variant="outline" @click="carica">Riprova</Button>
      </div>

      <div v-else-if="caricamentoIniziale" class="flex flex-col gap-2 p-4">
        <div v-for="n in 6" :key="n" class="h-9 animate-pulse rounded-lg bg-(--hover)" />
      </div>

      <template v-else-if="paginaDati && paginaDati.contenuto.length > 0">
        <div class="overflow-x-auto" :class="{ 'pointer-events-none opacity-60': aggiornamentoInCorso }">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>
                  <button type="button" class="flex items-center gap-1 uppercase tracking-wide text-(--fg4)" @click="ordina('nome')">
                    Paziente
                    <component :is="iconaOrdinamento('nome')" :size="12" :class="ordinaPer === 'nome' ? 'text-(--fg)' : 'text-(--fg4)'" />
                  </button>
                </TableHead>
                <TableHead class="uppercase tracking-wide text-(--fg4)">Contatto</TableHead>
                <TableHead class="uppercase">
                  <button type="button" class="flex items-center gap-1 uppercase tracking-wide text-(--fg4)" @click="ordina('dataNascita')">
                    Età
                    <component :is="iconaOrdinamento('dataNascita')" :size="12" :class="ordinaPer === 'dataNascita' ? 'text-(--fg)' : 'text-(--fg4)'" />
                  </button>
                </TableHead>
                <TableHead>
                  <button type="button" class="flex items-center gap-1 uppercase tracking-wide text-(--fg4)" @click="ordina('statoAccount')">
                    Stato account
                    <component :is="iconaOrdinamento('statoAccount')" :size="12" :class="ordinaPer === 'statoAccount' ? 'text-(--fg)' : 'text-(--fg4)'" />
                  </button>
                </TableHead>
                <TableHead class="text-right uppercase tracking-wide text-(--fg4)">Azioni</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              <TableRow v-for="paziente in paginaDati.contenuto" :key="paziente.id" class="hover:bg-(--soft)">
                <TableCell>
                  <div class="flex items-center gap-2.5">
                    <span class="flex h-9 w-9 items-center justify-center rounded-full font-heading font-semibold" :class="CLASSI_STATO_ACCOUNT[paziente.statoAccount]">
                      {{ paziente.nome[0] }}{{ paziente.cognome[0] }}
                    </span>
                    <div>
                      <router-link :to="`/pazienti/${paziente.id}`" class="font-heading font-semibold text-(--fg)">
                        {{ paziente.nome }} {{ paziente.cognome }}
                      </router-link>
                      <div class="text-xs text-(--fg4)">{{ paziente.email }} · {{ paziente.codiceFiscale }}</div>
                    </div>
                  </div>
                </TableCell>
                <TableCell>{{ paziente.telefono ?? '—' }}</TableCell>
                <TableCell>{{ paziente.dataNascita ? (calcolaEta(paziente.dataNascita) ?? '—') : '—' }}</TableCell>
                <TableCell><Badge :class="CLASSI_STATO_ACCOUNT[paziente.statoAccount]">{{ ETICHETTE_STATO_ACCOUNT[paziente.statoAccount] }}</Badge></TableCell>
                <TableCell class="text-right">
                  <PazienteRigaAzioni
                    :paziente="paziente"
                    :mostra-archiviati="mostraArchiviati"
                    @invita="onInvita"
                    @archivia="onArchivia"
                    @de-archivia="onDeArchivia"
                  />
                </TableCell>
              </TableRow>
            </TableBody>
          </Table>
        </div>
      </template>

      <div v-else-if="!filtriAttivi" class="flex flex-col items-center gap-2 p-16 text-center">
        <p class="font-heading text-lg italic">Nessun paziente presente</p>
        <p class="text-xs text-muted-foreground max-w-xs">Inizia aggiungendo il primo paziente per consultare la sua scheda o invitarlo.</p>
        <Button as-child class="active:not-aria-[haspopup]:translate-y-0.5"><router-link to="/pazienti/nuovo">Nuovo paziente</router-link></Button>
      </div>
      <div v-else class="flex flex-col items-center gap-2 p-16 text-center">
        <p class="font-bold">Nessun risultato trovato</p>
        <p class="text-xs text-muted-foreground max-w-xs">Prova a modificare o resettare i filtri applicati.</p>
        <Button type="button" variant="outline" @click="pulisciFiltri">Pulisci filtri</Button>
      </div>

      <div v-if="paginaDati && !errore" class="flex items-center justify-between gap-3 border-t border-(--div) bg-(--soft) px-4.5 py-3">
        <span class="text-xs text-(--fg3)">{{ conteggioTesto }}</span>
        <div class="flex gap-2">
          <Button type="button" variant="outline" size="sm" :disabled="pagina === 0" @click="paginaPrecedente">Precedente</Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            :disabled="!paginaDati || pagina >= paginaDati.totalePagine - 1"
            @click="paginaSuccessiva"
          >
            Successivo
          </Button>
        </div>
      </div>
    </section>
  </AppShell>
</template>
