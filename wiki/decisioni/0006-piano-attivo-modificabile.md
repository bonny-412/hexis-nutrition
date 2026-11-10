---
title: 0006 - Piano alimentare attivo modificabile
tags: [adr, piano-alimentare]
stato: stabile
creato: 2026-11-10
aggiornato: 2026-11-10
fonti: [sorgenti/2026-11-10-piano-attivo-modificabile.md]
---

# ADR 0006 — Piano alimentare attivo modificabile

## Decisione

- Un piano è modificabile finché il suo **stato effettivo** è `BOZZA` o `ATTIVO`. Solo un piano `CONCLUSO` (`TERMINATO`, oppure `ATTIVO` con `data_fine` passata) è in sola lettura: `PUT /piani-alimentari/{id}` risponde 409 (`PianoAlimentareNonModificabileException`) solo in quel caso.
- Su un piano attivo si può cambiare **tutto**: nome, `data_fine`, `obiettivo_kcal`, modalità (Pasti/Macro/Esempi) e contenuti. Il piano **resta attivo** mentre lo si modifica.
- **Nessuna versione e nessuna "pubblicazione"**: il piano è uno solo e il paziente vede sempre l'ultima versione salvata.
- **`data_inizio` non è modificabile** in nessuno stato (decisione di Andrea, "per il momento"): non è nella richiesta di `PUT`.
- Le **note di aggiornamento** (introdotte il 2026-10-20, vedi [stato](../stato.md)) restano com'erano: si aggiungono solo su piano attivo e si leggono su ogni piano non in bozza. Proposta dell'agente, **confermata da Andrea** il 2026-11-10.
- Un piano **concluso resta in sola lettura**. Proposta dell'agente, **confermata da Andrea** il 2026-11-10: vedi Motivazione.

## Contesto

Dal 2026-10-20 ([stato](../stato.md)) un piano non più in bozza era in sola lettura: per annotare cambiamenti serviva una nota di aggiornamento, ma non si poteva correggere il piano. Il professionista, in base ai feedback del paziente, ha bisogno di **aggiornare il piano direttamente**, senza duplicarlo. Nelle note di quella sessione "prolungare la data di fine di un piano attivo" era già rimasto aperto.

## Alternative considerate

- **Lasciare il piano attivo in sola lettura** con le sole note di aggiornamento (com'era): scartata, non permette di correggere il piano.
- **Versionare il piano** (ogni salvataggio crea una nuova versione, il paziente vede quella "pubblicata"): scartata, più complessa (modello dati, storico, scelta di quale versione mostrare) e non richiesta; Andrea vuole che il paziente veda sempre il piano aggiornato.
- **Rendere modificabile anche il concluso**: scartata. Cambiare la `data_fine` di un piano scaduto lo riporterebbe ad `ATTIVO` di nascosto; per proseguire un piano concluso resta "Duplica come nuovo piano".
- **Rendere modificabile anche `data_inizio`**: rinviata (decisione di Andrea), per ora immutabile dopo la creazione.

## Motivazione

Il piano è uno strumento di lavoro che si affina sul campo. Un solo documento sempre aggiornato è il modello più semplice e coincide con quello che vede il paziente. La regola si appoggia su `statoEffettivo()` (già usato per liste e filtri), non sullo stato persistito, così un piano `ATTIVO` con data di fine passata resta bloccato in modo coerente con il badge "Concluso" mostrato in interfaccia.

## Conseguenze

- **Nessuno storico delle modifiche**: una correzione sovrascrive la versione precedente. Le note di aggiornamento sono l'unico modo di tenere traccia a parole di cosa e perché è cambiato.
- Il paziente vede le modifiche **appena salvate**, senza passaggi intermedi: un errore di salvataggio sul piano attivo è subito visibile al paziente.
- **Interfaccia**: pulsanti "Annulla"/"Salva" in fondo alla pagina del piano; "Annulla" e il link in alto chiedono conferma ("Uscire senza salvare?") se ci sono modifiche non salvate. Il riquadro "sola lettura" compare solo sui concluso.
- **API**: aggiornata la riga del `PUT` in [api-contracts](../api-contracts.md). Nessuna migrazione.
- **Aperto**: la modifica di `data_inizio` su un piano attivo, se servirà (ad esempio per correggere un errore di inserimento), richiederà il campo nella richiesta e le regole di validazione della creazione.