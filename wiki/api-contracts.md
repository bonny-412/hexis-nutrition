---
title: Contratti API
tags: [api]
stato: in-discussione
creato: 2026-08-08
aggiornato: 2026-08-09
fonti: [sorgenti/2026-08-08-scope-e-stack-iniziali.md, sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md, sorgenti/2026-08-09-migrazione-a-repo-unico.md]
---

# Contratti API — hexis-nutrition

Endpoint del sotto-progetto "Fondamenta" (vedi [decisioni/0002](decisioni/0002-autenticazione-e-onboarding.md)): codice scritto e committato in `backend/` (`70e2141`), **coperto da test eseguiti su PostgreSQL reale** il 2026-08-09 (33 test verdi — vedi [stato](stato.md)). Non ancora provato a mano contro l'app in esecuzione.

| Metodo | Path | Ruolo | Descrizione |
|---|---|---|---|
| POST | `/auth/login` | pubblico | Login, restituisce `{token, ruolo}` — JWT con claim di ruolo (PROFESSIONISTA/PAZIENTE) |
| POST | `/auth/password-dimenticata` | pubblico | Richiede reset password; risponde sempre 204 (mai rivela se l'email esiste) |
| POST | `/auth/reset-password` | pubblico (con token) | Imposta nuova password da token di reset; 400 se token non valido/scaduto/usato/tipo errato |
| POST | `/pazienti` | PROFESSIONISTA | Crea anagrafica paziente, 201 |
| GET | `/pazienti` | PROFESSIONISTA | Lista pazienti del professionista autenticato (isolamento multi-tenant) |
| GET | `/pazienti/{id}` | PROFESSIONISTA | Dettaglio paziente; 404 se appartiene a un altro professionista |
| POST | `/pazienti/{id}/invito` | PROFESSIONISTA | Genera token invito e invia email; 409 se il paziente è già ATTIVO |
| POST | `/inviti/{token}/attiva` | pubblico (con token) | Il paziente imposta la password e attiva l'account; 400 se token non valido, 409 se l'email è già in uso da un account attivo |

Richiesta/risposta dettagliate (campi, validazioni) sono nel codice: `AuthController`/`PazienteController`/`InvitoController` e i relativi DTO in `backend/src/main/java/com/hexisnutrition/backend/{auth,pazienti,inviti}/`. Nessun endpoint di self-signup professionista esiste (per scelta, vedi ADR 0002).

Quando `frontend-professionisti/` e `frontend-cliente/` inizieranno a consumare questi endpoint, tenere questa tabella allineata a eventuali cambi di contratto.
