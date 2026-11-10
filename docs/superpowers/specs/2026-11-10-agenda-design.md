# Design — Agenda

Data: 2026-11-10
Percorso: architetturale (brainstorming → questo spec → piano di implementazione)
Fonti: mockup `Hexis Agenda.dc.html` (cartella "Hexis Nutrition" scaricata da Andrea, non versionata, materiale grezzo esterno al repo); decisioni prese in conversazione con Andrea il 2026-11-10.
Solo `frontend-professionisti` e `backend`. `frontend-cliente` non esiste e non è coinvolto.

## Obiettivo

Dare al professionista un'agenda: un calendario di appuntamenti con i pazienti e di promemoria personali, in vista Mese / Settimana / Giorno. L'agenda sblocca anche tre cose rimaste in sospeso: la "Prossima visita" in lista pazienti e nel dettaglio paziente, e la data di fine del nuovo piano alimentare precompilata.

## Terminologia (importante: esiste già una "Nuova visita")

- **Appuntamento** (o evento d'agenda): la cosa nuova di questo progetto, che vive nel calendario.
- **Visita registrata**: l'entità `Visita` già esistente (misure, plicometria, note), creata da `VisitaFormView.vue` (`/pazienti/:id/visite/nuova`, `/pazienti/visite/nuova`). Non cambia significato.
- Nel mockup il menu "Aggiungi" dice "Nuova visita": in questo progetto, nell'interfaccia dell'agenda, l'azione che crea un appuntamento di tipo visita va etichettata in modo che non si confonda con la visita registrata (es. "Nuovo appuntamento"). La scelta dell'etichetta esatta è del builder.

## Perimetro

Dentro (tutto il mockup, deciso da Andrea): viste Mese/Settimana/Giorno; filtri Tutti/Visite/Promemoria; appuntamenti di tipo visita in studio e videoconsulto; promemoria con e senza orario, anche senza paziente; blocco di fascia oraria; spostare, annullare, concludere; "Avvia visita".

Fuori (deciso): notifiche ed email di promemoria ai pazienti, campanella dell'header, ricorrenze, generazione automatica di link di videoconsulto o integrazioni Meet/Zoom, lato paziente, stato clinico del paziente (In target / A rischio, scartato da Andrea: lo stato paziente resta solo "mai invitato / invitato / attivo", già implementato), aderenza o feedback del paziente.

## Tipi di evento e dati

Quattro tipi: `VISITA_STUDIO`, `VIDEOCONSULTO`, `PROMEMORIA`, `BLOCCO`. Ogni evento appartiene a un professionista e nessun altro lo vede o lo tocca (404 su eventi di altri, come per pazienti e piani).

- **Visita in studio / videoconsulto**: paziente (obbligatorio, non archiviato), data, ora di inizio, durata in minuti, un testo breve libero (nel mockup "Controllo mensile", "Prima visita"). Stato: `PROGRAMMATA`, `CONCLUSA`, `ANNULLATA`. Il videoconsulto ha in più un **link facoltativo** incollato a mano dal professionista: solo `http://` o `https://`, validato sia nel form sia nel backend, mostrato come voce di menu "Apri videoconsulto" che apre una nuova scheda; se il tipo passa a visita in studio il link viene scartato.
- **Promemoria**: testo, data, ora facoltativa, paziente facoltativo, flag "fatto". Senza ora compare nella fascia in cima alla colonna del giorno (vista Settimana) e nel pannello "Da fare oggi" (vista Giorno).
- **Blocco**: intervallo (data, ora di inizio, durata) e un motivo facoltativo. Nessun paziente.

Il builder decide la forma di tabelle e colonne (una tabella unica con tipo, o più tabelle), il tipo di colonna per data/ora (il progetto usa già `TIMESTAMPTZ` altrove) e la numerazione migrazione (la prossima libera è V30). Fuso orario di riferimento: Europa/Roma.

## Comportamenti decisi

- **Sovrapposizioni**: se una visita (nuova o spostata) si sovrappone a un'altra visita o a un blocco, il sistema **avvisa e il professionista decide** se confermare. Non rifiuta. Il backend riporta gli eventi in conflitto e accetta la richiesta solo con una conferma esplicita; il frontend mostra l'avviso. Le visite annullate non contano come conflitto.
- **Annullare**: solo le visite `PROGRAMMATA`. L'appuntamento **resta visibile in agenda, grigio e barrato**, con etichetta "Annullata" e un accento rossastro tenue per distinguerlo da "Conclusa" (che nel mockup ha lo stesso stile grigio/barrato). Promemoria e blocchi non si annullano: si eliminano.
- **Spostare**: cambia data e/o ora, senza storico degli spostamenti. Solo visite `PROGRAMMATA`.
- **Concludere con "Avvia visita"**: "Avvia visita" apre il form esistente di visita registrata per quel paziente. Quando la visita viene **salvata**, l'appuntamento passa a `CONCLUSA` e resta collegato a quella visita registrata. Un appuntamento `CONCLUSA` non si sposta né si annulla. "Avvia visita" vale anche per i videoconsulti.
- **Promemoria**: "Segna come fatto" (e da fare di nuovo), "Sposta a domani", "Elimina".
- **Archiviare un paziente**: le sue visite future `PROGRAMMATA` (da adesso in avanti) vengono **eliminate**. Quelle concluse, passate o annullate restano. De-archiviare non le ripristina. Anche i **promemoria collegati al paziente** vengono eliminati (deciso da Andrea); applicando la stessa regola delle visite, riguardano quelli da adesso in avanti (interpretazione dell'agente: i promemoria passati non hanno storia da conservare e non vanno toccati senza motivo).
- **Paziente archiviato**: non si possono fissare nuovi appuntamenti per lui.
- **Prossima visita** di un paziente = la prima visita `PROGRAMMATA` con inizio da adesso in avanti. Alimenta: colonna "Prossima visita" e relativo filtro (Tutte / 1 settimana / 2 / 3 / 1 mese, oggi disattivato e non inviato al backend) in `PazientiListView.vue`; la riga "Nessuna visita programmata" in `PazienteDettaglioView.vue`.
- **Data di fine del nuovo piano alimentare**: precompilata con la data della prossima visita del paziente se esiste, altrimenti vuota; resta modificabile. Vale per "Nuovo piano" e, per scelta dell'agente, anche per "Duplica come nuovo piano" (oggi lascia la data vuota). Punto di partenza: `dataFineInput` in `PianoAlimentareFormView.vue`.

## Interfaccia

- **Pagina Agenda** nel frontend professionisti, con voce nella sidebar. Riprende il mockup: titolo "Agenda" in Fraunces corsivo, ‹ Oggi ›, intervallo del periodo, filtri a pillola, selettore Mese/Settimana/Giorno, legenda dei colori, tema chiaro e scuro, weekend sempre visibile.
- **Settimana**: griglia 08:00–20:00 (58 px/ora nel mockup), linea rossa dell'ora corrente nel giorno di oggi, eventi posizionati e dimensionati per durata, promemoria senza orario in cima alla colonna.
- **Mese**: celle con al massimo 2 eventi più "+N altri"; il click su un giorno apre la vista Giorno. Nel mockup le frecce ‹ › in vista Mese non fanno nulla: nella versione reale spostano di un mese.
- **Giorno**: elenco degli impegni a sinistra, pannello "Da fare oggi" a destra, con stato vuoto "Giornata libera".
- **Giorni festivi** (richiesta di Andrea): le feste nazionali italiane sono evidenziate in tutte e tre le viste (intestazione del giorno in Settimana, cella in Mese, titolo in Giorno) con uno sfondo ambrato tenue (token esistenti, anche in tema scuro) e il nome della festa in piccolo. Calcolate nel frontend con una funzione pura, senza backend: 1 gen, 6 gen, 25 apr, 1 mag, 2 giu, 15 ago, 1 nov, 8 dic, 25 dic, 26 dic, più Pasqua e Lunedì dell'Angelo (algoritmo della Pasqua). Restano giorni normali (si possono fissare appuntamenti) e "oggi" mantiene il suo evidenziato verde. Le domeniche non sono evidenziate; il santo patrono locale è fuori perché dipende dalla città dello studio (scelte dell'agente).
- **Legenda**: visita in studio, videoconsulto, promemoria, conclusa, più **annullata** e **blocco** (aggiunte rispetto al mockup).
- **Creazione e modifica** con un dialog (paziente, data, ora, durata, tipo, testo, link se videoconsulto; per il promemoria testo, data, ora facoltativa, paziente facoltativo; per il blocco intervallo e motivo). Il mockup non disegna questi form: li costruisce il builder coi componenti esistenti (Dialog, DatePicker, Select), seguendo la convenzione di validazione delle form del progetto (obbligatori con errore live, numeri con virgola italiana, mai `type="number"`) e il dialog "Uscire senza salvare?" già presente.
- **Punti d'ingresso**: sidebar; "Nuovo appuntamento" nel menu "Crea nuovo" della dashboard (oggi disabilitato) e nella command palette (`CommandPalette.vue`, dove oggi è disabilitato), con anche "Nuovo promemoria" e "Blocca fascia oraria" come azioni della palette; dalla scheda paziente un modo per fissare un appuntamento già con quel paziente.
- Filtro **Visite** mostra visite e blocchi, **Promemoria** solo promemoria, **Tutti** tutto (scelta dell'agente per i blocchi).

## Vincoli

- Convenzioni di progetto: stack esistente (Spring Boot, Flyway, Vue 3, shadcn-vue, Tailwind), nessuno `style="..."` inline ma classi Tailwind, kebab-case nei file della wiki, API in italiano come il resto.
- **Mai `git commit`** (regola di Andrea); solo staging, e segnalare quando è pronto.
- **Test**: non includerli di default nel piano. Andrea preferisce decidere se scriverli lui; adattare solo i test esistenti che si rompono.
- Aggiornare la wiki: `api-contracts.md`, `modello-dati.md`, un ADR sul legame appuntamento↔visita registrata, e `stato.md`/`log.md` a fine sessione.

## Lasciato al builder

Forma di tabelle e colonne, endpoint e contratto API (ricerca per intervallo di date, protocollo di conferma delle sovrapposizioni), struttura dei componenti frontend, etichette esatte, gestione degli orari fuori dalla griglia 08–20 (proposta dell'agente: rifiutarli per visite e blocchi), cosa succede all'appuntamento `CONCLUSA` se la visita registrata collegata viene poi eliminata (proposta: resta `CONCLUSA` con il collegamento azzerato), visite `PROGRAMMATA` con data già passata e mai concluse (proposta: restano così, nessun cambio automatico di stato).
