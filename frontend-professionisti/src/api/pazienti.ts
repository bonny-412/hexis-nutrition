import { apiRequest } from './client'

export interface Paziente {
  id: string
  nome: string
  cognome: string
  email: string
  telefono: string | null
  dataNascita: string | null
  sesso: string
  lavoro: string | null
  tipoLavoro: 'SEDENTARIO' | 'POCO_ATTIVO' | 'ATTIVO' | 'MOLTO_ATTIVO' | null
  statoAccount: 'MAI_INVITATO' | 'INVITATO' | 'ATTIVO'
}

export interface CreaPlicometriaRequest {
  protocollo: 'JACKSON_POLLOCK_3' | 'JACKSON_POLLOCK_7' | 'DURNIN_WOMERSLEY_4' | 'FAULKNER_4' | 'SLAUGHTER_PEDIATRICO' | 'EVANS_ATLETI'
  etniaAtleta?: 'CAUCASICO' | 'AFROAMERICANO'
  plicaPettoraleMm?: number
  plicaAscellareMm?: number
  plicaTricipitaleMm?: number
  plicaBicipitaleMm?: number
  plicaSottoscapolareMm?: number
  plicaSoprailiacaMm?: number
  plicaAddominaleMm?: number
  plicaCosciaMm?: number
  plicaPolpaccioMm?: number
}

export interface CreaVisitaRequest {
  dataVisita?: string
  altezzaCm: number
  pesoKg: number
  circonferenzaVitaCm?: number
  circonferenzaFianchiCm?: number
  circonferenzaAddomeCm?: number
  circonferenzaBraccioRilassatoCm?: number
  circonferenzaCosciaCm?: number
  circonferenzaPolpaccioCm?: number
  circonferenzaColloCm?: number
  circonferenzaToraceCm?: number
  circonferenzaBraccioContrattoCm?: number
  circonferenzaAvambraccioCm?: number
  circonferenzaCavigliaCm?: number
  protocolloVita?: 'OMS' | 'OMBELICALE' | 'ALTRO'
  plicometria?: CreaPlicometriaRequest
}

export interface CreaPazienteRequest {
  nome: string
  cognome: string
  email: string
  telefono?: string
  dataNascita: string
  sesso: 'M' | 'F' | 'ALTRO'
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
