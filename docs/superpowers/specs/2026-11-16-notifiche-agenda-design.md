# Design — Notifiche dell'agenda

Data: 2026-11-16
Percorso: architetturale (brainstorming → questo spec → piano di implementazione)
Fonti: conversazione con Andrea del 2026-11-16 (che riporta le scelte fatte con il dietologo che userà l'app); codice attuale di `backend/agenda`, `AppHeader.vue`, `AppSidebar.vue`.
Solo `frontend-professionisti` e `backend`. `frontend-cliente` non è coinvolto.

## Obiettivo e perché

Avvisare il professionista che una visita o un promemoria sta per iniziare, anche quando Hexis non è aperto nel browser. Oggi il campanello nell'header è solo decorativo (puntino verde fisso) e la voce Agenda nella sidebar non ha indicatori. Il professionista in studio deve poter contare sull'avviso senza tenere la scheda aperta.

## Obiettivi

- Ogni visita (studio o videoconsulto) e ogni promemoria **con orario** può generare una notifica, un certo anticipo prima dell'inizio.
- Il professionista sceglie, per ogni evento, se riceverla e con quale anticipo.
- La notifica compare in tre posti: **push del browser** (anche con Hexis chiuso), **lista del campanello** nell'header, **puntino** sulla voce Agenda della sidebar.
- Funziona nei browser desktop su **Windows e macOS** (Chrome, Edge, Firefox, Safari 16+ su macOS 13+).

## Non-obiettivi (decisi)

- Notifiche email (Andrea: "hanno poco senso").
- Notifiche per i pazienti; `frontend-cliente`.
- Mobile / iOS / app installata come PWA.
- Notifiche non legate all'agenda (il campanello nasce solo per l'agenda; altri tipi in futuro).
- Posticipo ("ricordamelo dopo"), orari silenziosi, impostazioni per dispositivo.

## Anti-obiettivi (sarebbe un fallimento anche se "funziona")

- Il **testo dell'avviso push non deve contenere dati sanitari né il nome del paziente** (può comparire a schermo bloccato). Il nome compare solo nel campanello dentro l'app.
- Nessuna notifica duplicata per lo stesso evento e lo stesso orario di scatto.
- Nessuna notifica per eventi che non ne hanno diritto (vedi regole).

## Decisioni di Andrea

1. Sia per le visite sia per i promemoria: una **checkbox "Ricevi notifica"**, attiva di default.
2. Se attiva, si sceglie l'**anticipo** tra 2 ore, 1 ora, 45 min, 30 min, 15 min. Predefinito **45 min**.
3. Il **puntino del campanello pulsa** finché il professionista non clicca sul bottone; **sparisce quando chiude la lista**.
4. Il **puntino nella sidebar** (voce Agenda) sparisce quando il professionista **apre la pagina Agenda**.
5. Un promemoria **senza orario** non mostra la sezione notifica e non genera notifiche. Le **fasce bloccate** e le visite **annullate** restano fuori.
6. L'avviso **push è generico** (nessun dato sul paziente).
7. Windows e macOS, da browser.

## Approcci considerati

- **A. Calcolo solo nel frontend.** Polling di `GET /agenda` di oggi, scadenze calcolate nel browser, "visto" in `localStorage`. Semplice, ma le notifiche esistono solo con l'app aperta: non copre le push con l'app chiusa. Scartato quando Andrea ha chiesto le push.
- **B1. Notifiche nel backend, campanello e push dalla stessa fonte (scelto).** Scheduler Spring che crea le notifiche e invia le push; il campanello legge le notifiche dalle API. Una sola logica di scadenza, stato di lettura persistente e condiviso tra dispositivi.
- **B2. Push nel backend, campanello calcolato nel frontend.** Scartato: la scadenza sarebbe calcolata in due posti e i risultati divergerebbero.

Scelta di Andrea: B1 (con le risposte del 2026-11-16 a seguire la proposta).

## Dati

### Su `appuntamenti` (nuova migrazione Flyway, dopo la V30)

- `notifica` boolean, not null, default `true`.
- `anticipo_notifica_minuti` int, not null, default `45`; valori ammessi 120, 60, 45, 30, 15 (vincolo CHECK e validazione lato API).
- Per i blocchi e per i promemoria senza orario i campi non hanno significato: il server li ignora e salva `notifica = false`. *(mia scelta)*

### Nuova tabella `notifiche`

Una riga per ogni scatto di un evento:
- `id`, `professionista_id`, `appuntamento_id` (FK, `ON DELETE CASCADE`), `scatta_il` (istante in cui doveva scattare), `creata_il`.
- `letta_il` (null finché la lista del campanello non viene chiusa) e `agenda_vista_il` (null finché la pagina Agenda non viene aperta). Sono due stati indipendenti, perché i due puntini hanno regole diverse.
- Unicità su (`appuntamento_id`, `scatta_il`): impedisce i duplicati.

### Nuova tabella `push_subscriptions`

`id`, `professionista_id`, `endpoint` (unico), `p256dh`, `auth`, `user_agent` (facoltativo), `creata_il`. Un professionista può avere più dispositivi.

## Regole di scadenza (backend)

- Un evento è **notificabile** se: tipo `VISITA_STUDIO`, `VIDEOCONSULTO` o `PROMEMORIA`; ha `oraInizio`; `notifica = true`; non è annullato; e (visita) è `PROGRAMMATA` oppure (promemoria) non è `fatto`.
- `scatta_il = data + oraInizio` (fuso `Europe/Rome`, `AgendaTempo.FUSO`) meno l'anticipo.
- Lo scheduler (già presente `@EnableScheduling`) gira **ogni minuto**: per ogni evento notificabile con `scatta_il ≤ ora` e senza notifica per quello stesso `scatta_il`, crea la riga e invia la push. *(mia scelta: se l'evento viene creato quando è già dentro la finestra di anticipo, la notifica scatta al primo passaggio utile, finché l'evento non è iniziato.)*
- Se un evento viene **modificato** (data, ora, anticipo, flag), **annullato**, segnato **fatto** o **eliminato**, le sue notifiche si eliminano; la nuova situazione genera, se serve, una nuova notifica. *(mia scelta)*
- **Durata in lista** *(mia scelta)*: una visita resta nell'elenco fino alla sua fine o finché è `PROGRAMMATA`; un promemoria finché non è fatto o fino a fine giornata.

## API (da documentare in `wiki/api-contracts.md`)

- Campi `notifica` e `anticipoNotificaMinuti` in richiesta e risposta di `POST /agenda` e `PUT /agenda/{id}` e nelle risposte di `GET /agenda`.
- `GET /notifiche`: elenco delle notifiche attive del professionista, con dati dell'evento per mostrarle (titolo, ora, nome del paziente) e i due stati. Include il conteggio delle non lette.
- `POST /notifiche/lette`: segna come lette tutte le notifiche (chiusura della lista).
- `POST /notifiche/agenda-viste`: segna come "viste in agenda" (apertura della pagina Agenda).
- `GET /notifiche/push/chiave`: chiave pubblica VAPID. `POST /notifiche/push/iscrizioni` registra l'iscrizione del dispositivo; `DELETE` la rimuove.
- Autenticazione JWT come per `/agenda`; ogni dato filtrato per professionista.

## Push

- **Web Push con VAPID.** Chiavi in configurazione (`VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`, subject), con segreti fuori dal repo come `JWT_SECRET`. Se mancano, le push sono disattivate senza errori e campanello e puntini continuano a funzionare. La scelta della libreria Java è del builder.
- Testo generico, per esempio "Hai una visita tra 45 minuti" / "Hai un promemoria tra 45 minuti"; il clic apre `/agenda`.
- Se il servizio push risponde che l'iscrizione non esiste più (404/410), la riga di `push_subscriptions` si elimina.
- **Service worker** in `frontend-professionisti/public`. *(mia scelta)* Se esiste già una scheda Hexis visibile e in primo piano, l'avviso del sistema non viene mostrato (il professionista vede già il campanello).
- **Permesso**: i browser lo richiedono con un gesto dell'utente (Safari in particolare). *(mia scelta)* Un invito "Attiva le notifiche su questo dispositivo" nella lista del campanello, con un testo di aiuto se il permesso è negato.
- Richiede HTTPS in produzione (su `localhost` funziona in sviluppo).

## Frontend

- **Dialog appuntamento** (`AppuntamentoDialog.vue`): per visite e per promemoria **con ora**, la checkbox "Ricevi notifica" e il select dell'anticipo (visibile se la checkbox è attiva). Nascosti per i blocchi e per i promemoria senza ora. Segue la convenzione dei form del progetto. `richiestaDa` ("Sposta a domani") deve conservare i due campi.
- **Campanello** (`AppHeader.vue`): lista delle notifiche e puntino. Pulsa finché esistono notifiche non lette e il bottone non è stato cliccato; resta fisso dopo il clic; sparisce alla chiusura della lista, che chiama `POST /notifiche/lette`.
- **Sidebar** (`AppSidebar.vue`): puntino sulla voce Agenda finché esistono notifiche non viste in agenda; entrando nella pagina Agenda si chiama `POST /notifiche/agenda-viste`. Il puntino della sidebar è statico (non pulsa). *(mia scelta)*
- **Polling**: `GET /notifiche` ogni minuto e quando la scheda torna visibile, in pausa quando è nascosta. Store Pinia dedicato.
- Cliccando una notifica nella lista si apre il dettaglio dell'evento in agenda. *(mia scelta)*

## Scelte aggiunte dopo la revisione (mie, da correggere se non vanno)

- **Eventi già esistenti alla prima installazione**: la migrazione li porta a `notifica = false`, così il primo avvio non genera una raffica di notifiche e push. Solo gli eventi creati o modificati dopo la migrazione usano il default `true`.
- **Stato "letta" e pulse**: sono server-side e condivisi tra dispositivi, mentre il pulse è stato dell'interfaccia. Dopo un ricaricamento della pagina, se ci sono notifiche non lette il puntino pulsa di nuovo finché non si clicca il bottone. `POST /notifiche/lette` riceve **gli id** delle notifiche mostrate nella lista, così una notifica arrivata mentre la lista era aperta non viene segnata come letta senza essere stata vista.
- **Testo della push**: "Hai una visita tra N minuti" per visita in studio e videoconsulto, "Hai un promemoria tra N minuti" per i promemoria; nessun titolo né nome.
- **Un solo processo backend**: si assume una sola istanza. La push parte solo se l'inserimento della riga in `notifiche` è riuscito (cioè non esisteva già), così un riavvio a metà ciclo non invia due volte. Se in futuro ci saranno più istanze servirà un lock (per esempio ShedLock).

## Vincoli

- Spring Boot 3.3.4, Java 21, PostgreSQL, Flyway; frontend Vue 3 + TypeScript.
- Fuso `Europe/Rome`; orari 07:00–20:00.
- Niente test inclusi di default (preferenza di Andrea): da concordare prima del piano.
- Mai `git commit`.

## Da aggiornare alla fine

`wiki/api-contracts.md`, `wiki/modello-dati.md`, una nuova decisione (ADR 0008) che supera la riga "notifiche" fuori scope della 0007, `wiki/stato.md` e `wiki/log.md`.

## Lasciato al builder

Libreria Web Push, struttura dei pacchetti Java, nomi di classi e file, aspetto esatto della lista del campanello, testi esatti degli avvisi, strategia di cache di polling, e come organizzare i test quando Andrea li vorrà.
