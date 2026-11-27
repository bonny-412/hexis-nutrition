<script setup lang="ts">
// Legenda dei colori degli eventi. Le classi coincidono con quelle usate in AgendaGiorno.
const voci = [
  { etichetta: 'Visita in studio', classe: 'bg-(--mint) border-l-(--green)' },
  { etichetta: 'Videoconsulto', classe: 'bg-(--surf) border-y border-r border-l-(--fg) border-y-(--fg4) border-r-(--fg4)' },
  { etichetta: 'Promemoria', classe: 'bg-(--warn-bg) border-l-(--warn-fg)' },
  { etichetta: 'Conclusa', classe: 'bg-(--hover) border-l-(--fg4)' },
  { etichetta: 'Annullata', classe: 'bg-(--hover) border-l-(--danger)/70' },
  { etichetta: 'Blocco', classe: 'border-l-(--fg3) tratteggio-blocco' },
  { etichetta: 'Festività', classe: 'border border-(--warn-fg)/50 bg-(--warn-bg)' },
]
</script>

<template>
  <div class="flex flex-wrap items-center gap-x-5 gap-y-2 px-1 text-xs font-medium text-(--fg2)" data-test="agenda-legenda" role="list" aria-label="Legenda dei colori">
    <span v-for="voce in voci" :key="voce.etichetta" class="flex items-center gap-2" role="listitem">
      <span class="inline-block size-3.5 shrink-0 rounded-[4px] border-l-[3px]" :class="voce.classe" aria-hidden="true" />
      {{ voce.etichetta }}
    </span>
  </div>
</template>
