/**
 * Codice colori unico per badge e avatar di stato in tutte le liste (pazienti, piani, command palette,
 * selettore paziente). Decisione del 2026-11-26, vedi wiki/decisioni/0010.
 *
 * - `daFare`: tocca al professionista fare qualcosa (paziente mai invitato, piano in bozza).
 * - `attivo`: tutto a posto, in corso (account attivo, piano attivo).
 * - `neutro`: in attesa di altri o concluso (paziente invitato, piano concluso, paziente archiviato).
 *
 * Il rosso non è uno stato: resta per errori e azioni distruttive.
 * Fonte degli alimenti (non è uno stato, ma usa gli stessi toni): BDA neutro, personalizzato `attivo`.
 */
export const TONI_STATO = {
  daFare: 'bg-(--warn-bg) text-(--warn-fg)',
  attivo: 'bg-(--mint) text-(--green-d)',
  // A contorno e non pieno: il grigio-verde di `--hover` era quasi uguale alla menta. Pieno/vuoto si distingue
  // anche senza percepire la tinta.
  neutro: 'bg-transparent text-(--fg3) ring-1 ring-inset ring-(--bd2)',
} as const

export type TonoStato = keyof typeof TONI_STATO
