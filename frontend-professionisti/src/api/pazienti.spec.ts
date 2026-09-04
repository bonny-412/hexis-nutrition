import { describe, expect, it, vi } from 'vitest'
import { apiRequest } from './client'
import { lista, dettaglio, crea, invita, archivia, deArchivia, cerca, visite, creaVisita, dettaglioVisita, aggiornaVisita, eliminaVisita } from './pazienti'

vi.mock('./client', () => ({ apiRequest: vi.fn() }))

const pazienteEsempio = {
  id: '1', nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com',
  telefono: null, dataNascita: null, sesso: 'M', lavoro: null, tipoLavoro: null, statoAccount: 'MAI_INVITATO',
  archiviato: false,
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
      nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com', dataNascita: '1990-01-01', sesso: 'M',
      visita: { altezzaCm: 178, pesoKg: 82.5 },
    })

    expect(apiRequest).toHaveBeenCalledWith('/pazienti', {
      method: 'POST',
      body: {
        nome: 'Luca', cognome: 'Verdi', codiceFiscale: 'RSSMRA80A01H501U', email: 'luca@example.com', dataNascita: '1990-01-01', sesso: 'M',
        visita: { altezzaCm: 178, pesoKg: 82.5 },
      },
    })
  })

  it('invita chiama POST /pazienti/{id}/invito', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await invita('1')

    expect(apiRequest).toHaveBeenCalledWith('/pazienti/1/invito', { method: 'POST' })
  })

  it('archivia chiama POST /pazienti/{id}/archivia', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await archivia('1')

    expect(apiRequest).toHaveBeenCalledWith('/pazienti/1/archivia', { method: 'POST' })
  })

  it('deArchivia chiama POST /pazienti/{id}/de-archivia', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await deArchivia('1')

    expect(apiRequest).toHaveBeenCalledWith('/pazienti/1/de-archivia', { method: 'POST' })
  })

  it('cerca chiama GET /pazienti/ricerca senza parametri se non specificati', async () => {
    const paginaEsempio = { contenuto: [pazienteEsempio], paginaCorrente: 0, dimensionePagina: 20, totaleElementi: 1, totalePagine: 1 }
    vi.mocked(apiRequest).mockResolvedValue(paginaEsempio)

    const risultato = await cerca()

    expect(apiRequest).toHaveBeenCalledWith('/pazienti/ricerca')
    expect(risultato).toEqual(paginaEsempio)
  })

  it('cerca costruisce la query string con tutti i filtri passati', async () => {
    vi.mocked(apiRequest).mockResolvedValue({ contenuto: [], paginaCorrente: 1, dimensionePagina: 10, totaleElementi: 0, totalePagine: 0 })

    await cerca({
      pagina: 1, dimensione: 10, ordinaPer: 'dataNascita', direzione: 'desc',
      ricerca: 'marco', statoAccount: 'ATTIVO', obiettivo: 'IPERTROFIA',
      dataUltimaVisitaDa: '1990-01-01', dataUltimaVisitaA: '2000-01-01', archiviato: true,
    })

    expect(apiRequest).toHaveBeenCalledWith(
      '/pazienti/ricerca?pagina=1&dimensione=10&ordinaPer=dataNascita&direzione=desc&ricerca=marco' +
      '&statoAccount=ATTIVO&obiettivo=IPERTROFIA&dataUltimaVisitaDa=1990-01-01&dataUltimaVisitaA=2000-01-01&archiviato=true',
    )
  })

  it('visite chiama GET /pazienti/{id}/visite', async () => {
    const visiteEsempio = [
      {
        id: 'v1', dataVisita: '2026-06-01', altezzaCm: 178, pesoKg: 80, bmi: 25.2, whr: null, whtr: null, mamcCm: null,
        circonferenze: {
          vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
          polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
        },
        plicometria: null,
      },
    ]
    vi.mocked(apiRequest).mockResolvedValue(visiteEsempio)

    const risultato = await visite('1')

    expect(apiRequest).toHaveBeenCalledWith('/pazienti/1/visite')
    expect(risultato).toEqual(visiteEsempio)
  })

  it('creaVisita chiama POST /pazienti/{id}/visite con i dati della visita', async () => {
    const visitaEsempio = {
      id: 'v1', dataVisita: '2026-06-01', altezzaCm: 178, pesoKg: 80, bmi: 25.2, whr: null, whtr: null, mamcCm: null,
      circonferenze: {
        vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
        polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
      },
      note: null, obiettivo: 'MANTENIMENTO', plicometria: null,
    }
    vi.mocked(apiRequest).mockResolvedValue(visitaEsempio)

    const risultato = await creaVisita('1', { altezzaCm: 178, pesoKg: 80 })

    expect(apiRequest).toHaveBeenCalledWith('/pazienti/1/visite', { method: 'POST', body: { altezzaCm: 178, pesoKg: 80 } })
    expect(risultato).toEqual(visitaEsempio)
  })

  it('dettaglioVisita chiama GET /pazienti/{id}/visite/{visitaId}', async () => {
    const visitaEsempio = {
      id: 'v1', dataVisita: '2026-06-01', altezzaCm: 178, pesoKg: 80, bmi: 25.2, whr: null, whtr: null, mamcCm: null,
      circonferenze: {
        vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
        polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
      },
      note: null, obiettivo: 'MANTENIMENTO', plicometria: null,
    }
    vi.mocked(apiRequest).mockResolvedValue(visitaEsempio)

    const risultato = await dettaglioVisita('1', 'v1')

    expect(apiRequest).toHaveBeenCalledWith('/pazienti/1/visite/v1')
    expect(risultato).toEqual(visitaEsempio)
  })

  it('aggiornaVisita chiama PUT /pazienti/{id}/visite/{visitaId} con i dati della visita', async () => {
    const visitaEsempio = {
      id: 'v1', dataVisita: '2026-06-01', altezzaCm: 180, pesoKg: 82, bmi: 25.3, whr: null, whtr: null, mamcCm: null,
      circonferenze: {
        vitaCm: null, fianchiCm: null, addomeCm: null, braccioRilassatoCm: null, cosciaCm: null,
        polpaccioCm: null, colloCm: null, toraceCm: null, braccioContrattoCm: null, avambraccioCm: null, cavigliaCm: null,
      },
      note: null, obiettivo: 'MANTENIMENTO', plicometria: null,
    }
    vi.mocked(apiRequest).mockResolvedValue(visitaEsempio)

    const risultato = await aggiornaVisita('1', 'v1', { altezzaCm: 180, pesoKg: 82 })

    expect(apiRequest).toHaveBeenCalledWith('/pazienti/1/visite/v1', { method: 'PUT', body: { altezzaCm: 180, pesoKg: 82 } })
    expect(risultato).toEqual(visitaEsempio)
  })

  it('eliminaVisita chiama DELETE /pazienti/{id}/visite/{visitaId}', async () => {
    vi.mocked(apiRequest).mockResolvedValue(undefined)

    await eliminaVisita('1', 'v1')

    expect(apiRequest).toHaveBeenCalledWith('/pazienti/1/visite/v1', { method: 'DELETE' })
  })
})
