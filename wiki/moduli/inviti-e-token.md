---
title: Inviti e token di azione
tags: [modulo, auth, inviti, token]
stato: stabile
creato: 2026-08-09
aggiornato: 2026-08-09
fonti: [sorgenti/2026-08-08-brainstorming-fondamenta-e-scope-funzionale.md]
---

# Modulo — Inviti e token di azione

Package `com.hexisnutrition.backend.inviti`, più i servizi che lo usano: [`PazienteService`](../../backend/src/main/java/com/hexisnutrition/backend/pazienti/PazienteService.java) e [`AuthService`](../../backend/src/main/java/com/hexisnutrition/backend/auth/AuthService.java).

## A cosa serve

`token_azione` è la tabella dei **permessi temporanei per chi non può autenticarsi**. Ci sono due momenti in cui una persona deve compiere un'azione senza poter fare login:

1. il **paziente appena invitato**, che non ha ancora una password;
2. chiunque **abbia dimenticato la password** e debba reimpostarla.

In entrambi i casi l'unica prova d'identità disponibile è il possesso della casella email. Il token è materialmente quella prova: una stringa casuale (`UUID.randomUUID()`) spedita dentro un link, che chi la riceve rimanda indietro per dimostrare di essere il destinatario legittimo.

Un'unica tabella copre i due scopi perché il meccanismo è lo stesso — scelta presa in [decisioni/0002](../decisioni/0002-autenticazione-e-onboarding.md).

## Struttura

Definita in [`V3__create_token_azione.sql`](../../backend/src/main/resources/db/migration/V3__create_token_azione.sql):

| Colonna | Ruolo |
|---|---|
| `token` | il segreto che viaggia nel link; UUID casuale, `UNIQUE` |
| `tipo` | `INVITO` oppure `RESET_PASSWORD` |
| `professionista_id` / `paziente_id` | a chi si riferisce il token |
| `scadenza` | `TIMESTAMPTZ`; 7 giorni per l'invito, 1 ora per il reset |
| `usato` | flag che rende il token monouso |
| `creato_il` | `TIMESTAMPTZ`, default `now()` |

Il vincolo `chk_token_azione_target` impone che sia valorizzato **esattamente uno** fra `professionista_id` e `paziente_id`, mai entrambi né nessuno: i professionisti possono solo resettare la password, i pazienti anche attivare l'account.

I campi temporali sono `TIMESTAMPTZ` e non `TIMESTAMP`: con un tipo senza fuso orario la scadenza veniva calcolata in modo errato fuori da UTC. È una correzione della revisione finale dell'8 agosto, coperta dal test `resetPasswordConTokenScadutoRestituisceErrore`.

## Flusso 1 — Invito di un paziente

`PazienteService.invita`, endpoint `POST /pazienti/{id}/invito` (solo PROFESSIONISTA):

1. Il professionista invita un paziente **già presente in anagrafica** — l'anagrafica esiste indipendentemente dall'account, vedi [modello-dati](../modello-dati.md).
2. Se il paziente è già `ATTIVO` → `409` (`PazienteGiaAttivoException`): non si reinvita chi ha già un account.
3. Nasce un token `INVITO` valido **7 giorni**, il paziente passa a `INVITATO`, parte l'email con il link.

`PazienteService.attiva`, endpoint `POST /inviti/{token}/attiva` (pubblico):

1. Il token deve essere valido e **di tipo `INVITO`**.
2. Controllo che l'email non sia già usata da un professionista o da un altro paziente `ATTIVO` → altrimenti `409` (`EmailGiaInUsoException`). Il controllo avviene qui e non alla creazione dell'anagrafica, perché l'unicità dell'email è richiesta solo fra gli account attivi.
3. Password salvata come hash BCrypt, stato → `ATTIVO`, token marcato come usato.

## Flusso 2 — Reset della password

`AuthService.richiediResetPassword`, endpoint `POST /auth/password-dimenticata` (pubblico): cerca l'email fra i professionisti e fra i pazienti `ATTIVO`, e per chi trova crea un token `RESET_PASSWORD` valido **1 ora**, poi invia il link.

L'endpoint risponde **sempre** 204, anche per un'email inesistente, e persino se l'invio dell'email fallisce (l'eccezione viene catturata e solo loggata): una risposta diversa nei due casi permetterebbe di scoprire quali indirizzi sono registrati.

`AuthService.resetPassword`, endpoint `POST /auth/reset-password` (pubblico): verifica il token, imposta il nuovo hash sul professionista **oppure** sul paziente a seconda di quale dei due riferimenti è valorizzato, e marca il token come usato. Il metodo è `@Transactional`: aggiornamento della password e consumo del token stanno o cadono insieme.

## Le due condizioni di validità

Sono la parte da non toccare senza capire cosa proteggono. In `attiva` e in `resetPassword`:

```java
.filter(TokenAzione::isValido)                        // non usato E non scaduto
.filter(t -> t.getTipo() == TipoToken.RESET_PASSWORD)  // E del tipo giusto
```

Il secondo filtro non è ridondante: senza, un token di **invito** — che dura sette giorni e viene consegnato a un utente non ancora verificato — potrebbe essere speso sull'endpoint di reset password, e viceversa. È uno dei buchi trovati nella revisione finale dell'8 agosto; il test `resetPasswordConTokenDiTipoInvitoRestituisce400` esiste apposta per impedirne il ritorno.

## Punti aperti

Tre comportamenti mai discussi in fase di progettazione, emersi rileggendo il codice il 2026-08-09 — dettaglio e alternative in [domande-aperte](../domande-aperte.md):

- i token sono salvati **in chiaro** in tabella;
- **nessuna pulizia** di token usati o scaduti;
- richieste di reset ripetute **non invalidano** le precedenti.

## Pagine collegate

- [api-contracts](../api-contracts.md) — contratto degli endpoint citati qui.
- [modello-dati](../modello-dati.md) — `Paziente`, `StatoAccountPaziente` e le altre entità.
- [decisioni/0002](../decisioni/0002-autenticazione-e-onboarding.md) — perché invito e reset condividono il meccanismo, e perché l'account paziente è opzionale.
