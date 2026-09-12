import { describe, expect, it, vi } from 'vitest'
import { apiRequest } from './client'
import { crea, dettaglio, aggiorna, attiva, elimina, cerca } from './pianiAlimentari'

vi.mock('./client', () => ({ apiRequest: vi.fn() }))

const pianoEsempio = {
  id: '1', pazienteId: 'p1', pazienteNomeCompleto: 'Mario Bianchi', nome: 'Fase 1', modalita: 'PASTI',
  stato: 'BOZZA', dataInizio: '2026-09-12', dataFine: null, obiettivoKcal: 2400, obiettivoKcalSuggerito: 2400,
  bmrCalcolato: 1700, tdeeCalcolato: 2400, formulaBmrUsata: 'MIFFLIN_ST_JEOR', sottoSogliaSicurezza: false,
  pasti: [], giorniMacroTarget: [], esempi: [],
}

describe('api/pianiAlimentari', () => {
  it('crea chiama POST /piani-alimentari', async () => {
    vi.mocked(apiRequest).mockResolvedValue(pianoEsempio)

    await crea({ pazienteId: 'p1', nome: 'Fase 1', modalita: 'PASTI' })

    expect(apiRequest).toHaveBeenCalledWith('/piani-alimentari', {
      method: 'POST',
      body: { pazienteId: 'p1', nome: 'Fase 1', modalita: 'PASTI' },
    })
  })

  it('dettaglio chiama GET /piani-alimentari/{id}', async () => {
    vi.mocked(apiRequest).mockResolvedValue(pianoEsempio)

    await dettaglio('1')

    expect(apiRequest).toHaveBeenCalledWith('/piani-alimentari/1')
  })

  it('aggiorna chiama PUT /piani-alimentari/{id} con la struttura completa', async () => {
    vi.mocked(apiRequest).mockResolvedValue(pianoEsempio)

    await aggiorna('1', {
      nome: 'Fase 1', dataFine: '2026-10-10', obiettivoKcal: 2400,
      pasti: [{ giornoSettimana: 'LUNEDI', nome: 'Colazione', tipo: 'COLAZIONE', nota: null, righe: [] }],
      giorniMacroTarget: null, esempi: null,
    })

    expect(apiRequest).toHaveBeenCalledWith('/piani-alimentari/1', {
      method: 'PUT',
      body: {
        nome: 'Fase 1', dataFine: '2026-10-10', obiettivoKcal: 2400,
        pasti: [{ giornoSettimana: 'LUNEDI', nome: 'Colazione', tipo: 'COLAZIONE', nota: null, righe: [] }],
        giorniMacroTarget: null, esempi: null,
      },
    })
  })

  it('attiva chiama POST /piani-alimentari/{id}/attiva', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await attiva('1')

    expect(apiRequest).toHaveBeenCalledWith('/piani-alimentari/1/attiva', { method: 'POST' })
  })

  it('elimina chiama DELETE /piani-alimentari/{id}', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await elimina('1')

    expect(apiRequest).toHaveBeenCalledWith('/piani-alimentari/1', { method: 'DELETE' })
  })

  it('cerca chiama GET /piani-alimentari/ricerca senza parametri se non specificati', async () => {
    const pagina = { contenuto: [], paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 0, totalePagine: 0 }
    vi.mocked(apiRequest).mockResolvedValue(pagina)

    await cerca()

    expect(apiRequest).toHaveBeenCalledWith('/piani-alimentari/ricerca')
  })

  it('cerca costruisce la query string con tutti i filtri passati', async () => {
    vi.mocked(apiRequest).mockResolvedValue({ contenuto: [], paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 0, totalePagine: 0 })

    await cerca({ pagina: 1, dimensione: 10, ricerca: 'mario', stato: 'ATTIVO', ordinaPer: 'dataFine', direzione: 'desc' })

    expect(apiRequest).toHaveBeenCalledWith(
      '/piani-alimentari/ricerca?pagina=1&dimensione=10&ordinaPer=dataFine&direzione=desc&ricerca=mario&stato=ATTIVO',
    )
  })
})
