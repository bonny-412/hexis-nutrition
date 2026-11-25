# Grafici della scheda paziente — piano di implementazione

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** variazioni colorate secondo l'obiettivo, Confronto visite a barre divergenti, valori sui punti della Panoramica.

**Architecture:** una utility pura per le direzioni per obiettivo, usata da indicatori, tabella e nuovo componente `VariazioniChart.vue` (HTML/CSS); etichette Unovis (`VisScatter.label`) per i grafici di andamento.

**Tech Stack:** Vue 3 + TS, Tailwind v4, `@unovis/vue`.

**Spec:** [`docs/superpowers/specs/2026-11-25-grafici-scheda-paziente-design.md`](../specs/2026-11-25-grafici-scheda-paziente-design.md)

## Global Constraints

- Colori solo da token ADR 0009: `--green` (positivo), `--danger` (negativo), `--fg3` (neutro); serie dei grafici restano `--chart-1..4`.
- Numeri con `formattaNumero` (virgola italiana). Niente maiuscole spaziate, niente gradienti.
- Nessun test nuovo di default; nessun commit.

## Review Focus

- Obiettivo neutro (Mantenimento ecc.): nessun verde/rosso da nessuna parte.
- Valore A = 0 o mancante: riga esclusa dalle barre (nessuna divisione per zero).
- Tutte le variazioni a zero: barre vuote ma etichette "0" leggibili, nessuna scala NaN.
- Etichette lunghe delle circonferenze a 390 px.
- Tema scuro: etichette dei punti leggibili sulle superfici scure.

---

### Task 1: `utils/obiettivo.ts` e indicatori

**Produces:** `type ParametroClinico = 'peso' | 'bmi' | 'percentualeGrasso' | 'massaGrassa' | 'massaMagra' | 'fmi' | 'ffmi' | keyof Visita['circonferenze']`; `type EsitoVariazione = 'positivo' | 'negativo' | 'neutro'`; `direzioneDesiderata(obiettivo: Visita['obiettivo'] | null | undefined, parametro: ParametroClinico): 1 | -1 | 0`; `esitoVariazione(obiettivo, parametro, delta: number | null): EsitoVariazione`; `CLASSE_ESITO: Record<EsitoVariazione, string>`.

- [ ] Tabella delle direzioni come nella spec.
- [ ] `PazienteDettaglioView.vue`: le quattro righe "vs prec." usano `CLASSE_ESITO[esitoVariazione(ultimaVisita?.obiettivo, …, delta)]`.

### Task 2: `VariazioniChart.vue` nel Confronto visite

**Consumes:** Task 1. **Produces:** props `gruppi: { titolo: string; righe: { label: string; parametro: ParametroClinico; a: number | null; b: number | null; unita: string; decimali?: number }[] }[]`, `obiettivo: Visita['obiettivo'] | undefined`.

- [ ] Componente: card "Variazioni", sottotitolo "Rispetto alla visita A, colore secondo l'obiettivo", righe con barra divergente e valore; esclude righe senza entrambi i lati o con A = 0.
- [ ] `PazienteTabConfrontoVisite.vue`: sostituisce i due `ConfrontoBarChart`, colonna Variazione della tabella colorata per esito. Elimina `ConfrontoBarChart.vue` e `ConfrontoBarChart.spec.ts`.

### Task 3: valori sui punti

- [ ] `AndamentoChart.vue`: `label`, `labelColor`, `labelPosition: 'top'` su `VisScatter`; ultimo punto più grande e con unità.
- [ ] `ComposizioneChart.vue`: etichetta solo sull'ultimo punto di ciascuna serie con il valore reale.

### Task 4: verifica e documentazione

- [ ] `vue-tsc --noEmit`, `vitest` (attesi 1 fallito noto in `PianiAlimentariListView.spec.ts`; i 6 di `ConfrontoBarChart.spec.ts` spariscono con il file).
- [ ] Chrome: paziente con 2+ visite, chiaro/scuro/390 px, obiettivo direzionale e neutro.
- [ ] ADR 0009 (regola colori delle variazioni), `stato.md`, `log.md`, review §6.
