---
title: Domande aperte
tags: [domande-aperte]
stato: stabile
creato: 2026-08-08
aggiornato: 2026-08-31
fonti: [sorgenti/2026-08-08-scope-e-stack-iniziali.md, sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md, sorgenti/2026-08-09-migrazione-a-repo-unico.md, log.md#2026-08-31-handoff--bug-fix-controlli-pazientenuovoview--ux-maiuscola-errori-live]
---

# Domande aperte — hexis-nutrition

- Perché Spring Boot (vs alternative Node/altro)? Motivazione non raccolta, potrebbe non servire documentarla se ovvia per Andrea.
- Quale dataset pubblico di alimenti usare per il catalogo base (es. CREA, USDA, altro)? Da decidere nel sotto-progetto "Piano alimentare".
- Deploy target (cloud, on-prem, provider) non ancora discusso.
- Con il repo unico ([decisioni/0003](decisioni/0003-repo-unico-per-progetto.md)), se le tre applicazioni dovranno essere rilasciate in modo indipendente servirà una CI con filtri di percorso per non ricostruire tutto a ogni push. Da decidere quando si affronterà il deploy.
- Su quale versione di PostgreSQL girerà la produzione? Sviluppo e test girano sulla **13** installata in locale ([decisioni/0004](decisioni/0004-test-su-postgres-locale.md)). La produzione sarà in Docker, quindi la versione dell'immagine è una scelta libera: usare `postgres:13` allinea i due ambienti, qualsiasi altra versione introduce una differenza che nessun test copre.
- Come sarà fatto il deploy in produzione con Docker (immagini, orchestrazione, provider, gestione delle variabili d'ambiente come `JWT_SECRET` e `RESEND_API_KEY`)? Deciso solo che Docker si userà lì e non in sviluppo.
- Modello dati per i sotto-progetti "Piano alimentare", "Monitoraggio" e "Chat" non ancora dettagliato (solo "Fondamenta" è stato progettato, vedi [decisioni/0002](decisioni/0002-autenticazione-e-onboarding.md)).
- Serve una convenzione esplicita sul non scrivere credenziali nella wiki, nemmeno locali? Sollevato il 2026-08-09 e rimasto senza risposta: le credenziali del Postgres di sviluppo erano riportate in [stato](stato.md), che finisce su GitHub con il resto del repo. Sono innocue finché il database resta su localhost, ma è un'abitudine che non conviene consolidare. Nell'handoff sono state sostituite da un rimando generico; da decidere se basta così o se serve una regola scritta.
- **Validazione server-side incompleta su `CreaPazienteRequest`** (backend): `nome`, `cognome`, `telefono`, `sesso`, `lavoro` non hanno alcun pattern lato server (solo `@NotBlank`/`@Email` dove presenti), a differenza dei campi numerici della visita in `VisitaRequest` (`@Positive`, `@Digits`, `@Max`, ben validati). Chi chiama l'API `/pazienti` bypassando il frontend può quindi inserire qualsiasi carattere in quei campi, anche se `frontend-professionisti/` ora filtra e valida correttamente lato client (bug fix del 2026-08-31, vedi [stato](stato.md)). Segnalato da un fix di UI, non ancora deciso se/quando colmarlo.

## Risolte

- ~~Perché due frontend Vue separati invece di una singola app con routing/permessi per ruolo?~~ Risolto in [architettura](architettura.md).
- ~~Terminologia "paziente" vs "cliente"~~: sono due concetti distinti, non sinonimi. Risolto in [glossario](glossario.md).
- ~~`token_azione`: token in chiaro, nessuna pulizia, reset ripetuti non invalidano i precedenti~~. Risolto il 2026-08-30, vedi [moduli/inviti-e-token](moduli/inviti-e-token.md).
