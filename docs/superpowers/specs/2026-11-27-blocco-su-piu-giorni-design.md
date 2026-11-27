# Blocco su più giorni — design

Data: 2026-11-27. Prima delle tre richieste di Edoardo ([fonte](../../../wiki/sorgenti/2026-11-26-richieste-di-edoardo.md), ordine in [fonte](../../../wiki/sorgenti/2026-11-27-ordine-dei-lavori.md)).

## Perché

Edoardo vuole bloccare l'agenda per ferie e assenze, indicando data e ora "da" e "a". Oggi un blocco è di un solo giorno: ora di inizio e durata da 5 a 480 minuti, dentro la fascia 07:00–20:00. Una settimana di ferie richiede sette blocchi a mano.

Il lavoro va fatto prima di Google Calendar (terza richiesta), così il calendario riceve già i blocchi nella forma definitiva: un evento per blocco, non uno per giorno.

## Obiettivi

- Un blocco ha un **inizio** (data e ora) e una **fine** (data e ora), anche su giorni diversi.
- Una casella **"Giornate intere"** nasconde gli orari e blocca i giorni interi.
- Un **solo form** per tutti i blocchi: anche la pausa pranzo si inserisce con inizio e fine, senza durata.
- Creando o modificando un blocco che copre **visite programmate**, l'app le elenca e chiede conferma. Le visite restano dove sono; le sposta il nutrizionista.
- Il blocco è **un'unica voce**: modificarlo o eliminarlo agisce su tutto l'intervallo.
- Viste: in Giorno e Settimana il blocco riempie le ore coperte di ogni giorno, con il tratteggio di oggi; in Mese e nella striscia della Dashboard i giorni coperti mostrano il motivo (es. "Ferie") al posto di "Libero".

## Non obiettivi

- Spostare o annullare in automatico le visite coperte, o avvisare i pazienti.
- Blocchi ricorrenti (es. pausa pranzo ogni giorno).
- Google Calendar: richiesta separata, dopo.
- Cambiare visite e promemoria: tengono ora di inizio e durata come oggi.

## Anti-obiettivi

- Due modi diversi di inserire un blocco: il form non cambia forma tra blocco breve e lungo.
- Un blocco lungo salvato come tante righe, che costringe ogni funzione a ricostruire la serie.

## Vincoli

- Durata massima di un blocco: **62 giorni**, come l'intervallo massimo di `GET /agenda` (`AgendaService.GIORNI_MASSIMI`).
- Fascia della griglia 07:00–20:00 (`AgendaTempo.INIZIO_GRIGLIA` / `FINE_GRIGLIA`): gli orari di inizio e fine di un blocco non a giornate intere devono starci dentro.
- I blocchi non hanno notifiche (già oggi: `applicaNotifica` le spegne per `BLOCCO`).
- Il backend riavviato applica la migrazione: i blocchi esistenti vanno convertiti, non persi.
- Regole del progetto: pulsanti e controlli shadcn, campi numerici e date come negli altri form, errori live sui campi obbligatori, come nei form paziente e visita; test solo se Andrea li chiede.

## Approcci considerati

1. **Data e ora di fine sulla riga del blocco — scelto.** Nuove colonne su `appuntamenti`, usate solo dai blocchi. Un blocco è una riga; il frontend lo "spezza" per giorno in una sola funzione usata da tutte le viste.
2. **Una riga per giorno, legate da un id di serie.** Viste quasi invariate, ma modifica, eliminazione, dettaglio e Google Calendar devono ricostruire la serie; cambiare l'intervallo aggiunge o toglie righe; una riga eliminata a mano lascia la serie a metà. Scartato: la complessità finisce in ogni punto che tocca il blocco.
3. **Tabella separata `assenze`.** Le ferie diventerebbero un concetto diverso dal blocco, con la pausa pranzo da una parte e le ferie dall'altra. Scartato: va contro il form unico.

Andrea ha scelto la 1 dopo aver chiesto il confronto diretto con la 2.

## Design

### Modello dati (migrazione V34)

Su `appuntamenti`:

- `data_fine DATE` — per i blocchi, il giorno di fine (uguale a `data` per un blocco di un giorno). `NULL` per visite e promemoria.
- `ora_fine TIME` — per i blocchi non a giornate intere, l'ora di fine. `NULL` altrimenti.
- `giornata_intera BOOLEAN NOT NULL DEFAULT FALSE`.

Per un blocco: `data` + `ora_inizio` = inizio, `data_fine` + `ora_fine` = fine, `durata_minuti` = `NULL`. Con `giornata_intera = true`, `ora_inizio` e `ora_fine` sono `NULL` e il blocco copre i giorni da `data` a `data_fine` compresi.

Conversione dei blocchi esistenti nella stessa migrazione: `data_fine = data`, `ora_fine = ora_inizio + durata_minuti`, `durata_minuti = NULL`.

Indice per la lettura per intervallo dei blocchi: `(professionista_id, data_fine)` o equivalente, a scelta del builder.

### API

`AppuntamentoRequest` e `AppuntamentoResponse` guadagnano `dataFine`, `oraFine` (`HH:mm`), `giornataIntera`. Per `BLOCCO`:

- obbligatori `data` e `dataFine`; se `giornataIntera` è falso anche `oraInizio` e `oraFine`;
- `durataMinuti` ignorato in ingresso e `null` in uscita;
- fine strettamente dopo l'inizio (per giornate intere: `dataFine >= data`);
- orari dentro 07:00–20:00;
- al massimo 62 giorni tra `data` e `dataFine`;
- messaggi di errore in italiano come gli altri (`AppuntamentoNonValidoException`, 400).

Per visite e promemoria i nuovi campi sono ignorati in ingresso e `null`/`false` in uscita.

`GET /agenda?da&a` restituisce anche i blocchi **iniziati prima di `da`** che finiscono dentro o dopo (`data <= a AND data_fine >= da`). Il limite di 62 giorni sull'intervallo richiesto resta.

### Sovrapposizioni (409, conferma come oggi)

Si riusa `SovrapposizioneException` e il parametro di conferma già esistente.

- **Visita creata o modificata**: oltre a visite e blocchi dello stesso giorno come oggi, considera i blocchi su più giorni che coprono quel giorno, confrontando l'orario della visita con il tratto del blocco in quel giorno.
- **Blocco creato o modificato** (nuovo): cerca le **visite programmate** che cadono nell'intervallo del blocco; se ce ne sono, 409 con l'elenco. Il frontend mostra la stessa conferma già usata per le sovrapposizioni delle visite ("Questo blocco copre N visite: …"). Le visite concluse o annullate non contano.
- **Modifica di un blocco**: l'avviso compare solo se cambia l'intervallo (date, orari o "Giornate intere"). Correggere solo il motivo non richiede di nuovo la conferma. Serve leggere il vecchio intervallo in `aggiorna` prima di sovrascrivere l'entità. *Scelta di Claude.*

### Il tratto di un blocco in un giorno

Una funzione sola, nel frontend (`utils/`), e la sua gemella nel backend per le sovrapposizioni: dato un blocco e un giorno, restituisce l'orario coperto quel giorno, oppure niente.

- giornate intere → 07:00–20:00 di ogni giorno coperto;
- primo giorno → da `oraInizio` alle 20:00 (se il blocco finisce in un altro giorno);
- giorni in mezzo → 07:00–20:00;
- ultimo giorno → dalle 07:00 a `oraFine`;
- blocco di un giorno → da `oraInizio` a `oraFine`, come oggi.

### Form "Blocca fascia oraria" (`AppuntamentoDialog.vue`)

```
Inizio  [27/11/2026] [13:00]
Fine    [27/11/2026] [14:00]
[ ] Giornate intere
Motivo  [Pausa pranzo        ]
```

- Valori iniziali alla creazione: inizio = giorno e ora di partenza attuali del dialog, fine = stesso giorno, un'ora dopo (come la durata di 60 minuti di oggi).
- "Giornate intere" nasconde i due orari.
- Spostando l'inizio oltre la fine, la fine si sposta con lui mantenendo la stessa durata (call del builder se preferisce un errore).
- Errori live sui campi, come gli altri form.

### Viste

- **Giorno, Settimana**: ogni giorno coperto mostra il tratto del blocco nella griglia, con stile e testo di oggi (`agendaStile.ts`). Cliccandolo si apre il dettaglio dell'unico blocco.
- **Mese**: i giorni coperti contano il blocco e ne mostrano il motivo (o "Bloccato").
- **Dashboard**, striscia della settimana (`SettimanaStrip.vue`, che oggi ignora i blocchi): un giorno conta come coperto **solo se un blocco ne copre tutta la fascia 07:00–20:00** (giornate intere, giorni in mezzo a un blocco lungo, o un blocco di un giorno 07–20). Un giorno coperto senza visite né promemoria mostra il motivo (o "Bloccato") al posto di "Libero"; un giorno coperto con visite o promemoria li mostra come oggi e aggiunge il motivo sotto. I blocchi brevi (es. pausa pranzo) non cambiano la striscia. *Deciso da Andrea.* "Prossimi impegni" tratta il blocco come oggi.
- **Dettaglio** (`AppuntamentoDettaglioDialog.vue`): per un blocco su più giorni, "Dal 4 dic, 13:00 al 9 dic, 18:00", oppure "Dal 4 al 9 dicembre · giornate intere". Per un blocco di un giorno, "13:00–14:00".

### Aggiornamenti della wiki

`modello-dati.md` (nuove colonne), `api-contracts.md` (campi e regole di `/agenda` per i blocchi), voce in `domande-aperte.md` spostata tra le risolte, `stato.md` e `log.md`.

## Lasciato al builder

- Nomi interni di funzioni e componenti, indice esatto.
- Come mostrare un blocco su più giorni in Settimana (un rettangolo per colonna è sufficiente).
- Comportamento del form quando l'inizio supera la fine (spostare la fine o mostrare un errore).
- Casi limite: fine alle 07:00 o inizio alle 20:00, 62 giorni compresi o esclusi (allineato a `GIORNI_MASSIMI`), visite programmate già passate dentro un blocco.
