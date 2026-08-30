# Fonte — Hash, pulizia e invalidazione di `token_azione` (30 agosto 2026)

Tipo: conversazione con Andrea, a partire dai punti aperti registrati il 9 agosto 2026 su `token_azione`.

## Richiesta

Andrea, rileggendo i tre punti aperti su `token_azione`:

> Per i token salvati in chiaro meglio in hash? direi di pulire i token scaduti/usati in modo che la tabella non cresca all'infinito. Direi che le richieste di reset invalidano quelle precedenti.

Tre decisioni confermate, tutte approvate:

1. **Hash invece di plaintext**: il token resta `UUID.randomUUID()` (122 bit di entropia, sufficiente — niente bisogno di un hash lento tipo BCrypt come per le password). Cambia solo cosa finisce nel DB: colonna `token` rinominata `token_hash`, contiene SHA-256 esadecimale. Il valore in chiaro vive solo in un campo `@Transient` dell'istanza appena creata (serve a comporre l'email), mai persistito.
2. **Pulizia**: eliminata la colonna/flag `usato`. Un token consumato viene **cancellato** subito (`resetPassword`, `attiva`), il che impedisce da solo il riuso. Per i token scaduti mai usati, nuovo componente `TokenAzionePulizia` con `@Scheduled(cron = "0 0 3 * * *")` che cancella le righe con `scadenza < now()`.
3. **Invalidazione dei reset precedenti**: `richiediResetPassword` cancella gli eventuali `TokenAzione` di tipo `RESET_PASSWORD` già esistenti per quella persona prima di crearne uno nuovo. Scoping per `tipo` così non tocca eventuali token di `INVITO` ancora validi per la stessa persona.

## Processo seguito

Bounded task (flusso già esistente in `TokenAzione`/`TokenAzioneRepository`/`AuthService`/`PazienteService`), design approvato in chat, poi implementazione in tre cicli TDD separati (hash+cancellazione-su-uso insieme perché condividono la stessa migrazione di schema; invalidazione reset; cleanup schedulato), ciascuno con test scritti prima e verificati RED prima dell'implementazione.

## Esito

- Migrazione `V4__token_azione_hash_e_pulizia.sql`: rinomina `token`→`token_hash`, elimina `usato`.
- `mvn test`: **36 test, 0 fallimenti, BUILD SUCCESS** (33 preesistenti + 3 nuovi: `salvaERitrovaPerTokenHash` aggiornato, `resetPasswordConTokenGiaUsatoRestituisceErrore`, `unaNuovaRichiestaDiResetInvalidaLaPrecedente` + variante paziente, `cancellaITokenScadutiELascialiValidi`).
- Un test end-to-end preesistente (`flussoCompletoInvitoAttivazioneELoginFunziona`) recuperava il token ricaricandolo dal DB via `findAll()` — con l'hash questo non funziona più (il valore in chiaro non è mai persistito). Corretto per estrarre il token dal corpo dell'email inviata (`FakeEmailSender`), che è il modo realistico in cui un paziente lo riceverebbe davvero.
