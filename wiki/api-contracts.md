---
title: Contratti API
tags: [api]
stato: in-discussione
creato: 2026-08-08
aggiornato: 2026-09-03
fonti: [sorgenti/2026-08-08-scope-e-stack-iniziali.md, sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md, sorgenti/2026-08-09-migrazione-a-repo-unico.md, log.md#2026-09-03-handoff--modifica-anagrafica-paziente-e-creazionemodifica-visita]
---

# Contratti API — hexis-nutrition

Endpoint del sotto-progetto "Fondamenta" (vedi [decisioni/0002](decisioni/0002-autenticazione-e-onboarding.md)): codice scritto e committato in `backend/` (`70e2141`), **coperto da test eseguiti su PostgreSQL reale** il 2026-08-09 (44 test verdi — vedi [stato](stato.md)). Non ancora provato a mano contro l'app in esecuzione.

| Metodo | Path | Ruolo | Descrizione |
|---|---|---|---|
| POST | `/auth/login` | pubblico | Login, restituisce `{token, ruolo}` — JWT con claim di ruolo (PROFESSIONISTA/PAZIENTE) |
| POST | `/auth/password-dimenticata` | pubblico | Richiede reset password; risponde sempre 204 (mai rivela se l'email esiste) |
| POST | `/auth/reset-password` | pubblico (con token) | Imposta nuova password da token di reset; 400 se token non valido/scaduto/usato/tipo errato |
| GET | `/auth/me` | autenticato (PROFESSIONISTA o PAZIENTE) | Restituisce i dati dell'utente autenticato: `{id, nome, cognome, email, ruolo}`; 401 senza token valido. Aggiunto per permettere al frontend di ripristinare la sessione e mostrare il nome utente. |
| POST | `/pazienti` | PROFESSIONISTA | Crea anagrafica paziente **e prima visita** (dati antropometrici, circonferenze, plicometria opzionale), 201; 400 se la visita manca, se altezza/peso al suo interno sono nulli, se `dataNascita` o `sesso` sono nulli (entrambi obbligatori dal 2026-09-01), se `codiceFiscale` è vuoto o non ha 16 caratteri alfanumerici (obbligatorio dal 2026-09-02, non univoco), se la plicometria è richiesta con `sesso: ALTRO`, se mancano pliche obbligatorie per il protocollo scelto, o se manca una riga di coefficienti Durnin-Womersley applicabile all'età |
| GET | `/pazienti` | PROFESSIONISTA | Lista pazienti del professionista autenticato (isolamento multi-tenant) |
| GET | `/pazienti/ricerca` | PROFESSIONISTA | Lista pazienti paginata/filtrata/ordinata (`pagina`, `dimensione`, `ordinaPer`, `direzione`, `ricerca`, `statoAccount`, `sesso`, `dataNascitaDa`/`dataNascitaA`, `archiviato`) — esclude i pazienti archiviati per default, `archiviato=true` mostra solo quelli |
| GET | `/pazienti/{id}` | PROFESSIONISTA | Dettaglio paziente; 404 se appartiene a un altro professionista |
| PUT | `/pazienti/{id}` | PROFESSIONISTA | Aggiorna **solo l'anagrafica** (stessi campi/validazioni di `POST /pazienti` esclusa `visita`); non tocca le visite del paziente; 404 se appartiene a un altro professionista, 400 su validazione. Nessun controllo di unicità sul codice fiscale (stesso comportamento della creazione) |
| GET | `/pazienti/{id}/visite` | PROFESSIONISTA | Storico visite del paziente, ordinato per `dataVisita` crescente; ogni voce include `plicometria` annidata (nullable, presente solo se la plicometria è stata eseguita per quella visita); 404 se il paziente appartiene a un altro professionista |
| POST | `/pazienti/{id}/visite` | PROFESSIONISTA | Crea una nuova visita per un paziente **già esistente** (stesso `VisitaRequest` annidato in `POST /pazienti`), 201; un paziente può avere più visite nel tempo; 404 se il paziente appartiene a un altro professionista, 400 su validazione |
| GET | `/pazienti/{id}/visite/{visitaId}` | PROFESSIONISTA | Dettaglio di una singola visita (usato per precompilare la pagina di modifica); 404 se la visita non esiste o non appartiene a quel paziente/professionista |
| PUT | `/pazienti/{id}/visite/{visitaId}` | PROFESSIONISTA | Aggiorna una visita esistente. Ricalcola sempre BMI/WHR/WHtR/MAMC da zero (azzerati prima del ricalcolo, così un campo rimosso in modifica non lascia un valore "stale"); se la richiesta include `plicometria`, la precedente (se presente) viene eliminata e ricalcolata da zero — stesso comportamento della creazione, applicato in modo speculare; 404 se la visita non esiste o non appartiene a quel paziente/professionista, 400 su validazione |
| POST | `/pazienti/{id}/invito` | PROFESSIONISTA | Genera token invito e invia email; 409 se il paziente è già ATTIVO, 400 se il paziente è archiviato |
| POST | `/pazienti/{id}/archivia` | PROFESSIONISTA | Archivia (soft-delete logico) il paziente, 204; idempotente |
| POST | `/pazienti/{id}/de-archivia` | PROFESSIONISTA | De-archivia il paziente, 204; idempotente |
| POST | `/inviti/{token}/attiva` | pubblico (con token) | Il paziente imposta la password e attiva l'account; 400 se token non valido, 409 se l'email è già in uso da un account attivo |

`POST /pazienti` richiede un oggetto `visita` obbligatorio nel body (altezza e peso obbligatori, le 11 circonferenze sono opzionali, il blocco `plicometria` è opzionale e annidato dentro `visita`) — creato in transazione con l'anagrafica. `sesso` del paziente è obbligatorio (`M`/`F`/`ALTRO`) dal 2026-09-01; la plicometria è disponibile solo per `M`/`F`. Dettagli campo per campo in `VisitaRequest` e `PlicometriaRequest` (`backend/src/main/java/com/hexisnutrition/backend/pazienti/`).

Dal 2026-09-03: `note` opzionale (testo libero) sia sul paziente sia sulla visita; `obiettivo` opzionale sulla visita (`DIMAGRIMENTO`/`AUMENTO_PESO`/`IPERTROFIA`/`RICOMPOSIZIONE`/`MANTENIMENTO`/`EDUCATIVO`/`PREPARAZIONE_SPORTIVA`, default `MANTENIMENTO` se omesso). Nessuna validazione lato server su `note` (stesso gap noto degli altri campi testuali liberi); il frontend applica un limite di 500 caratteri solo lato client.

Dal 2026-09-03 (creazione/modifica visita standalone): `VisitaResponse` include ora anche `protocolloVita` (mancava — necessario per precompilare la modifica). `PlicometriaResponse` annidata in `VisitaResponse` include, oltre ai valori calcolati (`percentualeGrassoCorporeo`/`massaGrassaKg`/`massaMagraKg`/`fmi`/`ffmi`), anche gli **input grezzi**: `protocollo`, `etniaAtleta`, le 9 pliche in mm. In precedenza esponeva solo i risultati calcolati — insufficiente per ripopolare `PlicometriaForm.vue` in modifica, dato che i valori derivati non bastano a ricostruire cosa l'utente aveva effettivamente inserito.

`GET /pazienti/ricerca` è separato da `GET /pazienti` (che resta la lista completa non paginata, usata dalla dashboard): risponde con `{contenuto, paginaCorrente, dimensionePagina, totaleElementi, totalePagine}`, tutti i filtri sono combinati in AND, `dimensione` è clampata lato server tra 1 e 100.

Richiesta/risposta dettagliate (campi, validazioni) sono nel codice: `AuthController`/`PazienteController`/`InvitoController` e i relativi DTO in `backend/src/main/java/com/hexisnutrition/backend/{auth,pazienti,inviti}/`. Nessun endpoint di self-signup professionista esiste (per scelta, vedi ADR 0002).

Quando `frontend-professionisti/` e `frontend-cliente/` inizieranno a consumare questi endpoint, tenere questa tabella allineata a eventuali cambi di contratto.
