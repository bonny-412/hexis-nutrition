# Barra azioni fissa nei form lunghi — piano di implementazione

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** "Annulla"/"Salva" e lo stato "Modifiche non salvate" sempre visibili in fondo a nuovo paziente, visita ed editor piano.

**Architecture:** un componente di presentazione `BarraAzioniForm.vue` con `sticky bottom-0` dentro il `<main>` scorrevole di `AppShell`; ogni view gli passa il proprio stato "modificato" e "in corso" già esistente e gestisce gli eventi come oggi.

**Tech Stack:** Vue 3 + TypeScript, Tailwind v4, shadcn-vue `Button`, `@lucide/vue`.

**Spec:** [`docs/superpowers/specs/2026-11-25-barra-azioni-form-design.md`](../specs/2026-11-25-barra-azioni-form-design.md)

## Global Constraints

- Solo token di `main.css` (ADR 0009), mai `style=""` inline né esadecimali; lime solo come segnale (punto), mai testo.
- `data-test` esistenti invariati: `btn-annulla` (paziente, visita), `annulla-modifiche` (piano).
- Etichette invariate: "Salva paziente", "Salva visita", "Salva"; "Salvataggio…" durante il salvataggio.
- Nessun test nuovo di default (preferenza di Andrea); i test esistenti devono restare verdi. Nessun commit.

## Review Focus

- Fine pagina: la barra non deve coprire l'ultimo campo o "Aggiungi pasto" (la barra sta nel flusso, quindi a fondo scorrimento si appoggia sotto).
- Piano in sola lettura: nessuna barra.
- Piano da mobile a 390 px: riepilogo macro fisso in alto + barra in basso, spazio utile ancora sufficiente.
- Stampa del piano: barra nascosta (`print:hidden`).
- Indicatore: compare alla prima modifica e sparisce tornando allo stato iniziale (solo dove la logica esistente lo prevede).

---

### Task 1: `BarraAzioniForm.vue`

**Files:** Create `frontend-professionisti/src/components/form/BarraAzioniForm.vue`

**Interfaces — Produces:**
- Props: `modificato: boolean`, `inCorso: boolean`, `etichettaSalva: string`, `salvaDisabilitato?: boolean`, `tipoSalva?: 'submit' | 'button'` (default `'submit'`), `dataTestAnnulla?: string` (default `'btn-annulla'`).
- Emits: `annulla()`, `salva()` (solo con `tipoSalva='button'`).

- [ ] Fascia `sticky bottom-0 z-20 -mx-4 -mb-4 border-t border-(--bd) bg-(--surf) px-4 py-3 print:hidden`; a sinistra "Modifiche non salvate" (`text-xs text-(--fg3)`, punto `bg-(--lime)`) con `aria-live="polite"`; a destra "Annulla" (`neutral`, `X`) e Salva (primario, `Save`, testo "Salvataggio…" se `inCorso`). Sotto 640 px: indicatore sopra, pulsanti `flex-1`.

### Task 2: collegare i tre form

**Files:** Modify `views/pazienti/PazienteNuovoView.vue`, `views/pazienti/VisitaFormView.vue`, `views/pianiAlimentari/PianoAlimentareFormView.vue` (sostituire la fila di pulsanti in fondo).

- [ ] Paziente: `:modificato="formValorizzata()"`, `:in-corso="inCorso"`, `etichetta-salva="Salva paziente"`, `@annulla="onAnnullaClick"`.
- [ ] Visita: `:modificato="datiVisitaForm?.modificato() ?? false"`, `etichetta-salva="Salva visita"`, `@annulla="onAnnullaClick"`.
- [ ] Piano: `v-if="!solaLettura"`, `:modificato="modificato"`, `:in-corso="salvataggioInCorso"`, `:salva-disabilitato="!nome.trim()"`, `tipo-salva="button"`, `data-test-annulla="annulla-modifiche"`, `@annulla="annulla"`, `@salva="salva"`.
- [ ] Verifica: `npx vue-tsc --noEmit` pulito; `npx vitest run` → stessi 7 falliti noti, nessuno nuovo.

### Task 3: verifica in browser e documentazione

- [ ] Chiaro, scuro, 390 px sui tre form: barra visibile scorrendo, indicatore alla prima modifica, fine pagina non coperta, piano in sola lettura senza barra.
- [ ] ADR 0009 (regola "Form lunghi"), `stato.md`, `log.md`, review §6 aggiornati. Staging, nessun commit.
