---
titolo: Brainstorming — design sezione Alimenti (catalogo BDA + custom)
data: 2026-09-05
tipo: estratto-conversazione
---

# Fonte grezza — non modificare

Sintesi delle decisioni prese con Andrea durante una sessione di brainstorming (skill `superpowers:brainstorming`) sul design della sezione "Alimenti", primo pezzo del sotto-progetto "Piano alimentare".

## Fonti esaminate

- `E:\Progetti_Personali\Frontend\ProgettoEdo` — vecchio progetto frontend con piani alimentari. Cartella `public/foods`: 1109 alimenti BDA-IEO, un file JSON per alimento (`<id>.json`, id numerico) più un indice `foods-index.json`. Schema per alimento: `id`, `nome`, `anno`, `categoria.{codice,nome}`, `parte_edibile_percentuale`, `valori_per_100g.{energia.kcal/kj, proteine_g, lipidi_g, carboidrati_g, fibra_g, acqua_g, alcol, colesterolo_mg, carboidrati_solubili_g}`, `minerali.{ferro_mg, calcio_mg, sodio_mg, ...}`, `zuccheri.{glucosio_g, fruttosio_g, galattosio_g, ...}`, `fonte.{database: "BDA IEO", pdf}`.
- Alimenti custom in ProgettoEdo (`useCustomFoodLibrary.ts`, `CustomFoodDialog.vue`): persistiti in `localStorage`, id negativo per non collidere con gli id BDA, categoria `"Personalizzato"`. Form: nome + quantità in grammi obbligatori; per 100g kcal/proteine/carboidrati/grassi obbligatori (default 0), zuccheri/fibra/ferro/calcio/sodio/acqua opzionali.
- Uso alimenti nei piani (`useWeekPlan.ts`, tipo `Food`): una riga del piano copia uno **snapshot** di `per100g` al momento dell'inserimento (`sourceId` opzionale come riferimento, ma i valori nutrizionali non si aggiornano retroattivamente se l'alimento sorgente cambia dopo).
- Mockup Hexis Nutrition (`Hexis Alimenti.dc.html`, fornito da Andrea): tabella unica BDA+custom, filtri "Tutti/BDA/Personalizzati", colonne Alimento/Fonte/Kcal/Prot/Carb/Grassi, footer con nota "le righe BDA non sono modificabili", menu riga con "Elimina" presente solo se personalizzato, dettaglio BDA in sola lettura con azione "Duplica come personalizzato". Form di creazione/modifica: nome, categoria (testo libero), 7 campi nutrizionali per 100g (kcal, proteine, grassi, carboidrati, acqua, fibre, zuccheri).
- Pattern esistenti hexis-nutrition: ownership via colonna diretta `professionista_id` (niente relazione JPA, vedi `Paziente.java`), autorizzazione nel service (non `@PreAuthorize`, pattern `dettaglio(professionistaId, id)` con 404 se non proprio), migrazioni Flyway sequenziali SQL puro, frontend con service layer diretto (`api/pazienti.ts`) senza Pinia per i dati di dominio, viste lista con ricerca server-side debounced + filtri + paginazione (`PazientiListView.vue`).

## Decisioni prese

1. **Campi nutrizionali**: oltre ai 7 del mockup, includere anche ferro/calcio/sodio (mg) — utili per un futuro modulo di analisi più approfondita o pazienti con esigenze cliniche specifiche. Acqua/fibre/zuccheri/ferro/calcio/sodio opzionali, kcal/proteine/grassi/carboidrati obbligatori.
2. **Categoria**: testo libero (come da mockup), niente dropdown/tabella categorie BDA ufficiali. Nessun vincolo.
3. **Import BDA**: migrazione Flyway con INSERT generati da uno script una tantum (non nel repo) che legge i 1109 JSON di ProgettoEdo. Coerente con lo stile "solo SQL" già in uso per le migrazioni.
4. **Scope frontend-cliente**: fuori scope per questa iterazione. Solo `frontend-professionisti` avrà la UI (catalogo). Il modello dati/API restano già pronti per essere letti in futuro anche dal lato paziente (stessa regola di visibilità), ma nessuna vista cliente viene costruita ora. Nessun collegamento a "Piano alimentare" (non esiste ancora come entità).
5. **Modello dati**: tabella unica `alimenti`, `professionista_id` nullable (NULL = BDA globale, valorizzato = custom di quel professionista) — "è BDA" si deduce da `professionista_id IS NULL`, niente colonna booleana ridondante. Colonna aggiuntiva `codice_bda` (nullable, unique) per tracciabilità verso l'id originale del dataset.
6. **Autorizzazione**: stesso pattern di `PazienteService` — visibile se BDA o proprio; modifica/eliminazione solo se proprio (mai su BDA, `AlimentoNonModificabileException` → 409); "duplica come personalizzato" crea una copia con `professionista_id` = professionista corrente e nome + " (copia)".
7. **Testing**: la migrazione di seed introduce 1109 righe BDA sempre presenti in `hexis_test` — vanno aggiunte al `TRUNCATE` di `AbstractIntegrationTest`, e i test non devono dipendere dal conteggio esatto delle righe BDA (filtrare per `fonte=PERSONALIZZATI` o nomi non ambigui).

## Design approvato

Vedi [decisioni/0005-alimenti-bda-e-custom](../decisioni/0005-alimenti-bda-e-custom.md) per la decisione formalizzata e [modello-dati](../modello-dati.md) per lo schema.
