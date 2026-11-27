import type { Appuntamento } from '@/api/agenda'
import { minutiDaOra } from '@/utils/agenda'

/** Aspetto e testi degli eventi dell'agenda, condivisi tra le viste (Giorno, Settimana, Mese). */

export function oraFine(a: Appuntamento): string | null {
  if (a.oraInizio === null || a.durataMinuti === null) return null
  const tot = minutiDaOra(a.oraInizio) + a.durataMinuti
  return `${String(Math.floor(tot / 60) % 24).padStart(2, '0')}:${String(tot % 60).padStart(2, '0')}`
}

/**
 * Orario di un blocco nel giorno mostrato (copia proiettata da `eventiDelGiorno`):
 * "Tutto il giorno" quando copre l'intera fascia 07:00–20:00, altrimenti "13:00–14:00".
 */
export function orarioBlocco(a: Appuntamento): string {
  const fine = oraFine(a)
  if (a.oraInizio === '07:00' && fine === '20:00') return 'Tutto il giorno'
  return fine ? `${a.oraInizio}–${fine}` : (a.oraInizio ?? '')
}

export function nomePaziente(a: Appuntamento): string {
  return [a.pazienteNome, a.pazienteCognome].filter(Boolean).join(' ')
}

export function titolo(a: Appuntamento): string {
  if (a.tipo === 'BLOCCO') return a.testo || 'Bloccato'
  if (a.tipo === 'PROMEMORIA') return a.testo || 'Promemoria'
  return nomePaziente(a) || 'Visita'
}

export function etichettaTipo(a: Appuntamento): string {
  if (a.stato === 'CONCLUSA') return 'Conclusa'
  if (a.stato === 'ANNULLATA') return 'Annullata'
  if (a.tipo === 'BLOCCO') return 'Bloccato'
  if (a.tipo === 'PROMEMORIA') return 'Promemoria'
  return a.tipo === 'VIDEOCONSULTO' ? 'Videoconsulto' : 'Visita in studio'
}

export function classiEvento(a: Appuntamento): string {
  if (a.stato === 'ANNULLATA') return 'border-l-(--danger)/50 bg-(--hover) opacity-70'
  if (a.stato === 'CONCLUSA') return 'border-l-(--fg4) bg-(--hover) opacity-70'
  if (a.tipo === 'BLOCCO') return 'border-l-(--fg3) tratteggio-blocco'
  if (a.tipo === 'PROMEMORIA') return 'border-l-(--warn-fg) bg-(--warn-bg)'
  if (a.tipo === 'VIDEOCONSULTO') return 'border-y border-r border-y-(--bd2) border-r-(--bd2) border-l-(--fg) bg-(--surf)'
  return 'border-l-(--green) bg-(--mint)'
}

export function barrato(a: Appuntamento): boolean {
  return a.stato === 'CONCLUSA' || a.stato === 'ANNULLATA' || (a.tipo === 'PROMEMORIA' && a.fatto)
}

export function sottotitolo(a: Appuntamento): string {
  if (a.tipo === 'PROMEMORIA' || a.tipo === 'BLOCCO') return nomePaziente(a)
  return a.testo ?? ''
}

/** Visita (studio o videoconsulto) non annullata: è ciò che il conteggio giornaliero conta. */
export function eVisitaAttiva(a: Appuntamento): boolean {
  return (a.tipo === 'VISITA_STUDIO' || a.tipo === 'VIDEOCONSULTO') && a.stato !== 'ANNULLATA'
}
