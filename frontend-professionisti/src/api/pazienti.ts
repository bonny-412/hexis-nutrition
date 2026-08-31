import { apiRequest } from './client'

export interface Paziente {
  id: string
  nome: string
  cognome: string
  email: string
  telefono: string | null
  dataNascita: string | null
  sesso: string | null
  lavoro: string | null
  tipoLavoro: 'SEDENTARIO' | 'POCO_ATTIVO' | 'ATTIVO' | 'MOLTO_ATTIVO' | null
  statoAccount: 'MAI_INVITATO' | 'INVITATO' | 'ATTIVO'
}

export interface CreaVisitaRequest {
  dataVisita?: string
  altezzaCm: number
  pesoKg: number
  circonferenzaVitaCm?: number
  circonferenzaOmbelicoCm?: number
  circonferenzaFianchiCm?: number
  circonferenzaPettoCm?: number
  circonferenzaCosciaDxCm?: number
  circonferenzaCosciaSxCm?: number
  circonferenzaPolpaccioDxCm?: number
  circonferenzaPolpaccioSxCm?: number
  larghezzaSpalleCm?: number
  circonferenzaSpalleCm?: number
  circonferenzaBicipiteDxCm?: number
  circonferenzaBicipiteSxCm?: number
}

export interface CreaPazienteRequest {
  nome: string
  cognome: string
  email: string
  telefono?: string
  dataNascita?: string
  sesso?: string
  lavoro?: string
  tipoLavoro?: 'SEDENTARIO' | 'POCO_ATTIVO' | 'ATTIVO' | 'MOLTO_ATTIVO'
  visita: CreaVisitaRequest
}

export function lista(): Promise<Paziente[]> {
  return apiRequest<Paziente[]>('/pazienti')
}

export function dettaglio(id: string): Promise<Paziente> {
  return apiRequest<Paziente>(`/pazienti/${id}`)
}

export function crea(request: CreaPazienteRequest): Promise<Paziente> {
  return apiRequest<Paziente>('/pazienti', { method: 'POST', body: request })
}

export function invita(id: string): Promise<void> {
  return apiRequest<void>(`/pazienti/${id}/invito`, { method: 'POST' })
}
