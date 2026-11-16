---
title: 0008 - Notifiche dell'agenda
tags: [adr, agenda, notifiche, push]
stato: stabile
creato: 2026-11-16
aggiornato: 2026-11-16
fonti: [../../docs/superpowers/specs/2026-11-16-notifiche-agenda-design.md, ../../docs/superpowers/plans/2026-11-16-notifiche-agenda.md, 0007-appuntamenti-e-visite-registrate.md]
---

# ADR 0008 — Notifiche dell'agenda

## Contesto

Il campanello nell'header era decorativo e la voce Agenda non aveva indicatori. Il professionista deve essere avvisato che una visita o un promemoria sta per iniziare, anche con Hexis chiuso. La 0007 lasciava le notifiche fuori scope.

## Decisione

- **Cosa notifica**: visite (studio e videoconsulto) e promemoria **con orario**. Non i blocchi, i promemoria senza ora, le visite annullate o concluse, i promemoria fatti.
- **Scelta per evento**: checkbox "Ricevi notifica" (default sì) e anticipo tra 2 ore, 1 ora, 45, 30, 15 minuti (default 45). Gli eventi già esistenti alla migrazione V31 passano a `notifica = false`.
- **Dove compare**: lista del campanello (con puntino che pulsa finché non si clicca il bottone e sparisce alla chiusura della lista), puntino statico sulla voce Agenda (sparisce aprendo la pagina Agenda), push del browser.
- **Push generica**: "Hai una visita/un promemoria tra N minuti", **senza nome del paziente né titolo** (può comparire a schermo bloccato). Il nome compare solo nel campanello.
- **Approccio**: notifiche nel backend (B1). Uno scheduler Spring (ogni minuto) crea le righe in `notifiche` e invia le push solo per quelle appena inserite; il campanello e i puntini leggono la stessa fonte via `GET /notifiche` (polling 60 s). Scartati: calcolo solo nel frontend (A: nessuna push a app chiusa) e push separate dal campanello (B2: la scadenza sarebbe calcolata in due posti).
- **Regole di scatto**: `data + oraInizio − anticipo` in `Europe/Rome`; se l'evento è creato già dentro la finestra scatta al primo passaggio utile, ma solo se non è ancora iniziato. Una modifica che non cambia lo scatto non ricrea la notifica; una che lo cambia, o un annullo/fatto/eliminazione, elimina le notifiche obsolete (`NotificheService.riallinea`). Una pulizia notturna (03:00) elimina le notifiche scattate prima di oggi.
- **Stato di lettura server-side**: `letta_il` e `agenda_vista_il`, indipendenti. `POST /notifiche/lette` riceve gli id mostrati, così una notifica arrivata a lista aperta non si segna letta senza essere stata vista.
- **Web Push**: libreria `nl.martijndwars:web-push` con BouncyCastle, chiavi VAPID da variabili d'ambiente (`VAPID_PUBLIC_KEY`, `VAPID_PRIVATE_KEY`, `VAPID_SUBJECT`; generabili con `npx web-push generate-vapid-keys`). **Senza chiavi le push sono disattivate** e il resto funziona. Service worker in `frontend-professionisti/public/sw.js`: non mostra l'avviso se esiste già una scheda Hexis visibile; le iscrizioni 404/410 si eliminano; al logout l'iscrizione del dispositivo si rimuove.
- **Browser**: Chrome, Edge, Firefox su Windows e macOS; Safari 16+ su macOS 13+. Il permesso si chiede con un gesto dell'utente (bottone nella lista del campanello). In produzione serve HTTPS (su `localhost` no).

- **Sicurezza dell'iscrizione push** (dalla revisione): l'`endpoint` lo sceglie il client e il backend vi invia richieste, quindi `POST /notifiche/push/iscrizioni` accetta solo **https** su una allowlist di servizi push (`app.push.endpoint-ammessi`: `fcm.googleapis.com`, `push.services.mozilla.com`, `notify.windows.com`, `push.apple.com`), massimo 10 dispositivi per professionista, campi con lunghezza massima; altrimenti 400. L'invio ha un timeout di 10 s e lo scheduler usa 2 thread (`spring.task.scheduling.pool.size`), così un servizio push lento non ferma gli altri job.

## Limiti noti

- Si assume **una sola istanza** del backend: la push parte solo se l'inserimento in `notifiche` è riuscito, quindi un riavvio a metà ciclo non duplica, ma con più istanze serve un lock (per esempio ShedLock).
- Niente email, niente iOS/PWA, niente posticipo né orari silenziosi, solo notifiche dell'agenda.
- Se lo scheduler è fermo nel momento dello scatto e l'evento è già iniziato al riavvio, la notifica non viene creata.

## Riferimenti

API in [api-contracts](../api-contracts.md) (sezione Notifiche), tabelle in [modello-dati](../modello-dati.md).
