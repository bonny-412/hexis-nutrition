---
title: 0011 - Gerarchia dei riquadri (protagonista, strumenti, contesto)
tags: [adr, ui, frontend, layout]
stato: in-discussione
creato: 2026-11-26
aggiornato: 2026-11-26
fonti: [../log.md]
---

# ADR 0011 — Gerarchia dei riquadri

## Contesto

Dopo il terzo giro di revisione UI, quasi ogni contenuto stava in una card bianca con lo stesso bordo, lo stesso arrotondamento e lo stesso spazio interno. In Dashboard c'erano 7 riquadri, nella scheda paziente circa 11. Le card erano coerenti, ma pesavano tutte uguale. Un contatore o "Nessuna plicometria registrata" pesavano quanto gli impegni del giorno o il grafico del peso. Era il punto in cui Hexis somigliava di più a un kit SaaS generico.

## Decisione

Tre livelli, applicati per ora a **Dashboard** e **scheda paziente**:

| Livello | Aspetto | Dove |
|---|---|---|
| Protagonista | card con ombra `--elev` (una per pagina o per tab) | Dashboard: "Prossimi impegni". Panoramica: grafici Peso, BMI, Composizione |
| Strumento | card piatta (bordo, nessuna ombra) | striscia della settimana, "Da fare", "Pazienti da richiamare", Profilo, card Plicometria e Circonferenze con dati |
| Contesto | nessun riquadro, appoggiato allo sfondo, separatori sottili | contatori della Dashboard, fascia degli indicatori del paziente (numeri in Fraunces più grandi) |

Regola: un blocco con **azioni per riga** (pulsanti, "Completa", "Nuovo appuntamento") è uno strumento e resta in card. Il livello contesto è solo per numeri e informazioni da leggere. "Pazienti da richiamare" era stata provata senza riquadro e scartata da Andrea: una lista di righe con pulsanti, senza contorno, sembrava sciolta nella pagina.

Le sezioni vuote (Plicometria e Circonferenze senza dati) diventano un riquadro tratteggiato, come la riga della BIA, invece di una card piena.

La testata petrolio (Dashboard e paziente) resta com'è: è l'identità della pagina, non un riquadro di contenuto.

`--elev` sta in `main.css` in entrambi i temi. In scuro l'ombra è più profonda, perché un'ombra chiara sul petrolio non si vedrebbe.

## Alternative scartate

- **Arrotondamenti diversi per livello**: rimandati. Con ombra e assenza di riquadro la gerarchia si legge già. Cambiare i raggi tocca tutti i componenti shadcn.
- **Rifare tutta l'app in un colpo**: si parte dalle due pagine più viste, poi si estende.

## Stato

`in-discussione`: provato da Andrea stasera mostrando l'app all'amico che finanzia il progetto. Se regge, si estende ad Agenda, liste Pazienti/Piani, editor della visita e del piano.
