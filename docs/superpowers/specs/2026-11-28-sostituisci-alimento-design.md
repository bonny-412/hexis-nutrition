# Sostituisci alimento — design

Data: 2026-11-28 · Richiesta di Edoardo ([sorgente](../../../wiki/sorgenti/2026-11-26-richieste-di-edoardo.md)) · Dimensione: piccola modifica, solo frontend.

## Perché

Il piano è già attivo e il paziente dice che un alimento non gli va (es. il pane). Il professionista apre il piano, che è modificabile anche da attivo, e lo cambia con un altro alimento **senza stravolgere il piano** e **senza fare i calcoli a mano**: sceglie quale valore tenere fermo (kcal, proteine, carboidrati o grassi) e l'app calcola i grammi del nuovo alimento che danno lo stesso valore.

## Obiettivi

- Su ogni riga di un pasto (modalità "Pasti") un'azione "Sostituisci alimento" che apre un popup.
- Il popup mostra la riga di partenza con i suoi valori, la scelta del valore da mantenere, la ricerca e, dopo la scelta, la riga del sostituto con i suoi valori; "Sostituisci" chiude il popup e aggiorna la riga in tabella.
- Opzione per sostituire lo stesso alimento in tutte le righe del piano in cui compare.

## Non obiettivi

- Alternative "oppure" nel piano (pane 50 g *oppure* riso 37 g): il sostituto **prende il posto** della riga.
- Modalità "Esempi" e "Macro": per ora solo "Pasti" (gli esempi potranno riusare il popup in futuro).
- Ordinare i candidati per somiglianza sugli altri valori: si usa l'ordine per pertinenza della ricerca.
- Alimenti manuali come sostituti: si cerca solo nel database (BDA + personalizzati).
- Salvataggio automatico o nota di aggiornamento automatica: la sostituzione cambia la pagina, poi si salva con "Salva" come ogni altra modifica.

## Da evitare

- Un sostituto che cambia di molto il pasto senza che il professionista se ne accorga: le differenze sugli altri valori devono essere visibili prima di confermare.
- Toccare backend, API, migrazioni o stampa: non serve.

## Approcci considerati

- **A. Popup nuovo che riusa la ricerca — scelto da Andrea.** `SostituisciAlimentoDialog.vue` nuovo; la logica di ricerca di `AggiungiAlimentoDialog.vue` (debounce 300 ms, `cerca({ ricerca, ordinaPer: 'pertinenza', dimensione: 30 })`, conversione a valori per 100 g) si estrae in un modulo condiviso usato da entrambi; i calcoli in funzioni pure in `utils/`.
- B. Modalità "sostituisci" dentro `AggiungiAlimentoDialog`: meno file, ma un popup già con due schede e il form manuale diventerebbe pieno di condizioni e fragile.
- C. Endpoint backend con i candidati già calcolati: utile solo per un ordinamento per somiglianza, escluso; aggiungerebbe API e test per una divisione.

## Comportamento deciso

**Dove compare.** Un pulsante icona "Sostituisci alimento" accanto alla X di rimozione, su ogni riga di un pasto: nella tabella da 768 px e nella lista compatta su mobile. Non compare in sola lettura (piani conclusi).

**Il popup, dall'alto in basso** (selettore nelle intestazioni del confronto e risultati con i soli grammi: modifiche di Andrea dopo la prova nel browser):

1. **Mantieni uguale**: le intestazioni delle colonne del riquadro di confronto (Kcal, Prot. g, Carb. g, Gras. g) sono il selettore, con lo stesso controllo segmentato di "Tabella/Schede" nelle liste (`SelettoreSegmentato`, con opzioni disattivate): si clicca la colonna da tenere uguale, che resta evidenziata nella riga di partenza e nel sostituto; parte da **Kcal**, si può cambiare in ogni momento e tutto il resto si ricalcola subito. Un valore che nella riga di partenza è 0 (es. grassi dell'albume) è **disattivato**. Se la riga di partenza ha 0 kcal, si parte dal primo valore non a zero; se sono tutti a zero la sostituzione non è possibile e il popup lo dice.
2. **Da sostituire**: nome, grammi e i quattro valori della riga (kcal, proteine, carboidrati, grassi), calcolati sui grammi attuali.
3. **Cerca**: campo di ricerca come in "Aggiungi alimento". Ogni risultato mostra nome, categoria, i **grammi equivalenti** (es. "41 g"); il valore mantenuto non si ripete su ogni risultato, perché è uguale per tutti e si legge nella colonna evidenziata. Un risultato con 0 per il valore scelto resta in lista ma **grigio e non selezionabile**, con la scritta "non ha carboidrati" (o kcal/proteine/grassi).
4. **Sostituto** (dopo aver scelto un risultato): nome, grammi e i quattro valori, con sotto la **differenza** rispetto alla riga di partenza per i tre valori non mantenuti (es. "+8 kcal · −1 g prot."; una differenza che arrotondata fa 0 non si mostra). Il valore mantenuto è evidenziato in entrambe le righe. Scegliere un altro risultato cambia il sostituto.
5. **Casella** "Sostituisci anche nelle altre N righe con «Nome»", **spenta di default**, visibile solo se l'alimento compare in altre righe del piano (qualsiasi giorno e pasto).
6. **Annulla** / **Sostituisci** (quest'ultimo attivo solo con un sostituto scelto).

**Calcolo.** `grammi = valoreDaMantenere / valorePer100gDelSostituto × 100`, dove `valoreDaMantenere` è il valore della riga di partenza ai suoi grammi attuali. Arrotondamento al **grammo intero**, minimo 1 g.

**Sostituzione.** La riga mantiene la sua posizione nel pasto e prende tutti i dati del sostituto (`alimentoId`, nome, i nove valori per 100 g) e i grammi calcolati. Il buffer di digitazione e l'eventuale errore dei grammi di quella riga (`grammiInputPerRiga`, `erroriGrammi`) vanno azzerati, altrimenti il campo mostrerebbe ancora il vecchio valore. Il popup si chiude e la pagina risulta modificata (conferma all'uscita, "Salva" come sempre).

**Con la casella accesa.** Si sostituiscono tutte le righe del piano con lo **stesso alimento**: stesso `alimentoId`, oppure, per un alimento manuale (`alimentoId` nullo), stesso nome. I grammi si ricalcolano **riga per riga** sul valore mantenuto di ciascuna (50 g di pane a colazione e 80 g a cena diventano due quantità diverse di riso). Una riga in cui il valore mantenuto è 0 (es. 0 g in un'altra riga, caso già impedito dalla validazione) si lascia com'è.

**Alimento di partenza manuale.** Si può sostituire come gli altri.

## Design visivo

Segue il sistema esistente dell'app (palette Hexis in `main.css`, `--green`, `--mint`, `--soft`, `--fg2`/`--fg3`, `--div`; titoli in Fraunces con `font-heading`; componenti shadcn: `Dialog`, `Input`, `Button`, `Tabs` o gruppo di toggle per "Mantieni uguale", `Checkbox`). Il dialog è ancorato in alto come "Aggiungi alimento", così non salta quando arrivano i risultati.

L'elemento che deve farsi notare è **il confronto tra le due righe**: "Da sostituire" e "Sostituto" impaginate allo stesso modo, con le stesse colonne allineate, e il valore mantenuto evidenziato (tinta `--mint`/`--green`) nella stessa colonna di entrambe, così l'occhio legge subito "questo resta uguale, questi cambiano di poco". Tutto il resto resta sobrio. Su mobile le due righe vanno a capo come la lista compatta del pasto (nome e grammi sopra, valori sotto).

Testi: "Sostituisci alimento" (titolo e pulsante sulla riga), "Mantieni uguale", "Da sostituire", "Sostituto", pulsante "Sostituisci"; nessun maiuscolo nelle etichette.

## Lasciato a chi costruisce

- Nome e forma del modulo di ricerca condiviso e delle funzioni pure in `utils/`.
- Icona del pulsante sulla riga (es. `ArrowLeftRight` o `Replace` di `@lucide/vue`).
- Testo esatto dei messaggi vuoti ("Scrivi il nome di un alimento…", "Nessun alimento trovato.") ripresi da "Aggiungi alimento".
- Test: da chiedere ad Andrea prima di scriverli.
