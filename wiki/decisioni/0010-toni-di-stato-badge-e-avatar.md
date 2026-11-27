---
title: 0010 - Toni di stato per badge e avatar
tags: [adr, ui, palette, frontend, liste]
stato: stabile
creato: 2026-11-26
aggiornato: 2026-11-26
fonti: [../../docs/ui-review-2026-11-26-giro3.md, ../../frontend-professionisti/src/utils/toniStato.ts, ../log.md]
---

# ADR 0010 — Toni di stato per badge e avatar

## Contesto

Nel terzo giro di revisione UI (26 novembre 2026) la lista Pazienti era tutta rossa: "Mai invitato" usava `--danger` sia sulla pillola sia sull'avatar. Il rosso nell'app vuol dire già due cose (errore o azione distruttiva; variazione contraria all'obiettivo), e "mai invitato" non è nessuna delle due. Anche "Archiviato" era rosso. I Piani avevano già un loro codice (bozza ambra, attivo menta, concluso grigio), scritto a mano in due file. Nei dati reali "Mai invitato" sarà raro, perché l'invito parte subito dopo la creazione del paziente.

## Decisione

Un solo codice colori per badge e avatar di stato in tutte le liste, in `frontend-professionisti/src/utils/toniStato.ts` (`TONI_STATO`):

| Tono | Significato | Dove |
|---|---|---|
| `daFare` — ambra pieno (`--warn-bg` / `--warn-fg`) | tocca al professionista fare qualcosa | paziente mai invitato, piano in bozza |
| `attivo` — menta pieno (`--mint` / `--green-d`) | in corso, tutto a posto | account attivo, piano attivo |
| `neutro` — a contorno (`--fg3`, bordo `--bd2`, fondo trasparente) | in attesa di altri o concluso | paziente invitato, piano concluso, paziente archiviato |

- **Il rosso non è uno stato**: resta per errori, azioni distruttive e variazioni contrarie all'obiettivo.
- **L'avatar delle liste prende il colore dello stato mostrato nella sua riga** (preferenza di Andrea). Lo stesso paziente può quindi avere colori diversi tra Pazienti (stato account) e Piani (stato del piano): è voluto, e il badge di testo accanto dice sempre perché. Il colore non porta mai l'informazione da solo.
- **Neutro a contorno, non pieno**: `--hover` (#E8F0ED) era quasi uguale a `--mint` (#E0F1EE). La differenza pieno/vuoto si vede anche senza distinguere le tinte.
- **Testata della scheda paziente**: "Mai invitato" è una pillola ambra; "Invitato", "Attivo" e "Paziente archiviato" usano la pillola neutra della testata (`--hero-pill`).
- **Fonte degli alimenti** (non è uno stato, ma usa gli stessi toni): BDA neutro, personalizzato `attivo`. Prima "Personalizzato" era ambra, che ora vorrebbe dire "da fare".

## Conseguenze

- **Dal 2026-11-27** lo stato account è nascosto finché non c'è l'area paziente ([0012](0012-primo-rilascio-solo-area-professionista.md)): nella lista Pazienti, nella ricerca globale e nella scelta paziente gli avatar sono tutti `attivo` (archiviati `neutro`). La lista Piani non cambia.

- Nuovi stati di lista vanno mappati su uno dei tre toni, non su colori nuovi.
- Le card Plicometria e Circonferenze usano la menta per i riquadri degli indici: è un accento di sezione, non uno stato, e resta l'unico colore della card.
- Collegata a [0009](0009-palette-verde-petrolio-lime.md), che definisce i token usati qui.
