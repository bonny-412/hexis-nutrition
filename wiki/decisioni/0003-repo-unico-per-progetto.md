---
title: 0003 - Un repo git unico per ogni progetto del workspace
tags: [adr, git, struttura, workspace]
stato: stabile
creato: 2026-08-09
aggiornato: 2026-08-09
fonti: [sorgenti/2026-08-09-migrazione-a-repo-unico.md]
---

# ADR 0003 — Un repo git unico per ogni progetto del workspace

## Decisione

- Ogni progetto sotto `progetti/` è un **unico repo git**, con la radice in `progetti/<nome>/`, **a prescindere da quante applicazioni contiene**: backend, frontend e wiki sono sottocartelle dello stesso repo, non repo separati.
- La regola vale per **tutto il workspace**, progetti presenti e futuri, non solo per hexis-nutrition: `/new-project` inizializza un solo repo, alla radice del progetto.
- Il repo radice del workspace (`bonny-412/workspace2.0`) tiene solo lo scheletro — `CLAUDE.md`, `index.md`, `templates/`, `.claude/` — e ignora `progetti/*/`. I progetti sono repo a sé, **non sottomoduli**.
- Applicata a hexis-nutrition il 9 agosto 2026: i quattro repo GitHub `backend`, `frontend-professionisti`, `frontend-cliente`, `wiki` sono stati sostituiti da un solo repo `bonny-412/hexis-nutrition` e poi archiviati.

## Contesto

Il progetto era nato con quattro repo GitHub indipendenti, come prescriveva la versione precedente dello schema di workspace. Andrea ha chiesto di pubblicare su GitHub il progetto come unità ("hexis-nutrition") invece dei quattro pezzi separati, e ha poi confermato che la regola deve valere per qualunque progetto del workspace, indipendentemente dal suo contenuto.

Al momento della migrazione i quattro repo contenevano 7 commit in tutto, tutti pushati e allineati: il backend di "Fondamenta" con il codice reale, la wiki, e due frontend con il solo `CLAUDE.md` (ancora da scaffoldare).

## Alternative considerate

- **Preservare la storia dei quattro repo con `git subtree add`** (i commit confluiscono nel monorepo, riscritti sotto la rispettiva sottocartella): scartata. I 7 commit esistenti hanno messaggi in gran parte non informativi ("a", "b", "v") e un valore storico basso, non sufficiente a giustificare i merge commit iniziali che il subtree introduce.
- **Lasciare attivi i quattro repo vecchi** accanto al monorepo: scartata — lo stesso codice in due posti, senza indicazione di quale sia quello buono. Sono stati archiviati.
- **Quattro repo come sottomoduli git di un repo `hexis-nutrition`**: non discussa esplicitamente con Andrea, quindi non valutata nel merito. Avrebbe mantenuto la separazione dei repo aggiungendo il costo del coordinamento dei puntatori di sottomodulo, cioè l'opposto dell'obiettivo della decisione.

## Motivazione

Un cambiamento che tocca insieme API, interfaccia e documentazione — il caso normale in questo progetto, dove la wiki va tenuta allineata al codice — sta in un **solo commit**, invece di essere sparso su repo diversi che possono disallinearsi silenziosamente. Con un unico sviluppatore, il costo di coordinare quattro repo non viene ripagato da alcun beneficio: non ci sono team separati, permessi differenziati o cicli di rilascio indipendenti da proteggere.

La separazione **logica** tra le tre applicazioni resta intatta: è la separazione in cartelle, non in repo, a esprimerla (vedi [architettura](../architettura.md)).

## Conseguenze

- La conseguenza di [decisioni/0001](0001-stack-tecnologico.md) che parlava di "tre repository di codice indipendenti da mantenere" è **superata da questa ADR** ed è stata corretta lì.
- Il monorepo parte da un commit iniziale unico (`70e2141`): la storia precedente non è nel repo. Resta leggibile nei quattro repo GitHub archiviati, che vanno quindi conservati e non eliminati se quella storia serve.
- `CLAUDE.md` del workspace e di progetto aggiornati di conseguenza; `progetti/hexis-nutrition/CLAUDE.md` non è più tracciato dal repo radice, appartiene al repo di progetto.
- Una sessione di lavoro aperta su `progetti/hexis-nutrition/` opera dentro un solo repo: `git status`, `git diff` e le review whole-branch vedono backend, frontend e wiki insieme, senza dover ripetere il comando per ogni repo.
- **Non deciso**: se in futuro le tre applicazioni dovranno essere rilasciate in modo indipendente, il repo unico richiederà pipeline CI con filtri di percorso (`paths:`) per non ricostruire tutto a ogni push. Vedi [domande-aperte](../domande-aperte.md).
