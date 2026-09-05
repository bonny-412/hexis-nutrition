import { describe, expect, it, vi } from 'vitest'
import { apiRequest } from './client'
import { cerca, dettaglio, crea, aggiorna, elimina, duplica } from './alimenti'

vi.mock('./client', () => ({ apiRequest: vi.fn() }))

const alimentoEsempio = {
  id: '1', nome: 'Petto di pollo, crudo', categoria: 'Carni', kcal: 100, proteineG: 23.3, grassiG: 0.8,
  carboidratiG: 0, acquaG: 74.4, fibreG: 0, zuccheriG: 0, ferroMg: null, calcioMg: null, sodioMg: null, bda: true,
}

describe('api/alimenti', () => {
  it('cerca chiama GET /alimenti/ricerca senza parametri se non specificati', async () => {
    const paginaEsempio = { contenuto: [alimentoEsempio], paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 1, totalePagine: 1 }
    vi.mocked(apiRequest).mockResolvedValue(paginaEsempio)

    const risultato = await cerca()

    expect(apiRequest).toHaveBeenCalledWith('/alimenti/ricerca')
    expect(risultato).toEqual(paginaEsempio)
  })

  it('cerca costruisce la query string con tutti i filtri passati', async () => {
    vi.mocked(apiRequest).mockResolvedValue({ contenuto: [], paginaCorrente: 1, dimensionePagina: 10, totaleElementi: 0, totalePagine: 0 })

    await cerca({ pagina: 1, dimensione: 10, ricerca: 'pollo', fonte: 'BDA' })

    expect(apiRequest).toHaveBeenCalledWith('/alimenti/ricerca?pagina=1&dimensione=10&ricerca=pollo&fonte=BDA')
  })

  it('dettaglio chiama GET /alimenti/{id}', async () => {
    vi.mocked(apiRequest).mockResolvedValue(alimentoEsempio)

    await dettaglio('1')

    expect(apiRequest).toHaveBeenCalledWith('/alimenti/1')
  })

  it('crea chiama POST /alimenti con i dati del form', async () => {
    vi.mocked(apiRequest).mockResolvedValue(alimentoEsempio)

    await crea({ nome: 'Frullato', categoria: 'Bevande', kcal: 180, proteineG: 25, grassiG: 3.5, carboidratiG: 12 })

    expect(apiRequest).toHaveBeenCalledWith('/alimenti', {
      method: 'POST',
      body: { nome: 'Frullato', categoria: 'Bevande', kcal: 180, proteineG: 25, grassiG: 3.5, carboidratiG: 12 },
    })
  })

  it('aggiorna chiama PUT /alimenti/{id} con i dati del form', async () => {
    vi.mocked(apiRequest).mockResolvedValue(alimentoEsempio)

    await aggiorna('1', { nome: 'Frullato', categoria: 'Bevande', kcal: 180, proteineG: 25, grassiG: 3.5, carboidratiG: 12 })

    expect(apiRequest).toHaveBeenCalledWith('/alimenti/1', {
      method: 'PUT',
      body: { nome: 'Frullato', categoria: 'Bevande', kcal: 180, proteineG: 25, grassiG: 3.5, carboidratiG: 12 },
    })
  })

  it('elimina chiama DELETE /alimenti/{id}', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await elimina('1')

    expect(apiRequest).toHaveBeenCalledWith('/alimenti/1', { method: 'DELETE' })
  })

  it('duplica chiama POST /alimenti/{id}/duplica', async () => {
    vi.mocked(apiRequest).mockResolvedValue(alimentoEsempio)

    await duplica('1')

    expect(apiRequest).toHaveBeenCalledWith('/alimenti/1/duplica', { method: 'POST' })
  })
})
