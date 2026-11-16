---
title: 0007 - Appuntamenti e visite registrate
tags: [adr, agenda]
stato: stabile
creato: 2026-11-15
aggiornato: 2026-11-15
fonti: [../../docs/superpowers/specs/2026-11-10-agenda-design.md, ../stato.md]
---

# ADR 0007 — Appuntamenti e visite registrate

## Decisione

- **Appuntamento e visita registrata sono due entità diverse.** L'*appuntamento* (tabella `appuntamenti`, V30) è un evento d'agenda: quando, con chi, di che tipo (visita in studio, videoconsulto, promemoria, blocco di fascia oraria). La *visita registrata* (`Visita`) è il rilievo di misure e plicometria. Un appuntamento non contiene misure e una visita registrata non richiede un appuntamento.
- **La conclusione avviene al salvataggio della visita registrata.** "Avvia visita" apre il normale form di visita con `?appuntamentoId=`; salvandolo (`POST /pazienti/{id}/visite?appuntamentoId=`) l'appuntamento diventa `CONCLUSA` e riceve `visita_id`, nella stessa transazione. Non esiste un'azione "segna come conclusa" separata. Se l'appuntamento non è valido per quel paziente (inesistente, di altri, non visita, non `PROGRAMMATA`) la visita **non viene creata**.
- **Sovrapposizioni: un avviso, non un blocco.** Se una visita si sovrappone ad altre visite (programmate o concluse) o a un blocco, il backend risponde 409 con l'elenco dei conflitti; il professionista può cambiare orario o **"Conferma comunque"** (`confermaSovrapposizione=true`). Le annullate non contano; due visite adiacenti (una finisce quando l'altra inizia) non si sovrappongono. Creare un blocco sopra visite esistenti non avvisa.
- **Archiviare un paziente elimina il suo futuro in agenda**: le visite `PROGRAMMATA` e i promemoria da adesso in avanti vengono cancellati. Restano concluse, annullate, blocchi e tutto ciò che è già passato. De-archiviare non ripristina nulla.
- **Data e ora come `DATE` + `TIME` locali (`Europe/Rome`)**, non come istante.
- Le **visite annullate restano visibili** (grigie e barrate) e non si eliminano; si eliminano solo promemoria e blocchi.

## Contesto

Fino al 2026-11-10 l'app non aveva un'agenda: la colonna e il filtro "Prossima visita" della lista pazienti erano predisposti ma vuoti, e "Nuovo appuntamento" era disabilitato. Il design (`docs/superpowers/specs/2026-11-10-agenda-design.md`) è nato da un brainstorming con Andrea a partire dal mockup `Hexis Agenda`; le scelte sopra sono sue, salvo dove indicato in [stato](../stato.md) come proposta dell'agente.

## Alternative considerate

- **Un'unica entità "visita" con stato programmata/conclusa**: scartata. Mescolerebbe un evento di calendario (anche promemoria e blocchi, senza paziente né misure) con un rilievo clinico a campi quasi tutti obbligatori, e avrebbe stravolto `Visita` e tutte le viste che la leggono (andamento, confronto, storico).
- **Rifiutare le sovrapposizioni (409 senza appello)**: scartata. Il professionista può sovrapporre di proposito (due pazienti in sequenza serrata, un videoconsulto durante un blocco): l'app avvisa e decide lui.
- **Mantenere i promemoria e le visite future di un paziente archiviato**: scartata. Resterebbero eventi senza un paziente attivo da seguire, e la "Prossima visita" lo indicherebbe ancora in lista.
- **`TIMESTAMPTZ` per data e ora**: scartata. Un appuntamento è "le 10:00 in studio": un istante UTC richiede conversioni a ogni confronto e sposta l'ora al cambio dell'ora legale. Il costo accettato è un solo fuso per lo studio.
- **Libreria di calendario**: scartata, vista Giorno/Settimana/Mese costruite a mano con Tailwind (festività nazionali italiane calcolate nel frontend).

## Motivazione

Tenere l'appuntamento separato dalla visita registrata lascia invariato tutto il flusso clinico esistente e rende l'agenda un livello leggero sopra di esso. Concludere al salvataggio garantisce che "conclusa" significhi davvero "misure registrate" e che lo stato non si disallinei. L'avviso sulle sovrapposizioni rispetta il lavoro reale dello studio senza togliere la protezione dagli errori. L'eliminazione del futuro alla archiviazione evita eventi orfani e mantiene coerenti "Prossima visita" e filtri.

## Conseguenze

- **Eliminare una visita registrata** collegata lascia l'appuntamento `CONCLUSA` con `visita_id` nullo (`ON DELETE SET NULL`), senza ripristinare "programmata".
- **L'eliminazione alla archiviazione è definitiva**: de-archiviare non recupera visite e promemoria cancellati.
- Un appuntamento modificato dopo l'archiviazione del paziente è rifiutato (400): la verifica "paziente non archiviato" vale anche in modifica.
- **Il 409 è riservato alle sovrapposizioni**; ogni altro errore di dominio è 400. Il client deve mandare `linkVideo: null`, mai `""` (che dà 400).
- Il confronto "da adesso" usa `AgendaTempo.adesso()` (fuso `Europe/Rome`): su un server con un fuso diverso funziona comunque, ma lo studio resta mono-fuso.
- **API e dati**: sezione Agenda in [api-contracts](../api-contracts.md), tabella `appuntamenti` in [modello-dati](../modello-dati.md).
- **Aperto**: lato paziente (richiesta di appuntamento e conferma), email di promemoria (le notifiche per il professionista sono state aggiunte dopo: vedi [0008](0008-notifiche-agenda.md)), ricorrenze e link automatici di videoconsulto sono fuori scope (decisione del design).
