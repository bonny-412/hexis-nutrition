# Hexis Nutrition — Bug riprodotti

Collaudo del 27 novembre 2026 su Chrome (`localhost:5173`), Spring Boot e PostgreSQL `hexis_test`.

## 01 — AGENDA: UN ORARIO NON VALIDO CANCELLA QUELLO SALVATO

- **Passaggi** creare un promemoria alle 17:42. Riaprirlo, sostituire l'ora con 25:99 e salvare.
- **Risultato** compare «Promemoria aggiornato», ma l'orario precedente viene rimosso e il promemoria risulta senza orario.
- **Atteso** segnalare l'orario non valido e conservare il valore salvato finché la modifica non viene corretta o l'ora viene rimossa esplicitamente.

## 02 — ALIMENTI: LA SOMMA DEI MACRONUTRIENTI SUPERA IL PESO DI RIFERIMENTO

- **Passaggi** creare un alimento personalizzato da 100 g con 400 kcal, 90 g di proteine, 80 g di grassi e 70 g di carboidrati.
- **Risultato** l'app salva l'alimento e mostra in tabella 240 g complessivi di macronutrienti per 100 g.
- **Atteso** bloccare il salvataggio e indicare che la somma supera la quantità di riferimento.

## 03 — STORICO VISITE: ORDINE INVERTITO PER DUE VISITE NELLO STESSO GIORNO

- **Passaggi** creare un paziente con una prima visita a 76,50 kg. Nello stesso giorno registrare una seconda visita a 75,80 kg. Aprire Storico visite e Confronto visite.
- **Risultato** lo storico etichetta la prima visita come «Più recente» e mostra +0,70 kg. Il confronto mostra -0,70 kg e la panoramica mostra 75,80 kg come peso corrente.
- **Atteso** lo storico dovrebbe mostrare per prima la seconda visita e usare lo stesso ordine del confronto, anche a parità di data.
- **Prova Spring/JPA su PostgreSQL** con pagina da una riga, la prima visita mostra una variazione assente. Con pagina da due righe, la stessa visita mostra +0,70 kg. Il calcolo dipende dalla dimensione della pagina.

## 04 — PIANI ALIMENTARI: ANTEPRIMA DI STAMPA INCOMPLETA

- **Passaggi** aprire il piano attivo QA-2711 Piano collaudo e scegliere Altre opzioni > Stampa PDF.
- **Risultato** nell'anteprima Chrome, l'unico foglio contiene controlli dell'editor come «Aggiungi alimento». L'intestazione del piano è assente e il contenuto è tagliato in fondo.
- **Atteso** un'anteprima con intestazione e contenuto completo, impaginato senza i comandi di modifica.
- **Verifica** osservato nell'anteprima. Nessun invio alla stampante.

## 05 — RICERCA: IL NOME COMPLETO NON TROVA PAZIENTE E PIANI

- **Passaggi** con il paziente fittizio Elio Collaudo presente, cercare «Collaudo» in Cerca ovunque. Ripetere con «Elio Collaudo». Eseguire la stessa prova nel campo di ricerca dell'elenco Pazienti.
- **Risultato** entrambe le ricerche trovano il paziente con «Collaudo» e restituiscono zero risultati con «Elio Collaudo». Il caso è stato ripetuto nella ricerca globale.
- **Atteso** il nome completo dovrebbe trovare la stessa scheda restituita con il solo cognome.
- **Estensione verificata** in Piani alimentari, «Elio Collaudo» restituisce zero piani e «Collaudo» restituisce entrambi i piani del paziente.

## 06 — EDITOR PIANO: UNA QUANTITÀ NEGATIVA DIVENTA POSITIVA

- **Passaggi** aprire la bozza QA-2711 Piano collaudo (copia), selezionare la quantità dell'alimento e incollare -10.
- **Risultato** il campo mostra 10 g e il totale di lunedì diventa 13 kcal, senza avviso. Verificato prima del salvataggio.
- **Atteso** segnalare la quantità negativa senza trasformarla in una porzione positiva.

## 07 — EDITOR PIANO: SALVATAGGIO DI UN ALIMENTO CON PORZIONE ZERO

- **Passaggi** nella stessa bozza impostare l'unico alimento a 0 g, salvare e ricaricare.
- **Risultato** l'app conferma il salvataggio. La riga dell'alimento resta con 0 g, mentre il riepilogo del giorno mostra un trattino.
- **Atteso** per una riga alimento, richiedere una porzione positiva o indicare chiaramente che la riga è incompleta.
Il valore della bozza è stato ripristinato a 160 g dopo la prova.

## 08 — EDITOR PIANO: DATA INESISTENTE SEGNALATA COME CAMPO VUOTO

- **Passaggi** inserire 31/02/2027 in Fine piano e premere Salva.
- **Risultato** il salvataggio è bloccato, ma il campo visibilmente compilato mostra «La data di fine è obbligatoria».
- **Atteso** indicare che la data non è valida.

## 09 — SPRING BOOT: IL SERVIZIO PUÒ RIATTIVARE UN PIANO TERMINATO

- **Ambiente** prova JVM isolata con PianoAlimentareService e repository simulati.
- **Passaggi** predisporre un piano TERMINATO e un altro ATTIVO per lo stesso paziente. Chiedere al servizio di attivare il piano terminato.
- **Risultato** il piano terminato torna ATTIVO e quello corrente passa a TERMINATO. L'interfaccia offre l'azione di attivazione per le bozze.
- **Atteso** rifiutare la transizione, preservando entrambi gli stati.

## 10 — SPRING BOOT: IL BACKEND SALVA UN INTERVALLO INVERTITO E UN OBIETTIVO NEGATIVO

- **Ambiente** prova JVM isolata con Hibernate Validator, PianoAlimentareService e repository simulati.
- **Passaggi** aggiornare un piano con partenza 11/12/2026, fine 04/12/2026 e obiettivo -5 kcal.
- **Risultato** la validazione del DTO non segnala violazioni e il servizio restituisce il piano con quei valori. In Chrome la data di fine precedente all'inizio viene bloccata correttamente.
- **Atteso** applicare anche nel backend il controllo sulle date e rifiutare un obiettivo calorico negativo.
- **Nuova prova Chrome** il modulo salva 0 kcal come obiettivo giornaliero. Il valore resta 0 dopo il ricaricamento. A fine test è stato ripristinato 2331.

## 11 — SPRING BOOT: RICHIESTA VUOTA RIAPRE UN PROMEMORIA COMPLETATO

- **Ambiente** MockMvc con controller e servizio Agenda reali e repository simulato.
- **Passaggi** predisporre un promemoria completato e chiamare l'azione Aggiorna stato fatto con un oggetto JSON vuoto.
- **Risultato** HTTP 200, stato modificato da true a false e salvataggio richiesto al repository.
- **Atteso** HTTP 400 per il campo fatto obbligatorio e stato del promemoria invariato.

## 12 — SPRING BOOT: DUE OBIETTIVI DIVERSI RISULTANO ENTRAMBI «ULTIMA VISITA»

- **Ambiente** prova Spring/JPA nuova su PostgreSQL hexis_test, con rollback a fine prova.
- **Passaggi** registrare due visite per lo stesso paziente nella stessa data, la prima con MANTENIMENTO e la seconda con DIMAGRIMENTO. Applicare separatamente i due filtri Obiettivo ultima visita.
- **Risultato** il paziente compare in entrambi i risultati.
- **Atteso** il paziente compare solo con l'obiettivo della visita più recente.

## 13 — EDITOR PIANO: IL MENU LATERALE PERDE LE MODIFICHE SENZA AVVISO

- **Passaggi** aprire la bozza QA-2711 Piano collaudo (copia), cambiare l'obiettivo da 2331 a 1999 kcal e selezionare Agenda dalla barra laterale.
- **Risultato** si apre Agenda senza conferma. Tornando alla bozza compare 2331. Sullo stesso campo, «Torna ai piani alimentari» apre invece la conferma.
- **Atteso** chiedere se scartare le modifiche anche quando si usa il menu laterale.

## 14 — SPRING BOOT: IL VIDEOCONSULTO ACCETTA UN LINK SENZA HOST

- **Ambiente** MockMvc con controller reale e servizio simulato.
- **Passaggi** creare un videoconsulto sintetico con link https://? e gli altri campi validi.
- **Risultato** HTTP 201 e link passato invariato al servizio. Una URL HTTPS valida viene accettata e FTP viene rifiutato.
- **Atteso** HTTP 400 per un indirizzo HTTPS privo di host.

## 15 — SPRING BOOT: LA DURATA DECIMALE DELL'APPUNTAMENTO VIENE TRONCATA

- **Ambiente** MockMvc con controller reale e servizio simulato.
- **Passaggi** inviare una durata di 15,9 oppure 480,9 minuti come numero JSON.
- **Risultato** entrambe le richieste restituiscono HTTP 201. Il servizio riceve rispettivamente 15 e 480. Il valore sopra il massimo di 480 è accettato dopo il troncamento.
- **Atteso** HTTP 400 per una durata non intera.

## 16 — PAZIENTI: IL MODULO NON SPIEGA IL LIMITE DEL NOME

- **Passaggi** aprire Modifica dati anagrafici sul paziente fittizio, inserire 101 caratteri in Nome e salvare.
- **Risultato** il dialogo resta aperto senza indicare il limite nel campo. Rimettendo «Elio», il salvataggio riesce. Il limite PostgreSQL è 100 caratteri, mentre il DTO di modifica non dichiara il limite.
- **Atteso** indicare il massimo di 100 caratteri, con un errore di validazione anche lato API.

## 17 — POSTGRESQL: VINCOLI MANCANTI SU QUANTITÀ, PESO E INTERVALLI

- **Ambiente** 13 prove nuove su PostgreSQL hexis_test, con savepoint e rollback finale verificato.
- **Risultato** scritture dirette accettate per quantità di riferimento alimenti 0 e -100, peso visita -10, grammatura nel piano -30, data fine piano precedente all'inizio e blocco agenda a giornata intera con fine precedente all'inizio.
- **Atteso** vincoli DB che preservino queste invarianti anche in scritture dirette. Alcuni valori sono già respinti dall'API Spring, quindi la prova SQL non implica che siano accettati anche dall'API.
- **Vincoli verificati** chiave esterna paziente, data obbligatoria piano, anticipo notifiche, esattamente un padre per riga alimento e unicità target giornaliero. Il caso valido di controllo è stato accettato.

## 18 — PIANI: DUE SCHEDE POSSONO SOVRASCRIVERE MODIFICHE DIVERSE

- **Passaggi** aprire la stessa bozza in due schede con obiettivo 2331 kcal e porzione 160 g. Nella seconda scheda salvare 2345 kcal. Nella prima, senza ricaricare, cambiare solo la porzione a 165 g e salvare.
- **Risultato** entrambi i salvataggi mostrano «Piano salvato». Al ricaricamento la porzione è 165 g, ma l'obiettivo è tornato a 2331 kcal.
- **Atteso** preservare la modifica già salvata oppure avvisare del conflitto prima di sovrascriverla. La bozza di test è stata ripristinata a 2331 kcal e 160 g.
- **Riscontro nel codice** l'aggiornamento non riceve una revisione della bozza. L'entità non usa @Version e il servizio sostituisce l'intera struttura del piano.

## 19 — SESSIONI: IL RESET PASSWORD NON INVALIDA I JWT GIÀ EMESSI

- **Ambiente** nuova prova Java 21 con servizi reali, account sintetico e repository e posta simulati.
- **Risultato** la password precedente smette di funzionare, la nuova permette l'accesso e il token di reset viene consumato. Il validatore JWT continua a considerare valido il token di sessione emesso prima del reset, con la stessa scadenza. La prova ha verificato il validatore senza usare il token su una rotta HTTP protetta.
Atteso per il recupero dell'account: invalidare anche le sessioni già emesse quando la password viene reimpostata.

## 20 — INVITI: L'EMAIL GENERATA PUNTA A UN DOMINIO SEGNAPOSTO

- **Ambiente** prova isolata con PazienteService reale, account sintetico e posta simulata.
- **Risultato** il messaggio generato usa app.hexisnutrition.example come host del link di attivazione. È un dominio segnaposto e il destinatario non può raggiungere l'app pubblica tramite quel link.
- **Atteso** costruire il link usando l'URL del portale paziente configurato per l'ambiente.
- **Controprove** lo stato passa a INVITATO, il token corrisponde al proprio hash e dura circa 7 giorni, l'attivazione porta il paziente ad ATTIVO, la password è codificata e il token consumato viene rifiutato. L'errore di invio simulato viene tradotto nell'eccezione applicativa. La consegna reale e il rollback PostgreSQL in quel caso non erano parte della nuova prova.

## 21 — CONCORRENZA: DUE ATTIVAZIONI POSSONO LASCIARE DUE PIANI ATTIVI

- **Ambiente** PianoAlimentareService reale, due transazioni indipendenti e PostgreSQL 13.23 hexis_test. Il caso era già riprodotto con repository simulato.
- **Passaggi** predisporre due bozze per lo stesso paziente senza piani attivi. Sincronizzare le due attivazioni dopo la lettura dei piani attivi e prima dell'UPDATE Hibernate.
- **Risultato** entrambe le transazioni eseguono il commit. Una lettura indipendente trova due righe ATTIVO. L'esecuzione sequenziale lascia un solo piano attivo.
- **Atteso** rispettare il contratto di al massimo un piano ATTIVO, serializzando le attivazioni o respingendo una delle operazioni concorrenti.

## 22 — CONCORRENZA: DUE VISITE SOVRAPPOSTE VENGONO SALVATE SENZA CONFERMA

- **Ambiente** AgendaService reale, due transazioni indipendenti e PostgreSQL 13.23 hexis_test.
- **Passaggi** creare due visite per lo stesso professionista e giorno, alle 10:00 per 30 minuti. Usare confermaSovrapposizione=false per entrambe. Sincronizzare le transazioni dopo il controllo e prima dell'INSERT Hibernate.
- **Risultato** entrambe eseguono il commit e una lettura indipendente trova due righe PROGRAMMATA per lo stesso orario. In sequenza la prima riesce e la seconda viene respinta per sovrapposizione.
- **Atteso** la seconda scrittura deve segnalare il conflitto o richiedere una conferma esplicita.

## Visibilità con due professionisti

Nuova prova con 14 richieste GET su Spring Boot e PostgreSQL `hexis_test` tramite MockMvc. I due account e i loro record erano sintetici. Il test ha annullato la transazione alla fine.

| Vista | Professionista A | Professionista B |
| --- | --- | --- |
| Dashboard e lista pazienti | 2 pazienti, entrambi propri | 1 paziente proprio |
| Lista piani alimentari | 1 piano proprio | 2 piani, entrambi propri |
| Agenda | 1 appuntamento proprio | 1 appuntamento proprio |
| Alimenti personalizzati | 1 alimento proprio | 1 alimento proprio |
| Notifiche attive | 1 notifica propria | 1 notifica propria |
| Catalogo BDA | Stessa voce condivisa | Stessa voce condivisa |

Le risposte delle viste elencate contenevano solo i record attesi nel rispettivo ambito e la voce del catalogo condiviso. Questa prova copre le liste delle due sessioni.

## Note di collaudo

- Riferimento separato, escluso dai risultati delle nuove prove: suite Vitest esistente 307 test superati in 40 file. Suite Maven esistente 196 test superati con Java 21 e PostgreSQL 13.23 su hexis_test. Flyway ha validato 34 migrazioni.
- La stessa prova Spring/JPA conferma il caso 05 su PostgreSQL per entrambi i filtri, pazienti e piani.
- Revisione difensiva: SecurityConfig limita pazienti, agenda, alimenti e notifiche al ruolo PROFESSIONISTA. I piani ricadono nella regola generale di autenticazione. Il servizio dei piani applica controlli di proprietà per dettaglio e modifica e filtra la lista per professionista. L'assenza del vincolo esplicito sul ruolo è una differenza di configurazione, senza accesso improprio osservato.
- Nuova matrice MockMvc: 16 richieste anonime di sola lettura hanno restituito HTTP 401. Coperti profilo, conteggio dashboard, ricerca e dettaglio pazienti, lista/storico/dettaglio visite, ricerca e dettaglio alimenti, agenda, notifiche e chiave push, ricerca/dettaglio/note dei piani.
- Nuove prove di validazione pubblica: login, password dimenticata e reset con JSON vuoto hanno restituito HTTP 400.
- Nuova prova autenticata MockMvc su PostgreSQL di test: 28 richieste con due professionisti e un paziente sintetici. Convalidati profili per entrambi i ruoli, 11 letture riservate al professionista (200 per il professionista e 403 per il paziente) e visibilità limitata delle liste pazienti e piani del professionista ai propri record. Dati di test annullati a fine prova.
- Tre ulteriori richieste con ruolo PAZIENTE alle rotte dei piani: ricerca con 200 e lista vuota, dettaglio e note per ID inesistente con 404. Confermata la differenza di policy al confine HTTP. Nessuna esposizione di dati osservata.
- La durata JWT trovata nella configurazione del progetto è 480 minuti. Il filtro controlla firma e scadenza senza consultare una versione di sessione. La prova del caso 19 usa l'implementazione reale del validatore.
- Inviti disabilitati per indicazione dell'utente. La consegna effettiva è sospesa fino alla riattivazione della funzione. Il test del caso 20 ha usato posta simulata.
- I dati sintetici dei test concorrenti 21 e 22 sono stati rimossi da hexis_test e la pulizia è stata verificata.
- Copertura: prove mirate Chrome, MockMvc/Spring, Hibernate Validator e PostgreSQL. La copertura effettiva del codice non è stata misurata. Il report non certifica copertura totale o assenza di altri difetti.
- La prova isolata con Hibernate Validator conferma che anche il backend accetta i macronutrienti del caso 02.
- In una prova isolata, attivare un piano con partenza tra due settimane lo rende subito attivo e conclude quello corrente. Comportamento osservato, da confrontare con le regole di programmazione prima di classificarlo come bug.
- In Chrome, il salvataggio blocca una data di fine precedente alla partenza. L'ordinamento crescente e decrescente per nome nella vista a schede dei piani ha funzionato.
- Con il servizio Agenda reale e un repository simulato, i blocchi su più giorni rispettano i giorni iniziale e finale. Una visita che inizia esattamente alla fine del blocco non si sovrappone.

## Verifiche da completare prima del rilascio

1. **Regressione dopo le correzioni.** Trasformare in test ripetibili le prove sul salvataggio da due schede (caso 18), sulla revoca delle sessioni dopo il reset password (19), sulle attivazioni simultanee (21) e sulle visite sovrapposte (22). Verificare che le modifiche vengano preservate o il conflitto segnalato, che i token precedenti al reset vengano invalidati e che le operazioni concorrenti rispettino le regole di piani e agenda.
2. **Permessi sulle singole risorse.** Con due professionisti sintetici, verificare i permessi di lettura e modifica su singoli pazienti, visite, piani e note. La prova già eseguita tra due professionisti copre le viste di elenco.
3. **Errori durante il salvataggio.** Simulare una rete interrotta, una risposta lenta e un doppio clic su **Salva**. Verificare che i dati inseriti restino recuperabili e che un nuovo tentativo o il doppio clic producano al massimo un salvataggio.

La consegna reale degli inviti sarà verificata alla riattivazione della funzione.
