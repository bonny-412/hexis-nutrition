---
title: Bug del collaudo del 27 novembre 2026
tags: [bug, collaudo, rilascio]
stato: in-discussione
creato: 2026-11-27
aggiornato: 2026-11-28
fonti: [sorgenti/2026-11-27-collaudo-bug.md]
---

# Bug del collaudo del 27 novembre 2026

Andrea ha collaudato l'app (Chrome su `localhost:5173`, Spring Boot, PostgreSQL `hexis_test`) e ha trovato 22 bug. Il report completo, con passaggi, risultati e prove, è in [sorgenti/2026-11-27-collaudo-bug.md](sorgenti/2026-11-27-collaudo-bug.md).

**Da correggere prima della messa online** ([decisioni/0012](decisioni/0012-primo-rilascio-solo-area-professionista.md)).

## Piano di correzione (deciso il 27 novembre 2026)

Verificati nel codice il 27/11: tutti confermati; il 13 è più ampio (nessuna guardia di rotta in tutta l'app: anche visita e nuovo paziente perdono le modifiche uscendo dalla sidebar). Ordine proposto da Claude e avviato da Andrea:

1. **Concorrenza e due schede (21, 22, 18)**: **corretti il 27/11**, non ancora provati a mano. 21: indice unico parziale (V35). 22: advisory lock per professionista in `AgendaService.crea`/`aggiorna`. 18: `@Version` sul piano, `versione` nel `PUT`, 409 `PIANO_MODIFICATO_ALTROVE` con toast.
2. **Sicurezza (19, ruolo sui piani)**: **corretti il 27/11**. 19: contatore `versione_sessione` su professionisti e pazienti (V36) nel claim `sv` del JWT, incrementato dal reset, verificato dal filtro a ogni richiesta (scelto al posto di una data di cambio password perché l'`iat` del JWT è arrotondato al secondo). Ruolo: `hasRole("PROFESSIONISTA")` su `/piani-alimentari/**` in `SecurityConfig`. Provati da Claude su backend di prova (`hexis_test`): dopo il reset il token vecchio riceve 401 su `/auth/me` e `/pazienti`, la password vecchia non entra, il nuovo login e il nuovo token funzionano; un paziente riceve 403 su ricerca e dettaglio dei piani, il professionista 200.
3. **Visite nello stesso giorno (03, 12)**: **corretti il 27/11**, `creato_il` come secondo criterio d'ordine ovunque (storico, precedente, ultima visita, data dell'obiettivo, suggerimento del piano, filtro con `NOT EXISTS` di una visita successiva; nel frontend il riepilogo «Ultima visita» usa la posizione nella lista). Verificati da Claude sull'ambiente di Andrea con le due visite di Elio Collaudo del 27/11: panoramica e storico mostrano 75,80 kg come più recente con −0,70 kg; il filtro verificato in SQL in una transazione annullata (con obiettivi diversi il vecchio filtro trovava il paziente in entrambi, il nuovo solo nell'obiettivo dell'ultima).
4. **Validazioni backend e vincoli DB (09, 10, 11, 14, 15, 16, 02, 07, 17)**: **corretti il 27/11** (il 09 già con il gruppo 1). 10: `obiettivoKcal` `@Positive`, `dataFine` prima della `dataInizio` → 400 (`PianoAlimentareNonValidoException`). 11: `fatto` `@NotNull`. 14: il link del videoconsulto richiede un host. 15: `accept-float-as-int: false`, un decimale in un campo intero è 400. 16: `@Size(max)` su **tutti** i campi di testo delle richieste, uguale alla colonna (5000 per i `TEXT`), su proposta di Andrea; `@DecimalMax` sui numeri. 02: somma dei macronutrienti ≤ `quantitaG` per gli alimenti personalizzati (non nel DB: 4 alimenti BDA la superano). 07: `grammi` `@Positive`. 17: migrazione **V37** con i `CHECK` (provata prima sul DB `hexis` in una transazione annullata: nessuna riga in violazione). Dettagli in [api-contracts](api-contracts.md#limiti-dei-campi-dal-2026-11-27-collaudo-gruppo-4) e [modello-dati](modello-dati.md).
5. **Form del frontend (01, 06, 08, 10, 16, 02, 07, 13)**: **corretti il 27/11**.
   - 01 e 08: `TimePicker` e `DatePicker` hanno un secondo modello, `v-model:non-valido`, vero quando nel campo c'è testo che non è un orario o una data valida. I form lo usano per dire "Orario non valido." o "La data di fine non è valida." invece di salvare senza orario o di dire "obbligatoria" (agenda, date del piano).
   - 10: obiettivo calorico maggiore di 0 (`erroreObiettivoKcal`).
   - 06 e 07: grammi delle righe del piano. Un meno incollato resta visibile con "La quantità non può essere negativa." e una porzione 0 o vuota blocca il salvataggio, con l'errore sotto la riga. Se l'errore è in un altro giorno, l'editor passa a quel giorno (`erroreGrammiPorzione`, `validaGrammi`).
   - 02: nel form alimento la somma di proteine, grassi e carboidrati non può superare la quantità di riferimento, che deve essere maggiore di 0.
   - 16 e limiti di lunghezza (proposta di Andrea): `maxlength` su tutti i campi di testo da `config/limitiCampi.ts`, sempre uguale o minore del backend, e `components/form/ContatoreCaratteri.vue`. Il contatore è sempre visibile sulle note e compare dall'80% sui campi brevi; al limite mostra "Massimo N caratteri".
   - 13: `composables/useConfermaUscita.ts`, una guardia globale (`router.beforeEach`) attiva finché la pagina è montata, più l'avviso del browser alla chiusura della scheda. Usata da editor del piano, visita e nuovo paziente: "Uscire senza salvare?" vale anche per sidebar, header, Ctrl+K e indietro del browser. Dopo un salvataggio riuscito `consentiUscita()` lascia passare la navigazione successiva.
   - Anche `scorriAlPrimoErrore` ora sceglie il primo errore visibile (mobile e desktop sono entrambi nel DOM).
   - Il link del videoconsulto nel frontend usa la stessa regola del backend (bug 14).
6. **Stampa PDF (04)**: **corretto il 27/11**. "Stampa PDF" non stampa più la pagina dell'editor ma `components/pianiAlimentari/PianoStampa.vue`, un documento A4 montato sul `body` con un Teleport. A schermo è nascosto; in stampa è l'unica cosa visibile (`body > :not(.stampa-piano)` nascosto, regole in fondo a `main.css`), così niente comandi e niente taglio al primo foglio.
   - Contenuto: intestazione con logo, "Hexis" e nome del professionista; nome del piano, paziente, periodo e obiettivo. Poi, a seconda della modalità: giorni e pasti con gli alimenti e i grammi su una linea di puntini; la tabella degli obiettivi del giorno; oppure gli esempi per pasto. In fondo "Stampato il …".
   - Si saltano pasti e giorni vuoti; i pasti non si spezzano tra due fogli.
   - **Sempre su carta bianca, anche con il tema scuro** (indicazione di Andrea): il documento usa colori propri, non i token del tema, e in stampa `html`/`body` sono forzati in chiaro.
   - Il documento riflette ciò che è a schermo, comprese le modifiche non salvate.
   - Il logo accanto a "Hexis" l'ha chiesto Andrea.
   - Test: `PianoStampa.spec.ts` (4 casi). Provato nel browser rendendo visibile il documento con uno stile temporaneo: piano a pasti, piano a macronutrienti e tema scuro. Una settimana piena (stato locale riempito per prova, non salvato) occupa circa quattro fogli e mezzo. L'anteprima di stampa vera di Chrome non è stata aperta da Claude.
   - **Verificato il 28/11 con PDF reali** da un agente, con Chrome headless e `Page.printToPDF` in A4. Sono stati provati i piani a pasti (7 giorni × 5 pasti × 4 alimenti, 6 fogli), a macronutrienti (1 foglio) e a esempi (3 fogli), ciascuno col tema chiaro e scuro. Esito:
     - nessun comando dell'app, contenuto completo e nessun pasto spezzato;
     - carta bianca anche col tema scuro (i PDF chiaro e scuro hanno la stessa dimensione);
     - intestazione e "Stampato il …" presenti.
   - **Difetto trovato (minore)**: quando un giorno continua sul foglio dopo, il nuovo foglio parte da un pasto senza dire di che giorno è (es. "Spuntino mattina" di Sabato in cima al foglio 5). Ci sono due correzioni possibili: `break-inside: avoid` sul giorno intero, che ha senso perché un giorno pieno occupa circa 0,8 fogli, oppure un'intestazione "Sabato (segue)". **Corretto il 28/11** su richiesta di Andrea. Un primo tentativo (`break-inside: avoid` sul giorno intero) è stato scartato dopo la prova con un giorno più lungo di un foglio: lasciava vuoto il primo foglio e il titolo mancava lo stesso. Soluzione finale: in `PianoStampa.vue` ogni giorno (piano a pasti) e ogni pasto (piano a esempi) è una tabella `.sp-blocco` con il titolo nel `<thead>`, che Chrome ripete in cima a ogni foglio su cui il blocco continua; si spezza solo tra un pasto e l'altro (`break-inside: avoid` sulle righe), bordi `separate` perché con `collapse` il filetto del pasto spostato restava in fondo al foglio prima.
   - **Controllo di tutte e tre le modalità il 28/11** (PDF reali, chiaro e scuro), anche con casi limite creati apposta ("Limite PASTI/MACRO/ESEMPI" del paziente Stampa Prova): un giorno con 5 pasti × 8 alimenti e note lunghe, un pasto degli esempi con 9 opzioni, note lunghe su tutti i giorni dei macronutrienti e un valore mancante. Pasti ed esempi: il titolo si ripete sul foglio dopo ("Lunedì", "Pranzo"), nessun foglio vuoto, settimana normale in 6 fogli. Macronutrienti: già corretto (tabella su un foglio, note per giorno intere, valore mancante come "—"), nessuna modifica. Altre note solo estetiche: filetto del pasto in cima al foglio, spazio vuoto in fondo al primo foglio.

Rimandato il **20** (inviti nascosti). **Test di regressione: li scrive Claude** (deciso da Andrea il 27/11). Scritti il 27/11, 30 test nuovi, suite 226/226:

- `PianoAlimentareRegressioniCollaudoTest`: 18 (seconda scheda con versione vecchia → 409, nessuna sovrascrittura), 21 (due attivazioni in parallelo, ripetuto 5 volte: un solo piano attivo, mai 500), 10, 07, nome oltre 200, ruolo (paziente → 403).
- `AgendaRegressioniCollaudoTest`: 22 (due visite sovrapposte in parallelo, 5 volte: una 201 e una 409), 11, 14, 15.
- `SessioneDopoResetCollaudoTest`: 19 (token vecchio → 401 su `/auth/me` e `/pazienti`, vecchia password rifiutata, nuovo login valido).
- `VincoliDatabaseCollaudoTest`: 17, scritture dirette con `JdbcTemplate` respinte dai `CHECK`, con casi validi di controllo.
- In coda ai test esistenti: 16 (`PazienteControllerTest`) e 02 (`AlimentoControllerTest`).
- Controprova: senza l'advisory lock dell'agenda il test del 22 fallisce 5 volte su 5. Helper `support/Concorrenza.inParallelo`.
- Frontend (gruppo 5, 15 test nuovi, vitest 322/322): `useConfermaUscita.spec.ts` (13), `TimePicker.spec.ts` (01), un caso in `DatePicker.spec.ts` (08), `utils/validators.spec.ts` (10, 06, 07, 08), due casi in `AlimentoFormDialog.spec.ts` (02).
- Provati da Claude nel browser sull'ambiente di Andrea, senza salvare nulla: bozza "QA-2711 Piano collaudo (copia)" con obiettivo 0, fine 31/02/2027 e -10 g incollato (tre errori, nessun PUT); clic su Agenda nella sidebar con modifiche aperte (compare la conferma, "Esci senza salvare" porta in Agenda); nuovo promemoria alle 25:99 ("Orario non valido.", nessun POST).

**Piano con inizio futuro**: deciso il 27/11 con Andrea, non si attiva finché la data non arriva (409 `PIANO_NON_ATTIVABILE`, voce del menu disabilitata con "Dal …"). Insieme è stato corretto anche il **09** (si attivano solo le bozze, stesso 409), anticipato dal gruppo 4 perché sta nello stesso metodo.

**Prove a mano del gruppo 1, fatte da Claude il 27/11** (backend e Vite di prova sulle porte 8081/5174, database `hexis_test`, professionista di prova): stessa bozza in due schede, la seconda salva e la prima riceve il toast "modificato in un'altra scheda" senza sovrascrivere (sul server restano i valori della seconda, `versione` 1); attivazione di una bozza con un piano già attivo, il precedente passa a `CONCLUSO`; da API, riattivare un piano non in bozza e attivare una bozza con inizio futuro rispondono 409. Nessun errore in console oltre al 409 atteso.

## Elenco

| # | Area | Dove | Sintesi |
|---|---|---|---|
| 01 | Agenda | frontend | Un orario non valido (25:99) in modifica cancella quello salvato, con toast di successo |
| 02 | Alimenti | frontend + backend | Somma dei macronutrienti superiore al peso di riferimento accettata (240 g su 100 g) |
| 03 | Visite | backend | Due visite nello stesso giorno: ordine dello storico diverso da confronto e panoramica; la variazione dipende dalla dimensione della pagina |
| 04 | Piani | frontend | "Stampa PDF": anteprima con i comandi dell'editor, senza intestazione, contenuto tagliato |
| 05 | Ricerca | backend | "Nome Cognome" non trova paziente né piani (solo il cognome funziona); ricerca globale, lista pazienti e lista piani |
| 06 | Editor piano | frontend | Una quantità negativa incollata diventa positiva, senza avviso |
| 07 | Editor piano | frontend + backend | Si salva un alimento con porzione 0 g |
| 08 | Editor piano | frontend | Data inesistente (31/02/2027) segnalata come "obbligatoria" invece che "non valida" |
| 09 | Piani | backend | Il servizio può riattivare un piano concluso (e conclude quello attivo) |
| 10 | Piani | backend + frontend | Il backend accetta fine prima dell'inizio e obiettivo calorico negativo; il form salva 0 kcal |
| 11 | Agenda | backend | `PATCH` "fatto" con JSON vuoto riapre un promemoria completato (200 invece di 400) |
| 12 | Pazienti | backend | Filtro "Obiettivo ultima visita": con due visite lo stesso giorno il paziente compare in entrambi gli obiettivi (stessa radice del 03) |
| 13 | Editor piano | frontend | La sidebar esce dalla bozza senza "Uscire senza salvare?" (il pulsante "Torna ai piani" invece chiede) |
| 14 | Agenda | backend | Videoconsulto: accettato un link `https://?` senza host |
| 15 | Agenda | backend | Durata decimale troncata (15,9 → 15; 480,9 → 480 supera il controllo sul massimo) |
| 16 | Pazienti | frontend + backend | Nome oltre 100 caratteri: nessun errore nel campo, nessun limite nel DTO di modifica |
| 17 | Database | DB | Mancano vincoli `CHECK` su quantità di riferimento, peso visita, grammatura nel piano, intervalli di date di piano e blocco |
| 18 | Piani | backend + frontend | Due schede sulla stessa bozza: l'ultimo salvataggio sovrascrive l'altro senza avviso (niente `@Version`) |
| 19 | Sicurezza | backend | Il reset password non invalida i JWT già emessi (durata 480 minuti) |
| 20 | Inviti | backend | Il link dell'invito usa il dominio segnaposto `app.hexisnutrition.example` invece dell'URL configurato del portale paziente (inviti oggi nascosti, [decisioni/0012](decisioni/0012-primo-rilascio-solo-area-professionista.md)) |
| 21 | Concorrenza | backend | Due attivazioni simultanee possono lasciare due piani attivi |
| 22 | Concorrenza | backend | Due visite sovrapposte create in contemporanea vengono salvate entrambe senza conferma |

## Da decidere

- ~~**Attivare un piano con data di inizio futura**~~ Deciso il 27/11: non si attiva finché la data non arriva (vedi sopra).
- **Ruolo sulle rotte dei piani**: `SecurityConfig` limita pazienti, agenda, alimenti e notifiche al ruolo `PROFESSIONISTA`, i piani no (solo autenticazione). Un paziente riceve 200 con lista vuota o 404, nessun dato esposto. Differenza di configurazione da allineare.

## Verifiche da fare prima del rilascio

Dal report:

1. ~~Test di regressione ripetibili per i casi 18, 19, 21 e 22 dopo le correzioni.~~ Fatti il 27/11 (vedi sopra).
2. ~~Permessi sulle **singole risorse**~~ **Verificati il 28/11, nessuna falla.**
   - **Codice**: ogni servizio carica la risorsa e confronta il `professionistaId`, con 404 identico per "inesistente" e "di un altro". I servizi sono `PazienteService.dettaglio`, `visitaDelPaziente`, `PianoAlimentareService.pianoDiProprieta`, `PianoNotaService.pianoDiProprieta` + `findByIdAndPianoId`, `AgendaService.trova`, `AlimentoService.dettaglio`; i BDA non sono modificabili. Le liste filtrano per professionista con le `Specifications`.
   - **Prove reali**: due agenti, uno per ciascun professionista di prova (Giulia e Marco sul DB `hexis`, backend di Andrea su 8080). Ognuno ha creato i propri dati, poi ha attaccato quelli dell'altro con corpi di richiesta validi. Gli attacchi coprono tutti gli endpoint con ID (GET, PUT, azioni, DELETE) e i casi incrociati: nota altrui con il proprio `pianoId`, visita altrui con il proprio `pazienteId`, `appuntamentoId` altrui su una propria visita, piano o appuntamento per un paziente altrui, ricerche con `pazienteId` altrui. Sono stati verificati anche i token mancanti, alterati o con `sub` cambiato.
   - **Esito**: tutti 404 o 401, nessun dato altrui nelle liste. I dati di entrambi sono rimasti intatti, e quelli di Andrea pure (conteggi uguali a prima).
   - **Limite**: `POST /notifiche/lette` con ID altrui risponde 204, ma il codice filtra per professionista (`NotificheService.segnaLette`). Non è stato provato con ID di notifiche vere.
3. ~~Errori durante il salvataggio~~: rete interrotta, risposta lenta, doppio clic su "Salva" (al massimo un salvataggio, dati recuperabili). **Provata il 28/11: sette problemi trovati e tutti corretti lo stesso giorno (scelta di Andrea: "tutte, come proposto").** Le correzioni sono sotto il problema che risolvono.
   - **Come**: nel browser, sull'ambiente di Andrea (8080/5173), con l'account di Giulia e il paziente "Stampa Prova". Un intercettore di `fetch` simulava quattro casi: rete interrotta (la richiesta non parte), risposta persa (il server salva ma il browser riceve un errore), risposta lenta (6 s) e sessione scaduta (401).
   - **Va bene**:
     - Con la risposta lenta, doppio clic, terzo clic e Invio in un campo producono una sola richiesta: il pulsante è disabilitato e mostra "Salvataggio…".
     - Con la rete interrotta i dati restano nei campi, compare un toast d'errore e "Modifiche non salvate".
   - **S1 — piano duplicato** (`PianoAlimentareFormView.salva`): al primo salvataggio, se `crea` riesce e `aggiorna` fallisce, l'indirizzo passa a `/piani-alimentari/<id>` ma `piano` resta vuoto, perché la vista è la stessa istanza e non ricarica. Al clic successivo su "Salva" si crea un secondo piano. Riprodotto: due bozze "Mantenimento · fase 1" per Stampa Prova.
     **Corretto**: appena `crea` riesce, `piano` prende il piano creato, quindi il tentativo successivo fa solo `PUT`. In più la creazione ha una chiave di idempotenza.
   - **S2 — falso conflitto dopo una risposta persa**: il server ha salvato, il browser no, quindi la `versione` locale è vecchia. Al nuovo "Salva" compare "Il piano è stato modificato in un'altra scheda. Ricarica la pagina…" e non si riesce più a salvare senza ricaricare. Le modifiche fatte dopo l'errore andrebbero perse.
     **Corretto**: dopo un errore di rete la pagina segna l'esito come incerto. Al "Salva" successivo rilegge il piano: se la versione sul server è avanti esattamente di uno, riparte da quella. Con più di una versione di differenza il conflitto resta, perché vuol dire un'altra scheda. Anche un'attivazione senza risposta viene ricontrollata prima di dare errore.
   - **S3 — modifiche durante il salvataggio sovrascritte**: i campi del piano restano modificabili mentre salva, e `applicaPiano` riscrive tutto con la risposta. Riprodotto: obiettivo portato a 2400 durante un salvataggio di 2250. Finito il salvataggio torna 2250 e la barra dice "Salvato alle …" senza "Modifiche non salvate".
     **Corretto**: alla risposta, se il contenuto è cambiato rispetto a quanto inviato, si prende dal server solo lo stato del piano (versione, ultimo salvataggio) e i campi restano. La barra mostra "Modifiche non salvate".
   - **S4 — visita duplicata dopo una risposta persa** (`VisitaFormView`): riprovando, il `POST` parte di nuovo. Riprodotto: due visite del 28/11 con 79,50 kg. Vale per ogni creazione (paziente, appuntamento, alimento, nota), perché il backend non riconosce una richiesta ripetuta. Per il paziente un CF o un'email già esistenti dovrebbero dare un errore invece del doppione: non provato. Con un `appuntamentoId`, il secondo tentativo direbbe probabilmente "l'appuntamento non è più valido: la visita non è stata salvata", che è falso: non provato.
     **Corretto**: header `Idempotency-Key` sui `POST` di creazione e filtro nel backend (V38, [api-contracts](api-contracts.md)). Riprovando con la stessa chiave si riceve la risposta della prima richiesta. Vale anche per il caso con `appuntamentoId`.
   - **S5 — sessione scaduta durante il salvataggio**: il token dura 8 ore (`expiration-minutes: 480`). Il 401 fa `auth.logout()` e porta al login, la conferma "Uscire senza salvare?" si apre e propone di uscire. Con "Continua a modificare" si resta sulla pagina ma senza sessione: nessun salvataggio è più possibile e il lavoro si perde.
     **Corretto**: con un profilo già caricato, il 401 non porta più al login. Apre `SessioneScadutaDialog` sopra la pagina: password per la stessa email, poi si torna alla pagina con i dati intatti e si salva di nuovo. "Esci" porta al login, passando dalla conferma di uscita. Il dialog non si chiude con Esc. Il 401 del login stesso non fa scattare il dialog: `onUnauthorized` parte solo per richieste con token.
   - **S6 — nessun limite di attesa**: `apiRequest` non ha timeout. Se il server non risponde, "Salvataggio…" resta all'infinito e "Annulla" è disabilitato. Visto dal codice, non provato fino in fondo.
     **Corretto**: `apiRequest` annulla la richiesta dopo 30 s (`TIMEOUT_MS`) e lancia `ErroreTimeout`, che è un `ErroreRete`.
   - **S7 — messaggi**: per piano, visita e nuovo paziente, un errore di rete o un 500 dicono "Controlla i dati e riprova", come un errore di validazione. `eErroreConnessione` esiste ma non è usato nei salvataggi.
     **Corretto**: `messaggioErroreSalvataggio` (`utils/erroreSalvataggio.ts`) distingue rete, timeout, 401, `RICHIESTA_IN_CORSO` e 5xx dal messaggio sui dati. È usata in piano, visita, nuovo paziente, modifica anagrafica, appuntamento, alimento e note.
   - **Prove dopo le correzioni** (28/11, ambiente di prova su `hexis_test`, backend 8081 e Vite 5174, account usa e getta): S1, S2, S3, S5 e S6 rifatti nel browser con l'intercettore, tutti corretti. S4 provato sull'API: la stessa chiave due volte dà la stessa risposta e una visita sola; quattro richieste in parallelo danno una creazione e tre `RICHIESTA_IN_CORSO`; chiave su un altro percorso → 422; dopo un 400 la chiave si libera. S4 non è stato rifatto dall'interfaccia, ma il `POST` del piano partiva con l'header. Suite: backend 242 senza errori (il filtro non ha test propri), vitest 341/341, `vue-tsc` pulito. Vitest segnala anche un errore non gestito già presente (`scrollIntoView` assente in jsdom, da un test di `PianoAlimentareFormView.spec.ts`).
   - **Test di regressione** (scritti da Claude su richiesta di Andrea, 28/11):
     - Backend: `IdempotenzaFilterTest`, 10 test. Stessa chiave → una visita e la stessa risposta; chiavi diverse; senza header; chiave liberata dopo un 400; altro percorso → 422; richiesta in corso → 409; prenotazione abbandonata; chiave per utente; chiave troppo lunga; pulizia dopo 24 ore.
     - Frontend, 28 test nuovi:
       - `client.spec.ts`: 401 senza token, header, timeout, errore di rete.
       - `erroreSalvataggio.spec.ts`: 6 test.
       - `auth.spec.ts`: 3 test sulla sessione scaduta.
       - `SessioneScadutaDialog.spec.ts`: 6 test.
       - `PianoAlimentareFormView.salvataggio.spec.ts`: 8 test (S1, S2, S3, chiave del piano, attivazione senza risposta).
       - `VisitaFormView.spec.ts`: stessa chiave al nuovo tentativo.
     - Controprova togliendo le correzioni: falliscono 4 test backend e 6 frontend. Gli altri provano casi in cui la correzione non cambia nulla.
     - Suite: backend 252 senza errori, vitest 369/369, `vue-tsc` pulito. L'errore non gestito già presente di `scrollIntoView` è stato tolto subito dopo (`src/test/setup.ts`, collegato in `vite.config.ts`).
   - **Dati di prova rimasti** sull'account di Giulia: due bozze "Mantenimento · fase 1" e due visite in più per Stampa Prova.
4. Consegna reale degli inviti, quando la funzione sarà riattivata.

## Cosa il collaudo ha confermato

Visibilità delle liste separata tra due professionisti; 401 sulle rotte senza autenticazione; 400 su login, password dimenticata e reset con JSON vuoto; blocchi su più giorni e sovrapposizioni agli estremi corretti; suite esistenti verdi (Vitest 307, Maven 196, 34 migrazioni Flyway valide).
