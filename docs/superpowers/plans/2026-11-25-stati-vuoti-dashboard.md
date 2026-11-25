# Stati vuoti e Dashboard orientata alle azioni — piano di implementazione

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Dashboard con "Da fare" e "Avvia visita", stati vuoti con un'azione in scheda paziente e agenda.

**Architecture:** un componente di presentazione `StatoVuoto.vue` usato ovunque; la Dashboard aggiunge due ricerche esistenti (piani attivi in scadenza, pazienti mai invitati) e sostituisce la card "Piani in bozza" con `DaFareCard.vue`.

**Tech Stack:** Vue 3 + TS, Tailwind v4, shadcn-vue `Button`, `@lucide/vue`.

**Spec:** [`docs/superpowers/specs/2026-11-25-stati-vuoti-dashboard-design.md`](../specs/2026-11-25-stati-vuoti-dashboard-design.md)

## Global Constraints

- Solo token ADR 0009; titoli in Fraunces corsivo (`font-heading italic`); icone 20/40 px per gli stati vuoti; niente maiuscole spaziate.
- Toast dell'invito identici alla scheda paziente: "Invito inviato." / "Non è stato possibile inviare l'invito.".
- Nessuna azione per i pazienti archiviati. Nessun test nuovo di default; test esistenti adeguati se cambia il testo. Nessun commit.

## Review Focus

- Dashboard con una chiamata fallita (es. piani): le altre sezioni restano utilizzabili, la card mostra un messaggio d'errore e non "Niente in sospeso".
- "Invita" premuto due volte: pulsante disabilitato durante l'invio.
- Visita di oggi senza paziente (blocco/promemoria): nessun "Avvia visita".
- Visita di oggi già iniziata/conclusa: solo `PROGRAMMATA` (già filtrato) ha "Avvia visita".
- Settimana con soli giorni festivi o un solo evento: nessun messaggio "vuota".

---

### Task 1: `StatoVuoto.vue`

**Files:** Create `frontend-professionisti/src/components/StatoVuoto.vue`.
**Produces:** props `icona: Component`, `titolo: string`, `testo?: string`, `compatto?: boolean`; slot default per i pulsanti; `data-test="stato-vuoto"`.

### Task 2: Dashboard

**Files:** Create `components/dashboard/DaFareCard.vue`; Modify `views/DashboardView.vue`, `views/DashboardView.spec.ts` se cambiano testi.
**Consumes:** Task 1. **Produces:** `DaFareCard` props `inScadenza: PianoAlimentareRigaLista[] | null`, `bozze: PianoAlimentareRigaLista[] | null`, `maiInvitati: Paziente[] | null`, `totali: { inScadenza: number; bozze: number; maiInvitati: number }`; emits `invitato(id: string)`.

- [ ] Ricerche: `cercaPiani({ stato: 'ATTIVO', dataFineDa: oggi, dataFineA: oggi+7, ordinaPer: 'dataFine', direzione: 'asc', dimensione: 3 })`, bozze come oggi con `dimensione: 3`, `ricerca` pazienti con `statoAccount: 'MAI_INVITATO', archiviato: false, ordinaPer: 'cognome', dimensione: 3`.
- [ ] Gruppi in quest'ordine, solo se con voci, "Vedi tutti" se totale > 3 (piani → `/piani-alimentari`, pazienti → `/pazienti`); tutti vuoti → `StatoVuoto` compatto "Niente in sospeso".
- [ ] "Avvia visita" sulle righe di oggi con `pazienteId` (fuori dal link della riga).
- [ ] Prossime visite vuote → `StatoVuoto` compatto + "Fissa un appuntamento".

### Task 3: scheda paziente

**Files:** Modify `components/pazienti/PazienteTabPianiAlimentari.vue`, `PazienteTabPanoramica.vue`, `PazienteTabStoricoVisite.vue`, `PazienteTabConfrontoVisite.vue` (+ spec se cambiano testi). Le tab ricevono `pazienteId` e `archiviato` se non li hanno già.

- [ ] Piani senza piano attivo né bozza: "Nuovo piano" → `/piani-alimentari/nuovo?pazienteId=`.
- [ ] Panoramica/Storico senza visite: `StatoVuoto` + "Registra la prima visita" → `/pazienti/{id}/visite/nuova`.
- [ ] Confronto < 2 visite: `StatoVuoto` + "Nuova visita".

### Task 4: agenda

**Files:** Modify `components/agenda/AgendaSettimana.vue`, `AgendaGiorno.vue`, `views/agenda/AgendaView.vue` (evento per "Nuovo appuntamento").

- [ ] Settimana: rimuovere "Nessun impegno" per colonna; se nessuna colonna ha eventi, `StatoVuoto` sopra la griglia con "Nuovo appuntamento" (emit `nuovo`).
- [ ] Giorno: "Giornata libera" come `StatoVuoto` con "Nuovo appuntamento".

### Task 5: verifica e documentazione

- [ ] `npx vue-tsc --noEmit` pulito; `npx vitest run` tutto verde (297 attesi, salvo test adeguati).
- [ ] Chrome: Dashboard, scheda paziente senza visite/piani, agenda vuota; chiaro, scuro, 390 px.
- [ ] Revisione indipendente del diff; ADR 0009 (regola stati vuoti), `stato.md`, `log.md`, review §6.
