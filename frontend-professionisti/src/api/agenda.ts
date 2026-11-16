import { apiRequest, ApiError } from './client'

export type TipoAppuntamento = 'VISITA_STUDIO' | 'VIDEOCONSULTO' | 'PROMEMORIA' | 'BLOCCO'
export type StatoAppuntamento = 'PROGRAMMATA' | 'CONCLUSA' | 'ANNULLATA'

export interface Appuntamento {
  id: string
  tipo: TipoAppuntamento
  /** null per BLOCCO e PROMEMORIA. */
  stato: StatoAppuntamento | null
  /** `YYYY-MM-DD`. */
  data: string
  /** `HH:mm`; null per gli eventi senza orario. */
  oraInizio: string | null
  /** null per PROMEMORIA. */
  durataMinuti: number | null
  pazienteId: string | null
  pazienteNome: string | null
  pazienteCognome: string | null
  testo: string | null
  linkVideo: string | null
  fatto: boolean
  visitaId: string | null
  /** Se true, il professionista riceve la notifica prima dell'inizio (solo visite e promemoria con orario). */
  notifica: boolean
  /** 15, 30, 45, 60 o 120. */
  anticipoNotificaMinuti: number
}

export interface AppuntamentoRichiesta {
  tipo: TipoAppuntamento
  pazienteId?: string | null
  data: string
  oraInizio?: string | null
  durataMinuti?: number | null
  testo?: string | null
  linkVideo?: string | null
  notifica?: boolean
  anticipoNotificaMinuti?: number
}

export class SovrapposizioneError extends Error {
  readonly conflitti: Appuntamento[]

  constructor(conflitti: Appuntamento[]) {
    super('Sovrapposizione con altri appuntamenti')
    this.conflitti = conflitti
  }
}

async function intercettaSovrapposizione<T>(chiamata: Promise<T>): Promise<T> {
  try {
    return await chiamata
  } catch (errore) {
    if (errore instanceof ApiError && errore.status === 409) {
      let conflitti: Appuntamento[] = []
      try {
        const corpo = JSON.parse(errore.message) as { conflitti?: Appuntamento[] }
        conflitti = corpo.conflitti ?? []
      } catch {
        // corpo non JSON: nessun dettaglio sui conflitti
      }
      throw new SovrapposizioneError(conflitti)
    }
    throw errore
  }
}

export function elenco(da: string, a: string): Promise<Appuntamento[]> {
  return apiRequest<Appuntamento[]>(`/agenda?da=${encodeURIComponent(da)}&a=${encodeURIComponent(a)}`)
}

export function crea(richiesta: AppuntamentoRichiesta, conferma = false): Promise<Appuntamento> {
  const query = conferma ? '?confermaSovrapposizione=true' : ''
  return intercettaSovrapposizione(apiRequest<Appuntamento>(`/agenda${query}`, { method: 'POST', body: richiesta }))
}

export function aggiorna(id: string, richiesta: AppuntamentoRichiesta, conferma = false): Promise<Appuntamento> {
  const query = conferma ? '?confermaSovrapposizione=true' : ''
  return intercettaSovrapposizione(apiRequest<Appuntamento>(`/agenda/${id}${query}`, { method: 'PUT', body: richiesta }))
}

export function annulla(id: string): Promise<void> {
  return apiRequest<void>(`/agenda/${id}/annulla`, { method: 'POST' })
}

export function impostaFatto(id: string, fatto: boolean): Promise<Appuntamento> {
  return apiRequest<Appuntamento>(`/agenda/${id}/fatto`, { method: 'PUT', body: { fatto } })
}

export function elimina(id: string): Promise<void> {
  return apiRequest<void>(`/agenda/${id}`, { method: 'DELETE' })
}
