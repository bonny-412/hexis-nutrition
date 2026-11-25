---
title: 0009 - Palette "Verde petrolio + Lime"
tags: [adr, ui, palette, frontend]
stato: in-discussione
creato: 2026-11-16
aggiornato: 2026-11-25
fonti: [../../frontend-professionisti/src/assets/main.css, ../stato.md, ../log.md]
---

# ADR 0009 — Palette "Verde petrolio + Lime"

## Contesto

Il dietologo che userà l'app ha scelto, da una conversazione con ChatGPT, la palette "Verde petrolio + Lime": `#087F74` (petrolio), `#C7F36B` (lime), `#F5F8F6` (sfondo chiaro), `#183C3A` (verde scuro), `#FFFFFF`. Prima l'app usava un verde bosco (`#1B5E3F`) su sfondo caldo. **Stato: in prova.** Il tema chiaro è approvato da Andrea; il tema scuro è stato rifatto due volte (vedi sotto), l'ultima versione vista nel browser da Claude il 2026-11-16.

## Decisione

- **Tutto passa dai token** di `frontend-professionisti/src/assets/main.css`: nessun colore esadecimale nei componenti. Cambiare palette significa rimappare i token, in chiaro e in scuro.
- **Petrolio per i pulsanti** (`--green`, hover `--green-d`), **lime solo per i segnali**: puntino del campanello, puntino sulla voce Agenda, pallino "non letta", barretta della voce attiva in sidebar. Il lime non è mai testo né pulsante su fondo bianco (1,3:1). Il cerchio del giorno corrente nel calendario resta petrolio (il lime non è piaciuto ad Andrea).
- **Blocchi "hero"** (`--hero`, `--hero-d`, `--hero-track`, `--on-hero`, `--on-hero-2`, `--on-hero-warn`, `--lime-d`, `--on-lime`, `--danger-fixed`): un solo blocco petrolio per pagina, con testi chiari e lime per l'azione principale e le barre di avanzamento. Dove sono: banner della Dashboard, intestazione della scheda paziente, card del piano attivo, tessera "Energia" nell'editor del piano. La fascia "Oggi" dell'agenda è stata tolta il 2026-11-16 (ripeteva la Dashboard; in agenda basta il pulsante "Oggi"). Pillole e pulsanti secondari sul petrolio usano i token `--hero-pill`, `--hero-pill-hover`, `--hero-pill-fg`, `--hero-pill-ring`: bianchi pieni in chiaro, vetro traslucido in scuro. Mai `bg-white` scritto a mano.
- **Errori e avvisi**: `--danger` rosso lampone `#B82E47` (scuro `#F27A8C`), `--warn-bg` `#FFF4D9` con `--warn-fg` ambra `#855600` (scuro `#2A2410` / `#E6C45C`). Scelti per stare con petrolio e lime, contrasti ≥ 4,8:1.
- **Contrasti**: testo secondario `--fg3` `#5A7673` (4,9:1) e `--fg4` `#77918D`; testo su menta con `--green-d` (5,8:1), non `--green` (4,2:1).
- **Hover dei pulsanti** centralizzato nel componente `Button`: il primario passa a `--green-d` (non più `bg-primary/80`), le conferme di eliminazione a `--danger-d`; niente classi hover manuali sui singoli pulsanti.
- **Etichette dei campi** in minuscolo (13 px, peso medio) in tutta l'app. Dal 2026-11-16 anche intestazioni di tabella, titoli delle card dei grafici e dei blocchi della scheda paziente sono in minuscolo; restano in maiuscolo solo giorni del calendario, sidebar e piccole etichette del profilo paziente.
- **Tipografia nelle tabelle**: Fraunces solo per i titoli di pagina e di card; nomi di pazienti e alimenti nelle righe in Figtree. I nomi della BDA (in maiuscolo) si mostrano con `nomeAlimentoLeggibile` (`utils/testo.ts`): "Acciughe o alici".
- **Tema scuro "Notte di petrolio"** (2026-11-16, terza versione, provata dal vivo nel browser prima di applicarla): fondo quasi nero con poco verde (`--bg` `#0B1413`, `--surf` `#121D1C`), sidebar più scura della pagina (`--side` `#060E0D`) come l'isola scura del tema chiaro, un solo petrolio per i primari (`--green` `#3DBFAE` con testo scuro `#04201C`), blocchi hero in petrolio profondo (`--hero` `#0E3A35`) invece del petrolio pieno che abbagliava, voce attiva della sidebar con tinta petrolio invece del lime (che diventava kaki). Il tema chiaro non è cambiato.
- **Calendario**: il videoconsulto ha il bordo sinistro verde scuro (`--fg`), per distinguerlo dalla visita in studio. Gli appuntamenti sovrapposti nella vista Settimana si sfalsano a cascata (`stileCorsia` in `utils/agenda.ts`) invece di dividersi la colonna.
- **Grafici** (2026-11-16, i quattro verdi erano indistinguibili): `--chart-1` petrolio `#087F74` (peso, proteine), `--chart-2` viola `#7559C4` (BMI, grassi), `--chart-3` ocra `#C27C12` (% grasso, carboidrati), `--chart-4` blu `#3A6CC2` (massa magra); in scuro `#3DBFAE` / `#AE97EE` / `#E5B04A` / `#82A6EE`. Nel confronto tra visite la visita A è grigia (`--fg4`), la B petrolio.
- **Logo e favicon** (2026-11-16): la H passa dal verde bosco `#236545` al petrolio chiaro `#4FB3A7`, la foglia dalla salvia `#85C49A` al lime `#C7F36B`; il logo vive su fondo scuro (sidebar, pannello login). La favicon e il logo del login da mobile stanno in una tessera verde scuro `#183C3A`, leggibile su fondi chiari e scuri. Originale salvato fuori dal repo solo per la sessione: si recupera da git.
- **Pulsanti testo**: niente sottolineatura; all'hover il colore passa a `--green-d` e la freccia, se c'è, si sposta di 2 px (solo con movimento consentito, `motion-safe`). La freccia `ChevronRight` solo sui link che portano a un'altra pagina, non sulle azioni. "Invita" nell'intestazione del paziente è diventato una pillola hero.
- **Ombre** (2026-11-16): nessuna ombra sulle card dentro la pagina, basta il bordo (`--bd`). L'ombra resta solo su ciò che sta sopra la pagina: dialog, sheet, menu, select, popover, command palette, tooltip dei grafici, pannello "visita precedente" aperto da mobile.
- **Pressione dei pulsanti azione**: tutti i `Button` scendono di 2 px al click (`active:translate-y-0.5` nella base di `buttonVariants`, anche per i trigger dei menu); la variante `link` (pulsante testo) no. Prima era 1 px ed escluso dai trigger dei menu, tranne "Crea nuovo" in Dashboard.
- **Link "Torna indietro"**: grigi (`--fg3`), petrolio all'hover, testo 12 px con `ArrowLeft` da 14 px che scorre di 2 px a sinistra all'hover (`motion-safe`). Stesso stile in scheda paziente, nuovo paziente, visita, piano e "Password dimenticata" (prima usava il carattere `←`).

- **Revisione del 2026-11-25** (vedi [`docs/ui-review-2026-11-25.md`](../../docs/ui-review-2026-11-25.md)):
  - *Contrasti, chiaro*: `--fg4` (3,4:1) non si usa più per testo che porta informazione: quel testo è `--fg3`; `--fg4` resta per icone, placeholder, separatori. `--on-hero-2` `#EAF6F4` → `#F2FAF8` (4,6:1).
  - *Contrasti, scuro*: superfici alzate perché card e pagina erano a 1,08:1 (`--surf` `#16231F`, `--soft` `#1A2926`, `--hover` `#1E2E2B`, `--div` `#203230`, `--bd` `#2C403C`, `--bd2` `#344A46`); `--fg4` `#7A8F8B` (4,7:1); `--side-bd` `#263935` per rendere visibile il bordo della sidebar.
  - *Pulsanti*: varianti `hero` (pillola sui blocchi petrolio) e `accent` (lime, azione principale del blocco) nel componente `Button`, mai più classi scritte a mano; `outline` non si usa, il secondario è `neutral`; le azioni distruttive usano solo le varianti `destructive-*`; i pulsanti solo icona hanno sempre `aria-label`; i pulsanti "aggiungi" dentro le card sono tratteggiati con icona `Plus` (mai il carattere "+").
  - *Icone*: quattro misure, 12 · 14 · 16 · 18 px (più 20/40 per gli stati vuoti).
  - *Dialog*: titolo in Fraunces corsivo 20 px (base di `DialogTitle` e, dal 2026-11-25, anche di `AlertDialogTitle` per le conferme); i dialog con form hanno sempre "Annulla" accanto all'azione.
  - *Barre di scorrimento*: sottili e colorate in tutta l'app con i token `--scroll-thumb` / `--scroll-thumb-hover` (petrolio chiaro in chiaro, petrolio scuro in scuro); nascondibili localmente con `[scrollbar-width:none]`.
  - *Form lunghi*: un salvataggio bloccato dalla validazione porta in vista il primo errore (`utils/form.ts`). "Annulla" e "Salva" stanno in `BarraAzioniForm.vue` (nuovo paziente, visita, editor piano non in sola lettura): pillola flottante `rounded-2xl` con bordo, superficie e ombra (sta sopra la pagina), `sticky` in fondo al `<main>` staccata di 16 px; su desktop larga quanto il contenuto e allineata a destra, da mobile a tutta larghezza con i pulsanti divisi a metà. Alla prima modifica mostra "Modifiche non salvate" con il punto lime.
  - *Liste*: Pazienti e Piani a schede sotto 768 px, interruttore "Tabella / Schede" su desktop; l'avatar della scheda mantiene il colore del badge di stato.
  - *Variazioni e obiettivo*: verde (`--green`) solo se una variazione va nel verso dell'obiettivo del paziente, rosso (`--danger`) se va nel verso opposto, `--fg3` se per quell'obiettivo il parametro è indifferente (tabella in `utils/obiettivo.ts`). Vale per gli indicatori della scheda paziente e per la tabella del Confronto; mai ocra sulle variazioni. Le serie dei grafici tengono i loro colori fissi.
  - *Grafici di confronto*: visita A in tinta chiara del colore della serie (`color-mix` al 32% con `--surf`), visita B piena, valore scritto sulle barre. Grafici di andamento con il valore su ogni punto e l'ultimo in evidenza.
  - *Stati vuoti*: sempre `StatoVuoto.vue` (icona 40 px in `--fg4` su cerchio `--soft`, 20 px nella versione compatta; titolo Fraunces corsivo; una riga in `--fg3`; un pulsante con l'azione che risolve il vuoto, assente per i pazienti archiviati o quando il vuoto dipende da un filtro).
  - *Pagine di accesso*: login, password dimenticata e nuova password condividono `components/auth/AccessoLayout.vue` (pannello `--side` con marchio da 768 px, logo compatto da mobile); campi `h-11 rounded-xl` su `--surf` anche in scuro, pulsante `h-11` a tutta larghezza. Il riempimento automatico di Chrome resta nei colori dell'app (regola `:-webkit-autofill` in fondo a `main.css`).
  - *Mobile*: agenda in vista Giorno sotto 768 px; testata del paziente sotto 640 px con avatar 40 px, "Nuova visita" e le altre azioni nel menu "⋯"; nella Panoramica i dati clinici prima del profilo a ogni larghezza. Lo stato dell'account e l'invito stanno in un'unica pillola.
  - *Lime in scuro invariato* (deciso il 2026-11-25, scartata la proposta `#B8E36A` della revisione): in scuro il lime compare solo in segnali piccoli e nel pulsante `accent` sui blocchi hero, che sono uguali nei due temi; essere l'elemento più luminoso è il suo compito. Un secondo lime solo per lo scuro dividerebbe il colore del marchio senza guadagno visibile su aree così piccole. Da riaprire solo se il lime finisce su superfici grandi in scuro (sfondi, barre dei grafici); si prova cambiando `--lime` nel blocco scuro di `main.css`.

## Aperto

- Controllo visivo da fare da Andrea (chiaro e scuro), in particolare il tema scuro e l'effetto dei blocchi petrolio su fondo grafite.
- ~~Badge "Concluso" rosso~~: deciso il 2026-11-16, ora grigio neutro (`--hover` / `--fg3`). "Mai invitato" resta rosso: è raro, il professionista invita subito. Gli avatar delle liste pazienti e piani hanno lo stesso colore del badge di stato (preferenza di Andrea).
- Applicazione a `frontend-cliente` (non esiste ancora): la palette è pensata per tutto il prodotto, ma il mockup scelto era la vista paziente.
