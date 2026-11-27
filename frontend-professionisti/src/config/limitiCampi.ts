/**
 * Lunghezza massima dei campi di testo delle form. Ogni valore è uguale o minore del limite del backend
 * (`@Size(max)` sui DTO, che segue la colonna del DB: vedi wiki/api-contracts.md, "Limiti dei campi").
 * Dove il form aveva già un limite più stretto del backend (note di paziente e visita) resta quello.
 * Il campo usa `maxlength`, così non si può superare il limite; il contatore lo rende visibile.
 */
export const LIMITI_CAMPI = {
  nomePersona: 100,
  email: 255,
  lavoro: 150,
  medicoRiferimento: 255,
  notePaziente: 2000,
  trattamenti: 2000,
  noteVisita: 500,
  nomeAlimento: 200,
  categoriaAlimento: 200,
  nomePiano: 200,
  nomePasto: 100,
  notaPiano: 2000,
  testoAgenda: 500,
  linkVideo: 500,
  notaAggiornamento: 5000,
} as const
