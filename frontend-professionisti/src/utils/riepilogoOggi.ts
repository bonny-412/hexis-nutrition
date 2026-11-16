import type { Appuntamento } from '@/api/agenda'
import { minutiDaOra } from '@/utils/agenda'

export interface RiepilogoOggi {
  visite: number
  promemoria: number
  /** La prossima visita programmata di oggi, se c'è ancora. */
  prossima: Appuntamento | null
  /** Frase pronta da mostrare, per esempio "Oggi hai 3 visite e 2 promemoria da fare." */
  frase: string
}

/** Sintesi della giornata a partire dagli appuntamenti di oggi (visite non annullate, promemoria non fatti). */
export function riepilogoOggi(appuntamentiDiOggi: Appuntamento[], adesso: Date = new Date()): RiepilogoOggi {
  const visiteAttive = appuntamentiDiOggi.filter(
    (a) => (a.tipo === 'VISITA_STUDIO' || a.tipo === 'VIDEOCONSULTO') && a.stato !== 'ANNULLATA',
  )
  const promemoria = appuntamentiDiOggi.filter((a) => a.tipo === 'PROMEMORIA' && !a.fatto).length
  const minutiAdesso = adesso.getHours() * 60 + adesso.getMinutes()

  const prossima =
    visiteAttive
      .filter((a) => a.stato === 'PROGRAMMATA' && a.oraInizio !== null && minutiDaOra(a.oraInizio) >= minutiAdesso)
      .sort((x, y) => minutiDaOra(x.oraInizio as string) - minutiDaOra(y.oraInizio as string))[0] ?? null

  const v = visiteAttive.length
  const visiteTesto = v === 1 ? 'hai 1 visita' : `hai ${v} visite`
  const promemoriaTesto = promemoria === 1 ? '1 promemoria da fare' : `${promemoria} promemoria da fare`

  let frase: string
  if (v === 0 && promemoria === 0) frase = 'Oggi non hai impegni in programma.'
  else if (v === 0) frase = `Oggi non hai visite in programma, ma ${promemoriaTesto}.`
  else if (promemoria === 0) frase = `Oggi ${visiteTesto}.`
  else frase = `Oggi ${visiteTesto} e ${promemoriaTesto}.`

  return { visite: v, promemoria, prossima, frase }
}
