# Revisione UI — secondo giro (25 novembre 2026, sera)

Dopo aver chiuso tutta la sezione 6 di [`ui-review-2026-11-25.md`](ui-review-2026-11-25.md). Due letture indipendenti, entrambe con la skill **frontend-design**:

- **Giro visivo in Chrome** (Claude): Dashboard, Pazienti, Piani, Alimenti, Agenda (Mese), command palette, notifiche; chiaro, scuro e 390 px.
- **Revisione del codice UI** (secondo agente, Opus, senza browser): tutte le view e i componenti di `frontend-professionisti`.

Dove le due letture coincidono è segnato **[entrambi]**.

## Verdetto

| Qualità | Giudizio | Perché |
|---|---|---|
| **Coerente** | In buona parte | Token, pulsanti, dialog, stati vuoti nuovi e pagine di accesso seguono l'ADR 0009. Restano fuori norma: stati vuoti delle liste, conferme distruttive, nomi delle azioni, il doppione "Invita" nella lista pazienti. |
| **Moderna** | Sì | Command palette, barra azioni flottante, liste a schede, Dashboard con azioni, agenda e scheda paziente pensate per mobile. Datati: paginazione "Precedente/Successivo", "Caricamento…" in testo. |
| **Accattivante** | Sì, con misura | Isola scura, petrolio, Fraunces corsivo e lime come segnale danno un carattere riconoscibile. Il rischio è la ripetizione: due blocchi petrolio e due lime nella scheda paziente (tab Piani). |
| **Chiara** | Quasi | Testo asciutto e italiano naturale, stati vuoti con un'azione. Ma la stessa azione ha nomi diversi, alcuni sottotitoli sono da template e ci sono voci di menu che non portano da nessuna parte. |

In breve: l'app è passata da "curata pagina per pagina" a "un sistema". Per dire "coerente e chiara" senza riserve mancano soprattutto **nomi delle azioni**, **conferme distruttive** e **voci morte**.

## Da fare, in ordine di impatto

1. **Voci "Chat" e "Analytics" nella barra laterale [entrambi].** Sono testo senza link (`AppSidebar.vue:21-22`): l'utente prova a cliccarle e non succede nulla. Toglierle finché non esistono, oppure "In arrivo" con `aria-disabled`.
2. **Un glossario delle azioni [entrambi].** Oggi la stessa cosa ha nomi diversi:
   - "Aggiungi" (Agenda), "Fissa appuntamento" (scheda), "Fissa un appuntamento" (Dashboard), "Nuovo appuntamento" (dialog, palette);
   - "Rinnova" / "Duplica" / "Duplica come nuovo piano";
   - "Nuovo piano" / "Nuovo piano alimentare";
   - "De-archivia" (meglio "Ripristina").

   Fissarli in una pagina wiki e allinearli ovunque.
3. **Conferme distruttive.** Circa 10 pulsanti colorati a mano con `bg-(--danger)` invece delle varianti `destructive-*`. "Conferma" generico invece di "Elimina alimento" / "Archivia paziente". "Archivia" con l'icona del cestino e il filtro archiviati rosso: l'archiviazione sembra un'eliminazione (usare l'icona `Archive` e un colore neutro).
4. **Doppione "Invita" nella lista pazienti [entrambi].** Badge "Mai invitato" più testo "Invita" nella stessa riga, sia in tabella sia nelle schede: applicare la stessa pillola unica della scheda paziente. Le azioni di riga mescolano icona, testo e "⋯".
5. **Stati vuoti delle liste fuori standard [entrambi].** Pazienti, Piani, Alimenti, tab Piani della scheda e notifiche ("Nessuna notifica" è una riga di testo) non usano ancora `StatoVuoto.vue`: titoli a volte in grassetto e a volte in corsivo, token shadcn `text-muted-foreground`.
6. **"Comprimi" nella barra laterale non resta compresso.** Lo stato riparte a ogni cambio pagina; da compresse le voci non hanno un nome accessibile.
7. **Un solo blocco petrolio per pagina.** Nella tab Piani della scheda paziente la card del piano attivo è un secondo blocco hero con un secondo pulsante lime: dentro la tab va su superficie o menta, con un primario petrolio.
8. **Command palette.** Non trattiene il focus, non si naviga con le frecce, un errore di rete appare come "Nessun risultato". Inoltre non mostra perché un paziente combacia (email o codice fiscale accanto al nome).
9. **Accessibilità dei form.** Errori non collegati ai campi (`aria-describedby`), tab della scheda paziente senza ruoli `tab`, etichette dell'editor piano come `span` invece di `label`, ricerca con il solo placeholder.
10. **Caricamenti ed errori in stili diversi.** Skeleton in alcune pagine, "Caricamento…" in testo in altre; errori con e senza "Riprova"; toast vaghi ("Errore imprevisto, riprova."); il login sbagliato finisce in un toast che sparisce invece di un messaggio fisso.
11. **Rifiniture.**
    - Titoli di sezione con pesi diversi.
    - Abbreviazioni "vs prec." e "pt" negli indicatori.
    - Sottotitoli da template ("Gestisci la lista dei pazienti…": meglio un dato utile o niente).
    - Menu "⋯" con una sola voce sugli alimenti BDA.
    - Nella vista Mese i giorni del mese precedente si distinguono poco.
    - Riquadro "Visite oggi" che ripete il banner della Dashboard.
    - Nessuna larghezza massima del contenuto oltre 1600 px.

## Riscontro in Chrome

Verifica su `localhost:5173` con un account di test. Il giro ha incluso desktop in tema chiaro e scuro e l'emulazione iPhone a 375 e 393 px.

| Priorità | Problema | Riproduzione ed esito |
|---|---|---|
| Alta | La ricerca globale non consente di aprire un risultato con freccia e Invio (punto 8). | Aprire la palette con `Ctrl+K`, cercare un paziente per cognome, premere `↓` e poi `Invio`. La palette resta aperta. Un clic sullo stesso risultato apre la scheda. |
| Alta | La barra laterale perde lo stato compresso e i link compressi perdono il nome accessibile (punto 6). | In Alimenti, premere «Comprimi». Nell'albero di accessibilità il collegamento ad Agenda è senza nome. Aprendo Agenda dalla sua icona, la barra torna espansa. |
| Media | Il selettore paziente copre i campi del promemoria. | In Agenda, aprire «Nuovo promemoria» oppure modificare un promemoria senza paziente. Il campo facoltativo riceve il focus e apre subito l'elenco delle visite recenti, che si sovrappone a data, ora e testo. |
| Media | Il messaggio di conferma indica il tipo di evento sbagliato. | Salvare un nuovo promemoria. L'evento compare in Agenda e il messaggio recita «Appuntamento creato.». |
| Media | «Chat» e «Analytics» sono esposte come testo semplice nella navigazione (punto 1). | Espandere la barra laterale. Nell'albero di accessibilità le due voci sono testo e non espongono un collegamento o un pulsante. |
| Media | La lista Pazienti mostra stato e invito come elementi separati (punto 4). | Aprire la lista in tabella o visualizzarla a 393 px. Lo stesso paziente presenta «Mai invitato» e «Invita» separati. La scheda paziente li raggruppa in una pillola. |
| Media | Le diciture per fissare un appuntamento variano tra le schermate (punto 2). | Confrontare «Fissa un appuntamento» in Dashboard, «Fissa appuntamento» nella scheda paziente, «Aggiungi» in Agenda e «Nuovo appuntamento» nel modulo. |
| Bassa | Lo stato vuoto di Notifiche è una riga di testo (punto 5). | Aprire la campanella con l'account di test senza notifiche. Il pannello mostra «Nessuna notifica». |

La ricerca locale ha restituito una corrispondenza per cognome in Pazienti e «Banana» dal catalogo BDA in Alimenti. In Agenda è stata verificata anche una settimana con alcuni eventi. Dopo il salvataggio di una visita, la scheda ha mostrato il grafico con due rilevazioni.

## Cosa funziona già bene

- Dashboard orientata alle azioni ("Avvia visita", "Da fare").
- Barra azioni flottante e scroll al primo errore nei form lunghi.
- Liste coerenti tra loro, con interruttore schede/tabella.
- Pagine di accesso unificate.
- Agenda e scheda paziente da mobile.
- `aria-label` sui pulsanti solo icona, `motion-safe` sulle micro-animazioni, virgola decimale e errori live nei form.
- Tema scuro coerente.

## Non verificato

- Contrasti dopo gli ultimi ritocchi dei token.
- Ordine del focus da tastiera.
- Vista Settimana piena di eventi.
- Grafici con molte visite.
- Tastiera mobile sopra la barra azioni.
- Autocompletamento di Chrome al login.
