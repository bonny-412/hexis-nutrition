# Design — Dettaglio paziente: sezione "Andamento" con grafici di confronto

Data: 2026-09-02
Percorso: architetturale (brainstorming → questo spec → piano di implementazione)

## Obiettivo

`PazienteDettaglioView.vue` oggi mostra solo anagrafica di base (nome, email, codice fiscale, stato account) e il bottone invito — nessun dato clinico/antropometrico, nessuno storico visite. Questo giro aggiunge una sezione "Andamento" con grafici a linee nel tempo per peso, BMI e % grasso corporeo, ciascuno con un indicatore di variazione rispetto alla visita precedente.

Il flusso "Nuova visita" su paziente esistente **non esiste ancora** (rimandato esplicitamente nella sessione lista-pazienti del 2 settembre 2026) — di conseguenza oggi ogni paziente ha al più una sola visita, quella creata insieme all'anagrafica. Per scelta di Andrea, questo giro costruisce comunque la UI per lo storico completo (N visite), anche se oggi N=1 per ogni paziente: il flusso "Nuova visita" resta un passo successivo separato, fuori scope qui.

## Decisioni prese in brainstorming

- **Solo dettaglio/confronto in questo giro**, non il flusso "Nuova visita" — le visite esistenti (una per paziente, oggi) bastano a validare la UI; popolare più visite per un paziente, se serve per verifica manuale, si fa a mano su DB.
- **UI pensata per lo storico futuro**: grafici e componenti gestiscono N visite fin da subito, anche con un solo punto dato oggi.
- **Metriche mostrate**: peso, BMI, % grasso corporeo (le tre più lette a colpo d'occhio). Le 11 circonferenze e WHR/WHtR/MAMC restano fuori scope per grafici dedicati in questo giro.
- **Confronto = linea temporale**, non un confronto puntuale a due soli valori: un grafico a linee con un punto per visita: il confronto con la precedente si legge dagli ultimi due punti della linea.
- **Indicatore di variazione numerico**, oltre al grafico: sopra ciascun grafico, valore dell'ultima visita + delta rispetto alla precedente (es. "73.5 kg · -1.5 kg"), colorato in base al segno.
- **Libreria grafici: shadcn-vue Chart (Unovis)**, non Chart.js — coerente con la convenzione del progetto di usare sempre componenti shadcn-vue per i controlli, e con lo stesso sistema di variabili CSS del resto del design system (a differenza di Chart.js, che renderizza su canvas con styling separato via opzioni JS). Si installa con `npx shadcn-vue@latest add chart-line`.

## Stato attuale (per contesto)

- `PazienteDettaglioView.vue`: carica il paziente con `dettaglio(id)` (`api/pazienti.ts`), mostra nome/cognome/email/codice fiscale/stato account + bottone invito/reinvia invito. Nessuna chiamata relativa alle visite.
- `Visita` (backend, entità): `dataVisita`, `altezzaCm`, `pesoKg`, 11 circonferenze opzionali, `protocolloVita`, `bmi`/`whr`/`whtr`/`mamcCm` (calcolati e persistiti in creazione). `VisitaRepository.findAllByPazienteId(UUID)` esiste già, non ordinato esplicitamente, non esposto da nessun controller.
- `Plicometria` (backend, entità, non letta in dettaglio in questa sessione ma nota dalla spec precedente): relazione 1:1 opzionale con `Visita`, contiene `%BF`/massa grassa/massa magra/FMI/FFMI e il flag `limiteSicurezzaApplicato`.
- Nessun endpoint espone oggi le visite di un paziente al frontend.
- Il progetto non ha ancora nessuna libreria di grafici né alcun componente `chart` di shadcn-vue installato.

## Contratto API

Nuovo endpoint, **`GET /pazienti/{id}/visite`** (ruolo PROFESSIONISTA, stesso controllo di ownership di `GET /pazienti/{id}`: 404 se il paziente appartiene a un altro professionista o non esiste).

Risposta: lista di `VisitaResponse`, ordinata per `dataVisita` **crescente** (dalla più vecchia alla più recente — comodo sia per il grafico che per il calcolo del delta lato frontend).

```json
[
  {
    "id": "uuid",
    "dataVisita": "2026-06-01",
    "altezzaCm": 175,
    "pesoKg": 75.0,
    "bmi": 24.5,
    "whr": 0.88,
    "whtr": 0.50,
    "mamcCm": 24.1,
    "circonferenze": { "vitaCm": 88.0, "fianchiCm": 100.0, "...": "..." },
    "plicometria": {
      "percentualeGrassoCorporeo": 18.2,
      "massaGrassaKg": 13.6,
      "massaMagraKg": 61.4,
      "fmi": 4.4,
      "ffmi": 20.1
    }
  }
]
```

`plicometria` è `null` quando la visita non ha una plicometria associata (protocollo non eseguito, o `sesso: ALTRO`). Il blocco `circonferenze` riusa gli stessi nomi già esposti altrove per coerenza, ma non è consumato da questo giro di frontend (solo peso/BMI/%BF lo sono) — incluso per non dover ritoccare il contratto al prossimo giro che li userà.

## Backend

- Nuovo `VisitaResponse` (record), mappato da `Visita` + `Plicometria` opzionale (letta con una query separata o join, a scelta dell'implementer — nessun requisito di performance stringente con un numero di visite per paziente atteso piccolo).
- `VisitaRepository`: nuovo metodo `findAllByPazienteIdOrderByDataVisitaAsc(UUID pazienteId)` (o `Sort` esplicito sul metodo esistente) — non affidarsi all'ordine di default del database.
- `PazienteController`: nuovo metodo `visite(...)` su `GET /pazienti/{id}/visite`, stesso controllo di ownership (`professionistaId` dal JWT vs `paziente.professionistaId`) già usato in `dettaglio(...)`.
- Nessuna migrazione — solo lettura di dati già persistiti.

## Frontend

### API client (`api/pazienti.ts`)

- Nuove interfacce `Visita` e `Plicometria` (solo i campi consumati da questo giro: `dataVisita`, `pesoKg`, `bmi`, `plicometria.percentualeGrassoCorporeo` — non serve tipizzare le circonferenze finché non sono usate).
- Nuova funzione `visite(id: string): Promise<Visita[]>` → `GET /pazienti/{id}/visite`.

### Preparazione dati (testabile, separata dal componente)

Nuova funzione pura `prepareAndamento(visite: Visita[])` in `frontend-professionisti/src/utils/andamento.ts`, che dalla lista di visite (già ordinata cronologicamente) produce, per ciascuna delle 3 metriche (peso, BMI, %BF):

```ts
{
  punti: { data: string, valore: number }[],  // solo visite dove la metrica è disponibile
  ultimo: number | null,
  delta: number | null,        // null se meno di 2 punti disponibili
}
```

Per %BF, `punti` include solo le visite con `plicometria` non nullo — se **nessuna** visita ha plicometria, `punti` è vuoto.

### UI (`PazienteDettaglioView.vue`)

Sotto la sezione anagrafica esistente (invariata), nuova sezione "Andamento" con 3 card (Peso, BMI, % Grasso corporeo), ciascuna:
- valore dell'ultima visita in evidenza + delta rispetto alla precedente (freccia/colore in base al segno; "Prima visita" al posto del delta se `punti.length < 2`)
- grafico a linee shadcn-vue/Unovis sotto, x = `dataVisita`, y = valore

La card "% Grasso corporeo" **non viene renderizzata** se `punti.length === 0` per quella metrica (nessuna visita con plicometria) — non uno stato vuoto esplicito, semplicemente assente, per non occupare spazio con un grafico permanentemente vuoto.

Caricamento: `visite(id)` chiamata in parallelo a `dettaglio(id)` in `onMounted` (`Promise.all`, o due chiamate indipendenti — a scelta dell'implementer, nessuna dipendenza tra le due). Se `visite(id)` fallisce, la sezione "Andamento" mostra un messaggio inline ("Non è stato possibile caricare lo storico visite.") senza bloccare il resto della pagina (l'anagrafica resta visibile anche se lo storico fallisce).

## Fuori scope (esplicito)

- Flusso "Nuova visita" su paziente esistente — resta disabilitato dove già presente (menu riga lista pazienti), non costruito qui.
- Grafici/andamento per circonferenze, WHR, WHtR, MAMC.
- Qualunque azione di modifica/eliminazione di una visita esistente.
- Selezione di un intervallo temporale o filtro sullo storico (si mostrano sempre tutte le visite disponibili).

## Testing (indicazioni per il piano)

**Backend**
- `PazienteControllerTest`: `GET /pazienti/{id}/visite` con 0/1/N visite, ordine cronologico crescente nella risposta, presenza/assenza di `plicometria` annidata, 404 su paziente di un altro professionista, 401 se non autenticato.
- `VisitaRepositoryTest`: nuovo metodo di query ordinata.

**Frontend**
- `utils/andamento.spec.ts`: `prepareAndamento` con 0/1/N visite, delta positivo/negativo/nullo (< 2 punti), filtro plicometria mancente su alcune/tutte le visite.
- `api/pazienti.spec.ts`: nuova funzione `visite()`.
- `PazienteDettaglioView.spec.ts` (esteso): sezione "Andamento" compare con dati mockati, card %BF assente se nessuna plicometria, delta mostrato/non mostrato in base al numero di visite, errore di caricamento storico non blocca l'anagrafica.

Aggiornare `wiki/api-contracts.md` (nuovo endpoint) e `wiki/modello-dati.md` (se rilevante) nello stesso passaggio dell'implementazione, come richiesto da `backend/CLAUDE.md`.
