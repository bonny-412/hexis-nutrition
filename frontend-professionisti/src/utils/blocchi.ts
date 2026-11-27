import type { Appuntamento } from '@/api/agenda'
import { minutiDaOra } from '@/utils/agenda'

const INIZIO_GRIGLIA = '07:00'
const FINE_GRIGLIA = '20:00'

/** Tratto di un blocco in un dato giorno (stessa regola del backend), o null se quel giorno non lo tocca. */
export function trattoNelGiorno(blocco: Appuntamento, giorno: string): { inizio: string; fine: string } | null {
  const dataFine = blocco.dataFine ?? blocco.data
  if (giorno < blocco.data || giorno > dataFine) return null
  if (blocco.giornataIntera) return { inizio: INIZIO_GRIGLIA, fine: FINE_GRIGLIA }
  const inizio = giorno === blocco.data && blocco.oraInizio ? blocco.oraInizio : INIZIO_GRIGLIA
  const fine = giorno === dataFine && blocco.oraFine ? blocco.oraFine : FINE_GRIGLIA
  if (minutiDaOra(inizio) >= minutiDaOra(fine)) return null
  return { inizio, fine }
}

/** Eventi di un giorno: i non-blocchi del giorno e, per ogni blocco che lo tocca, una copia ristretta al tratto di quel giorno. */
export function eventiDelGiorno(appuntamenti: Appuntamento[], giorno: string): Appuntamento[] {
  const risultato: Appuntamento[] = []
  for (const a of appuntamenti) {
    if (a.tipo !== 'BLOCCO') {
      if (a.data === giorno) risultato.push(a)
      continue
    }
    const tratto = trattoNelGiorno(a, giorno)
    if (tratto) {
      risultato.push({
        ...a,
        data: giorno,
        oraInizio: tratto.inizio,
        durataMinuti: minutiDaOra(tratto.fine) - minutiDaOra(tratto.inizio),
      })
    }
  }
  return risultato
}

/** Il blocco copre l'intera fascia 07:00-20:00 in quel giorno. */
export function coperturaIntera(blocco: Appuntamento, giorno: string): boolean {
  const tratto = trattoNelGiorno(blocco, giorno)
  return tratto !== null && tratto.inizio === INIZIO_GRIGLIA && tratto.fine === FINE_GRIGLIA
}
