---
title: 0005 - Catalogo Alimenti — tabella unica BDA + custom, ownership nullable
tags: [adr, dati, alimenti, piano-alimentare]
stato: stabile
creato: 2026-09-05
aggiornato: 2026-09-05
fonti: [sorgenti/2026-09-05-brainstorming-alimenti-design.md]
---

# ADR 0005 — Catalogo Alimenti: tabella unica BDA + custom, ownership nullable

## Decisione

- **Una sola tabella** `alimenti`, non due tabelle separate per BDA e custom. La colonna `professionista_id` è **nullable**: `NULL` significa alimento BDA globale (visibile a tutti), valorizzato significa alimento custom di quel professionista (visibile solo a lui e ai suoi pazienti). "È un alimento BDA" si deduce da `professionista_id IS NULL`, senza colonna booleana ridondante.
- Stesso pattern di ownership già in uso per `Paziente`: colonna diretta, nessuna relazione JPA con `Professionista`. **Diversamente da `pazienti.professionista_id`, questa colonna non ha un vincolo FK verso `professionisti`** (vedi sezione Conseguenze per il motivo, emerso in fase di pianificazione).
- Dataset base: **BDA-IEO**, importato dai 1109 file JSON già presenti in `ProgettoEdo/public/foods` (progetto precedente di Andrea), non un nuovo dataset da scegliere — risolve la domanda aperta corrispondente.
- Campi nutrizionali per 100g: `kcal`, `proteine_g`, `grassi_g`, `carboidrati_g` obbligatori; `acqua_g`, `fibre_g`, `zuccheri_g`, `ferro_mg`, `calcio_mg`, `sodio_mg` opzionali. `categoria` è testo libero, non un dominio controllato.
- `codice_bda` (INTEGER, nullable, UNIQUE): id originale del dataset BDA-IEO, presente solo sulle righe seminate, per tracciabilità verso la fonte.
- Import via **migrazione Flyway** con `INSERT` generati da uno script una tantum (fuori dal repo), non un loader Java a startup — coerente con lo stile "solo SQL" già in uso per tutte le migrazioni del progetto.
- **Immutabilità BDA**: le righe con `professionista_id IS NULL` non sono mai modificabili né eliminabili da nessun professionista, applicativamente (nel service, non con un vincolo DB). Un professionista può però "duplicare come personalizzato" un alimento BDA: crea una copia con `professionista_id` = se stesso e nome + " (copia)", editabile da lì in poi.
- **Scope di questa iterazione**: solo `frontend-professionisti` ha una UI (catalogo alimenti). Nessuna vista in `frontend-cliente`, nessun collegamento all'entità "Piano alimentare" (non esiste ancora).

## Contesto

Il modello dati (vedi [modello-dati](../modello-dati.md)) prevedeva da tempo un'entità `Alimento` per il sotto-progetto "Piano alimentare", con la domanda aperta "quale dataset pubblico usare" mai risolta. Andrea ha indicato un vecchio progetto personale (`ProgettoEdo`) che risolve sia il dataset (BDA-IEO, 1109 alimenti già in JSON) sia un mockup UI già pronto per la pagina Alimenti di hexis-nutrition.

## Alternative considerate

- **Due tabelle separate** (`alimenti_bda` sola lettura + `alimenti_custom` per professionista), unite a runtime per la vista lista: scartata. Complica ogni query che deve mostrare "tutti gli alimenti visibili" (richiede sempre una `UNION`), duplica lo schema nutrizionale in due posti, e non porta benefici concreti dato che le regole di immutabilità si possono applicare altrettanto bene nel service con un solo campo nullable — stesso principio già validato per `Paziente`/`professionista_id`.
- **Colonna booleana `is_bda` esplicita** invece di dedurla da `professionista_id IS NULL`: scartata, ridondante — i due stati non possono mai divergere per costruzione (un alimento BDA non ha mai un proprietario), quindi un campo derivato evita un'incoerenza possibile (booleano e proprietario che si contraddicono).
- **Loader Java a startup** che legge i JSON/CSV a runtime: scartata. Introduce un pattern di seeding mai usato nel progetto, richiede portarsi dietro un file dati grande nelle risorse dell'applicazione, e non offre vantaggi concreti rispetto a una migrazione Flyway dato che il dataset BDA non cambia dopo l'import iniziale.

## Motivazione

Coerenza con il pattern di multi-tenancy già stabilito (`professionista_id` come colonna diretta) e con lo stile delle migrazioni (SQL puro, niente logica applicativa di seeding). Una tabella sola con owner nullable è anche il modello più semplice per esprimere "globale vs. privato per professionista", che si ripresenterà probabilmente anche altrove nel prodotto.

## Conseguenze

- **`alimenti` non ha un vincolo FK da `professionista_id` verso `professionisti(id)`, e la tabella è esclusa dal `TRUNCATE` globale di `AbstractIntegrationTest`** (vedi [decisioni/0004](0004-test-su-postgres-locale.md)) — stesso trattamento già riservato a `durnin_womersley_coefficienti` (tabella di riferimento seminata una tantum, mai svuotata dai test). Motivo: `TRUNCATE ... CASCADE` su `professionisti` propaga automaticamente a **qualunque** tabella che la referenzi via FK, anche se non elencata esplicitamente nello statement — con un vincolo FK, le 1109 righe BDA seminate da `V19` verrebbero cancellate al primo test eseguito e mai più ripristinate (Flyway applica le migrazioni di seed una sola volta, all'avvio del contesto Spring, non ad ogni test). Le classi di test che creano alimenti custom durante l'esecuzione puliscono quelle righe da sole (`@AfterEach`, ambito ristretto alla singola classe), per non far crescere indefinitamente la tabella nel database locale persistente.
- `hexis_test` avrà quindi sempre 1109 righe BDA presenti dopo l'applicazione della migrazione di seed, per tutta la vita del database locale.
- Se in futuro servirà aggiornare il dataset BDA (nuova versione, correzioni), non c'è un meccanismo di re-seed: andrà scritta una nuova migrazione additiva. Accettabile per ora, il dataset BDA-IEO 2024 è considerato stabile.
- Nessuna UI paziente né collegamento a "Piano alimentare" in questa iterazione: quando quel sotto-progetto verrà progettato, andrà deciso se una "riga" di piano copia uno snapshot dei valori nutrizionali (come faceva `ProgettoEdo`, vedi [sorgenti/2026-09-05-brainstorming-alimenti-design](../sorgenti/2026-09-05-brainstorming-alimenti-design.md)) o referenzia l'alimento a runtime — non ancora deciso, segnalato in [domande-aperte](../domande-aperte.md).
