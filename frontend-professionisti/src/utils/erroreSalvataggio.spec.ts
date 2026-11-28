import { describe, expect, it } from 'vitest'
import { ApiError, ErroreRete, ErroreTimeout } from '@/api/client'
import { messaggioErroreSalvataggio } from './erroreSalvataggio'

// Verifica 3 prima del rilascio (28/11/2026), bug S7: un problema di rete o del server non è un errore nei dati.
describe('messaggioErroreSalvataggio', () => {
  const DATI = 'Non è stato possibile salvare. Controlla i dati e riprova.'

  it('per un errore di rete dice che le modifiche sono ancora lì, senza parlare dei dati', () => {
    const messaggio = messaggioErroreSalvataggio(new ErroreRete(new TypeError('Failed to fetch')), DATI)

    expect(messaggio).toBe('Connessione al server non riuscita. Le modifiche sono ancora qui: riprova tra poco.')
  })

  it('per un timeout dice che il server non ha risposto in tempo', () => {
    expect(messaggioErroreSalvataggio(new ErroreTimeout(), DATI))
      .toBe('Il server non ha risposto in tempo. Le modifiche sono ancora qui: riprova tra poco.')
  })

  it('per un errore del server (5xx) non parla dei dati', () => {
    expect(messaggioErroreSalvataggio(new ApiError(500, 'boom'), DATI))
      .toBe('Errore del server. Le modifiche sono ancora qui: riprova tra poco.')
  })

  it('per una sessione scaduta rimanda al nuovo accesso', () => {
    expect(messaggioErroreSalvataggio(new ApiError(401, 'Sessione scaduta'), DATI))
      .toBe('Sessione scaduta: accedi di nuovo, poi salva.')
  })

  it('per una richiesta uguale ancora in corso chiede di attendere', () => {
    expect(messaggioErroreSalvataggio(new ApiError(409, 'RICHIESTA_IN_CORSO'), DATI))
      .toBe('Il salvataggio precedente è ancora in corso. Attendi qualche secondo e riprova.')
  })

  it('per una richiesta rifiutata (400, 404, altri 409) usa il messaggio della pagina', () => {
    expect(messaggioErroreSalvataggio(new ApiError(400, 'non valido'), DATI)).toBe(DATI)
    expect(messaggioErroreSalvataggio(new ApiError(404, 'non trovato'), DATI)).toBe(DATI)
    expect(messaggioErroreSalvataggio(new ApiError(409, 'PIANO_GIA_ATTIVO'), DATI)).toBe(DATI)
    expect(messaggioErroreSalvataggio(new Error('altro'), DATI)).toBe(DATI)
  })
})
