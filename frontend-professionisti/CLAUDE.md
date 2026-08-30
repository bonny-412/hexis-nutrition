# hexis-nutrition — frontend-professionisti

Questa è l'app Vue 3 + TypeScript, Tailwind CSS + shadcn-vue (icone: `@lucide/vue`), per l'area professionisti di hexis-nutrition. I componenti shadcn-vue vivono in `src/components/ui/` (generati via `npx shadcn-vue@latest add <componente>`, non modificarli a mano se non per adattamenti minimi): usali per qualunque nuovo controllo (bottoni, campi, tabelle, menu, dialog...) invece di scrivere markup HTML nativo da zero. I token colore semantici di shadcn (`--primary`, `--background`, ecc., in `src/assets/main.css`) sono rimappati sulla palette brand Hexis (`--green`, `--bg`, ecc.) — non toccare i valori oklch di default se mai rigenerati dal CLI. Contesto completo, decisioni e stato: [`../wiki/`](../wiki/) — leggi `../wiki/index.md` e `../wiki/stato.md` prima di modifiche sostanziali.

Se consumi o modifichi una chiamata API, verifica ed eventualmente aggiorna `../wiki/api-contracts.md`.

Panoramica progetto: [`../CLAUDE.md`](../CLAUDE.md).
