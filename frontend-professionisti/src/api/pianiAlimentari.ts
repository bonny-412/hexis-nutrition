import { apiRequest } from './client'

export type ModalitaPiano = 'PASTI' | 'MACRO' | 'ESEMPI'
export type StatoPiano = 'BOZZA' | 'ATTIVO' | 'SCADUTO' | 'TERMINATO'
export type FormulaBmr = 'KATCH_MCARDLE' | 'MIFFLIN_ST_JEOR'
export type GiornoSettimana = 'LUNEDI' | 'MARTEDI' | 'MERCOLEDI' | 'GIOVEDI' | 'VENERDI' | 'SABATO' | 'DOMENICA'
export type TipoPasto = 'COLAZIONE' | 'SPUNTINO_MATTINA' | 'PRANZO' | 'SPUNTINO_POMERIGGIO' | 'CENA' | 'ALTRO'

export interface RigaAlimento {
  id: string
  alimentoId: string | null
  nome: string
  kcal100g: number
  proteine100g: number
  carboidrati100g: number
  grassi100g: number
  zuccheri100g: number | null
  grammi: number
}

export interface Pasto {
  id: string
  giornoSettimana: GiornoSettimana
  nome: string
  tipo: TipoPasto
  nota: string | null
  ordine: number
  righe: RigaAlimento[]
}

export interface GiornoMacroTarget {
  giornoSettimana: GiornoSettimana
  kcalTarget: number | null
  proteineTarget: number | null
  carboidratiTarget: number | null
  grassiTarget: number | null
}

export interface Esempio {
  id: string
  tipoPasto: TipoPasto
  nome: string
  ordine: number
  righe: RigaAlimento[]
}

export interface PianoAlimentare {
  id: string
  pazienteId: string
  pazienteNomeCompleto: string
  nome: string
  modalita: ModalitaPiano
  stato: StatoPiano
  dataInizio: string
  dataFine: string | null
  obiettivoKcal: number | null
  obiettivoKcalSuggerito: number | null
  bmrCalcolato: number | null
  tdeeCalcolato: number | null
  formulaBmrUsata: FormulaBmr | null
  sottoSogliaSicurezza: boolean
  pasti: Pasto[]
  giorniMacroTarget: GiornoMacroTarget[]
  esempi: Esempio[]
}

export interface CreaPianoAlimentareRequest {
  pazienteId: string
  nome: string
  modalita: ModalitaPiano
}

export interface RigaAlimentoRequest {
  alimentoId: string | null
  nome: string
  kcal100g: number
  proteine100g: number
  carboidrati100g: number
  grassi100g: number
  zuccheri100g: number | null
  grammi: number
}

export interface PastoRequest {
  giornoSettimana: GiornoSettimana
  nome: string
  tipo: TipoPasto
  nota: string | null
  righe: RigaAlimentoRequest[]
}

export interface GiornoMacroTargetRequest {
  giornoSettimana: GiornoSettimana
  kcalTarget: number | null
  proteineTarget: number | null
  carboidratiTarget: number | null
  grassiTarget: number | null
}

export interface EsempioRequest {
  tipoPasto: TipoPasto
  nome: string
  righe: RigaAlimentoRequest[]
}

export interface AggiornaPianoAlimentareRequest {
  nome: string
  dataFine: string | null
  obiettivoKcal: number | null
  pasti: PastoRequest[] | null
  giorniMacroTarget: GiornoMacroTargetRequest[] | null
  esempi: EsempioRequest[] | null
}

export interface CriteriRicercaPianiAlimentari {
  pagina?: number
  dimensione?: number
  ordinaPer?: 'nome' | 'dataFine'
  direzione?: 'asc' | 'desc'
  ricerca?: string
  stato?: StatoPiano
}

export interface PianoAlimentareRigaLista {
  id: string
  pazienteNomeCompleto: string
  nome: string
  stato: StatoPiano
  obiettivoKcal: number | null
  dataFine: string | null
}

export interface PaginaPianiAlimentari {
  contenuto: PianoAlimentareRigaLista[]
  paginaCorrente: number
  dimensionePagina: number
  totaleElementi: number
  totalePagine: number
}

export function crea(request: CreaPianoAlimentareRequest): Promise<PianoAlimentare> {
  return apiRequest<PianoAlimentare>('/piani-alimentari', { method: 'POST', body: request })
}

export function dettaglio(id: string): Promise<PianoAlimentare> {
  return apiRequest<PianoAlimentare>(`/piani-alimentari/${id}`)
}

export function aggiorna(id: string, request: AggiornaPianoAlimentareRequest): Promise<PianoAlimentare> {
  return apiRequest<PianoAlimentare>(`/piani-alimentari/${id}`, { method: 'PUT', body: request })
}

export function attiva(id: string): Promise<void> {
  return apiRequest<void>(`/piani-alimentari/${id}/attiva`, { method: 'POST' })
}

export function elimina(id: string): Promise<void> {
  return apiRequest<void>(`/piani-alimentari/${id}`, { method: 'DELETE' })
}

export function cerca(criteri: CriteriRicercaPianiAlimentari = {}): Promise<PaginaPianiAlimentari> {
  const parametri = new URLSearchParams()
  if (criteri.pagina !== undefined) parametri.set('pagina', String(criteri.pagina))
  if (criteri.dimensione !== undefined) parametri.set('dimensione', String(criteri.dimensione))
  if (criteri.ordinaPer) parametri.set('ordinaPer', criteri.ordinaPer)
  if (criteri.direzione) parametri.set('direzione', criteri.direzione)
  if (criteri.ricerca) parametri.set('ricerca', criteri.ricerca)
  if (criteri.stato) parametri.set('stato', criteri.stato)

  const query = parametri.toString()
  return apiRequest<PaginaPianiAlimentari>(`/piani-alimentari/ricerca${query ? `?${query}` : ''}`)
}
