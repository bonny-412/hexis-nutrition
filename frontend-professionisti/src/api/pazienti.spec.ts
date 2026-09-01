import { describe, expect, it, vi } from 'vitest'
import { apiRequest } from './client'
import { lista, dettaglio, crea, invita } from './pazienti'

vi.mock('./client', () => ({ apiRequest: vi.fn() }))

const pazienteEsempio = {
  id: '1', nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com',
  telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO',
}

describe('api/pazienti', () => {
  it('lista chiama GET /pazienti', async () => {
    vi.mocked(apiRequest).mockResolvedValue([pazienteEsempio])

    const risultato = await lista()

    expect(apiRequest).toHaveBeenCalledWith('/pazienti')
    expect(risultato).toEqual([pazienteEsempio])
  })

  it('dettaglio chiama GET /pazienti/{id}', async () => {
    vi.mocked(apiRequest).mockResolvedValue(pazienteEsempio)

    await dettaglio('1')

    expect(apiRequest).toHaveBeenCalledWith('/pazienti/1')
  })

  it('crea chiama POST /pazienti con i dati del form', async () => {
    vi.mocked(apiRequest).mockResolvedValue(pazienteEsempio)

    await crea({
      nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com', dataNascita: '1990-01-01', sesso: 'M',
      visita: { altezzaCm: 178, pesoKg: 82.5 },
    })

    expect(apiRequest).toHaveBeenCalledWith('/pazienti', {
      method: 'POST',
      body: {
        nome: 'Luca', cognome: 'Verdi', email: 'luca@example.com', dataNascita: '1990-01-01', sesso: 'M',
        visita: { altezzaCm: 178, pesoKg: 82.5 },
      },
    })
  })

  it('invita chiama POST /pazienti/{id}/invito', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await invita('1')

    expect(apiRequest).toHaveBeenCalledWith('/pazienti/1/invito', { method: 'POST' })
  })
})
