# Mobile e pagine di accesso — piano di implementazione

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** agenda e scheda paziente leggibili a 390 px; tre pagine di accesso con lo stesso layout del login.

**Architecture:** classi responsive nei componenti esistenti; un nuovo `AccessoLayout.vue` con il pannello del login, usato dalle tre view di accesso; una regola `:-webkit-autofill` in `main.css`.

**Tech Stack:** Vue 3 + TS, Tailwind v4, shadcn-vue (`Button`, `DropdownMenu`), `@lucide/vue`.

**Spec:** [`docs/superpowers/specs/2026-11-25-mobile-e-accesso-design.md`](../specs/2026-11-25-mobile-e-accesso-design.md)

## Global Constraints

- Breakpoint: agenda mobile `max-width: 767px`; testata paziente compatta sotto `sm` (640 px).
- Solo token ADR 0009; niente `style=""` inline salvo valori dinamici.
- `data-test` esistenti invariati; testi dei toast invariati. Nessun test nuovo di default; nessun commit.

## Review Focus

- Paziente archiviato da mobile: nessun "Nuova visita" né menu, solo "De-archivia".
- Paziente `ATTIVO`: nessun pulsante d'invito; `INVITATO`: "Reinvia invito".
- `matchMedia` assente (jsdom/test): l'agenda parte in Settimana senza errori.
- Login con `?redirect=`: invariato dopo lo spostamento nel layout.
- Reset password senza token o con token scaduto: stesso toast con "Richiedi un nuovo link".

---

### Task 1: agenda da mobile
**Files:** `views/agenda/AgendaView.vue`. `vista` iniziale da `window.matchMedia?.('(max-width: 767px)').matches`.

### Task 2: scheda paziente
**Files:** `views/pazienti/PazienteDettaglioView.vue`, `components/pazienti/PazienteTabPanoramica.vue` (+ spec se cambiano testi).
- [ ] Badge di stato come pulsante d'invito (non `ATTIVO`, non archiviato), pillola accanto al nome rimossa.
- [ ] Sotto `sm`: avatar `size-10`, nome `text-xl`, email nascosta, azioni "Nuova visita" + `DropdownMenu` "⋯" (`aria-label="Altre azioni"`) con "Nuovo piano" e "Fissa appuntamento"; da `sm` i tre pulsanti di oggi.
- [ ] Panoramica: colonna clinica `order-1`, profilo `order-2` a ogni larghezza.

### Task 3: pagine di accesso
**Files:** Create `components/auth/AccessoLayout.vue` (slot default); Modify `views/auth/LoginView.vue`, `PasswordDimenticataView.vue`, `ResetPasswordView.vue`, `assets/main.css`.
- [ ] Layout: pannello del login (con `ShieldCheck` importata) da `md`, logo compatto da mobile, colonna form `max-w-sm`.
- [ ] Le tre view nel layout; campi `h-11 rounded-xl`, pulsanti `h-11 w-full rounded-xl`; conferma invio con `MailCheck`.
- [ ] `main.css`: `input:-webkit-autofill` con `-webkit-box-shadow: 0 0 0 1000px var(--surf) inset` e `-webkit-text-fill-color: var(--fg)`.

### Task 4: verifica e documentazione
- [ ] `npx vue-tsc --noEmit` pulito; `npx vitest run` verde.
- [ ] Chrome 390 px e desktop, chiaro e scuro: agenda, scheda paziente (normale e archiviato), login, password dimenticata, reset.
- [ ] Revisione indipendente; ADR 0009, `stato.md`, `log.md`, review §6.
