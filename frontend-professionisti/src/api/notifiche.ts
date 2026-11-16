import { apiRequest } from './client'
import type { TipoAppuntamento } from './agenda'

export interface Notifica {
  id: string
  appuntamentoId: string
  tipo: TipoAppuntamento
  /** `YYYY-MM-DD`. */
  data: string
  /** `HH:mm`. */
  oraInizio: string | null
  anticipoMinuti: number
  testo: string | null
  pazienteNome: string | null
  pazienteCognome: string | null
  scattaIl: string
  letta: boolean
  agendaVista: boolean
}

export interface NotificheRisposta {
  notifiche: Notifica[]
  nonLette: number
  nonVisteInAgenda: number
}

export interface IscrizionePush {
  endpoint: string
  p256dh: string
  auth: string
}

export function elencoNotifiche(): Promise<NotificheRisposta> {
  return apiRequest<NotificheRisposta>('/notifiche')
}

/** Segna come lette solo le notifiche indicate (quelle mostrate nella lista). */
export function segnaLette(ids: string[]): Promise<void> {
  return apiRequest<void>('/notifiche/lette', { method: 'POST', body: { ids } })
}

export function segnaAgendaViste(): Promise<void> {
  return apiRequest<void>('/notifiche/agenda-viste', { method: 'POST' })
}

/** Chiave pubblica VAPID; `null` se le push non sono configurate sul server. */
export async function chiavePush(): Promise<string | null> {
  const risposta = await apiRequest<{ chiave: string | null }>('/notifiche/push/chiave')
  return risposta.chiave
}

export function registraPush(iscrizione: IscrizionePush): Promise<void> {
  return apiRequest<void>('/notifiche/push/iscrizioni', { method: 'POST', body: iscrizione })
}

export function rimuoviPush(endpoint: string): Promise<void> {
  return apiRequest<void>('/notifiche/push/iscrizioni', { method: 'DELETE', body: { endpoint } })
}
