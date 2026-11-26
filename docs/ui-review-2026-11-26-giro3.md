# Revisione UI — terzo giro (26 novembre 2026)

Giro visivo nel browser integrato dell'app (Claude in Chrome non era collegato), con la skill **frontend-design**. Pagine: accesso, Dashboard, Pazienti (tabella e schede), scheda paziente (4 tab), Piani alimentari (lista ed editor), Alimenti, Agenda (Settimana), ricerca globale, notifiche. Desktop 1440 px in chiaro, scheda paziente anche in scuro e a 390 px.

Base di partenza: [`ui-review-2026-11-25-giro2.md`](ui-review-2026-11-25-giro2.md) e le correzioni del commit `7ae8c61`.

## Verdetto

| Qualità | Giudizio | Rispetto al giro 2 |
|---|---|---|
| **Coerente** | Quasi | Sistemate la ricerca globale (frecce e Invio funzionano), la barra laterale compressa, la pillola unica stato/invito, lo stato vuoto delle notifiche. Restano: colori di stato usati per significati diversi, nomi delle azioni, etichette in maiuscolo nel Profilo. |
| **Moderna** | Sì | Paginazione "Precedente/Successivo" ancora presente anche con una sola pagina. |
| **Chiara** | Quasi | La lista Pazienti tutta rossa comunica un'emergenza che non c'è; la card Circonferenze è per lo più trattini. |
| **Accattivante** | Sì | La testata petrolio con Fraunces corsivo regge bene in chiaro, scuro e mobile. Il punto debole sono i grafici piccoli con etichette che si sovrappongono. |

## Da fare, in ordine di impatto

### 1. Il rosso è usato per "mai invitato" — la lista Pazienti sembra in errore
In `utils/paziente.ts:10` lo stato `MAI_INVITATO` usa `--danger`, e lo stesso colore va **sia sulla pillola sia sull'avatar**. Con 10 pazienti su 10 mai invitati, l'intera lista è rossa. "Mai invitato" non è un errore: è lo stato iniziale normale.
- Proposta: rosso solo per errori e azioni distruttive. "Mai invitato" neutro (superficie `--soft`, testo `--fg2`), "Invitato" ambra tenue (in attesa), "Attivo" verde.
- L'avatar non deve codificare lo stato: oggi lo stesso paziente ha l'avatar rosso in Pazienti e ambra in Piani (colore della bozza). Avatar sempre nello stesso colore neutro, o un colore stabile derivato dal nome.

### 2. Card Circonferenze — vedi proposta dedicata sotto

### 3. Colori delle variazioni non allineati all'obiettivo
Il 25 novembre si è deciso che le variazioni si colorano secondo l'obiettivo del paziente (`utils/obiettivo.ts`, `esitoVariazione`). Il Confronto lo fa; lo **Storico visite no** (`PazienteTabStoricoVisite.vue:203` e `:219`: calo = verde, aumento = rosso a prescindere). Per un paziente in "Aumento peso" un +1 kg appare rosso nello Storico e verde nel Confronto.

### 4. Glossario delle azioni (ancora aperto dal giro 2)
- Pulsante principale delle pagine: "Crea nuovo ▾" (Dashboard), "Aggiungi ▾" (Agenda), "Nuovo paziente", "Nuovo piano", "Nuovo alimento". Proposta: sempre "Nuovo …"; dove c'è un menu, "Nuovo ▾".
- Stessa bozza di piano: "Completa" in Dashboard, "Modifica" nella tab Piani della scheda.
- Ritorno: "Torna alla lista pazienti" nella scheda, "Torna indietro" nell'editor del piano. Proposta: dire sempre dove si torna.
- Ricerca: "Filtra per nome, email o codice fiscale…" (Pazienti), "Cerca paziente o piano…" (Piani), "Cerca alimento o categoria…" (Alimenti). Proposta: "Cerca …" ovunque.
- Terminologia: "% Massa grassa" (Panoramica), "% Grasso" (Storico, legenda grafici), "% Grasso corporeo" (Confronto). Sceglierne una.
- Editor piano: "Macros" (inglese, plurale); "Prot. / Carb. / Grassi" (due abbreviati, uno no).

### 5. Grafici dell'andamento
- Con due visite vicine le date dell'asse si sovrappongono ("08/09/2610/10/26").
- Le etichette dei valori toccano la linea e il punto.
- La didascalia dice "Ultime 3 visite" su Peso/BMI e "Ultima visita" su Massa magra quando la plicometria c'è in una sola visita: corretto, ma visivamente il terzo grafico è un punto isolato accanto a due linee. Con un solo valore meglio il messaggio "compare dalla seconda plicometria", come già si fa per le visite.
- Peso con due decimali sui punti ("80,00") e uno nella striscia ("79,50 kg"): uniformare.

### 6. Profilo del paziente
Etichette in maiuscolo con spaziatura ("CODICE FISCALE", "DATA DI NASCITA") e Title Case ("Codice Fiscale" nel sorgente), mentre tabelle e resto dell'app sono stati portati in minuscolo il 16 novembre. Proposta: etichette in minuscolo, `text-xs text-(--fg3)`.

### 7. Testata della scheda paziente
- In tema scuro i pulsanti secondari ("Nuovo piano", "Nuovo appuntamento") sono bianchi pieni: i due elementi più luminosi della pagina dopo il lime.
- A 1440 px i pulsanti finiscono sotto il nome per Luca Bianchi e a destra per Beatrice De Luca: dipende dalla lunghezza della pillola della prossima visita. Fissare la posizione (sempre a destra da `xl`, sempre sotto prima).
- Cambiando tab la testata si sposta di qualche pixel: compare e scompare la barra di scorrimento. `scrollbar-gutter: stable` sul `main`.

### 8. Rifiniture
- Voci "Chat" e "Analytics" ancora testo semplice nella barra laterale (giro 2, punto 1).
- Paginazione visibile anche con una sola pagina ("Mostrati 1-2 di 2 piani", "Precedente/Successivo" disattivati).
- Due stili di selettore a segmenti: Mese/Settimana/Giorno con attivo verde pieno, Tabella/Schede con attivo menta.
- Sottotitoli da template ("Gestisci la lista dei pazienti, gli inviti e le cartelle cliniche.", "Gestisci i piani alimentari dei tuoi pazienti.").
- Pagina di accesso: pillola "Piattaforma clinica avanzata" decorativa e sottotitolo da brochure ("…pensata per valorizzare la tua professione ogni giorno").
- Icona cartella per "Apri piano" e "Apri cartella": il nome della riga è già un collegamento; un'icona a freccia/chevron o nessuna.
- Editor piano: la "×" rossa per togliere un alimento da ogni riga; neutra, rossa solo al passaggio del mouse.
- Ricerca globale: "luca" trova anche Beatrice De Luca senza dire perché (cognome); mostrare il campo che combacia.
- Dati di prova visibili in Agenda ("QA Chrome 25-1…", "Test Promemoria").

## Proposta: card Circonferenze

**Problema.** La card elenca sempre le 12 circonferenze. Per Beatrice De Luca 10 righe su 12 sono "—": la card è la più alta della pagina e la meno informativa. Le etichette "(non dominante)/(dominante)" ripetute raddoppiano la lunghezza delle righe. Non c'è confronto con la visita precedente, a differenza della striscia di indicatori in alto.

**Proposta.**
1. **Solo le misure rilevate.** Sotto, una riga "+ 10 non rilevate" che le mostra se serve.
2. **Due gruppi**: *Tronco* (vita, fianchi, addome, torace) e *Arti*.
3. **Arti come piccola tabella a due colonne**, "Non dom." e "Dom.": una riga per distretto (braccio rilassato, braccio contratto, coscia, polpaccio). Si vede subito l'asimmetria.
4. **Variazione rispetto alla visita precedente** accanto a ogni valore, colorata secondo l'obiettivo (`esitoVariazione`), come nella striscia in alto.
5. **Due indici derivati in testa**, perché sono quelli che si leggono davvero: **rapporto vita/fianchi** (soglia OMS: 0,85 donne, 0,90 uomini) e **rapporto vita/altezza** (soglia 0,5). Un punto colorato indica sopra o sotto soglia. Le soglie andrebbero confermate da te prima di implementarle.
6. Stato vuoto con `StatoVuoto` e azione "Registra circonferenze" che apre la nuova visita.

Da decidere: i due indici derivati (sì/no, e se le soglie OMS vanno bene).

### Fatto il 26 novembre (dopo il giro)

Decisioni di Andrea: **niente soglie** sugli indici (sono riferimenti di letteratura, non dati del paziente) e **niente variazioni** nelle card (c'è la tab Confronto).

- Nuove `CirconferenzeCard.vue` e `PlicometriaCard.vue`, con la stessa struttura: indici in testa (vita/fianchi e vita/altezza dal backend; FMI e FFMI), poi i valori, solo le misure rilevate. Gli arti stanno in una tabella non dominante/dominante, le misure mancanti in una riga in fondo ("Non rilevate: addome, torace e arti"). La plicometria mostra le pliche misurate e il protocollo. Centimetri con un decimale.
- Usate in Panoramica, nel dettaglio dello Storico visite (variante `annidata`) e nel riepilogo della visita precedente accanto al form di visita.
- Confronto visite: tolte le circonferenze assenti in entrambe le visite. Il grafico a barre non compare se nessuna misura è presente in tutte e due (prima mostrava una card con il solo messaggio "Nessuna misura presente in entrambe le visite").
- `ETICHETTE_PLICHE` ed `ETICHETTE_PROTOCOLLO_PLICOMETRIA` spostate in `utils/visita.ts`.
- Nuova `ProfiloPazienteCard.vue` (punto 6): solo i campi compilati, etichette in minuscolo, "Non indicati: …" in fondo. Livello di attività in una riga sua (prima "— · Poco attivo"). Storia e terapie con un titolo; se sono vuote, una riga con "Aggiungi". "Modifica" nella testata della card. Sia "Modifica" sia "Aggiungi" aprono il dialog dei dati anagrafici.
- Test adeguati: 3 asserzioni sul nuovo formato e l'esempio del grafico del Confronto con dati in entrambe le visite. `vue-tsc` pulito, `vitest` 301/301.

- Toni di stato unici ([decisione 0010](../wiki/decisioni/0010-toni-di-stato-badge-e-avatar.md), `utils/toniStato.ts`): "Mai invitato" e "Bozza" ambra (da fare), "Attivo" menta, "Invitato", "Concluso" e "Archiviato" neutri a contorno. Il rosso non è più uno stato (punto 1). Indici delle card su menta.
- Tutti i badge in grassetto (componente `Badge` e badge di stato dell'editor piano).
- Filtro "Mostra pazienti archiviati": stato acceso come i chip di stato, con spunta.
- Intestazione delle tabelle fissa durante lo scroll della pagina da 1280 px (`xl`), senza scroll verticale interno. Sotto i 1280 px resta lo scroll orizzontale e l'intestazione non è fissa. Per far entrare la tabella Pazienti a 1280 px il codice fiscale è uscito dalla riga sotto il nome (compare passando il mouse sull'email).
- Lista Piani: email del paziente sotto il nome. Il backend espone `pazienteEmail` in `PianoAlimentareRigaListaResponse`: **serve riavviare il backend**.

- Palette: BMI in petrolio come il Peso (via il viola dalla scheda). Il lime nella barra di salvataggio è solo il puntino "Modifiche non salvate", un segnale come prevede la 0009. Palette base invariata: è la scelta del dietologo.

### Fatto il 26 novembre, "fai tutto"

- **Glossario delle azioni** (punto 4): nuova sezione in [`wiki/glossario.md`](../wiki/glossario.md). Allineati "Nuovo ▾" (Dashboard, Agenda), "Completa" per la bozza nella scheda, "% massa grassa" (grafici, Confronto), "Macronutrienti", segnaposto "Cerca …" in Pazienti, "Apri scheda paziente".
- **Storico visite** (punto 3): variazioni di peso e % massa grassa colorate secondo l'obiettivo (`esitoVariazione`), come Confronto e striscia in alto.
- **Conferme distruttive**: già a posto. "Archivia paziente" resta rosso, perché cancella visite e promemoria futuri (confermato da Andrea).
- **"Chat" e "Analytics"**: restano visibili con un lucchetto e "in arrivo" (title e `aria-label`), `aria-disabled`.
- **Grafici** (punto 5): asse sempre proporzionale al tempo (scelta coperta dai test), ma etichette corte ("8 set"), anno solo su anni diversi, etichette diradate se due visite sono troppo vicine (`diradaTick`, `suAnniDiversi`, `formattaDataBreve`). Titolo "Composizione corporea".
- **Paginazione** nascosta con una sola pagina (resta il conteggio).
- **Un solo selettore a segmenti**: Mese/Settimana/Giorno come Tabella/Schede.
- **Testata della scheda paziente** (punto 7): da 1024 px pulsanti sempre a destra; `scrollbar-gutter: stable` sul `main` (niente salto cambiando tab). In scuro i pulsanti sono già vetro traslucido: il bianco visto nel giro era la transizione di tema.
- **Pillole "Prossima visita" / "Nessuna visita programmata" cliccabili**: aprono l'Agenda su quel giorno (nuovo `?data=YYYY-MM-DD`) o "Nuovo appuntamento" per il paziente.
- **Tabelle**: icona cartella per aprire la riga, nome non cliccabile né in Pazienti né in Piani (scelta di Andrea, per coerenza).
- **"Rimuovi alimento"** nell'editor: nuova variante `rimuovi` del `Button`, grigia e rossa solo al passaggio.
- **Pagina di accesso**: via la pillola "Piattaforma clinica avanzata" e "Privacy Policy" (non era un link); sottotitolo riscritto. Sottotitoli di Pazienti e Piani meno da template.
- **Dashboard**: striscia dei prossimi 7 giorni (`SettimanaStrip.vue`: un pallino per visita, festivi in ambra, ogni giorno apre l'Agenda su quella data); prossime visite raggruppate per giorno con obiettivo e stato del piano del paziente; "Visite in 7 giorni" al posto di "Visite oggi". Il backend ora riempie obiettivo e piano anche in `GET /pazienti/{id}` (prima `null`): **riavviare il backend**.

### Verifiche chieste da Andrea e correzioni (26 novembre, sera)

- **Tema scuro** di Dashboard, Pazienti, Piani, Alimenti, Agenda (Settimana, Mese, Giorno), Nuovo paziente, dialog Nuovo alimento: nessun problema di contrasto a vista.
- **Mobile** (375 px) di Pazienti, Piani, Alimenti, Agenda, editor del piano, Nuova visita: corretto un difetto nella vista **Giorno**, dove la colonna si allargava quanto il titolo più lungo e la riga usciva dallo schermo (`grid-cols-[minmax(0,1fr)]`); da mobile orario più stretto e tipo nascosto (lo dicono colore e legenda).
- **Agenda**: nel Mese il numero nell'angolo diventa "1 visita"; nel Giorno il tipo di evento non è più in maiuscolo.
- **Dialog e conferme come pannelli dal basso** sotto i 640 px (`DialogContent`, `AlertDialogContent`), centrati come prima da 640 px. "Close" → "Chiudi" per gli screen reader. Segnaposto del selettore paziente accorciato ("Cerca paziente…").
- **Form del paziente**: "Livello di attività", "Storia del paziente", "Terapie in corso" sia nei form sia nella card Profilo; la voce per togliere il livello di attività si chiama "Non indicato" (prima "Seleziona", che sembrava un valore scelto).
- **Accessibilità dei form**: controllo automatico su Nuovo paziente, Nuova visita, Nuovo alimento, Nuovo appuntamento (tutti i campi con nome, errori collegati con `aria-describedby`). Nell'editor del piano aggiunti i nomi a nome del piano, note di esempi e pasti, grammi nella tabella dei pasti. Tab della scheda con ruoli corretti; titoli di Plicometria e Circonferenze portati a `h3`.
- **Icone su tutti i pulsanti con testo** (66 pulsanti): stessa azione, stessa icona (× Annulla, cestino per Elimina/Rimuovi, freccia per Indietro, frecce di paginazione, ecc.), anche nei dialog e nelle conferme come nella barra di salvataggio dei form.
- **Dashboard**: freccia sulle righe delle prossime visite (aprono la scheda) e "Ultima visita 25 nov" / "Prima visita" accanto all'obiettivo; l'hover dell'ultima riga non esce più dagli angoli della card.

## Cosa funziona già bene
- Ricerca globale da tastiera.
- Stato vuoto delle notifiche con icona e titolo.
- Scheda paziente da mobile: testata compatta, menu "⋯", tab come selettore, indicatori in griglia 2×2.
- Editor del piano: fascia menta con data, obiettivo e fine; "Nuovo esempio" tratteggiato; barra azioni flottante.
- Agenda Settimana: oggi evidenziato, linea dell'ora corrente, filtri coerenti con le liste.

## Non verificato
- Tema scuro e mobile delle pagine diverse dalla scheda paziente.
- Agenda Mese e Giorno, dialog di creazione.
- Contrasti misurati (solo a vista).
