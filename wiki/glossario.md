---
title: Glossario
tags: [glossario]
stato: in-discussione
creato: 2026-08-08
aggiornato: 2026-11-26
fonti: [sorgenti/2026-08-08-scope-e-stack-iniziali.md, sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md]
---

# Glossario — hexis-nutrition

- **Professionista**: utente dietista/nutrizionista, opera in `frontend-professionisti`; gestisce pazienti, piani alimentari e monitoraggio. Un account SaaS = un professionista (nessun team/studio condiviso).
- **Paziente**: persona seguita da un professionista, opera in `frontend-cliente`. Ha sempre un'anagrafica creata dal professionista; l'account di accesso è opzionale e attivato solo tramite invito.
- **Cliente**: termine *business*, non di dominio — indica il professionista in quanto abbonato pagante del SaaS. **Correzione rispetto alla versione precedente di questa pagina**: "paziente" e "cliente" non sono sinonimi intercambiabili, sono due concetti distinti riferiti a persone diverse. Il nome della cartella `frontend-cliente` resta invariato ma è fuorviante rispetto a questa distinzione (l'app serve i pazienti, non i "clienti" in questo senso) — nota per chi legge il codice.
- **Piano alimentare**: entità centrale assegnata dal professionista a un paziente; strutturato in giorni/pasti/alimenti con calcolo automatico di calorie/macro (dettagli in [modello-dati](modello-dati.md), sotto-progetto "Piano alimentare").
- **Invito**: meccanismo a token+email con cui il professionista attiva l'accesso di un paziente all'app.
- **Fondamenta**: primo sotto-progetto della roadmap di prodotto — autenticazione, anagrafica paziente, invito/onboarding. Vedi [stato](stato.md).
- **Appuntamento** (o evento d'agenda): voce del calendario del professionista (visita in studio, videoconsulto, promemoria, blocco di fascia oraria); tabella `appuntamenti` (vedi [modello-dati](modello-dati.md), [decisioni/0007](decisioni/0007-appuntamenti-e-visite-registrate.md)). **Non** è una visita registrata.
- **Notifica** (dell'agenda): avviso al professionista che una visita o un promemoria con orario sta per iniziare, con un anticipo scelto (15/30/45/60/120 min). Compare nel campanello, con un puntino in sidebar e come push del browser. Vedi [decisioni/0008](decisioni/0008-notifiche-agenda.md).
- **Scatto**: l'istante in cui una notifica diventa dovuta (`inizio dell'evento − anticipo`).
- **Push** (Web Push): avviso di sistema mostrato dal browser anche con Hexis chiuso, tramite service worker e chiavi VAPID. Testo sempre generico.
- **Visita registrata**: l'entità `Visita` con misure antropometriche e plicometria (creata dal form "Nuova visita"). Salvandola da "Avvia visita" conclude l'appuntamento collegato.

## Azioni e diciture dell'interfaccia

Fissate il 2026-11-26 (terzo giro di revisione UI, [`docs/ui-review-2026-11-26-giro3.md`](../docs/ui-review-2026-11-26-giro3.md)). Una stessa azione ha lo stesso nome ovunque; un'azione nuova riusa queste parole.

| Azione | Dicitura | Non usare |
|---|---|---|
| Creare qualcosa (pulsante principale di pagina) | "Nuovo paziente", "Nuovo piano", "Nuovo alimento"; con un menu, "Nuovo ▾" | "Crea nuovo", "Aggiungi" |
| Fissare un appuntamento | "Nuovo appuntamento" | "Fissa appuntamento", "Aggiungi" |
| Aprire una bozza di piano | "Completa" | "Modifica" |
| Copiare un piano | "Duplica piano" | "Rinnova", "Duplica come nuovo piano" |
| Riportare un paziente archiviato | "Ripristina paziente" | "De-archivia" |
| Aprire un paziente | "Apri scheda paziente" (icona cartella nelle tabelle) | "Apri cartella" |
| Cercare in una lista | segnaposto "Cerca …" | "Filtra per …" |
| Conferme distruttive | verbo + oggetto: "Elimina visita", "Archivia paziente" (rosso: cancella i futuri appuntamenti) | "Conferma" |
| Funzioni non ancora disponibili | voce visibile con lucchetto, "in arrivo" | voce cliccabile che non fa nulla |

Termini clinici: **"% massa grassa"** (non "% grasso" né "% grasso corporeo"), **"Macronutrienti"** (non "Macros"), **"Composizione corporea"** per il grafico massa magra / massa grassa.

