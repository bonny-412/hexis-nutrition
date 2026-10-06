import { isoATimestamp } from '@/utils/data'

const MS_PER_GIORNO = 24 * 60 * 60 * 1000

/** Oggi come data di calendario `YYYY-MM-DD` nel fuso dell'utente (le date dei piani sono date di calendario). */
export function oggiIso(): string {
  const d = new Date()
  const anno = String(d.getFullYear()).padStart(4, '0')
  const mese = String(d.getMonth() + 1).padStart(2, '0')
  const giorno = String(d.getDate()).padStart(2, '0')
  return `${anno}-${mese}-${giorno}`
}

/** Giorni di calendario da `da` ad `a` (negativo se `a` precede `da`). */
export function giorniTra(da: string, a: string): number {
  return Math.round((isoATimestamp(a) - isoATimestamp(da)) / MS_PER_GIORNO)
}

export interface AvanzamentoPiano {
  /** 0-100, null se il piano non ha una data di fine. */
  percentuale: number | null
  durataLabel: string
  scadenzaLabel: string
  scadenzaScaduta: boolean
  scadenzaInGiornata: boolean
}

function pluralizza(n: number, singolare: string, plurale: string): string {
  return `${n} ${n === 1 ? singolare : plurale}`
}

function durataLeggibile(giorni: number): string {
  if (giorni >= 14 && giorni % 7 === 0) return pluralizza(giorni / 7, 'settimana', 'settimane')
  return pluralizza(giorni, 'giorno', 'giorni')
}

export function avanzamentoPiano(dataInizio: string, dataFine: string | null, oggi: string = oggiIso()): AvanzamentoPiano {
  if (!dataFine) {
    return {
      percentuale: null,
      durataLabel: 'Nessuna data di fine',
      scadenzaLabel: '',
      scadenzaScaduta: false,
      scadenzaInGiornata: false,
    }
  }
  const durata = Math.max(giorniTra(dataInizio, dataFine), 1)
  const trascorsi = giorniTra(dataInizio, oggi)
  const percentuale = Math.min(Math.max(Math.round((trascorsi / durata) * 100), 0), 100)
  const rimasti = giorniTra(oggi, dataFine)

  let scadenzaLabel: string
  if (rimasti === 0) scadenzaLabel = 'Scade oggi'
  else if (rimasti > 0) scadenzaLabel = `Scade tra ${pluralizza(rimasti, 'giorno', 'giorni')}`
  else scadenzaLabel = `Scaduto da ${pluralizza(-rimasti, 'giorno', 'giorni')}`

  return {
    percentuale,
    durataLabel: `Durata ${durataLeggibile(durata)}`,
    scadenzaLabel,
    scadenzaScaduta: rimasti < 0,
    scadenzaInGiornata: rimasti === 0,
  }
}
