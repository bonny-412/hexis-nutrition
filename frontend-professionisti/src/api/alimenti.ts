import { apiRequest } from './client'

export interface Alimento {
  id: string
  nome: string
  categoria: string
  quantitaG: number
  kcal: number
  proteineG: number
  grassiG: number
  carboidratiG: number
  acquaG: number | null
  fibreG: number | null
  zuccheriG: number | null
  ferroMg: number | null
  calcioMg: number | null
  sodioMg: number | null
  bda: boolean
}

export interface CriteriRicercaAlimenti {
  pagina?: number
  dimensione?: number
  ordinaPer?: 'nome'
  direzione?: 'asc' | 'desc'
  ricerca?: string
  fonte?: 'TUTTI' | 'BDA' | 'PERSONALIZZATI'
}

export interface PaginaAlimenti {
  contenuto: Alimento[]
  paginaCorrente: number
  dimensionePagina: number
  totaleElementi: number
  totalePagine: number
}

export interface CreaAlimentoRequest {
  nome: string
  categoria: string
  quantitaG: number
  kcal: number
  proteineG: number
  grassiG: number
  carboidratiG: number
  acquaG?: number
  fibreG?: number
  zuccheriG?: number
  ferroMg?: number
  calcioMg?: number
  sodioMg?: number
}

export type AggiornaAlimentoRequest = CreaAlimentoRequest

export function cerca(criteri: CriteriRicercaAlimenti = {}): Promise<PaginaAlimenti> {
  const parametri = new URLSearchParams()
  if (criteri.pagina !== undefined) parametri.set('pagina', String(criteri.pagina))
  if (criteri.dimensione !== undefined) parametri.set('dimensione', String(criteri.dimensione))
  if (criteri.ordinaPer) parametri.set('ordinaPer', criteri.ordinaPer)
  if (criteri.direzione) parametri.set('direzione', criteri.direzione)
  if (criteri.ricerca) parametri.set('ricerca', criteri.ricerca)
  if (criteri.fonte) parametri.set('fonte', criteri.fonte)

  const query = parametri.toString()
  return apiRequest<PaginaAlimenti>(`/alimenti/ricerca${query ? `?${query}` : ''}`)
}

export function dettaglio(id: string): Promise<Alimento> {
  return apiRequest<Alimento>(`/alimenti/${id}`)
}

export function crea(request: CreaAlimentoRequest): Promise<Alimento> {
  return apiRequest<Alimento>('/alimenti', { method: 'POST', body: request })
}

export function aggiorna(id: string, request: AggiornaAlimentoRequest): Promise<Alimento> {
  return apiRequest<Alimento>(`/alimenti/${id}`, { method: 'PUT', body: request })
}

export function elimina(id: string): Promise<void> {
  return apiRequest<void>(`/alimenti/${id}`, { method: 'DELETE' })
}

export function duplica(id: string): Promise<Alimento> {
  return apiRequest<Alimento>(`/alimenti/${id}/duplica`, { method: 'POST' })
}
