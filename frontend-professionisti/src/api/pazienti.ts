import { apiRequest } from './client'

export interface Paziente {
  id: string
  nome: string
  cognome: string
  codiceFiscale: string
  email: string
  telefono: string | null
  dataNascita: string | null
  sesso: string
  lavoro: string | null
  tipoLavoro: 'SEDENTARIO' | 'POCO_ATTIVO' | 'ATTIVO' | 'MOLTO_ATTIVO' | null
  note: string | null
  statoAccount: 'MAI_INVITATO' | 'INVITATO' | 'ATTIVO'
  archiviato: boolean
  obiettivoUltimaVisita: 'DIMAGRIMENTO' | 'AUMENTO_PESO' | 'IPERTROFIA' | 'RICOMPOSIZIONE' | 'MANTENIMENTO' | 'EDUCATIVO' | 'PREPARAZIONE_SPORTIVA' | null
  dataUltimaVisita: string | null
}

export interface CriteriRicercaPazienti {
  pagina?: number
  dimensione?: number
  ordinaPer?: 'nome' | 'cognome' | 'dataNascita' | 'statoAccount'
  direzione?: 'asc' | 'desc'
  ricerca?: string
  statoAccount?: Paziente['statoAccount']
  obiettivo?: NonNullable<Paziente['obiettivoUltimaVisita']>
  dataUltimaVisitaDa?: string
  dataUltimaVisitaA?: string
  archiviato?: boolean
}

export interface PaginaPazienti {
  contenuto: Paziente[]
  paginaCorrente: number
  dimensionePagina: number
  totaleElementi: number
  totalePagine: number
}

export interface Circonferenze {
  vitaCm: number | null
  fianchiCm: number | null
  addomeCm: number | null
  braccioRilassatoCm: number | null
  cosciaCm: number | null
  polpaccioCm: number | null
  colloCm: number | null
  toraceCm: number | null
  braccioContrattoCm: number | null
  avambraccioCm: number | null
  cavigliaCm: number | null
}

export interface Plicometria {
  protocollo: 'JACKSON_POLLOCK_3' | 'JACKSON_POLLOCK_7' | 'DURNIN_WOMERSLEY_4' | 'FAULKNER_4' | 'SLAUGHTER_PEDIATRICO' | 'EVANS_ATLETI'
  etniaAtleta: 'CAUCASICO' | 'AFROAMERICANO' | null
  plicaPettoraleMm: number | null
  plicaAscellareMm: number | null
  plicaTricipitaleMm: number | null
  plicaBicipitaleMm: number | null
  plicaSottoscapolareMm: number | null
  plicaSoprailiacaMm: number | null
  plicaAddominaleMm: number | null
  plicaCosciaMm: number | null
  plicaPolpaccioMm: number | null
  percentualeGrassoCorporeo: number
  massaGrassaKg: number
  massaMagraKg: number
  fmi: number
  ffmi: number
}

export interface Visita {
  id: string
  dataVisita: string
  altezzaCm: number
  pesoKg: number
  bmi: number | null
  whr: number | null
  whtr: number | null
  mamcCm: number | null
  circonferenze: Circonferenze
  protocolloVita: 'OMS' | 'OMBELICALE' | 'ALTRO'
  note: string | null
  obiettivo: 'DIMAGRIMENTO' | 'AUMENTO_PESO' | 'IPERTROFIA' | 'RICOMPOSIZIONE' | 'MANTENIMENTO' | 'EDUCATIVO' | 'PREPARAZIONE_SPORTIVA'
  plicometria: Plicometria | null
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
  note?: string
  obiettivo?: 'DIMAGRIMENTO' | 'AUMENTO_PESO' | 'IPERTROFIA' | 'RICOMPOSIZIONE' | 'MANTENIMENTO' | 'EDUCATIVO' | 'PREPARAZIONE_SPORTIVA'
  plicometria?: CreaPlicometriaRequest
}

export interface CreaPazienteRequest {
  nome: string
  cognome: string
  codiceFiscale: string
  email: string
  telefono?: string
  dataNascita: string
  sesso: 'M' | 'F' | 'ALTRO'
  lavoro?: string
  tipoLavoro?: 'SEDENTARIO' | 'POCO_ATTIVO' | 'ATTIVO' | 'MOLTO_ATTIVO'
  note?: string
  visita: CreaVisitaRequest
}

export interface AggiornaPazienteRequest {
  nome: string
  cognome: string
  codiceFiscale: string
  email: string
  telefono?: string
  dataNascita: string
  sesso: 'M' | 'F' | 'ALTRO'
  lavoro?: string
  tipoLavoro?: 'SEDENTARIO' | 'POCO_ATTIVO' | 'ATTIVO' | 'MOLTO_ATTIVO'
  note?: string
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

export function aggiorna(id: string, request: AggiornaPazienteRequest): Promise<Paziente> {
  return apiRequest<Paziente>(`/pazienti/${id}`, { method: 'PUT', body: request })
}

export function invita(id: string): Promise<void> {
  return apiRequest<void>(`/pazienti/${id}/invito`, { method: 'POST' })
}

export function archivia(id: string): Promise<void> {
  return apiRequest<void>(`/pazienti/${id}/archivia`, { method: 'POST' })
}

export function deArchivia(id: string): Promise<void> {
  return apiRequest<void>(`/pazienti/${id}/de-archivia`, { method: 'POST' })
}

export function cerca(criteri: CriteriRicercaPazienti = {}): Promise<PaginaPazienti> {
  const parametri = new URLSearchParams()
  if (criteri.pagina !== undefined) parametri.set('pagina', String(criteri.pagina))
  if (criteri.dimensione !== undefined) parametri.set('dimensione', String(criteri.dimensione))
  if (criteri.ordinaPer) parametri.set('ordinaPer', criteri.ordinaPer)
  if (criteri.direzione) parametri.set('direzione', criteri.direzione)
  if (criteri.ricerca) parametri.set('ricerca', criteri.ricerca)
  if (criteri.statoAccount) parametri.set('statoAccount', criteri.statoAccount)
  if (criteri.obiettivo) parametri.set('obiettivo', criteri.obiettivo)
  if (criteri.dataUltimaVisitaDa) parametri.set('dataUltimaVisitaDa', criteri.dataUltimaVisitaDa)
  if (criteri.dataUltimaVisitaA) parametri.set('dataUltimaVisitaA', criteri.dataUltimaVisitaA)
  if (criteri.archiviato !== undefined) parametri.set('archiviato', String(criteri.archiviato))

  const query = parametri.toString()
  return apiRequest<PaginaPazienti>(`/pazienti/ricerca${query ? `?${query}` : ''}`)
}

export function visite(id: string): Promise<Visita[]> {
  return apiRequest<Visita[]>(`/pazienti/${id}/visite`)
}

export function creaVisita(pazienteId: string, request: CreaVisitaRequest): Promise<Visita> {
  return apiRequest<Visita>(`/pazienti/${pazienteId}/visite`, { method: 'POST', body: request })
}

export function dettaglioVisita(pazienteId: string, visitaId: string): Promise<Visita> {
  return apiRequest<Visita>(`/pazienti/${pazienteId}/visite/${visitaId}`)
}

export function aggiornaVisita(pazienteId: string, visitaId: string, request: CreaVisitaRequest): Promise<Visita> {
  return apiRequest<Visita>(`/pazienti/${pazienteId}/visite/${visitaId}`, { method: 'PUT', body: request })
}

export function eliminaVisita(pazienteId: string, visitaId: string): Promise<void> {
  return apiRequest<void>(`/pazienti/${pazienteId}/visite/${visitaId}`, { method: 'DELETE' })
}
