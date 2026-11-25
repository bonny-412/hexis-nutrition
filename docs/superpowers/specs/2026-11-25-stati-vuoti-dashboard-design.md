# Stati vuoti e Dashboard orientata alle azioni — design breve

Data: 2026-11-25. Approvato da Andrea in chat ("procedi"). Origine: §4 e §6 punto 3 di [`docs/ui-review-2026-11-25.md`](../../ui-review-2026-11-25.md). Proposta costruita con le skill frontend-design e superpowers:brainstorming.

## Perché

La Dashboard mostra numeri ma non dice cosa fare; gli stati vuoti sono una riga di testo senza via d'uscita; in agenda "Nessun impegno" ripetuto in 7 colonne sembra un evento.

## Obiettivi / non obiettivi

- Obiettivi: una card "Da fare" con azioni concrete; "Avvia visita" diretto sulle visite di oggi; ogni stato vuoto dice cosa manca e offre il pulsante per farlo.
- Non obiettivi: backend (tutti i dati sono già esposti), nuove metriche, redesign dell'hero, agenda da mobile (punto 4 della review).
- Anti-obiettivo: una Dashboard affollata. Un gruppo di "Da fare" senza voci non compare.

## Approcci considerati

1. **Componente `StatoVuoto.vue` condiviso + card "Da fare" che sostituisce "Piani in bozza" (scelto)**: un solo stile per tutti gli stati vuoti, nessuna chiamata nuova al backend oltre a due ricerche già esistenti.
2. Stati vuoti scritti a mano in ogni pagina: già oggi divergono (titolo in alcuni, solo testo in altri).
3. Una pagina "Da fare" separata: un clic in più per la cosa che il professionista deve vedere per prima.

## Design

- `components/StatoVuoto.vue`: props `icona` (componente lucide), `titolo`, `testo?`, `compatto?`; slot di default per le azioni. Icona 40 px in `--fg4` dentro un cerchio `--soft` (20 px in compatto), titolo Fraunces corsivo, testo `--fg3`, contenuto centrato.
- Dashboard:
  - "Prossime visite": sulle visite di oggi con paziente, pulsante "Avvia visita" → `/pazienti/{id}/visite/nuova?appuntamentoId={id}` (stesso flusso dell'agenda). Vuota: `StatoVuoto` "Nessuna visita nei prossimi 7 giorni" + "Fissa un appuntamento".
  - Card "Da fare" al posto di "Piani in bozza", tre gruppi in quest'ordine, ciascuno solo se ha voci (massimo 3 righe + "Vedi tutti" se di più):
    1. Piani in scadenza: piani `ATTIVO` con data di fine tra oggi e oggi + 7 → "Rinnova" (`/piani-alimentari/nuovo?duplicaDa={id}`).
    2. Piani in bozza → "Completa" (`/piani-alimentari/{id}`).
    3. Mai invitati: pazienti non archiviati con `statoAccount = MAI_INVITATO` → "Invita" (chiama l'API, toast "Invito inviato.", la riga sparisce).
    Tutti vuoti: "Niente in sospeso".
- Scheda paziente: tab Piani senza piani → "Nuovo piano" (`/piani-alimentari/nuovo?pazienteId=`); Panoramica e Storico senza visite → "Registra la prima visita"; Confronto con meno di 2 visite → "Nuova visita". Nessun pulsante per i pazienti archiviati.
- Agenda: vista Settimana senza scritte per colonna; settimana tutta vuota → un solo `StatoVuoto` sopra la griglia con "Nuovo appuntamento"; vista Giorno "Giornata libera" con lo stesso pulsante.

## Aggiunte dopo la revisione indipendente

- Le visite di oggi restano in "Prossime visite" (con "Avvia visita") finché non sono finite (inizio + durata, 60 minuti se manca), non solo finché non sono iniziate.
- Con un filtro attivo in agenda (Visite/Promemoria) niente "Settimana libera"/"Giornata libera" né "Nuovo appuntamento": il messaggio dice che non ci sono impegni di quel tipo.
- In vista Settimana il messaggio di settimana vuota è una striscia sopra la griglia (non uno `StatoVuoto` centrato), perché la griglia resta visibile e utile.

## Lasciato a chi implementa

Spaziature, testi secondari, adeguamento dei test esistenti. Nessun test nuovo di default.
