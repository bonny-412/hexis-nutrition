<script setup lang="ts">
// Legenda dei colori degli eventi. Le classi coincidono con quelle usate in AgendaGiorno.
const voci = [
  { etichetta: 'Visita in studio', classe: 'bg-(--mint) border-l-(--green)' },
  { etichetta: 'Videoconsulto', classe: 'bg-(--surf) border-l-(--sage) border-y-(--bd2) border-r-(--bd2)' },
  { etichetta: 'Promemoria', classe: 'bg-(--warn-bg) border-l-(--warn-fg)' },
  { etichetta: 'Conclusa', classe: 'bg-(--hover) border-l-(--fg4) opacity-70' },
  { etichetta: 'Annullata', classe: 'bg-(--hover) border-l-(--danger)/50 opacity-70' },
  { etichetta: 'Blocco', classe: 'border border-dashed border-(--fg4) bg-(--soft)' },
  { etichetta: 'Festività', classe: 'bg-(--warn-bg) border-(--warn-fg)/30' },
]
</script>

<template>
  <div class="flex flex-wrap items-center gap-x-4 gap-y-2 text-xs text-(--fg3)" data-test="agenda-legenda">
    <span v-for="voce in voci" :key="voce.etichetta" class="flex items-center gap-1.5">
      <span class="inline-block h-3 w-3 rounded-[3px] border-l-[3px]" :class="voce.classe" />
      {{ voce.etichetta }}
    </span>
  </div>
</template>
