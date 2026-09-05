---
title: Contratti API
tags: [api]
stato: in-discussione
creato: 2026-08-08
aggiornato: 2026-09-05
fonti: [sorgenti/2026-08-08-scope-e-stack-iniziali.md, sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md, sorgenti/2026-08-09-migrazione-a-repo-unico.md, log.md#2026-09-03-handoff--modifica-anagrafica-paziente-e-creazionemodifica-visita, decisioni/0005-alimenti-bda-e-custom.md]
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
| GET | `/pazienti/ricerca` | PROFESSIONISTA | Lista pazienti paginata/filtrata/ordinata (`pagina`, `dimensione`, `ordinaPer`, `direzione`, `ricerca`, `statoAccount`, `obiettivo`, `dataUltimaVisitaDa`/`dataUltimaVisitaA`, `archiviato`) — esclude i pazienti archiviati per default, `archiviato=true` mostra solo quelli |
| GET | `/pazienti/{id}` | PROFESSIONISTA | Dettaglio paziente; 404 se appartiene a un altro professionista |
| PUT | `/pazienti/{id}` | PROFESSIONISTA | Aggiorna **solo l'anagrafica** (stessi campi/validazioni di `POST /pazienti` esclusa `visita`); non tocca le visite del paziente; 404 se appartiene a un altro professionista, 400 su validazione. Nessun controllo di unicità sul codice fiscale (stesso comportamento della creazione) |
| GET | `/pazienti/{id}/visite` | PROFESSIONISTA | Storico visite del paziente, ordinato per `dataVisita` crescente; ogni voce include `plicometria` annidata (nullable, presente solo se la plicometria è stata eseguita per quella visita); 404 se il paziente appartiene a un altro professionista |
| POST | `/pazienti/{id}/visite` | PROFESSIONISTA | Crea una nuova visita per un paziente **già esistente** (stesso `VisitaRequest` annidato in `POST /pazienti`), 201; un paziente può avere più visite nel tempo; 404 se il paziente appartiene a un altro professionista, 400 su validazione |
| GET | `/pazienti/{id}/visite/{visitaId}` | PROFESSIONISTA | Dettaglio di una singola visita (usato per precompilare la pagina di modifica); 404 se la visita non esiste o non appartiene a quel paziente/professionista |
| PUT | `/pazienti/{id}/visite/{visitaId}` | PROFESSIONISTA | Aggiorna una visita esistente. Ricalcola sempre BMI/WHR/WHtR/MAMC da zero (azzerati prima del ricalcolo, così un campo rimosso in modifica non lascia un valore "stale"); se la richiesta include `plicometria`, la precedente (se presente) viene eliminata e ricalcolata da zero — stesso comportamento della creazione, applicato in modo speculare; 404 se la visita non esiste o non appartiene a quel paziente/professionista, 400 su validazione |
| DELETE | `/pazienti/{id}/visite/{visitaId}` | PROFESSIONISTA | Elimina definitivamente una visita (204), plicometria associata inclusa (nessun `ON DELETE CASCADE` sulla FK, eliminata esplicitamente prima della visita); nessun vincolo sul numero minimo di visite rimaste — un paziente può restare senza visite, l'app gestisce già quello stato ovunque; 404 se la visita non esiste o non appartiene a quel paziente/professionista |
| POST | `/pazienti/{id}/invito` | PROFESSIONISTA | Genera token invito e invia email; 409 se il paziente è già ATTIVO, 400 se il paziente è archiviato |
| POST | `/pazienti/{id}/archivia` | PROFESSIONISTA | Archivia (soft-delete logico) il paziente, 204; idempotente |
| POST | `/pazienti/{id}/de-archivia` | PROFESSIONISTA | De-archivia il paziente, 204; idempotente |
| POST | `/inviti/{token}/attiva` | pubblico (con token) | Il paziente imposta la password e attiva l'account; 400 se token non valido, 409 se l'email è già in uso da un account attivo |

`POST /pazienti` richiede un oggetto `visita` obbligatorio nel body (altezza e peso obbligatori, le 11 circonferenze sono opzionali, il blocco `plicometria` è opzionale e annidato dentro `visita`) — creato in transazione con l'anagrafica. `sesso` del paziente è obbligatorio (`M`/`F`/`ALTRO`) dal 2026-09-01; la plicometria è disponibile solo per `M`/`F`. Dettagli campo per campo in `VisitaRequest` e `PlicometriaRequest` (`backend/src/main/java/com/hexisnutrition/backend/pazienti/`).

Dal 2026-09-03: `note` opzionale (testo libero) sia sul paziente sia sulla visita; `obiettivo` opzionale sulla visita (`DIMAGRIMENTO`/`AUMENTO_PESO`/`IPERTROFIA`/`RICOMPOSIZIONE`/`MANTENIMENTO`/`PREPARAZIONE_SPORTIVA`/`EDUCATIVO`/`PATOLOGIA_CLINICA`/`GRAVIDANZA_ALLATTAMENTO` — questi ultimi due aggiunti il 2026-09-05, default `MANTENIMENTO` se omesso). Nessuna validazione lato server su `note` (stesso gap noto degli altri campi testuali liberi); il frontend applica un limite di 500 caratteri solo lato client.

Dal 2026-09-03 (creazione/modifica visita standalone): `VisitaResponse` include ora anche `protocolloVita` (mancava — necessario per precompilare la modifica). `PlicometriaResponse` annidata in `VisitaResponse` include, oltre ai valori calcolati (`percentualeGrassoCorporeo`/`massaGrassaKg`/`massaMagraKg`/`fmi`/`ffmi`), anche gli **input grezzi**: `protocollo`, `etniaAtleta`, le 9 pliche in mm. In precedenza esponeva solo i risultati calcolati — insufficiente per ripopolare `PlicometriaForm.vue` in modifica, dato che i valori derivati non bastano a ricostruire cosa l'utente aveva effettivamente inserito.

`GET /pazienti/ricerca` è separato da `GET /pazienti` (che resta la lista completa non paginata, usata dalla dashboard): risponde con `{contenuto, paginaCorrente, dimensionePagina, totaleElementi, totalePagine}`, tutti i filtri sono combinati in AND, `dimensione` è clampata lato server tra 1 e 100.

Dal 2026-09-04: i filtri `sesso` e `dataNascitaDa`/`dataNascitaA` sono stati sostituiti da `obiettivo` e `dataUltimaVisitaDa`/`dataUltimaVisitaA`, entrambi calcolati sull'**ultima visita** del paziente (quella con `dataVisita` massima), non su un campo diretto di `Paziente` — non essendoci relazione JPA tra `Paziente` e `Visita` (FK grezza), sono implementati come subquery correlate in `PazienteSpecifications` (`conObiettivoUltimaVisita`, `conDataUltimaVisitaTra`). Un paziente senza visite non compare mai quando uno di questi due filtri è attivo (la subquery su `MAX(dataVisita)` restituisce `null`, che non soddisfa nessun confronto).

Dal 2026-09-04: `PazienteResponse` (in tutti gli endpoint che lo restituiscono) include anche `obiettivoUltimaVisita` e `dataUltimaVisita`, entrambi nullable — valorizzati solo se il paziente ha almeno una visita, `null` altrimenti. Su `GET /pazienti/ricerca` sono calcolati con un'unica query batch su tutte le visite dei pazienti della pagina corrente (`VisitaRepository.findAllByPazienteIdIn`, ridotta all'ultima per data in `PazienteService.ultimeVisitePerPazienti`), per evitare N+1; sugli altri endpoint (`crea`/`dettaglio`/`aggiorna`) restano sempre `null` (non calcolati, quegli endpoint non hanno bisogno del dato). Usato dalla lista pazienti del frontend per la colonna "Obiettivo".

Richiesta/risposta dettagliate (campi, validazioni) sono nel codice: `AuthController`/`PazienteController`/`InvitoController` e i relativi DTO in `backend/src/main/java/com/hexisnutrition/backend/{auth,pazienti,inviti}/`. Nessun endpoint di self-signup professionista esiste (per scelta, vedi ADR 0002).

Quando `frontend-professionisti/` e `frontend-cliente/` inizieranno a consumare questi endpoint, tenere questa tabella allineata a eventuali cambi di contratto.

## Endpoint del catalogo Alimenti (sotto-progetto "Piano alimentare", primo pezzo)

Vedi [decisioni/0005](decisioni/0005-alimenti-bda-e-custom.md) e [modello-dati](modello-dati.md) per il modello (`alimenti`, tabella unica BDA + custom, `professionista_id` nullable). Consumati solo da `frontend-professionisti/` in questa iterazione; nessuna vista in `frontend-cliente`.

| Metodo | Path | Ruolo | Descrizione |
|---|---|---|---|
| GET | `/alimenti/ricerca` | PROFESSIONISTA | Lista alimenti paginata/filtrata: `pagina` (default 0), `dimensione` (default 20, clampata 1-100), `ordinaPer` (solo `nome`, default `nome`), `direzione` (`asc`/`desc`, default `asc`), `ricerca` (testo libero su nome o categoria, opzionale), `fonte` (`TUTTI`/`BDA`/`PERSONALIZZATI`, default `TUTTI`). Restituisce sempre gli alimenti BDA globali più i soli alimenti custom del professionista autenticato, mai quelli di altri professionisti. 401 senza token valido |
| GET | `/alimenti/{id}` | PROFESSIONISTA | Dettaglio di un alimento (`AlimentoResponse`, include `bda: boolean`); 404 se l'alimento è un custom di un altro professionista o non esiste |
| POST | `/alimenti` | PROFESSIONISTA | Crea un alimento custom del professionista autenticato (mai BDA, `professionista_id` è sempre valorizzato lato server), 201; `nome`/`categoria` obbligatori non vuoti, `quantitaG` obbligatorio (`> 0`, quantità di riferimento in grammi a cui si riferiscono i valori nutrizionali sottostanti — il frontend precompila `100`), `kcal`/`proteineG`/`grassiG`/`carboidratiG` obbligatori (`>= 0`), `acquaG`/`fibreG`/`zuccheriG`/`ferroMg`/`calcioMg`/`sodioMg` opzionali (se presenti, `>= 0`); 400 su validazione |
| PUT | `/alimenti/{id}` | PROFESSIONISTA | Aggiorna un alimento custom **proprio** (stessi campi/validazioni di `POST /alimenti`); 409 se l'alimento è BDA (`AlimentoNonModificabileException`, immutabile per qualunque professionista), 404 se è un custom di un altro professionista o non esiste, 400 su validazione |
| DELETE | `/alimenti/{id}` | PROFESSIONISTA | Elimina definitivamente un alimento custom **proprio**, 204; 409 se l'alimento è BDA, 404 se è un custom di un altro professionista o non esiste |
| POST | `/alimenti/{id}/duplica` | PROFESSIONISTA | "Duplica come personalizzato": crea una copia di un alimento (tipicamente BDA, ma funziona anche su un custom altrui visibile solo se BDA data la regola di visibilità) con `professionista_id` = professionista autenticato, nome originale + `" (copia)"`, stessi valori nutrizionali e `quantitaG`, 201, `bda: false` nella risposta; 404 se l'alimento sorgente non esiste o è un custom di un altro professionista |

Nessun endpoint di ricerca/lettura richiede parametri di query obbligatori oltre all'autenticazione. `AlimentoResponse` espone `quantitaG` (V20, 2026-09-05: quantità di riferimento in grammi, sempre `100` per i dati BDA seminati) più tutti i campi nutrizionali riferiti a quella quantità, e `bda` (derivato da `professionista_id IS NULL`, mai una colonna propria). Richiesta/risposta dettagliate: `AlimentoController`/`AlimentoService` e i DTO in `backend/src/main/java/com/hexisnutrition/backend/alimenti/`.
