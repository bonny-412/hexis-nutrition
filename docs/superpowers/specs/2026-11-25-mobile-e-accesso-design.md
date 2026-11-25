# Mobile e pagine di accesso — design breve

Data: 2026-11-25. Approvato da Andrea in chat ("procedi"). Origine: §4 e §6 punti 4-5 (più il piccolo "Invita doppione") di [`docs/ui-review-2026-11-25.md`](../../ui-review-2026-11-25.md). Costruito con frontend-design e superpowers:brainstorming.

## Perché

A 390 px l'agenda parte su una settimana illeggibile e la scheda paziente spende il primo schermo sulla testata, con il profilo prima dei dati clinici. "Password dimenticata" e "Imposta nuova password" sembrano pagine di un'altra app.

## Obiettivi / non obiettivi

- Obiettivi: agenda in vista Giorno da mobile; testata del paziente compatta da mobile con un'azione principale; dati clinici prima del profilo da mobile; un solo controllo per l'invito; tre pagine di accesso con lo stesso layout del login; autocompletamento nei colori dell'app.
- Non obiettivi: backend, scorrimento/trascinamento in agenda, ridisegno della scheda paziente su desktop.
- Anti-obiettivo: nascondere azioni senza lasciarle raggiungibili (tutto ciò che sparisce dalla testata resta nel menu "⋯").

## Approcci considerati

1. **Classi responsive nei componenti esistenti + un `AccessoLayout.vue` (scelto)**: poche righe per pagina, nessuna duplicazione del pannello del login.
2. Una scheda paziente separata per mobile: due template da tenere allineati.
3. Pagine di accesso copiate dal login: tre copie del pannello.

## Design

- Agenda: `vista` iniziale `GIORNO` se `matchMedia('(max-width: 767px)')`, altrimenti `SETTIMANA`.
- Scheda paziente sotto 640 px: avatar 40 px, nome `text-xl`; sotto il nome solo obiettivo e prossima visita (email nel profilo); azioni: "Nuova visita" (`accent`) + menu "⋯" con "Nuovo piano" e "Fissa appuntamento". Da 640 px in su tre pulsanti come oggi.
- Invito: niente pillola accanto al nome; il badge di stato `MAI_INVITATO`/`INVITATO` (non archiviato) è un pulsante "Invita"/"Reinvia invito" con l'icona dello stato; `ATTIVO` resta un badge.
- Panoramica: il profilo va dopo i dati clinici anche da mobile (`order` invertito).
- `components/auth/AccessoLayout.vue`: pannello scuro del login (logo, pillola con `ShieldCheck`, titolo e testo, footer) da `md`, logo compatto sopra il form da mobile; slot per il contenuto. Login, Password dimenticata e Reset lo usano; campi `h-11 rounded-xl`, pulsante `h-11` a tutta larghezza come il login. Conferma di invio come stato con icona `MailCheck`.
- `main.css`: `:-webkit-autofill` con sfondo `--surf` e testo `--fg`.

## Cambi durante l'implementazione e dopo la revisione

- Invito: invece di un badge che diventa pulsante, una pillola unica con lo stato e, separato da una linea, il pulsante "Invita"/"Reinvia invito" (lo stato resta leggibile, i test esistenti restano validi).
- Autocompletamento: la regola sta fuori da `@layer` con `!important` (le utility di Tailwind la scavalcavano al focus) e somma l'anello di focus; i campi delle pagine di accesso usano `--surf` anche in scuro.
- Minor corretti: riga vuota sotto il nome da mobile, `role="status"` sulla conferma d'invio, testo "8 caratteri" non ripetuto, logo con `alt=""`, "Privacy Policy" non più finto link. Rimandati: area di tocco del pulsante "Invita" nella pillola (~24 px), colori `white/*` del pannello non a token, ordine `h2` del pannello prima dell'`h1`, vista dell'agenda scelta solo all'apertura (ruotando il tablet non cambia).

## Lasciato a chi implementa

Spaziature, adeguamento dei test esistenti. Nessun test nuovo di default; nessun commit.
