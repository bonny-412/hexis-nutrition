---
title: Bug del collaudo del 27 novembre 2026
tags: [bug, collaudo, rilascio]
stato: in-discussione
creato: 2026-11-27
aggiornato: 2026-11-27
fonti: [sorgenti/2026-11-27-collaudo-bug.md]
---

# Bug del collaudo del 27 novembre 2026

Andrea ha collaudato l'app (Chrome su `localhost:5173`, Spring Boot, PostgreSQL `hexis_test`) e ha trovato 22 bug. Il report completo, con passaggi, risultati e prove, è in [sorgenti/2026-11-27-collaudo-bug.md](sorgenti/2026-11-27-collaudo-bug.md).

**Da discutere con Andrea prima della messa online** ([decisioni/0012](decisioni/0012-primo-rilascio-solo-area-professionista.md)). Nessuno è ancora corretto né confermato nel codice: questa pagina li riassume e basta. Priorità e correzioni si decidono insieme.

## Elenco

| # | Area | Dove | Sintesi |
|---|---|---|---|
| 01 | Agenda | frontend | Un orario non valido (25:99) in modifica cancella quello salvato, con toast di successo |
| 02 | Alimenti | frontend + backend | Somma dei macronutrienti superiore al peso di riferimento accettata (240 g su 100 g) |
| 03 | Visite | backend | Due visite nello stesso giorno: ordine dello storico diverso da confronto e panoramica; la variazione dipende dalla dimensione della pagina |
| 04 | Piani | frontend | "Stampa PDF": anteprima con i comandi dell'editor, senza intestazione, contenuto tagliato |
| 05 | Ricerca | backend | "Nome Cognome" non trova paziente né piani (solo il cognome funziona); ricerca globale, lista pazienti e lista piani |
| 06 | Editor piano | frontend | Una quantità negativa incollata diventa positiva, senza avviso |
| 07 | Editor piano | frontend + backend | Si salva un alimento con porzione 0 g |
| 08 | Editor piano | frontend | Data inesistente (31/02/2027) segnalata come "obbligatoria" invece che "non valida" |
| 09 | Piani | backend | Il servizio può riattivare un piano concluso (e conclude quello attivo) |
| 10 | Piani | backend + frontend | Il backend accetta fine prima dell'inizio e obiettivo calorico negativo; il form salva 0 kcal |
| 11 | Agenda | backend | `PATCH` "fatto" con JSON vuoto riapre un promemoria completato (200 invece di 400) |
| 12 | Pazienti | backend | Filtro "Obiettivo ultima visita": con due visite lo stesso giorno il paziente compare in entrambi gli obiettivi (stessa radice del 03) |
| 13 | Editor piano | frontend | La sidebar esce dalla bozza senza "Uscire senza salvare?" (il pulsante "Torna ai piani" invece chiede) |
| 14 | Agenda | backend | Videoconsulto: accettato un link `https://?` senza host |
| 15 | Agenda | backend | Durata decimale troncata (15,9 → 15; 480,9 → 480 supera il controllo sul massimo) |
| 16 | Pazienti | frontend + backend | Nome oltre 100 caratteri: nessun errore nel campo, nessun limite nel DTO di modifica |
| 17 | Database | DB | Mancano vincoli `CHECK` su quantità di riferimento, peso visita, grammatura nel piano, intervalli di date di piano e blocco |
| 18 | Piani | backend + frontend | Due schede sulla stessa bozza: l'ultimo salvataggio sovrascrive l'altro senza avviso (niente `@Version`) |
| 19 | Sicurezza | backend | Il reset password non invalida i JWT già emessi (durata 480 minuti) |
| 20 | Inviti | backend | Il link dell'invito usa il dominio segnaposto `app.hexisnutrition.example` invece dell'URL configurato del portale paziente (inviti oggi nascosti, [decisioni/0012](decisioni/0012-primo-rilascio-solo-area-professionista.md)) |
| 21 | Concorrenza | backend | Due attivazioni simultanee possono lasciare due piani attivi |
| 22 | Concorrenza | backend | Due visite sovrapposte create in contemporanea vengono salvate entrambe senza conferma |

## Da decidere

- **Attivare un piano con data di inizio futura** lo rende subito attivo e conclude quello corrente. Il report lo segnala come comportamento da confrontare con le regole prima di chiamarlo bug: va deciso se un piano futuro debba restare in attesa fino alla data di inizio.
- **Ruolo sulle rotte dei piani**: `SecurityConfig` limita pazienti, agenda, alimenti e notifiche al ruolo `PROFESSIONISTA`, i piani no (solo autenticazione). Un paziente riceve 200 con lista vuota o 404, nessun dato esposto. Differenza di configurazione da allineare.

## Verifiche da fare prima del rilascio

Dal report:

1. Test di regressione ripetibili per i casi 18, 19, 21 e 22 dopo le correzioni.
2. Permessi sulle **singole risorse** (lettura e modifica di pazienti, visite, piani e note di un altro professionista): finora sono provate solo le liste.
3. Errori durante il salvataggio: rete interrotta, risposta lenta, doppio clic su "Salva" (al massimo un salvataggio, dati recuperabili).
4. Consegna reale degli inviti, quando la funzione sarà riattivata.

## Cosa il collaudo ha confermato

Visibilità delle liste separata tra due professionisti; 401 sulle rotte senza autenticazione; 400 su login, password dimenticata e reset con JSON vuoto; blocchi su più giorni e sovrapposizioni agli estremi corretti; suite esistenti verdi (Vitest 307, Maven 196, 34 migrazioni Flyway valide).
