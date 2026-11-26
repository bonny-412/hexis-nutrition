<script setup lang="ts">
import { ref } from 'vue'
import { Accordion, AccordionContent, AccordionItem, AccordionTrigger } from '@/components/ui/accordion'
import { Button } from '@/components/ui/button'
import { Activity, Download, FileSpreadsheet, Plus, Upload, X } from '@lucide/vue'

// Solo interfaccia: i file restano in memoria e non vengono letti né salvati con la visita.
// Lettura del CSV e modello dati si decidono quando ci saranno file veri di BodyGram (wiki/stato.md).

const aperto = ref('')
const file = ref<File[]>([])
const errore = ref('')
const trascinando = ref(false)
const inputFile = ref<HTMLInputElement | null>(null)

function eCsv(f: File): boolean {
  return f.name.toLowerCase().endsWith('.csv')
}

function aggiungi(lista: FileList | null | undefined) {
  if (!lista) return
  const scartati: string[] = []
  for (const f of Array.from(lista)) {
    if (!eCsv(f)) {
      scartati.push(f.name)
      continue
    }
    const giaPresente = file.value.some((g) => g.name === f.name && g.size === f.size)
    if (!giaPresente) file.value.push(f)
  }
  errore.value = scartati.length
    ? `${scartati.length === 1 ? `"${scartati[0]}" non è un file CSV` : `${scartati.length} file non sono CSV`}: esporta da BodyGram in formato .csv.`
    : ''
}

function onScelta(evento: Event) {
  const input = evento.target as HTMLInputElement
  aggiungi(input.files)
  input.value = ''
}

function onDrop(evento: DragEvent) {
  trascinando.value = false
  aggiungi(evento.dataTransfer?.files)
}

function togli(indice: number) {
  file.value.splice(indice, 1)
}

function scarica(f: File) {
  const url = URL.createObjectURL(f)
  const link = document.createElement('a')
  link.href = url
  link.download = f.name
  link.click()
  setTimeout(() => URL.revokeObjectURL(url), 0)
}

function formattaDimensione(byte: number): string {
  if (byte < 1024) return `${byte} B`
  return `${(byte / 1024).toLocaleString('it-IT', { maximumFractionDigits: 1 })} KB`
}
</script>

<!-- Testi in <div> e non in <p>: AccordionContent dà mb-4 ai <p> che non sono ultimi, sommato al gap. -->
<template>
  <Accordion v-model="aperto" type="single" collapsible>
    <AccordionItem value="bia" class="overflow-hidden rounded-xl border border-(--bd)">
      <AccordionTrigger class="group px-4 py-3.5 hover:no-underline sm:px-5">
        <div class="flex items-center gap-3">
          <div class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-(--mint) text-(--green-d)">
            <Activity :size="18" />
          </div>

          <div class="flex flex-col items-start">
            <span class="text-sm font-bold text-(--fg)">Bioimpedenziometria (BIA)</span>
            <span class="mt-0.5 text-xs text-(--fg3)">
              {{ file.length ? `${file.length} ${file.length === 1 ? 'file caricato' : 'file caricati'}` : 'File CSV esportati da BodyGram' }}
            </span>
          </div>
        </div>
      </AccordionTrigger>
      <AccordionContent>
        <div class="mx-2 border-t-2 border-t-(--bd)"></div>
        <!-- Si può trascinare su tutta la sezione: con i file già caricati l'area grande sparisce e resta una riga. -->
        <div
          class="relative flex flex-col gap-4 px-6 py-4"
          @dragover.prevent="trascinando = true"
          @dragleave.self.prevent="trascinando = false"
          @drop.prevent="onDrop"
        >
          <input
            ref="inputFile"
            type="file"
            accept=".csv,text/csv"
            multiple
            class="sr-only"
            aria-label="Scegli i file CSV della BIA"
            @change="onScelta"
          />

          <div
            v-if="!file.length"
            data-test="bia-area-caricamento"
            class="flex flex-col items-center gap-2 rounded-xl border border-dashed px-4 py-6 text-center transition-colors"
            :class="trascinando ? 'border-(--green) bg-(--mint)' : 'border-(--bd2) bg-(--soft)'"
          >
            <Upload :size="20" class="text-(--fg3)" aria-hidden="true" />
            <div class="text-sm text-(--fg2)">Trascina qui i file CSV di BodyGram, oppure</div>
            <Button type="button" variant="outline" size="sm" @click="inputFile?.click()">Scegli i file</Button>
          </div>

          <ul
            v-if="file.length"
            class="flex flex-col divide-y divide-(--div) rounded-xl border transition-colors"
            :class="trascinando ? 'border-(--green) bg-(--mint)/40' : 'border-(--bd)'"
          >
            <li v-for="(f, i) in file" :key="`${f.name}-${f.size}`" class="flex items-center gap-3 px-4 py-2.5">
              <span class="flex size-9 shrink-0 items-center justify-center rounded-full bg-(--mint) text-(--green-d)" aria-hidden="true">
                <FileSpreadsheet :size="16" />
              </span>
              <div class="flex min-w-0 flex-1 flex-col">
                <span class="truncate text-sm font-medium text-(--fg)">{{ f.name }}</span>
                <span class="text-xs text-(--fg3)">{{ formattaDimensione(f.size) }}</span>
              </div>
              <div class="-mr-1.5 flex items-center gap-0.5">
                <Button type="button" variant="ghost-primary" size="icon-sm" :aria-label="`Scarica ${f.name}`" :title="`Scarica ${f.name}`" @click="scarica(f)">
                  <Download :size="14" />
                </Button>
                <Button type="button" variant="rimuovi" size="icon-sm" :aria-label="`Togli ${f.name}`" :title="`Togli ${f.name}`" @click="togli(i)">
                  <X :size="14" />
                </Button>
              </div>
            </li>
          </ul>

          <div v-if="file.length" class="-mt-1 flex flex-wrap items-center gap-x-2 gap-y-1">
            <Button type="button" variant="ghost-primary" size="sm" class="-ml-2.5" @click="inputFile?.click()">
              <Plus /> Aggiungi altri file
            </Button>
            <span class="text-xs text-(--fg3)">o trascinali qui</span>
          </div>

          <div v-if="errore" class="text-xs font-medium text-(--danger)" role="alert">{{ errore }}</div>

          <div class="rounded-lg bg-(--warn-bg) px-3 py-2 text-xs text-(--warn-fg)">
            La lettura dei valori dai file arriverà a breve: per ora i file non vengono salvati con la visita.
          </div>
        </div>
      </AccordionContent>
    </AccordionItem>
  </Accordion>
</template>
