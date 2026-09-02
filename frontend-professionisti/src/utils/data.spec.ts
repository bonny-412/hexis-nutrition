import { describe, expect, it, vi, afterEach } from 'vitest'
import {
  calcolaEta,
  formattaDataItaliana,
  formattaDataItalianaConMese,
  formattaDataItalianaEstesa,
  isoATimestamp,
  timestampAIso,
} from './data'

// `@types/node` non è installato: leggiamo l'ambiente dal globale in modo tipizzato.
const ambiente = (globalThis as unknown as { process: { env: Record<string, string | undefined> } }).process.env

/**
 * Esegue `fn` come se il browser dell'utente fosse nel fuso indicato.
 * Serve a dimostrare che la formattazione delle date non dipende dal fuso:
 * con un fuso a ovest di Greenwich la mezzanotte UTC cade nel giorno
 * precedente, quindi qualunque uso di getter locali di `Date` sbaglierebbe
 * il giorno di calendario e farebbe fallire il test.
 */
function conFuso<T>(fuso: string, fn: () => T): T {
  const precedente = ambiente.TZ
  ambiente.TZ = fuso
  try {
    return fn()
  } finally {
    ambiente.TZ = precedente
  }
}

describe('isoATimestamp', () => {
  it('restituisce la mezzanotte UTC del giorno indicato', () => {
    expect(isoATimestamp('1970-01-01')).toBe(0)
    expect(isoATimestamp('2026-06-01')).toBe(Date.UTC(2026, 5, 1))
  })

  it('non dipende dal fuso orario del browser', () => {
    const atteso = Date.UTC(2026, 5, 1)

    expect(conFuso('America/New_York', () => isoATimestamp('2026-06-01'))).toBe(atteso)
    expect(conFuso('Pacific/Kiritimati', () => isoATimestamp('2026-06-01'))).toBe(atteso)
  })

  it('mantiene ordinati e proporzionati gli intervalli fra le date', () => {
    const giorno = 24 * 60 * 60 * 1000

    expect(isoATimestamp('2026-06-02') - isoATimestamp('2026-06-01')).toBe(giorno)
    expect(isoATimestamp('2027-06-01') - isoATimestamp('2026-06-01')).toBe(365 * giorno)
  })
})

describe('timestampAIso', () => {
  it('converte un timestamp nel giorno UTC corrispondente', () => {
    expect(timestampAIso(Date.UTC(2026, 5, 1))).toBe('2026-06-01')
    expect(timestampAIso(Date.UTC(2026, 0, 31, 23, 59, 59))).toBe('2026-01-31')
  })

  it('usa i getter UTC anche con un fuso a ovest di Greenwich', () => {
    expect(conFuso('America/New_York', () => timestampAIso(Date.UTC(2026, 5, 1)))).toBe('2026-06-01')
    expect(conFuso('Pacific/Kiritimati', () => timestampAIso(Date.UTC(2026, 5, 1)))).toBe('2026-06-01')
  })
})

describe('formattaDataItaliana', () => {
  it('formatta una data ISO come GG/MM/AA', () => {
    expect(formattaDataItaliana('2026-06-01')).toBe('01/06/26')
    expect(formattaDataItaliana('2025-12-31')).toBe('31/12/25')
    expect(formattaDataItaliana('2026-01-01')).toBe('01/01/26')
    expect(formattaDataItaliana('2000-02-29')).toBe('29/02/00')
  })

  it('formatta un timestamp come GG/MM/AA usando il giorno UTC', () => {
    expect(formattaDataItaliana(isoATimestamp('2026-06-01'))).toBe('01/06/26')
    expect(formattaDataItaliana(isoATimestamp('2025-12-31'))).toBe('31/12/25')
    expect(formattaDataItaliana(Date.UTC(2026, 7, 15, 12, 0, 0))).toBe('15/08/26')
  })

  it('non sposta il giorno di calendario in nessun fuso orario (stringa ISO)', () => {
    for (const fuso of ['UTC', 'America/New_York', 'Pacific/Honolulu', 'Pacific/Kiritimati', 'Asia/Tokyo']) {
      expect(conFuso(fuso, () => formattaDataItaliana('2026-01-01'))).toBe('01/01/26')
      expect(conFuso(fuso, () => formattaDataItaliana('2025-12-31'))).toBe('31/12/25')
    }
  })

  it('non sposta il giorno di calendario in nessun fuso orario (timestamp)', () => {
    // Capodanno: la mezzanotte UTC è il 31/12 a New York e il 01/01 alle 14 a Kiritimati.
    // Un'implementazione con getter locali qui sbaglierebbe l'anno, non solo il giorno.
    const capodanno = isoATimestamp('2026-01-01')

    for (const fuso of ['UTC', 'America/New_York', 'Pacific/Honolulu', 'Pacific/Kiritimati', 'Asia/Tokyo']) {
      expect(conFuso(fuso, () => formattaDataItaliana(capodanno))).toBe('01/01/26')
    }
  })

  it('è coerente fra stringa ISO e timestamp della stessa data', () => {
    for (const iso of ['2024-02-29', '2026-06-01', '2026-08-01', '2026-12-31']) {
      expect(formattaDataItaliana(isoATimestamp(iso))).toBe(formattaDataItaliana(iso))
    }
  })
})

describe('formattaDataItalianaEstesa', () => {
  it('formatta una data ISO come GG/MM/AAAA', () => {
    expect(formattaDataItalianaEstesa('1990-05-20')).toBe('20/05/1990')
    expect(formattaDataItalianaEstesa('2026-01-01')).toBe('01/01/2026')
    expect(formattaDataItalianaEstesa('2000-02-29')).toBe('29/02/2000')
  })

  it('non sposta il giorno di calendario in nessun fuso orario (pura manipolazione di stringhe, nessun uso di Date)', () => {
    for (const fuso of ['UTC', 'America/New_York', 'Pacific/Honolulu', 'Pacific/Kiritimati', 'Asia/Tokyo']) {
      expect(conFuso(fuso, () => formattaDataItalianaEstesa('1990-05-20'))).toBe('20/05/1990')
      expect(conFuso(fuso, () => formattaDataItalianaEstesa('2026-01-01'))).toBe('01/01/2026')
    }
  })
})

describe('formattaDataItalianaConMese', () => {
  it('formatta una data ISO come GG mmm AAAA con il mese abbreviato in italiano', () => {
    expect(formattaDataItalianaConMese('2026-08-01')).toBe('01 ago 2026')
    expect(formattaDataItalianaConMese('2026-01-01')).toBe('01 gen 2026')
    expect(formattaDataItalianaConMese('2026-12-25')).toBe('25 dic 2026')
    expect(formattaDataItalianaConMese('1990-05-20')).toBe('20 mag 1990')
  })

  it('non sposta il giorno di calendario in nessun fuso orario (pura manipolazione di stringhe, nessun uso di Date)', () => {
    for (const fuso of ['UTC', 'America/New_York', 'Pacific/Honolulu', 'Pacific/Kiritimati', 'Asia/Tokyo']) {
      expect(conFuso(fuso, () => formattaDataItalianaConMese('2026-08-01'))).toBe('01 ago 2026')
      expect(conFuso(fuso, () => formattaDataItalianaConMese('2026-01-01'))).toBe('01 gen 2026')
    }
  })
})

describe('calcolaEta', () => {
  afterEach(() => {
    vi.useRealTimers()
  })

  it('calcola gli anni compiuti quando il compleanno di quest\'anno è già passato', () => {
    vi.setSystemTime(new Date('2026-09-01'))
    expect(calcolaEta('2000-01-15')).toBe(26)
  })

  it('non conta l\'anno in corso se il compleanno non è ancora arrivato', () => {
    vi.setSystemTime(new Date('2026-09-01'))
    expect(calcolaEta('2000-12-25')).toBe(25)
  })

  it('conta correttamente se il compleanno è esattamente oggi', () => {
    vi.setSystemTime(new Date('2026-09-01'))
    expect(calcolaEta('2000-09-01')).toBe(26)
  })

  it('restituisce null se la data di nascita è vuota', () => {
    expect(calcolaEta('')).toBeNull()
  })

  it('restituisce null se la data di nascita è nel futuro', () => {
    vi.setSystemTime(new Date('2026-09-01'))
    expect(calcolaEta('2030-01-01')).toBeNull()
  })
})
