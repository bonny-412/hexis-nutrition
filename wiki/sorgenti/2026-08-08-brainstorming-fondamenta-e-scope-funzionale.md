---
titolo: Brainstorming — scope funzionale e design "Fondamenta"
data: 2026-08-08
tipo: estratto-conversazione
---

# Fonte grezza — non modificare

Sintesi delle decisioni prese con Andrea durante una sessione di brainstorming (skill `superpowers:brainstorming`) sul nucleo funzionale di hexis-nutrition e sul design del primo sotto-progetto da implementare.

## Scope funzionale del prodotto

- Il professionista usa l'app per due flussi ugualmente centrali: creare/assegnare piani alimentari e gestire monitoraggio/appuntamenti dei pazienti.
- L'area cliente (paziente) permette di: vedere i piani alimentari assegnati, chattare con il professionista, e (dal lato monitoraggio) auto-registrare misurazioni.
- Un account professionista gestisce un solo professionista (no studio/team con più professionisti che condividono pazienti).
- Onboarding paziente: il professionista crea sempre l'anagrafica; l'attivazione dell'account (via invito) è opzionale — il paziente può esistere nel sistema senza mai attivare un account.
- Piano alimentare: strutturato (giorni/pasti/alimenti), con calcolo automatico di calorie/macro. Database alimenti: dataset pubblico importato come base + alimenti custom per professionista.
- Monitoraggio: appuntamenti (con flusso di richiesta dal paziente + conferma del professionista) e misurazioni corporee (peso, ecc.), inseribili sia dal professionista (in visita) sia dal paziente (auto-registrazione).
- Fatturazione/pagamenti: fuori scope per l'MVP, rimandata.
- Terminologia chiarita: "cliente" è il termine business per il professionista (chi paga l'abbonamento SaaS); "paziente" è la persona seguita, che usa `frontend-cliente`. Sono due concetti distinti nonostante il nome della cartella `frontend-cliente`.

## Decomposizione in sotto-progetti

Lo scope è stato diviso in 4 sotto-progetti sequenziali, ciascuno da specificare e implementare separatamente:

1. **Fondamenta** — autenticazione, anagrafica paziente, invito/onboarding.
2. **Piano alimentare** — database alimenti, costruzione/assegnazione piani.
3. **Monitoraggio** — misurazioni e appuntamenti (richiesta/conferma).
4. **Chat** — messaggistica professionista-paziente.

Ordine motivato dalla dipendenza tra i pezzi (non si assegna un piano senza un paziente; non si prenota una visita senza un paziente collegato) e dal fatto che il piano alimentare è il valore centrale del prodotto.

## Design dettagliato — sotto-progetto "Fondamenta"

**Scope**: autenticazione JWT unica con ruoli (PROFESSIONISTA, PAZIENTE); creazione manuale degli account professionista (no self-signup, beta chiusa); anagrafica paziente (nome, cognome, email, telefono, data di nascita, sesso, altezza) creata dal professionista; invito paziente via email con link di attivazione; recupero password self-service per entrambi i ruoli, stessa infrastruttura email dell'invito.

**Entità**: `Professionista`, `Paziente` (con stato account: mai invitato / invitato / attivo), token di invito/reset (stesso meccanismo per invito paziente e reset password).

**Provider email**: Resend, scelto su raccomandazione per API semplice via HTTP e piano gratuito (3000 email/mese) adeguato a un SaaS in fase iniziale. Integrato dietro un'interfaccia astratta (`EmailSender`) per restare sostituibile.

**Flusso**:
1. Account professionista creato manualmente da Andrea.
2. Professionista fa login → JWT con ruolo PROFESSIONISTA.
3. Professionista crea anagrafica paziente (senza account).
4. Professionista invia invito → token generato, email via Resend.
5. Paziente apre link, imposta password → account attivato.
6. Paziente fa login → JWT con ruolo PAZIENTE.
7. Reset password disponibile per entrambi i ruoli via lo stesso meccanismo a token + email.

**Error handling**: token scaduto/già usato gestito con messaggio chiaro e possibilità di rigenerare (per l'invito); email duplicata gestita a livello di vincolo DB; fallimento invio email non silenzioso, il professionista può ritentare.

**Testing**: test con Testcontainers su repository/flussi auth-inviti-reset; verifica che i claim di ruolo limitino correttamente gli endpoint; `EmailSender` mockabile nei test.

**Fuori scope per "Fondamenta"**: self-signup professionista.
